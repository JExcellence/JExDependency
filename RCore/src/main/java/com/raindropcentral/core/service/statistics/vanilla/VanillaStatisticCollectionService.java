/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.statistics.vanilla;

import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import com.raindropcentral.core.service.statistics.vanilla.aggregation.StatisticAggregationEngine;
import com.raindropcentral.core.service.statistics.vanilla.batch.BatchCollectionProcessor;
import com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.core.service.statistics.vanilla.event.EventDrivenCollectionHandler;
import com.raindropcentral.core.service.statistics.vanilla.monitoring.CollectionMetrics;
import com.raindropcentral.core.service.statistics.vanilla.monitoring.PerformanceLogger;
import com.raindropcentral.core.service.statistics.vanilla.privacy.PlayerPrivacyManager;
import com.raindropcentral.core.service.statistics.vanilla.scheduler.CollectionScheduler;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Main orchestrator service for vanilla Minecraft statistic collection.
 * Coordinates all components of the vanilla statistics system including collectors,
 * cache management, aggregation, scheduling, and integration with the existing
 * statistics delivery infrastructure.
 *
 * <p>This service manages the complete lifecycle of vanilla statistic collection:
 * <ul>
 *   <li>Initialization and configuration of all subsystems</li>
 *   <li>Scheduled periodic collection from online players</li>
 *   <li>Event-driven collection for critical moments (disconnect, death, etc.)</li>
 *   <li>Delta computation and caching for efficient transmission</li>
 *   <li>Integration with queue manager and delivery engine</li>
 *   <li>Backpressure-aware collection throttling</li>
 *   <li>Graceful shutdown with data persistence</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class VanillaStatisticCollectionService {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final Plugin plugin;
    private final VanillaStatisticConfig config;
    private final StatisticsQueueManager queueManager;
    private final PlayerPrivacyManager privacyManager;
    
    // Core components
    private final VanillaStatisticCollector collector;
    private final StatisticCacheManager cacheManager;
    private final StatisticAggregationEngine aggregationEngine;
    private final BatchCollectionProcessor batchProcessor;
    private final CollectionScheduler scheduler;
    private final EventDrivenCollectionHandler eventHandler;
    
    // Monitoring
    private final CollectionMetrics metrics;
    private final PerformanceLogger performanceLogger;
    
    private volatile boolean initialized = false;

    /**
     * Creates a new vanilla statistic collection service.
     *
     * @param plugin the plugin instance
     * @param config the vanilla statistics configuration
     * @param queueManager the statistics queue manager for enqueueing collected statistics
     * @param privacyManager the privacy manager for checking player opt-out status
     * @param collector the vanilla statistic collector
     * @param cacheManager the cache manager for delta computation
     * @param aggregationEngine the aggregation engine for computing summary statistics
     * @param batchProcessor the batch processor for efficient multi-player collection
     * @param scheduler the collection scheduler for periodic tasks
     * @param eventHandler the event handler for event-driven collection
     */
    public VanillaStatisticCollectionService(
        final @NotNull Plugin plugin,
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticsQueueManager queueManager,
        final @NotNull PlayerPrivacyManager privacyManager,
        final @NotNull VanillaStatisticCollector collector,
        final @NotNull StatisticCacheManager cacheManager,
        final @NotNull StatisticAggregationEngine aggregationEngine,
        final @NotNull BatchCollectionProcessor batchProcessor,
        final @NotNull CollectionScheduler scheduler,
        final @NotNull EventDrivenCollectionHandler eventHandler
    ) {
        this.plugin = plugin;
        this.config = config;
        this.queueManager = queueManager;
        this.privacyManager = privacyManager;
        this.collector = collector;
        this.cacheManager = cacheManager;
        this.aggregationEngine = aggregationEngine;
        this.batchProcessor = batchProcessor;
        this.scheduler = scheduler;
        this.eventHandler = eventHandler;
        this.metrics = new CollectionMetrics();
        this.performanceLogger = new PerformanceLogger(LOGGER);
    }

    /**
     * Initializes the vanilla statistic collection service.
     * Loads cached data, registers event listeners, and starts scheduled collection.
     *
     * @return a future that completes when initialization is finished
     */
    public CompletableFuture<Void> initialize() {
        if (initialized) {
            LOGGER.warning("Vanilla statistic collection service already initialized");
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Initializing vanilla statistic collection service...");

        return CompletableFuture.runAsync(() -> {
            // Load cache from disk
            cacheManager.loadCache();
            LOGGER.info("Loaded statistic cache with " + cacheManager.getCacheSize() + " players");

            // Register event listeners
            Bukkit.getPluginManager().registerEvents(eventHandler, plugin);
            LOGGER.info("Registered event-driven collection handlers");

            // Set collection callback before starting scheduler
            scheduler.setCollectionCallback(() -> {
                collectAll().exceptionally(error -> {
                    LOGGER.warning("Scheduled collection failed: " + error.getMessage());
                    return CollectionResult.empty();
                });
            });

            // Start scheduled collection
            scheduler.start();
            LOGGER.info("Started collection scheduler with frequency: " + 
                config.getCollectionFrequency() + "s");

            initialized = true;
            LOGGER.info("Vanilla statistic collection service initialized successfully");
        });
    }

    /**
     * Shuts down the vanilla statistic collection service.
     * Stops scheduled tasks, persists cache, and performs final collection for online players.
     *
     * @return a future that completes when shutdown is finished
     */
    public CompletableFuture<Void> shutdown() {
        if (!initialized) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Shutting down vanilla statistic collection service...");

        return CompletableFuture.runAsync(() -> {
            // Stop scheduler
            scheduler.stop();
            LOGGER.info("Stopped collection scheduler");

            // Perform final collection for all online players
            try {
                CollectionResult result = collectAll().join();
                LOGGER.info("Final collection completed: " + result.statisticsCollected() + 
                    " statistics from " + result.playerCount() + " players");
            } catch (Exception e) {
                LOGGER.warning("Error during final collection: " + e.getMessage());
            }

            // Persist cache
            cacheManager.persistCache();
            LOGGER.info("Persisted statistic cache");

            initialized = false;
            LOGGER.info("Vanilla statistic collection service shut down successfully");
        });
    }

    /**
     * Collects statistics from all online players.
     * Uses batch processing for efficiency and respects backpressure.
     *
     * @return a future containing the collection result
     */
    public CompletableFuture<CollectionResult> collectAll() {
        if (!initialized) {
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        // Check backpressure
        if (queueManager.isBackpressureActive()) {
            LOGGER.fine("Backpressure active, skipping collection");
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        long startTime = System.currentTimeMillis();

        return batchProcessor.collectAllPlayers()
            .thenApply(result -> {
                // Compute aggregates (returns empty list for null player - global aggregates not yet implemented)
                // aggregationEngine.computeAggregates(result.statistics(), null);

                // Queue statistics with NORMAL priority
                int queued = queueManager.enqueueBatch(result.statistics());
                
                long duration = System.currentTimeMillis() - startTime;
                
                // Record metrics
                metrics.recordCollection(queued, duration);
                metrics.updateCacheSize(cacheManager.getCacheSize());
                
                // Log performance
                performanceLogger.logBatchCollectionPerformance(
                    (int) Bukkit.getOnlinePlayers().stream().count(),
                    duration,
                    queued
                );

                return result;
            })
            .exceptionally(error -> {
                LOGGER.severe("Error during collection: " + error.getMessage());
                return CollectionResult.empty();
            });
    }

    /**
     * Collects statistics for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future containing the collection result
     */
    public CompletableFuture<CollectionResult> collectForPlayer(final @NotNull UUID playerId) {
        if (!initialized) {
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        return collectForPlayer(player, DeliveryPriority.NORMAL);
    }

    /**
     * Collects statistics for a specific player with a given priority.
     *
     * @param player the player
     * @param priority the delivery priority
     * @return a future containing the collection result
     */
    public CompletableFuture<CollectionResult> collectForPlayer(
        final @NotNull Player player,
        final @NotNull DeliveryPriority priority
    ) {
        // Check if player has opted out
        if (privacyManager.hasOptedOut(player.getUniqueId())) {
            LOGGER.fine("Player " + player.getName() + " has opted out of statistics collection");
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            // Collect all statistics for player
            List<QueuedStatistic> statistics = collector.collectAllForPlayer(player);

            // Compute aggregates for this player and add them to the statistics list
            List<QueuedStatistic> aggregates = aggregationEngine.computeAggregates(statistics, player);
            List<QueuedStatistic> allStatistics = new java.util.ArrayList<>(statistics);
            allStatistics.addAll(aggregates);

            // Queue with specified priority
            int queued = queueManager.enqueueBatch(allStatistics);

            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            metrics.recordCollection(queued, duration);
            
            // Log performance
            performanceLogger.logCollectionPerformance(
                player.getUniqueId(),
                duration,
                queued
            );

            return new CollectionResult(
                statistics,
                System.currentTimeMillis(),
                duration,
                1
            );
        }).exceptionally(error -> {
            LOGGER.warning("Error collecting statistics for player " + 
                player.getName() + ": " + error.getMessage());
            return CollectionResult.empty();
        });
    }

    /**
     * Collects only changed statistics (deltas) from all online players.
     * More efficient than full collection when most statistics haven't changed.
     *
     * @return a future containing the collection result
     */
    public CompletableFuture<CollectionResult> collectDelta() {
        if (!initialized) {
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        // Check backpressure
        if (queueManager.isBackpressureActive()) {
            LOGGER.fine("Backpressure active, skipping delta collection");
            return CompletableFuture.completedFuture(CollectionResult.empty());
        }

        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            List<QueuedStatistic> allStatistics = new java.util.ArrayList<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    // Collect current values
                    List<QueuedStatistic> current = collector.collectAllForPlayer(player);
                    
                    // Compute deltas using cache
                    Map<String, Integer> currentValues = new java.util.HashMap<>();
                    for (QueuedStatistic stat : current) {
                        if (stat.value() instanceof Number num) {
                            currentValues.put(stat.statisticKey(), num.intValue());
                        }
                    }
                    
                    Map<String, Integer> deltas = cacheManager.getDelta(
                        player.getUniqueId(), 
                        currentValues
                    );
                    
                    // Only include statistics with non-zero deltas
                    List<QueuedStatistic> deltaStats = current.stream()
                        .filter(stat -> {
                            Integer delta = deltas.get(stat.statisticKey());
                            return delta != null && delta != 0;
                        })
                        .toList();
                    
                    allStatistics.addAll(deltaStats);
                    
                    // Update cache
                    cacheManager.updateCache(player.getUniqueId(), currentValues);
                    
                } catch (Exception e) {
                    LOGGER.warning("Error collecting deltas for player " + 
                        player.getName() + ": " + e.getMessage());
                }
            }

            // Queue delta statistics
            int queued = queueManager.enqueueBatch(allStatistics);

            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            metrics.recordCollection(queued, duration);
            metrics.updateCacheSize(cacheManager.getCacheSize());

            return new CollectionResult(
                allStatistics,
                System.currentTimeMillis(),
                duration,
                Bukkit.getOnlinePlayers().size()
            );
        }).exceptionally(error -> {
            LOGGER.severe("Error during delta collection: " + error.getMessage());
            return CollectionResult.empty();
        });
    }

    /**
     * Gets collection statistics for monitoring.
     *
     * @return the collection statistics
     */
    public CollectionStatistics getStatistics() {
        return metrics.getStatistics();
    }

    /**
     * Gets the collection metrics tracker.
     *
     * @return the metrics tracker
     */
    public CollectionMetrics getMetrics() {
        return metrics;
    }

    /**
     * Gets the performance logger.
     *
     * @return the performance logger
     */
    public PerformanceLogger getPerformanceLogger() {
        return performanceLogger;
    }

    /**
     * Checks if the service is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the configuration.
     *
     * @return the vanilla statistic configuration
     */
    public VanillaStatisticConfig getConfig() {
        return config;
    }

    /**
     * Gets the cache manager.
     *
     * @return the cache manager
     */
    public StatisticCacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Gets the queue manager.
     *
     * @return the queue manager
     */
    public StatisticsQueueManager getQueueManager() {
        return queueManager;
    }

    /**
     * Gets the privacy manager.
     *
     * @return the privacy manager
     */
    public PlayerPrivacyManager getPrivacyManager() {
        return privacyManager;
    }

    /**
     * Clears the cache for a specific player.
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayerCache(final @NotNull UUID playerId) {
        cacheManager.clearPlayer(playerId);
        LOGGER.info("Cleared cache for player: " + playerId);
    }

    /**
     * Clears all cached statistics.
     */
    public void clearAllCache() {
        int size = cacheManager.getCacheSize();
        cacheManager.clearAll();
        LOGGER.info("Cleared all cache (" + size + " players)");
    }
}
