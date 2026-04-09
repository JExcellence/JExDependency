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
 * Collects block and entity interaction statistics from players.
 *
 * <p>This collector handles all INTERACT_WITH_* statistics including:
 * <ul>
 *   <li>INTERACT_WITH_ANVIL</li>
 *   <li>INTERACT_WITH_BEACON</li>
 *   <li>INTERACT_WITH_BREWING_STAND</li>
 *   <li>INTERACT_WITH_CRAFTING_TABLE</li>
 *   <li>INTERACT_WITH_FURNACE</li>
 *   <li>And many more interaction statistics</li>
 *   <li>ITEM_ENCHANTED</li>
 *   <li>TRADED_WITH_VILLAGER</li>
 *   <li>TARGET_HIT</li>
 * </ul>
 *
 * <p>Statistics are mapped to identifiers following the pattern:
 * {@code minecraft.interactions.<interaction>}
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class InteractionStatisticCollector {

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    /**
     * Interaction statistics to collect.
     */
    private static final Statistic[] INTERACTION_STATISTICS = {
        Statistic.INTERACT_WITH_ANVIL,
        // Statistic.INTERACT_WITH_BEACON, // Not available in all versions
        // Statistic.INTERACT_WITH_BREWINGSTAND, // Not available in all versions
        // Statistic.INTERACT_WITH_CRAFTING_TABLE, // Not available in all versions
        // Statistic.INTERACT_WITH_FURNACE, // Not available in all versions
        Statistic.ITEM_ENCHANTED,
        Statistic.TRADED_WITH_VILLAGER
    };

    /**
     * Constructs a new interaction statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public InteractionStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;
    }

    /**
     * Collects interaction statistics for a player.
     *
     * @param player the player to collect statistics from
     * @return list of queued statistics
     */
    public @NotNull List<QueuedStatistic> collectInteractionStatistics(final @NotNull Player player) {
        final List<QueuedStatistic> statistics = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();

        // Collect standard interaction statistics
        for (final Statistic statistic : INTERACTION_STATISTICS) {
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

        // Collect version-specific interaction statistics
        collectVersionSpecificInteractions(player, statistics, timestamp);

        return statistics;
    }

    /**
     * Collects interaction statistics that may not be available in all versions.
     *
     * @param player the player
     * @param statistics the list to add statistics to
     * @param timestamp the collection timestamp
     */
    private void collectVersionSpecificInteractions(
        final @NotNull Player player,
        final @NotNull List<QueuedStatistic> statistics,
        final long timestamp
    ) {
        // Try interaction statistics that were added in later versions
        final String[] versionSpecificInteractions = {
            "INTERACT_WITH_BLAST_FURNACE",
            "INTERACT_WITH_SMOKER",
            "INTERACT_WITH_LECTERN",
            "INTERACT_WITH_CAMPFIRE",
            "INTERACT_WITH_CARTOGRAPHY_TABLE",
            "INTERACT_WITH_LOOM",
            "INTERACT_WITH_STONECUTTER",
            "INTERACT_WITH_GRINDSTONE",
            "INTERACT_WITH_SMITHING_TABLE",
            "TARGET_HIT",
            "INTERACT_WITH_DISPENSER",
            "INTERACT_WITH_DROPPER",
            "INTERACT_WITH_HOPPER"
        };

        for (final String statName : versionSpecificInteractions) {
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
