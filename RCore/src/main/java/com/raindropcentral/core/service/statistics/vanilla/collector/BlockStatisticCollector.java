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
 * Collects block-related statistics from players.
 *
 * <p>This collector handles:
 * <ul>
 *   <li>MINE_BLOCK for all mineable materials</li>
 *   <li>USE_ITEM for placeable blocks</li>
 *   <li>Aggregate: total_blocks_mined</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.blocks.mined.<material>} or {@code minecraft.blocks.used.<material>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class BlockStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * Constructs a new block statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public BlockStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects block statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectBlockStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();
        int totalBlocksMined = 0;
        int totalBlocksPlaced = 0;

        // Collect MINE_BLOCK for all materials
        if (availabilityChecker.isStatisticAvailable(Statistic.MINE_BLOCK)) {
            for (final Material material : Material.values()) {
                // Skip materials that can't be collected - avoid Material.isBlock() which triggers DataFixer
                if (!config.shouldCollectMaterial(material)) {
                    continue;
                }

                if (!availabilityChecker.isStatisticAvailable(Statistic.MINE_BLOCK, material)) {
                    continue;
                }

                try {
                    final int value = player.getStatistic(Statistic.MINE_BLOCK, material);
                    if (value > 0) {
                        final String identifier = mapper.mapStatistic(Statistic.MINE_BLOCK, material, null);
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
                        
                        totalBlocksMined += value;
                    }
                } catch (final IllegalArgumentException ignored) {
                    // Statistic not available for this material
                }
            }
        }

        // Collect USE_ITEM for placeable blocks
        if (availabilityChecker.isStatisticAvailable(Statistic.USE_ITEM)) {
            for (final Material material : Material.values()) {
                // Skip materials that can't be collected - avoid Material.isBlock()/isSolid() which trigger DataFixer
                if (!config.shouldCollectMaterial(material)) {
                    continue;
                }

                if (!availabilityChecker.isStatisticAvailable(Statistic.USE_ITEM, material)) {
                    continue;
                }

                try {
                    final int value = player.getStatistic(Statistic.USE_ITEM, material);
                    if (value > 0) {
                        final String identifier = mapper.mapStatistic(Statistic.USE_ITEM, material, null);
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
                        
                        totalBlocksPlaced += value;
                    }
                } catch (final IllegalArgumentException ignored) {
                    // Statistic not available for this material
                }
            }
        }

        // Add aggregate: total_blocks_mined
        if (totalBlocksMined > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey("minecraft.blocks.total_mined")
                .value(totalBlocksMined)
                .dataType(StatisticDataType.NUMBER)
                .collectionTimestamp(timestamp)
                .priority(DeliveryPriority.NORMAL)
                .isDelta(false)
                .sourcePlugin("RCore-Vanilla")
                .build());
        }

        // Add aggregate: total_blocks_placed
        if (totalBlocksPlaced > 0) {
            statistics.add(QueuedStatistic.builder()
                .playerUuid(player.getUniqueId())
                .statisticKey("minecraft.blocks.total_placed")
                .value(totalBlocksPlaced)
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
