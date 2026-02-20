package com.raindropcentral.core.service.statistics;

import com.raindropcentral.core.service.central.RCentralApiClient;
import com.raindropcentral.core.service.statistics.aggregation.StatisticsAggregator;
import com.raindropcentral.core.service.statistics.aggregation.TimeWindowedAccumulator;
import com.raindropcentral.core.service.statistics.collector.EventDrivenCollector;
import com.raindropcentral.core.service.statistics.collector.NativeStatisticCollector;
import com.raindropcentral.core.service.statistics.collector.PlayerStatisticCollector;
import com.raindropcentral.core.service.statistics.collector.ServerMetricsCollector;
import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.delivery.*;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import com.raindropcentral.core.service.statistics.security.PayloadSigner;
import com.raindropcentral.core.service.statistics.sync.ConflictResolver;
import com.raindropcentral.core.service.statistics.sync.CrossServerSyncManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Main orchestrator service for statistics delivery.
 * Coordinates all components: collectors, queue, delivery engine, and sync.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsDeliveryService {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final Plugin plugin;
    private final StatisticsDeliveryConfig config;
    private final String apiKey;
    private final String serverUuid;

    // Core components
    private final StatisticsQueueManager queueManager;
    private final StatisticsDeliveryEngine deliveryEngine;
    private final StatisticsAggregator aggregator;
    private final TimeWindowedAccumulator accumulator;

    // Collectors
    private final PlayerStatisticCollector playerCollector;
    private final NativeStatisticCollector nativeCollector;
    private final ServerMetricsCollector serverMetricsCollector;
    private final EventDrivenCollector eventCollector;

    // Sync
    private final CrossServerSyncManager syncManager;

    // Security
    private final PayloadSigner payloadSigner;

    // API Client
    private final RCentralApiClient apiClient;

    // Executor and scheduled tasks
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> deliveryTask;
    private ScheduledFuture<?> nativeCollectionTask;
    private ScheduledFuture<?> criticalProcessorTask;
    private ScheduledFuture<?> highProcessorTask;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public StatisticsDeliveryService(
        final @NotNull Plugin plugin,
        final @NotNull StatisticsDeliveryConfig config,
        final @NotNull String apiKey,
        final @NotNull String serverUuid,
        final @NotNull RCentralApiClient apiClient,
        final @NotNull StatisticsQueueManager queueManager,
        final @NotNull PlayerStatisticCollector playerCollector,
        final @NotNull NativeStatisticCollector nativeCollector,
        final @NotNull ServerMetricsCollector serverMetricsCollector
    ) {
        this.plugin = plugin;
        this.config = config;
        this.apiKey = apiKey;
        this.serverUuid = serverUuid;
        this.apiClient = apiClient;
        this.queueManager = queueManager;
        this.playerCollector = playerCollector;
        this.nativeCollector = nativeCollector;
        this.serverMetricsCollector = serverMetricsCollector;

        // Initialize components
        this.aggregator = new StatisticsAggregator();
        this.accumulator = new TimeWindowedAccumulator();
        this.payloadSigner = new PayloadSigner();

        // Initialize delivery engine
        RateLimiter rateLimiter = new RateLimiter(config);
        RetryHandler retryHandler = new RetryHandler(config);
        BatchProcessor batchProcessor = new BatchProcessor(serverUuid, config);
        PayloadCompressor payloadCompressor = new PayloadCompressor(config);

        this.deliveryEngine = new StatisticsDeliveryEngine(
            rateLimiter, retryHandler, batchProcessor, payloadCompressor, config
        );

        // Initialize sync manager
        ConflictResolver conflictResolver = new ConflictResolver(config.getDefaultConflictStrategy());
        this.syncManager = new CrossServerSyncManager(apiClient, apiKey, conflictResolver, config);

        // Initialize event collector
        this.eventCollector = new EventDrivenCollector(
            plugin, queueManager, playerCollector, nativeCollector, config, syncManager
        );

        // Create executor
        this.executor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "StatisticsDelivery");
            t.setDaemon(true);
            return t;
        });

        // Set up delivery callback
        setupDeliveryCallback();
    }


    private void setupDeliveryCallback() {
        deliveryEngine.setDeliveryCallback((batch, compressionResult) -> {
            // Sign the payload
            BatchPayload signedBatch = payloadSigner.sign(batch, apiKey);

            if (compressionResult.compressed()) {
                return apiClient.deliverStatisticsCompressed(
                    apiKey, compressionResult.data(), signedBatch.batchId()
                );
            } else {
                return apiClient.deliverStatistics(apiKey, signedBatch);
            }
        });
    }

    /**
     * Initializes the service, starting scheduled tasks and registering listeners.
     */
    public void initialize() {
        if (!config.isEnabled()) {
            LOGGER.info("Statistics delivery is disabled");
            return;
        }

        if (running.getAndSet(true)) {
            LOGGER.warning("Statistics delivery service already running");
            return;
        }

        LOGGER.info("Initializing statistics delivery service...");

        // Initialize queue manager
        queueManager.initialize();
        queueManager.startPersistence(executor, config.getPersistenceIntervalSeconds());

        // Set executor for event collector
        eventCollector.setExecutor(executor);
        eventCollector.register();

        // Start scheduled tasks
        startScheduledTasks();

        LOGGER.info("Statistics delivery service initialized");
    }

    /**
     * Shuts down the service, flushing queues and persisting state.
     */
    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }

        LOGGER.info("Shutting down statistics delivery service...");

        // Stop scheduled tasks
        stopScheduledTasks();

        // Flush remaining statistics
        flushQueue();

        // Shutdown event collector
        eventCollector.shutdown();

        // Shutdown queue manager
        queueManager.shutdown();

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Statistics delivery service shut down");
    }

    /**
     * Flushes all queued statistics for immediate delivery.
     */
    public void flushQueue() {
        if (paused.get()) {
            LOGGER.warning("Cannot flush queue while paused");
            return;
        }

        LOGGER.info("Flushing statistics queue...");

        // Process all priorities
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            processQueue(priority, Integer.MAX_VALUE);
        }
    }

    /**
     * Delivers statistics for a specific player immediately.
     *
     * @param playerUuid the player UUID
     * @return a future containing the delivery result
     */
    public CompletableFuture<DeliveryResult> deliverPlayerStatistics(final @NotNull UUID playerUuid) {
        List<QueuedStatistic> stats = queueManager.dequeueByPlayer(playerUuid, 1000);
        if (stats.isEmpty()) {
            return CompletableFuture.completedFuture(DeliveryResult.empty());
        }

        return deliveryEngine.deliverWithPriority(stats, DeliveryPriority.HIGH);
    }

    /**
     * Pauses statistics delivery.
     */
    public void pauseDelivery() {
        paused.set(true);
        LOGGER.info("Statistics delivery paused");
    }

    /**
     * Resumes statistics delivery.
     */
    public void resumeDelivery() {
        paused.set(false);
        LOGGER.info("Statistics delivery resumed");
    }

    /**
     * Checks if delivery is paused.
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * Checks if the service is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    // ==================== Scheduled Tasks ====================

    private void startScheduledTasks() {
        // Normal priority delivery at configured interval
        deliveryTask = executor.scheduleAtFixedRate(
            this::processNormalDelivery,
            config.getDeliveryIntervalSeconds(),
            config.getDeliveryIntervalSeconds(),
            TimeUnit.SECONDS
        );

        // Native statistic collection
        nativeCollectionTask = executor.scheduleAtFixedRate(
            this::collectNativeStatistics,
            config.getNativeStatCollectionIntervalSeconds(),
            config.getNativeStatCollectionIntervalSeconds(),
            TimeUnit.SECONDS
        );

        // Critical priority processor (2-second max delay)
        criticalProcessorTask = executor.scheduleAtFixedRate(
            () -> processQueue(DeliveryPriority.CRITICAL, 100),
            2, 2, TimeUnit.SECONDS
        );

        // High priority processor (10-second max delay)
        highProcessorTask = executor.scheduleAtFixedRate(
            () -> processQueue(DeliveryPriority.HIGH, 500),
            10, 10, TimeUnit.SECONDS
        );

        LOGGER.info("Scheduled tasks started");
    }

    private void stopScheduledTasks() {
        if (deliveryTask != null) deliveryTask.cancel(false);
        if (nativeCollectionTask != null) nativeCollectionTask.cancel(false);
        if (criticalProcessorTask != null) criticalProcessorTask.cancel(false);
        if (highProcessorTask != null) highProcessorTask.cancel(false);
    }

    private void processNormalDelivery() {
        if (paused.get() || !running.get()) return;

        try {
            // Collect delta statistics for all online players
            List<QueuedStatistic> stats = playerCollector.collectAllOnlinePlayers();
            if (!stats.isEmpty()) {
                queueManager.enqueueBatch(stats);
                aggregator.updateCache(stats);
            }

            // Process normal priority queue
            processQueue(DeliveryPriority.NORMAL, config.getMaxBatchSizeNormal());

            // Process low priority during low activity
            if (Bukkit.getOnlinePlayers().size() < 10) {
                processQueue(DeliveryPriority.LOW, 500);
                processQueue(DeliveryPriority.BULK, 1000);
            }

        } catch (Exception e) {
            LOGGER.warning("Error in normal delivery: " + e.getMessage());
        }
    }

    private void collectNativeStatistics() {
        if (paused.get() || !running.get()) return;

        try {
            List<QueuedStatistic> stats = nativeCollector.collectAllOnlinePlayers();
            if (!stats.isEmpty()) {
                queueManager.enqueueBatch(stats);
                aggregator.updateCache(stats);
            }
        } catch (Exception e) {
            LOGGER.warning("Error collecting native statistics: " + e.getMessage());
        }
    }

    private void processQueue(final DeliveryPriority priority, final int maxCount) {
        if (paused.get() || !running.get()) return;

        List<QueuedStatistic> stats = queueManager.dequeue(priority, maxCount);
        if (stats.isEmpty()) return;

        deliveryEngine.deliverWithPriority(stats, priority)
            .exceptionally(e -> {
                LOGGER.warning("Delivery failed for " + priority + ": " + e.getMessage());
                // Re-queue failed statistics
                queueManager.enqueueBatch(stats);
                return null;
            });
    }

    // ==================== Getters ====================

    public StatisticsQueueManager getQueueManager() { return queueManager; }
    public StatisticsDeliveryEngine getDeliveryEngine() { return deliveryEngine; }
    public StatisticsAggregator getAggregator() { return aggregator; }
    public CrossServerSyncManager getSyncManager() { return syncManager; }
    public EventDrivenCollector getEventCollector() { return eventCollector; }
    public StatisticsDeliveryConfig getConfig() { return config; }
}
