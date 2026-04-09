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
 * Collects general gameplay statistics from players.
 *
 * <p>This collector handles miscellaneous statistics including:
 * <ul>
 *   <li>DEATHS</li>
 *   <li>PLAYER_KILLS</li>
 *   <li>MOB_KILLS</li>
 *   <li>ANIMALS_BRED</li>
 *   <li>FISH_CAUGHT</li>
 *   <li>PLAY_ONE_MINUTE</li>
 *   <li>TIME_SINCE_DEATH</li>
 *   <li>TIME_SINCE_REST</li>
 *   <li>SNEAK_TIME</li>
 *   <li>JUMP</li>
 *   <li>DAMAGE_DEALT</li>
 *   <li>DAMAGE_TAKEN</li>
 *   <li>And other general statistics</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.general.<statistic>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class GeneralStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * General statistics to collect.
     */
    private static final Statistic[] GENERAL_STATISTICS = {
        Statistic.DEATHS,
        Statistic.PLAYER_KILLS,
        Statistic.MOB_KILLS,
        Statistic.ANIMALS_BRED,
        Statistic.FISH_CAUGHT,
        Statistic.PLAY_ONE_MINUTE,
        Statistic.TIME_SINCE_DEATH,
        Statistic.TIME_SINCE_REST,
        Statistic.SNEAK_TIME,
        Statistic.JUMP,
        Statistic.DAMAGE_DEALT,
        Statistic.DAMAGE_TAKEN,
        Statistic.DAMAGE_DEALT_ABSORBED,
        Statistic.DAMAGE_DEALT_RESISTED,
        Statistic.DAMAGE_BLOCKED_BY_SHIELD,
        Statistic.DAMAGE_ABSORBED,
        Statistic.DAMAGE_RESISTED,
        // Statistic.CLEAN_ARMOR, // Not available in all versions
        // Statistic.CLEAN_BANNER, // Not available in all versions
        Statistic.CLEAN_SHULKER_BOX,
        Statistic.OPEN_BARREL,
        Statistic.BELL_RING,
        Statistic.RAID_TRIGGER,
        Statistic.RAID_WIN,
        Statistic.SLEEP_IN_BED,
        Statistic.TALKED_TO_VILLAGER,
        Statistic.TRADED_WITH_VILLAGER,
        // Statistic.EAT_CAKE_SLICE, // Not available in all versions
        // Statistic.FILL_CAULDRON, // Not available in all versions
        // Statistic.USE_CAULDRON, // Not available in all versions
        // Statistic.CLEAN_SHULKER_BOX, // Duplicate
        // Statistic.OPEN_CHEST, // Not available in all versions
        // Statistic.OPEN_ENDERCHEST, // Not available in all versions
        // Statistic.OPEN_SHULKER_BOX, // Not available in all versions
        Statistic.DROP_COUNT,
        Statistic.LEAVE_GAME,
        Statistic.ITEM_ENCHANTED,
        Statistic.RECORD_PLAYED,
        Statistic.FLOWER_POTTED,
        Statistic.TRAPPED_CHEST_TRIGGERED,
        Statistic.NOTEBLOCK_PLAYED,
        Statistic.NOTEBLOCK_TUNED
        // Statistic.POT_FLOWER, // Not available in all versions
        // Statistic.CHEST_OPENED, // Not available in all versions
        // Statistic.ENDERCHEST_OPENED, // Not available in all versions
        // Statistic.SHULKER_BOX_OPENED // Not available in all versions
    };

    /**
     * Constructs a new general statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public GeneralStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects general statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectGeneralStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();

        // Collect all general statistics
        for (final Statistic statistic : GENERAL_STATISTICS) {
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
                }
            } catch (final IllegalArgumentException ignored) {
                // Statistic not available
            }
        }

        // Try to collect version-specific statistics
        collectVersionSpecificStatistics(player, statistics, timestamp);

        return statistics;
    }

    /**
     * Collects statistics that may not be available in all versions.
     *
     * @param player the player
     * @param statistics the list to add statistics to
     * @param timestamp the collection timestamp
     */
    private void collectVersionSpecificStatistics(
        final @NotNull Player player,
        final @NotNull List<QueuedStatistic> statistics,
        final long timestamp
    ) {
        // Try statistics that were added in later versions
        final String[] versionSpecificStats = {
            "TARGET_HIT",
            "INSPECT_DISPENSER",
            "INSPECT_DROPPER",
            "INSPECT_HOPPER",
            "PLAY_NOTEBLOCK",
            "TUNE_NOTEBLOCK",
            "PLAY_RECORD",
            "INTERACT_WITH_BLAST_FURNACE",
            "INTERACT_WITH_SMOKER",
            "INTERACT_WITH_LECTERN",
            "INTERACT_WITH_CAMPFIRE",
            "INTERACT_WITH_CARTOGRAPHY_TABLE",
            "INTERACT_WITH_LOOM",
            "INTERACT_WITH_STONECUTTER",
            "INTERACT_WITH_GRINDSTONE",
            "INTERACT_WITH_SMITHING_TABLE"
        };

        for (final String statName : versionSpecificStats) {
            try {
                final Statistic statistic = Statistic.valueOf(statName);
                if (availabilityChecker.isStatisticAvailable(statistic) && 
                    !config.isStatisticExcluded(statName)) {
                    
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
                    }
                }
            } catch (final IllegalArgumentException ignored) {
                // Statistic not available in this version
            }
        }
    }
}
