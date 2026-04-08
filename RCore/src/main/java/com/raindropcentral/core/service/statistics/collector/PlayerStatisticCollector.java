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

package com.raindropcentral.core.service.statistics.collector;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.type.EStatisticType;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticCategory;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Collects custom player statistics from RPlayerStatistic entities.
 * Supports delta tracking, filtering by category/key, and batch collection.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PlayerStatisticCollector {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final String SOURCE_PLUGIN = "RCore";

    private final StatisticsDeliveryConfig config;

    // Cache of player statistics for collection
    private final Map<UUID, RPlayerStatistic> playerStatisticsCache;
    // Tracks last delivered values for delta detection
    private final Map<UUID, Map<String, Object>> lastDeliveredValues;
    // Tracks last delivery timestamps
    private final Map<UUID, Map<String, Long>> lastDeliveryTimestamps;

    /**
     * Creates a new player statistic collector.
     *
     * @param config the statistics delivery configuration
     */
    public PlayerStatisticCollector(final @NotNull StatisticsDeliveryConfig config) {
        this.config = config;
        this.playerStatisticsCache = new ConcurrentHashMap<>();
        this.lastDeliveredValues = new ConcurrentHashMap<>();
        this.lastDeliveryTimestamps = new ConcurrentHashMap<>();
    }

    /**
     * Legacy constructor for compatibility.
     */
    public PlayerStatisticCollector(
        final @NotNull Object repository,
        final @NotNull StatisticsDeliveryConfig config
    ) {
        this(config);
    }

    /**
     * Registers player statistics for collection.
     * Called when player statistics are loaded.
     *
     * @param playerUuid the player UUID
     * @param statistics the player's statistics
     */
    public void registerPlayerStatistics(
        final @NotNull UUID playerUuid,
        final @NotNull RPlayerStatistic statistics
    ) {
        playerStatisticsCache.put(playerUuid, statistics);
    }

    /**
     * Collects all statistics for a player.
     *
     * @param playerUuid the player UUID
     * @return list of queued statistics
     */
    public List<QueuedStatistic> collectForPlayer(final @NotNull UUID playerUuid) {
        return collectForPlayer(playerUuid, false);
    }

    /**
     * Collects delta statistics for a player (only changed values).
     *
     * @param playerUuid the player UUID
     * @return list of queued statistics that have changed
     */
    public List<QueuedStatistic> collectDeltaForPlayer(final @NotNull UUID playerUuid) {
        return collectForPlayer(playerUuid, true);
    }

    /**
     * Collects statistics for a player with optional delta filtering.
     */
    private List<QueuedStatistic> collectForPlayer(
        final @NotNull UUID playerUuid,
        final boolean deltaOnly
    ) {
        List<QueuedStatistic> result = new ArrayList<>();

        try {
            RPlayerStatistic playerStat = playerStatisticsCache.get(playerUuid);
            if (playerStat == null) {
                return result;
            }

            long timestamp = System.currentTimeMillis();

            Map<String, Object> lastValues = lastDeliveredValues.computeIfAbsent(
                playerUuid, k -> new ConcurrentHashMap<>()
            );

            for (RAbstractStatistic stat : playerStat.getStatistics()) {
                String key = stat.getIdentifier();
                Object value = stat.getValue();

                // Normalize value - convert temporal types to epoch millis
                value = normalizeValue(value);

                // Apply filtering
                if (!shouldCollect(key)) {
                    continue;
                }

                // Check category filtering
                EStatisticType type = EStatisticType.getByKey(key);
                if (type != null && !config.isCategoryEnabled(type.getCategory())) {
                    continue;
                }

                // Delta check
                boolean isDelta = false;
                if (deltaOnly) {
                    Object lastValue = lastValues.get(key);
                    if (lastValue != null && lastValue.equals(value)) {
                        continue; // No change
                    }
                    isDelta = true;
                }

                // Determine data type
                StatisticDataType dataType = determineDataType(type, value);

                QueuedStatistic queued = QueuedStatistic.builder()
                    .playerUuid(playerUuid)
                    .statisticKey(key)
                    .value(value)
                    .dataType(dataType)
                    .collectionTimestamp(timestamp)
                    .priority(DeliveryPriority.NORMAL)
                    .isDelta(isDelta)
                    .sourcePlugin(SOURCE_PLUGIN)
                    .build();

                result.add(queued);
            }

        } catch (Exception e) {
            LOGGER.warning("Failed to collect statistics for player " + playerUuid + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Collects statistics for all online players.
     *
     * @return list of queued statistics for all online players
     */
    public List<QueuedStatistic> collectAllOnlinePlayers() {
        List<QueuedStatistic> result = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            result.addAll(collectDeltaForPlayer(player.getUniqueId()));
        }

        return result;
    }

    /**
     * Collects statistics by category for all online players.
     */
    public List<QueuedStatistic> collectByCategory(final @NotNull StatisticCategory category) {
        List<QueuedStatistic> result = new ArrayList<>();

        if (!config.isCategoryEnabled(category)) {
            return result;
        }

        Set<String> categoryKeys = new HashSet<>();
        for (EStatisticType type : EStatisticType.getByCategory(category)) {
            categoryKeys.add(type.getKey());
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            List<QueuedStatistic> playerStats = collectForPlayer(player.getUniqueId(), false);
            for (QueuedStatistic stat : playerStats) {
                if (categoryKeys.contains(stat.statisticKey())) {
                    result.add(stat);
                }
            }
        }

        return result;
    }

    /**
     * Checks if a statistic key should be collected based on configuration.
     */
    public boolean shouldCollect(final @NotNull String key) {
        return config.shouldCollectKey(key);
    }

    /**
     * Checks if a statistic type should be collected.
     */
    public boolean shouldCollect(final @NotNull EStatisticType type) {
        if (!config.isCategoryEnabled(type.getCategory())) {
            return false;
        }
        return config.shouldCollectKey(type.getKey());
    }

    /**
     * Marks a statistic as delivered, updating tracking data.
     */
    public void markDelivered(
        final @NotNull UUID playerUuid,
        final @NotNull String statisticKey,
        final @NotNull Object value,
        final long timestamp
    ) {
        lastDeliveredValues
            .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
            .put(statisticKey, value);

        lastDeliveryTimestamps
            .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
            .put(statisticKey, timestamp);
    }

    /**
     * Marks multiple statistics as delivered.
     */
    public void markDelivered(final @NotNull Collection<QueuedStatistic> statistics) {
        for (QueuedStatistic stat : statistics) {
            markDelivered(stat.playerUuid(), stat.statisticKey(), stat.value(), stat.collectionTimestamp());
        }
    }

    /**
     * Checks if a statistic has changed since last delivery.
     */
    public boolean hasChanged(
        final @NotNull UUID playerUuid,
        final @NotNull String statisticKey,
        final @NotNull Object currentValue
    ) {
        Map<String, Object> lastValues = lastDeliveredValues.get(playerUuid);
        if (lastValues == null) {
            return true;
        }

        Object lastValue = lastValues.get(statisticKey);
        if (lastValue == null) {
            return true;
        }

        return !lastValue.equals(currentValue);
    }

    /**
     * Gets the last delivery timestamp for a statistic.
     */
    public @Nullable Long getLastDeliveryTimestamp(
        final @NotNull UUID playerUuid,
        final @NotNull String statisticKey
    ) {
        Map<String, Long> timestamps = lastDeliveryTimestamps.get(playerUuid);
        return timestamps != null ? timestamps.get(statisticKey) : null;
    }

    /**
     * Clears tracking data for a player (e.g., on disconnect).
     */
    public void clearPlayerTracking(final @NotNull UUID playerUuid) {
        lastDeliveredValues.remove(playerUuid);
        lastDeliveryTimestamps.remove(playerUuid);
        playerStatisticsCache.remove(playerUuid);
    }

    /**
     * Determines the data type for a statistic value.
     */
    private StatisticDataType determineDataType(
        final @Nullable EStatisticType type,
        final @NotNull Object value
    ) {
        if (type != null) {
            return type.getDataType();
        }

        if (value instanceof Boolean) {
            return StatisticDataType.BOOLEAN;
        } else if (value instanceof Number) {
            return StatisticDataType.NUMBER;
        } else if (value instanceof Long && (Long) value > 1_000_000_000_000L) {
            return StatisticDataType.TIMESTAMP;
        } else {
            return StatisticDataType.STRING;
        }
    }

    /**
     * Normalizes a statistic value by converting temporal types to epoch milliseconds.
 *
 * <p>This ensures that timestamps are stored as Long values rather than
     * LocalDateTime or Instant objects, which would be serialized as ISO strings
     * and cause parsing issues.
     * </p>
     *
     * @param value the value to normalize
     * @return the normalized value (epoch millis for temporal types, original value otherwise)
     */
    private @NotNull Object normalizeValue(final @NotNull Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } else if (value instanceof Instant instant) {
            return instant.toEpochMilli();
        } else if (value instanceof java.util.Date date) {
            return date.getTime();
        }
        return value;
    }
}
