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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects travel and movement statistics from players.
 *
 * <p>This collector handles all movement statistics including:
 * <ul>
 *   <li>WALK_ONE_CM</li>
 *   <li>SPRINT_ONE_CM</li>
 *   <li>CROUCH_ONE_CM</li>
 *   <li>SWIM_ONE_CM</li>
 *   <li>FLY_ONE_CM</li>
 *   <li>CLIMB_ONE_CM</li>
 *   <li>FALL_ONE_CM</li>
 *   <li>MINECART_ONE_CM</li>
 *   <li>BOAT_ONE_CM</li>
 *   <li>PIG_ONE_CM</li>
 *   <li>HORSE_ONE_CM</li>
 *   <li>AVIATE_ONE_CM</li>
 *   <li>WALK_ON_WATER_ONE_CM</li>
 *   <li>WALK_UNDER_WATER_ONE_CM</li>
 *   <li>STRIDER_ONE_CM</li>
 *   <li>Aggregate: total_distance_traveled</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.travel.<method>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TravelStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * All travel-related statistics to collect.
     */
    private static final Statistic[] TRAVEL_STATISTICS = {
        Statistic.WALK_ONE_CM,
        Statistic.SPRINT_ONE_CM,
        Statistic.CROUCH_ONE_CM,
        Statistic.SWIM_ONE_CM,
        Statistic.FLY_ONE_CM,
        Statistic.CLIMB_ONE_CM,
        Statistic.FALL_ONE_CM,
        Statistic.MINECART_ONE_CM,
        Statistic.BOAT_ONE_CM,
        Statistic.PIG_ONE_CM,
        Statistic.HORSE_ONE_CM,
        Statistic.AVIATE_ONE_CM,
        Statistic.WALK_ON_WATER_ONE_CM,
        Statistic.WALK_UNDER_WATER_ONE_CM
    };

    /**
     * Constructs a new travel statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public TravelStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects travel statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectTravelStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();
        int totalDistance = 0;

        // Collect all travel statistics
        for (final Statistic statistic : TRAVEL_STATISTICS) {
            if (!availabilityChecker.isStatisticAvailable(statistic)) {
                continue;
            }

            if (config.isStatisticExcluded(statistic.name())) {
                continue;
            }

            try {
                final int value = player.getStatistic(statistic);
                if (value > 0) {
                    final String identifier = mapper.mapStatistic(statistic);
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
                    
                    totalDistance += value;
                }
            } catch (final IllegalArgumentException ignored) {
                // Statistic not available
            }
        }

        // Try to collect STRIDER_ONE_CM (added in 1.16)
        try {
            final Statistic striderStat = Statistic.valueOf("STRIDER_ONE_CM");
            if (availabilityChecker.isStatisticAvailable(striderStat)) {
                final int value = player.getStatistic(striderStat);
                if (value > 0) {
                    final String identifier = mapper.mapStatistic(striderStat);
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
                    
                    totalDistance += value;
                }
            }
        } catch (final IllegalArgumentException ignored) {
            // STRIDER_ONE_CM not available in this version
        }

        // Add aggregate: total_distance_traveled
        if (totalDistance > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey("minecraft.travel.total_distance")
                .value(totalDistance)
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
