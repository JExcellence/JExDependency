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
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects item-related statistics from players.
 *
 * <p>This collector handles:
 * <ul>
 *   <li>CRAFT_ITEM for all craftable materials</li>
 *   <li>USE_ITEM for all usable items</li>
 *   <li>BREAK_ITEM for all breakable items</li>
 *   <li>PICKUP for all materials</li>
 *   <li>DROP for all materials</li>
 *   <li>Aggregates: total_items_crafted, total_items_used</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.items.<action>.<material>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ItemStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * Constructs a new item statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public ItemStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects item statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectItemStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();
        int totalItemsCrafted = 0;
        int totalItemsUsed = 0;
        int totalItemsBroken = 0;
        int totalItemsPickedUp = 0;
        int totalItemsDropped = 0;

        // Collect CRAFT_ITEM
        if (availabilityChecker.isStatisticAvailable(Statistic.CRAFT_ITEM)) {
            totalItemsCrafted = collectMaterialStatistic(
                player, Statistic.CRAFT_ITEM, statistics, timestamp
            );
        }

        // Collect USE_ITEM
        if (availabilityChecker.isStatisticAvailable(Statistic.USE_ITEM)) {
            totalItemsUsed = collectMaterialStatistic(
                player, Statistic.USE_ITEM, statistics, timestamp
            );
        }

        // Collect BREAK_ITEM
        if (availabilityChecker.isStatisticAvailable(Statistic.BREAK_ITEM)) {
            totalItemsBroken = collectMaterialStatistic(
                player, Statistic.BREAK_ITEM, statistics, timestamp
            );
        }

        // Collect PICKUP
        if (availabilityChecker.isStatisticAvailable(Statistic.PICKUP)) {
            totalItemsPickedUp = collectMaterialStatistic(
                player, Statistic.PICKUP, statistics, timestamp
            );
        }

        // Collect DROP
        if (availabilityChecker.isStatisticAvailable(Statistic.DROP)) {
            totalItemsDropped = collectMaterialStatistic(
                player, Statistic.DROP, statistics, timestamp
            );
        }

        // Add aggregates
        addAggregate(statistics, player, "minecraft.items.total_crafted", totalItemsCrafted, timestamp);
        addAggregate(statistics, player, "minecraft.items.total_used", totalItemsUsed, timestamp);
        addAggregate(statistics, player, "minecraft.items.total_broken", totalItemsBroken, timestamp);
        addAggregate(statistics, player, "minecraft.items.total_picked_up", totalItemsPickedUp, timestamp);
        addAggregate(statistics, player, "minecraft.items.total_dropped", totalItemsDropped, timestamp);

        return statistics;
    }

    /**
     * Collects a material-based statistic for all materials.
     *
     * @param player the player
     * @param statistic the statistic to collect
     * @param statistics the list to add statistics to
     * @param timestamp the collection timestamp
     * @return the total count across all materials
     */
    private int collectMaterialStatistic(
        final @NotNull Player player,
        final @NotNull Statistic statistic,
        final @NotNull List<QueuedStatistic> statistics,
        final long timestamp
    ) {
        int total = 0;

        for (final Material material : Material.values()) {
            // Skip materials that can't be collected - avoid Material.isItem() which triggers DataFixer
            if (!config.shouldCollectMaterial(material)) {
                continue;
            }

            if (!availabilityChecker.isStatisticAvailable(statistic, material)) {
                continue;
            }

            try {
                final int value = player.getStatistic(statistic, material);
                if (value > 0) {
                    final String identifier = mapper.mapStatistic(statistic, material, null);
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
                    
                    total += value;
                }
            } catch (final IllegalArgumentException ignored) {
                // Statistic not available for this material
            }
        }

        return total;
    }

    /**
     * Adds an aggregate statistic if the value is greater than zero.
     *
     * @param statistics the list to add to
     * @param player the player
     * @param key the statistic key
     * @param value the value
     * @param timestamp the timestamp
     */
    private void addAggregate(
        final @NotNull List<QueuedStatistic> statistics,
        final @NotNull Player player,
        final @NotNull String key,
        final int value,
        final long timestamp
    ) {
        if (value > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey(key)
                .value(value)
                .dataType(StatisticDataType.NUMBER)
                .collectionTimestamp(timestamp)
                .priority(DeliveryPriority.NORMAL)
                .isDelta(false)
                .sourcePlugin("RCore-Vanilla")
                .build());
        }
    }
}
