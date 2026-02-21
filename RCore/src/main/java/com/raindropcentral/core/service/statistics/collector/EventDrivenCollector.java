package com.raindropcentral.core.service.statistics.collector;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import com.raindropcentral.core.service.statistics.sync.CrossServerSyncManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Handles event-triggered statistic collection.
 * Captures player disconnect snapshots, significant changes, and game events.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class EventDrivenCollector implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final Plugin plugin;
    private final StatisticsQueueManager queueManager;
    private final PlayerStatisticCollector playerCollector;
    private final NativeStatisticCollector nativeCollector;
    private final StatisticsDeliveryConfig config;
    private final CrossServerSyncManager syncManager;

    // Consolidation window for batching events per player
    private final Map<UUID, List<QueuedStatistic>> pendingPlayerEvents;
    private final Map<UUID, ScheduledFuture<?>> consolidationTasks;
    private ScheduledExecutorService executor;

    public EventDrivenCollector(
        final @NotNull Plugin plugin,
        final @NotNull StatisticsQueueManager queueManager,
        final @NotNull PlayerStatisticCollector playerCollector,
        final @NotNull NativeStatisticCollector nativeCollector,
        final @NotNull StatisticsDeliveryConfig config,
        final @Nullable CrossServerSyncManager syncManager
    ) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.playerCollector = playerCollector;
        this.nativeCollector = nativeCollector;
        this.config = config;
        this.syncManager = syncManager;
        this.pendingPlayerEvents = new ConcurrentHashMap<>();
        this.consolidationTasks = new ConcurrentHashMap<>();
    }

    /**
     * Sets the executor for consolidation tasks.
     *
     * @param executor the scheduled executor
     */
    public void setExecutor(final @NotNull ScheduledExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Registers this collector as a Bukkit event listener.
     */
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        LOGGER.info("EventDrivenCollector registered");
    }

    // ==================== Bukkit Event Handlers ====================

    /**
     * Handles player quit - captures full snapshot with HIGH priority.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        LOGGER.fine("Player quit event: " + player.getName());

        try {
            // Collect all statistics for the player
            List<QueuedStatistic> customStats = playerCollector.collectForPlayer(playerUuid);
            List<QueuedStatistic> nativeStats = nativeCollector.collectForPlayer(player);

            // Upgrade all to HIGH priority
            List<QueuedStatistic> allStats = new ArrayList<>();
            for (QueuedStatistic stat : customStats) {
                allStats.add(stat.withPriority(DeliveryPriority.HIGH));
            }
            for (QueuedStatistic stat : nativeStats) {
                allStats.add(stat.withPriority(DeliveryPriority.HIGH));
            }

            // Queue immediately (bypass consolidation for disconnect)
            int enqueued = queueManager.enqueueBatch(allStats);
            LOGGER.fine("Queued " + enqueued + " statistics for disconnecting player " + player.getName());

            // Clear tracking data
            playerCollector.clearPlayerTracking(playerUuid);
            nativeCollector.clearPlayerSnapshot(playerUuid);

            // Cancel any pending consolidation
            cancelConsolidation(playerUuid);

        } catch (Exception e) {
            LOGGER.warning("Failed to collect disconnect statistics for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Handles player join - triggers cross-server sync if enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        LOGGER.fine("Player join event: " + player.getName());

        // Trigger cross-server sync if enabled
        if (config.isEnableCrossServerSync() && syncManager != null) {
            syncManager.requestLatestStatistics(playerUuid)
                .exceptionally(e -> {
                    LOGGER.warning("Failed to sync statistics for " + player.getName() + ": " + e.getMessage());
                    return null;
                });
        }
    }

    // ==================== Custom Event Hooks ====================

    /**
     * Called when a player completes a quest.
     *
     * @param playerUuid the player UUID
     * @param questId    the quest ID
     */
    public void onQuestComplete(final @NotNull UUID playerUuid, final @NotNull String questId) {
        LOGGER.fine("Quest complete event: " + questId + " for " + playerUuid);

        // Collect progression-related statistics with HIGH priority
        List<QueuedStatistic> stats = playerCollector.collectForPlayer(playerUuid);
        List<QueuedStatistic> highPriorityStats = stats.stream()
            .filter(s -> s.statisticKey().contains("quest") || s.statisticKey().contains("progression"))
            .map(s -> s.withPriority(DeliveryPriority.HIGH))
            .toList();

        queueWithConsolidation(playerUuid, highPriorityStats);
    }

    /**
     * Called when a player levels up.
     *
     * @param playerUuid the player UUID
     * @param newLevel   the new level
     */
    public void onLevelUp(final @NotNull UUID playerUuid, final int newLevel) {
        LOGGER.fine("Level up event: level " + newLevel + " for " + playerUuid);

        List<QueuedStatistic> stats = playerCollector.collectForPlayer(playerUuid);
        List<QueuedStatistic> highPriorityStats = stats.stream()
            .filter(s -> s.statisticKey().contains("level") || s.statisticKey().contains("experience"))
            .map(s -> s.withPriority(DeliveryPriority.HIGH))
            .toList();

        queueWithConsolidation(playerUuid, highPriorityStats);
    }

    /**
     * Called when a significant economy transaction occurs.
     *
     * @param playerUuid the player UUID
     * @param amount     the transaction amount
     */
    public void onEconomyTransaction(final @NotNull UUID playerUuid, final double amount) {
        if (Math.abs(amount) < config.getEconomyTransactionThreshold()) {
            return; // Below threshold
        }

        LOGGER.fine("Economy transaction event: " + amount + " for " + playerUuid);

        List<QueuedStatistic> stats = playerCollector.collectForPlayer(playerUuid);
        List<QueuedStatistic> highPriorityStats = stats.stream()
            .filter(s -> s.statisticKey().contains("money") || s.statisticKey().contains("economy") || s.statisticKey().contains("balance"))
            .map(s -> s.withPriority(DeliveryPriority.HIGH))
            .toList();

        queueWithConsolidation(playerUuid, highPriorityStats);
    }

    /**
     * Called when a player activates a perk.
     *
     * @param playerUuid the player UUID
     * @param perkId     the perk ID
     */
    public void onPerkActivation(final @NotNull UUID playerUuid, final @NotNull String perkId) {
        LOGGER.fine("Perk activation event: " + perkId + " for " + playerUuid);

        List<QueuedStatistic> stats = playerCollector.collectForPlayer(playerUuid);
        List<QueuedStatistic> normalPriorityStats = stats.stream()
            .filter(s -> s.statisticKey().contains("perk"))
            .toList();

        queueWithConsolidation(playerUuid, normalPriorityStats);
    }

    /**
     * Called when a player unlocks an achievement.
     *
     * @param playerUuid    the player UUID
     * @param achievementId the achievement ID
     */
    public void onAchievementUnlock(final @NotNull UUID playerUuid, final @NotNull String achievementId) {
        LOGGER.fine("Achievement unlock event: " + achievementId + " for " + playerUuid);

        List<QueuedStatistic> stats = playerCollector.collectForPlayer(playerUuid);
        List<QueuedStatistic> highPriorityStats = stats.stream()
            .filter(s -> s.statisticKey().contains("achievement"))
            .map(s -> s.withPriority(DeliveryPriority.HIGH))
            .toList();

        queueWithConsolidation(playerUuid, highPriorityStats);
    }

    /**
     * Called when a statistic value changes significantly.
     *
     * @param playerUuid   the player UUID
     * @param statisticKey the statistic key
     * @param oldValue     the old value
     * @param newValue     the new value
     */
    public void onSignificantChange(
        final @NotNull UUID playerUuid,
        final @NotNull String statisticKey,
        final @NotNull Object oldValue,
        final @NotNull Object newValue
    ) {
        if (!exceedsThreshold(statisticKey, oldValue, newValue)) {
            return;
        }

        LOGGER.fine("Significant change event: " + statisticKey + " for " + playerUuid);

        // Create a single statistic entry for the change
        QueuedStatistic stat = QueuedStatistic.builder()
            .playerUuid(playerUuid)
            .statisticKey(statisticKey)
            .value(newValue)
            .dataType(com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType.NUMBER)
            .collectionTimestamp(System.currentTimeMillis())
            .priority(DeliveryPriority.NORMAL)
            .isDelta(true)
            .sourcePlugin("RCore")
            .build();

        queueWithConsolidation(playerUuid, List.of(stat));
    }

    // ==================== Threshold Checking ====================

    /**
     * Checks if a value change exceeds the configured threshold.
     *
     * @param statisticKey the statistic key
     * @param oldValue     the old value
     * @param newValue     the new value
     * @return true if the change exceeds the threshold
     */
    public boolean exceedsThreshold(
        final @NotNull String statisticKey,
        final @NotNull Object oldValue,
        final @NotNull Object newValue
    ) {
        if (!(oldValue instanceof Number oldNum) || !(newValue instanceof Number newNum)) {
            return !oldValue.equals(newValue);
        }

        double oldVal = oldNum.doubleValue();
        double newVal = newNum.doubleValue();

        if (oldVal == 0) {
            return newVal != 0;
        }

        double changePercent = Math.abs((newVal - oldVal) / oldVal) * 100;
        return changePercent >= config.getSignificantChangeThresholdPercent();
    }

    // ==================== Consolidation ====================

    /**
     * Queues statistics with consolidation window.
     * Multiple events within the window are batched together.
     */
    private void queueWithConsolidation(final @NotNull UUID playerUuid, final @NotNull List<QueuedStatistic> statistics) {
        if (statistics.isEmpty()) {
            return;
        }

        if (executor == null) {
            // No executor, queue immediately
            queueManager.enqueueBatch(statistics);
            return;
        }

        // Add to pending events
        pendingPlayerEvents.computeIfAbsent(playerUuid, k -> new ArrayList<>()).addAll(statistics);

        // Schedule or reschedule consolidation flush
        ScheduledFuture<?> existingTask = consolidationTasks.get(playerUuid);
        if (existingTask == null || existingTask.isDone()) {
            ScheduledFuture<?> task = executor.schedule(
                () -> flushConsolidation(playerUuid),
                config.getEventConsolidationWindowMs(),
                TimeUnit.MILLISECONDS
            );
            consolidationTasks.put(playerUuid, task);
        }
    }

    /**
     * Flushes consolidated events for a player.
     */
    private void flushConsolidation(final @NotNull UUID playerUuid) {
        List<QueuedStatistic> pending = pendingPlayerEvents.remove(playerUuid);
        consolidationTasks.remove(playerUuid);

        if (pending != null && !pending.isEmpty()) {
            // Deduplicate - keep highest priority and most recent for each key
            Map<String, QueuedStatistic> deduplicated = new HashMap<>();
            for (QueuedStatistic stat : pending) {
                String key = stat.getDeduplicationKey();
                QueuedStatistic existing = deduplicated.get(key);
                if (existing == null ||
                    stat.priority().isHigherThan(existing.priority()) ||
                    stat.collectionTimestamp() > existing.collectionTimestamp()) {
                    deduplicated.put(key, stat);
                }
            }

            int enqueued = queueManager.enqueueBatch(deduplicated.values());
            LOGGER.fine("Flushed " + enqueued + " consolidated statistics for " + playerUuid);
        }
    }

    /**
     * Cancels pending consolidation for a player.
     */
    private void cancelConsolidation(final @NotNull UUID playerUuid) {
        ScheduledFuture<?> task = consolidationTasks.remove(playerUuid);
        if (task != null) {
            task.cancel(false);
        }
        pendingPlayerEvents.remove(playerUuid);
    }

    /**
     * Shuts down the collector, flushing all pending events.
     */
    public void shutdown() {
        // Flush all pending consolidations
        for (UUID playerUuid : new HashSet<>(pendingPlayerEvents.keySet())) {
            flushConsolidation(playerUuid);
        }

        // Cancel all tasks
        for (ScheduledFuture<?> task : consolidationTasks.values()) {
            task.cancel(false);
        }
        consolidationTasks.clear();
    }
}
