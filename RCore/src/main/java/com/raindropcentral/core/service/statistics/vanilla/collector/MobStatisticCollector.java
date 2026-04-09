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

package com.raindropcentral.core.service.statistics.vanilla.collector;

import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.core.service.statistics.vanilla.version.StatisticAvailabilityChecker;
import com.raindropcentral.core.service.statistics.vanilla.version.StatisticMapper;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects mob-related statistics from players.
 *
 * <p>This collector handles:
 * <ul>
 *   <li>KILL_ENTITY for all entity types</li>
 *   <li>ENTITY_KILLED_BY for all entity types</li>
 *   <li>Aggregates: total_mobs_killed, total_deaths_by_mob</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.mobs.killed.<entity>} or {@code minecraft.mobs.killed_by.<entity>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class MobStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * Constructs a new mob statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public MobStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects mob statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectMobStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();
        int totalMobsKilled = 0;
        int totalDeathsByMob = 0;

        // Collect KILL_ENTITY
        if (availabilityChecker.isStatisticAvailable(Statistic.KILL_ENTITY)) {
            for (final EntityType entityType : EntityType.values()) {
                if (!entityType.isAlive()) {
                    continue;
                }

                if (!config.shouldCollectEntity(entityType)) {
                    continue;
                }

                if (!availabilityChecker.isStatisticAvailable(Statistic.KILL_ENTITY, entityType)) {
                    continue;
                }

                try {
                    final int value = player.getStatistic(Statistic.KILL_ENTITY, entityType);
                    if (value > 0) {
                        final String identifier = mapper.mapStatistic(Statistic.KILL_ENTITY, null, entityType);
                        statistics.add(QueuedStatistic.builder()
                            .playerUuid(player.getUniqueId())
                            .statisticKey(identifier)
                            .value(value)
                            .dataType(StatisticDataType.NUMBER)
                            .collectionTimestamp(timestamp)
                            .priority(DeliveryPriority.NORMAL)
                            .isDelta(false)
                            .sourcePlugin("RCore-Vanilla")
                            .build());
                        
                        totalMobsKilled += value;
                    }
                } catch (final IllegalArgumentException ignored) {
                    // Statistic not available for this entity type
                }
            }
        }

        // Collect ENTITY_KILLED_BY
        if (availabilityChecker.isStatisticAvailable(Statistic.ENTITY_KILLED_BY)) {
            for (final EntityType entityType : EntityType.values()) {
                if (!entityType.isAlive()) {
                    continue;
                }

                if (!config.shouldCollectEntity(entityType)) {
                    continue;
                }

                if (!availabilityChecker.isStatisticAvailable(Statistic.ENTITY_KILLED_BY, entityType)) {
                    continue;
                }

                try {
                    final int value = player.getStatistic(Statistic.ENTITY_KILLED_BY, entityType);
                    if (value > 0) {
                        final String identifier = mapper.mapStatistic(Statistic.ENTITY_KILLED_BY, null, entityType);
                        statistics.add(QueuedStatistic.builder()
                            .playerUuid(player.getUniqueId())
                            .statisticKey(identifier)
                            .value(value)
                            .dataType(StatisticDataType.NUMBER)
                            .collectionTimestamp(timestamp)
                            .priority(DeliveryPriority.NORMAL)
                            .isDelta(false)
                            .sourcePlugin("RCore-Vanilla")
                            .build());
                        
                        totalDeathsByMob += value;
                    }
                } catch (final IllegalArgumentException ignored) {
                    // Statistic not available for this entity type
                }
            }
        }

        // Add aggregates
        if (totalMobsKilled > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey("minecraft.mobs.total_killed")
                .value(totalMobsKilled)
                .dataType(StatisticDataType.NUMBER)
                .collectionTimestamp(timestamp)
                .priority(DeliveryPriority.NORMAL)
                .isDelta(false)
                .sourcePlugin("RCore-Vanilla")
                .build());
        }

        if (totalDeathsByMob > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey("minecraft.mobs.total_deaths_by")
                .value(totalDeathsByMob)
                .dataType(StatisticDataType.NUMBER)
                .collectionTimestamp(timestamp)
                .priority(DeliveryPriority.NORMAL)
                .isDelta(false)
                .sourcePlugin("RCore-Vanilla")
                .build());
        }

        return statistics;
    }
}
