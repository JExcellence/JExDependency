package com.raindropcentral.core.service.statistics.offline;

import com.raindropcentral.core.service.central.RCentralApiClient;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages connectivity detection and recovery for offline scenarios.
 * Detects backend unreachability and handles recovery when connection is restored.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ConnectivityManager {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    /** Number of consecutive failures before switching to offline mode */
    private static final int FAILURE_THRESHOLD = 3;

    /** Probe interval in seconds during offline mode */
    private static final int PROBE_INTERVAL_SECONDS = 30;

    private final RCentralApiClient apiClient;
    private final String apiKey;
    private final StatisticsQueueManager queueManager;

    private final AtomicBoolean offlineMode = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private ScheduledFuture<?> probeTask;
    private Consumer<Boolean> connectivityListener;

    public ConnectivityManager(
        final @NotNull RCentralApiClient apiClient,
        final @NotNull String apiKey,
        final @NotNull StatisticsQueueManager queueManager
    ) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.queueManager = queueManager;
    }

    /**
     * Records a successful delivery, resetting failure count.
     */
    public void recordSuccess() {
        int previousFailures = consecutiveFailures.getAndSet(0);

        if (offlineMode.getAndSet(false)) {
            LOGGER.info("Connectivity restored - exiting offline mode");
            stopProbing();
            notifyListener(true);
        }
    }

    /**
     * Records a failed delivery attempt.
     *
     * @param executor the executor for scheduling probe tasks
     */
    public void recordFailure(final @NotNull ScheduledExecutorService executor) {
        int failures = consecutiveFailures.incrementAndGet();

        if (failures >= FAILURE_THRESHOLD && !offlineMode.get()) {
            LOGGER.warning("Backend unreachable after " + failures +
                " consecutive failures - switching to offline mode");
            offlineMode.set(true);
            queueManager.setOfflineMode(true);
            startProbing(executor);
            notifyListener(false);
        }
    }

    /**
     * Checks if currently in offline mode.
     */
    public boolean isOffline() {
        return offlineMode.get();
    }

    /**
     * Gets the consecutive failure count.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * Sets a listener for connectivity changes.
     *
     * @param listener callback receiving true when online, false when offline
     */
    public void setConnectivityListener(final Consumer<Boolean> listener) {
        this.connectivityListener = listener;
    }

    /**
     * Starts connectivity probing during offline mode.
     */
    private void startProbing(final ScheduledExecutorService executor) {
        if (probeTask != null && !probeTask.isDone()) {
            return;
        }

        LOGGER.info("Starting connectivity probe every " + PROBE_INTERVAL_SECONDS + " seconds");

        probeTask = executor.scheduleAtFixedRate(
            this::probeConnectivity,
            PROBE_INTERVAL_SECONDS,
            PROBE_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * Stops connectivity probing.
     */
    private void stopProbing() {
        if (probeTask != null) {
            probeTask.cancel(false);
            probeTask = null;
            LOGGER.info("Stopped connectivity probing");
        }
    }

    /**
     * Probes backend connectivity.
     */
    private void probeConnectivity() {
        LOGGER.fine("Probing backend connectivity...");

        try {
            // Use heartbeat endpoint as connectivity probe
            apiClient.sendHeartbeat(apiKey, 0, 0, 20.0, null)
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Connectivity probe successful");
                        recordSuccess();
                        initiateRecovery();
                    } else {
                        LOGGER.fine("Connectivity probe failed: " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    LOGGER.fine("Connectivity probe error: " + e.getMessage());
                    return null;
                });

        } catch (Exception e) {
            LOGGER.fine("Connectivity probe exception: " + e.getMessage());
        }
    }

    /**
     * Initiates recovery after connectivity is restored.
     * Transmits queued statistics in chronological order with BULK priority.
     */
    private void initiateRecovery() {
        LOGGER.info("Initiating recovery - transmitting queued statistics");

        queueManager.setOfflineMode(false);

        // Re-prioritize all queued statistics to BULK for recovery
        // They will be processed in chronological order
        int recoveredCount = 0;

        // Process each priority level, moving to BULK
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            if (priority == DeliveryPriority.BULK) continue;

            List<QueuedStatistic> stats = queueManager.dequeue(priority, Integer.MAX_VALUE);
            for (QueuedStatistic stat : stats) {
                QueuedStatistic bulkStat = QueuedStatistic.builder()
                    .playerUuid(stat.playerUuid())
                    .statisticKey(stat.statisticKey())
                    .value(stat.value())
                    .dataType(stat.dataType())
                    .collectionTimestamp(stat.collectionTimestamp())
                    .priority(DeliveryPriority.BULK)
                    .isDelta(stat.isDelta())
                    .sourcePlugin(stat.sourcePlugin())
                    .build();
                queueManager.enqueue(bulkStat);
                recoveredCount++;
            }
        }

        LOGGER.info("Recovery initiated - " + recoveredCount + " statistics queued for BULK delivery");
    }

    /**
     * Notifies the connectivity listener of state changes.
     */
    private void notifyListener(final boolean online) {
        if (connectivityListener != null) {
            try {
                connectivityListener.accept(online);
            } catch (Exception e) {
                LOGGER.warning("Error notifying connectivity listener: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the connectivity manager.
     */
    public void shutdown() {
        stopProbing();
    }
}
