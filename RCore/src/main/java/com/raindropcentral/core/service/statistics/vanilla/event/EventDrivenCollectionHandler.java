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

package com.raindropcentral.core.service.statistics.vanilla.event;

import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.vanilla.VanillaStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.core.service.statistics.vanilla.sync.VanillaCrossServerSyncManager;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles event-driven collection of vanilla statistics.
 *
 * <p>This listener captures statistics immediately when significant events occur:
 * <ul>
 *   <li>Player disconnect - full snapshot with HIGH priority</li>
 *   <li>Player death - death-related statistics with HIGH priority</li>
 *   <li>Advancement completion - related statistics with NORMAL priority</li>
 *   <li>Playtime milestones - full snapshot with NORMAL priority</li>
 * </ul>
 *
 * <p>Event consolidation is implemented to prevent duplicate collections when
 * multiple events fire within a 5-second window for the same player.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * EventDrivenCollectionHandler handler = new EventDrivenCollectionHandler(
 *     plugin, collector, cacheManager, config, queueConsumer
 * );
 * 
 * // Register with plugin
 * plugin.getServer().getPluginManager().registerEvents(handler, plugin);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class EventDrivenCollectionHandler implements Listener {

    private static final Logger LOGGER = Logger.getLogger(EventDrivenCollectionHandler.class.getName());
    private static final long EVENT_CONSOLIDATION_WINDOW_MS = 5_000L;
    private static final int PLAYTIME_MILESTONE_TICKS = 72_000; // 1 hour in ticks (20 ticks/sec * 60 sec * 60 min)

    private final Plugin plugin;
    private final VanillaStatisticCollector collector;
    private final StatisticCacheManager cacheManager;
    private final VanillaStatisticConfig config;
    private final StatisticQueueConsumer queueConsumer;
    private final VanillaCrossServerSyncManager syncManager;

    /**
     * Tracks last event time per player for consolidation.
     * UUID -> last event timestamp in milliseconds
     */
    private final ConcurrentHashMap<UUID, Long> lastEventTime;

    /**
     * Tracks last playtime check per player for milestone detection.
     * UUID -> last checked playtime in ticks
     */
    private final ConcurrentHashMap<UUID, Integer> lastPlaytimeCheck;

    /**
     * Creates a new event-driven collection handler.
     *
     * @param plugin        the plugin instance
     * @param collector     the vanilla statistic collector
     * @param cacheManager  the cache manager
     * @param config        the configuration
     * @param queueConsumer the consumer for queuing collected statistics
     * @param syncManager   the cross-server sync manager (nullable if sync disabled)
     */
    public EventDrivenCollectionHandler(
        final @NotNull Plugin plugin,
        final @NotNull VanillaStatisticCollector collector,
        final @NotNull StatisticCacheManager cacheManager,
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticQueueConsumer queueConsumer,
        final @Nullable VanillaCrossServerSyncManager syncManager
    ) {
        this.plugin = plugin;
        this.collector = collector;
        this.cacheManager = cacheManager;
        this.config = config;
        this.queueConsumer = queueConsumer;
        this.syncManager = syncManager;
        this.lastEventTime = new ConcurrentHashMap<>();
        this.lastPlaytimeCheck = new ConcurrentHashMap<>();

        LOGGER.info("Initialized EventDrivenCollectionHandler" + 
            (syncManager != null ? " with cross-server sync" : ""));
    }

    /**
     * Handles player join events.
     * Requests latest statistics from backend for cross-server synchronization.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();

        // Request latest statistics from backend if sync is enabled
        if (syncManager != null) {
            LOGGER.fine("Player joined: " + player.getName() + ", requesting latest statistics");
            
            syncManager.requestLatestStatistics(playerId)
                .exceptionally(error -> {
                    LOGGER.warning("Failed to request statistics for " + player.getName() + 
                        " on join: " + error.getMessage());
                    return null;
                });
        }
    }

    /**
     * Handles player quit events.
     * Captures a full snapshot of all statistics with HIGH priority and clears the cache.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();

        LOGGER.fine("Player quit event for " + player.getName() + ", scheduling async collection");

        // Move collection to async thread immediately to avoid blocking main thread
        // player.getStatistic() can trigger DataFixer initialization which blocks for 10-30 seconds
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Collect all statistics (this may trigger DataFixer on first call)
                final List<QueuedStatistic> statistics = collector.collectAllForPlayer(player);

                // Update statistics with HIGH priority
                final List<QueuedStatistic> highPriorityStats = statistics.stream()
                    .map(stat -> stat.withPriority(DeliveryPriority.HIGH))
                    .toList();

                // Queue statistics
                queueConsumer.accept(highPriorityStats);

                // Clear player from cache (back on main thread)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    cacheManager.clearPlayer(playerId);
                    lastEventTime.remove(playerId);
                    lastPlaytimeCheck.remove(playerId);
                });

                LOGGER.fine("Collected " + statistics.size() + " statistics on quit for " + player.getName());

            } catch (Exception e) {
                LOGGER.warning("Failed to collect statistics on quit for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Handles player death events.
     * Collects death-related statistics with HIGH priority.
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final UUID playerId = player.getUniqueId();

        // Check event consolidation
        if (!shouldProcessEvent(playerId)) {
            LOGGER.fine("Skipping death event for " + player.getName() + " due to consolidation window");
            return;
        }

        LOGGER.fine("Player death event for " + player.getName() + ", scheduling async collection");

        // Move collection to async thread to avoid DataFixer blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Collect death-related statistics
                final List<QueuedStatistic> deathStats = collectDeathStatistics(player);

                // Update statistics with HIGH priority
                final List<QueuedStatistic> highPriorityStats = deathStats.stream()
                    .map(stat -> stat.withPriority(DeliveryPriority.HIGH))
                    .toList();

                // Queue statistics
                queueConsumer.accept(highPriorityStats);

                LOGGER.fine("Collected " + deathStats.size() + " death statistics for " + player.getName());

            } catch (Exception e) {
                LOGGER.warning("Failed to collect death statistics for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Handles player advancement completion events.
     * Collects related statistics with NORMAL priority.
     *
     * @param event the player advancement done event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancementDone(final @NotNull PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();

        // Check event consolidation
        if (!shouldProcessEvent(playerId)) {
            LOGGER.fine("Skipping advancement event for " + player.getName() + " due to consolidation window");
            return;
        }

        LOGGER.fine("Player advancement event for " + player.getName() + ", scheduling async collection");

        // Move collection to async thread to avoid DataFixer blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Collect all statistics (advancement-related stats are part of general collection)
                final List<QueuedStatistic> statistics = collector.collectAllForPlayer(player);

                // Queue with NORMAL priority
                queueConsumer.accept(statistics);

                // Check for playtime milestone
                checkPlaytimeMilestone(player);

                LOGGER.fine("Collected " + statistics.size() + " statistics on advancement for " + player.getName());

            } catch (Exception e) {
                LOGGER.warning("Failed to collect statistics on advancement for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Collects death-related statistics from a player.
     *
     * @param player the player
     * @return list of death-related statistics
     */
    private @NotNull List<QueuedStatistic> collectDeathStatistics(final @NotNull Player player) {
        // For now, collect all general statistics which include death stats
        // In a more optimized implementation, we could collect only specific stats
        return collector.collectAllForPlayer(player);
    }

    /**
     * Checks if a playtime milestone has been reached and triggers collection if so.
     *
     * @param player the player to check
     */
    private void checkPlaytimeMilestone(final @NotNull Player player) {
        try {
            final UUID playerId = player.getUniqueId();
            final int currentPlaytime = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            final int lastChecked = lastPlaytimeCheck.getOrDefault(playerId, 0);

            // Check if player has crossed a milestone boundary
            final int currentMilestone = currentPlaytime / PLAYTIME_MILESTONE_TICKS;
            final int lastMilestone = lastChecked / PLAYTIME_MILESTONE_TICKS;

            if (currentMilestone > lastMilestone) {
                LOGGER.fine("Playtime milestone reached for " + player.getName() + 
                    " (" + (currentMilestone * PLAYTIME_MILESTONE_TICKS / 1200) + " hours)");

                // Collect full snapshot
                final List<QueuedStatistic> statistics = collector.collectAllForPlayer(player);
                queueConsumer.accept(statistics);

                LOGGER.fine("Collected " + statistics.size() + " statistics on playtime milestone for " + player.getName());
            }

            // Update last checked playtime
            lastPlaytimeCheck.put(playerId, currentPlaytime);

        } catch (Exception e) {
            LOGGER.warning("Failed to check playtime milestone for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Checks if an event should be processed based on consolidation window.
     * Events within 5 seconds of the last event for the same player are consolidated.
     *
     * @param playerId the player UUID
     * @return true if the event should be processed
     */
    private boolean shouldProcessEvent(final @NotNull UUID playerId) {
        final long now = System.currentTimeMillis();
        final Long lastEvent = lastEventTime.get(playerId);

        if (lastEvent != null && (now - lastEvent) < EVENT_CONSOLIDATION_WINDOW_MS) {
            return false;
        }

        lastEventTime.put(playerId, now);
        return true;
    }

    /**
     * Cleans up tracking data for a player.
     * Should be called when a player disconnects.
     *
     * @param playerId the player UUID
     */
    public void cleanup(final @NotNull UUID playerId) {
        lastEventTime.remove(playerId);
        lastPlaytimeCheck.remove(playerId);
    }

    /**
     * Gets the number of players currently being tracked.
     *
     * @return the number of tracked players
     */
    public int getTrackedPlayerCount() {
        return lastEventTime.size();
    }

    /**
     * Functional interface for consuming collected statistics.
     * Allows the handler to queue statistics without direct dependency on queue manager.
     */
    @FunctionalInterface
    public interface StatisticQueueConsumer {
        /**
         * Accepts a list of statistics for queuing.
         *
         * @param statistics the statistics to queue
         */
        void accept(@NotNull List<QueuedStatistic> statistics);
    }
}
