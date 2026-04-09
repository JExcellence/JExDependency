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

package com.raindropcentral.core.service.statistics.vanilla.batch;

import com.google.common.collect.Lists;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.vanilla.CollectionResult;
import com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Processes batch collection of vanilla statistics for multiple players.
 *
 * <p>This processor handles:
 * <ul>
 *   <li>Filtering out AFK players based on configured threshold</li>
 *   <li>Splitting players into batches for parallel processing</li>
 *   <li>Priority-based collection (players online longest since last collection)</li>
 *   <li>Parallel batch processing using thread pool</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Batch size: Configurable (default 100 players)</li>
 *   <li>Parallel threads: Configurable (default 4 threads)</li>
 *   <li>Target: &lt; 50ms per player, &lt; 5s for 100 players</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * BatchCollectionProcessor processor = new BatchCollectionProcessor(
 *     config, collector, cacheManager
 * );
 * 
 * // Collect from all online players
 * CompletableFuture<CollectionResult> future = processor.collectAllPlayers();
 * future.thenAccept(result -> {
 *     logger.info("Collected " + result.statisticCount() + " statistics");
 * });
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class BatchCollectionProcessor {

    private static final Logger LOGGER = Logger.getLogger(BatchCollectionProcessor.class.getName());

    private final VanillaStatisticConfig config;
    private final VanillaStatisticCollector collector;
    private final StatisticCacheManager cacheManager;
    private final ExecutorService executor;

    /**
     * Tracks last collection time per player for priority-based collection.
     */
    private final Map<UUID, Instant> lastCollectionTimes;

    /**
     * Tracks last activity time per player for AFK detection.
     */
    private final Map<UUID, Instant> lastActivityTimes;

    /**
     * Creates a new batch collection processor.
     *
     * @param config       the vanilla statistic configuration
     * @param collector    the vanilla statistic collector
     * @param cacheManager the cache manager for delta computation
     */
    public BatchCollectionProcessor(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull VanillaStatisticCollector collector,
        final @NotNull StatisticCacheManager cacheManager
    ) {
        this.config = config;
        this.collector = collector;
        this.cacheManager = cacheManager;
        this.executor = Executors.newFixedThreadPool(
            config.getParallelThreads(),
            r -> {
                Thread thread = new Thread(r, "VanillaStats-BatchProcessor");
                thread.setDaemon(true);
                return thread;
            }
        );
        this.lastCollectionTimes = new ConcurrentHashMap<>();
        this.lastActivityTimes = new ConcurrentHashMap<>();
    }

    /**
     * Collects statistics from all online players.
     *
     * <p>This method:
     * <ol>
     *   <li>Filters out AFK players</li>
     *   <li>Sorts players by priority (longest since last collection)</li>
     *   <li>Splits into batches</li>
     *   <li>Processes batches in parallel</li>
     *   <li>Aggregates results</li>
     * </ol>
     *
     * @return future containing the collection result
     */
    public @NotNull CompletableFuture<CollectionResult> collectAllPlayers() {
        long startTime = System.currentTimeMillis();
        
        // Get online players
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPlayers = onlinePlayers.size();

        // Filter out AFK players
        int afkThreshold = config.getAfkThresholdSeconds();
        onlinePlayers.removeIf(player -> isAFK(player, afkThreshold));
        int activePlayers = onlinePlayers.size();

        if (activePlayers < totalPlayers) {
            LOGGER.fine("Filtered out " + (totalPlayers - activePlayers) + 
                       " AFK players (threshold: " + afkThreshold + "s)");
        }

        if (onlinePlayers.isEmpty()) {
            LOGGER.fine("No active players to collect statistics from");
            return CompletableFuture.completedFuture(
                new CollectionResult(List.of(), startTime, 0, 0)
            );
        }

        // Sort by priority (longest since last collection first)
        onlinePlayers.sort((p1, p2) -> {
            Instant t1 = lastCollectionTimes.getOrDefault(p1.getUniqueId(), Instant.MIN);
            Instant t2 = lastCollectionTimes.getOrDefault(p2.getUniqueId(), Instant.MIN);
            return t1.compareTo(t2);
        });

        // Split into batches
        int batchSize = config.getBatchSize();
        List<List<Player>> batches = Lists.partition(onlinePlayers, batchSize);
        
        LOGGER.fine("Processing " + activePlayers + " players in " + 
                   batches.size() + " batches (size: " + batchSize + ")");

        // Process batches in parallel
        List<CompletableFuture<List<QueuedStatistic>>> futures = batches.stream()
            .map(batch -> CompletableFuture.supplyAsync(
                () -> collectBatch(batch),
                executor
            ))
            .toList();

        // Aggregate results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<QueuedStatistic> allStatistics = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

                long duration = System.currentTimeMillis() - startTime;
                
                LOGGER.fine("Batch collection completed: " + allStatistics.size() + 
                           " statistics from " + activePlayers + " players in " + 
                           duration + "ms");

                return new CollectionResult(allStatistics, startTime, duration, activePlayers);
            });
    }

    /**
     * Collects statistics from a batch of players.
     *
     * @param batch the batch of players to collect from
     * @return list of collected statistics
     */
    private @NotNull List<QueuedStatistic> collectBatch(final @NotNull List<Player> batch) {
        List<QueuedStatistic> batchStatistics = new ArrayList<>();
        Instant collectionTime = Instant.now();

        for (Player player : batch) {
            try {
                // Collect all statistics for player
                List<QueuedStatistic> playerStats = collector.collectAllForPlayer(player);

                // Compute deltas
                Map<String, Integer> currentValues = new HashMap<>();
                for (QueuedStatistic stat : playerStats) {
                    if (stat.value() instanceof Number) {
                        currentValues.put(stat.statisticKey(), ((Number) stat.value()).intValue());
                    }
                }

                Map<String, Integer> deltas = cacheManager.getDelta(
                    player.getUniqueId(),
                    currentValues
                );

                // Only queue statistics with meaningful deltas
                for (Map.Entry<String, Integer> entry : deltas.entrySet()) {
                    String key = entry.getKey();
                    int deltaValue = entry.getValue();

                    // Find original statistic to get metadata
                    QueuedStatistic original = playerStats.stream()
                        .filter(s -> s.statisticKey().equals(key))
                        .findFirst()
                        .orElse(null);

                    if (original != null) {
                        // Create new statistic with delta value
                        QueuedStatistic deltaStat = new QueuedStatistic(
                            original.playerUuid(),
                            original.statisticKey(),
                            deltaValue,
                            original.dataType(),
                            original.collectionTimestamp(),
                            original.priority(),
                            true, // This is a delta
                            original.sourcePlugin()
                        );
                        batchStatistics.add(deltaStat);
                    }
                }

                // Update cache with current values
                cacheManager.updateCache(player.getUniqueId(), currentValues);

                // Update last collection time
                lastCollectionTimes.put(player.getUniqueId(), collectionTime);

            } catch (Exception e) {
                LOGGER.warning("Failed to collect statistics for player " + 
                              player.getName() + ": " + e.getMessage());
            }
        }

        return batchStatistics;
    }

    /**
     * Checks if a player is AFK based on last activity time.
     *
     * <p>A player is considered AFK if they haven't moved or interacted
     * for longer than the specified threshold.
     *
     * @param player    the player to check
     * @param threshold the AFK threshold in seconds
     * @return true if the player is AFK
     */
    public boolean isAFK(final @NotNull Player player, int threshold) {
        if (threshold <= 0) {
            return false; // AFK detection disabled
        }

        Instant lastActivity = lastActivityTimes.get(player.getUniqueId());
        
        if (lastActivity == null) {
            // First time seeing this player, assume active
            updateActivity(player);
            return false;
        }

        long secondsSinceActivity = Instant.now().getEpochSecond() - 
                                   lastActivity.getEpochSecond();
        
        return secondsSinceActivity >= threshold;
    }

    /**
     * Updates the last activity time for a player.
     *
     * <p>This should be called when the player moves, interacts, or performs
     * any action that indicates they are not AFK.
     *
     * @param player the player whose activity to update
     */
    public void updateActivity(final @NotNull Player player) {
        lastActivityTimes.put(player.getUniqueId(), Instant.now());
    }

    /**
     * Clears tracking data for a player.
     *
     * <p>This should be called when a player disconnects to free memory.
     *
     * @param playerId the player UUID
     */
    public void clearPlayer(final @NotNull UUID playerId) {
        lastCollectionTimes.remove(playerId);
        lastActivityTimes.remove(playerId);
    }

    /**
     * Gets the last collection time for a player.
     *
     * @param playerId the player UUID
     * @return the last collection time, or null if never collected
     */
    public Instant getLastCollectionTime(final @NotNull UUID playerId) {
        return lastCollectionTimes.get(playerId);
    }

    /**
     * Gets the last activity time for a player.
     *
     * @param playerId the player UUID
     * @return the last activity time, or null if never tracked
     */
    public Instant getLastActivityTime(final @NotNull UUID playerId) {
        return lastActivityTimes.get(playerId);
    }

    /**
     * Gets the number of players currently being tracked.
     *
     * @return the tracked player count
     */
    public int getTrackedPlayerCount() {
        return lastCollectionTimes.size();
    }

    /**
     * Shuts down the batch processor and its thread pool.
     *
     * <p>This method attempts a graceful shutdown, waiting up to 5 seconds
     * for running tasks to complete.
     */
    public void shutdown() {
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                LOGGER.warning("Forced shutdown of batch collection processor");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Batch collection processor shut down");
    }
}
