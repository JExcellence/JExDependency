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

package com.raindropcentral.core.service.statistics.vanilla;

import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.vanilla.collector.BlockStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.collector.GeneralStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.collector.InteractionStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.collector.ItemStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.collector.MobStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.collector.TravelStatisticCollector;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.core.service.statistics.vanilla.privacy.UuidAnonymizer;
import com.raindropcentral.core.service.statistics.vanilla.version.StatisticAvailabilityChecker;
import com.raindropcentral.core.service.statistics.vanilla.version.StatisticMapper;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core collector for vanilla Minecraft statistics.
 *
 * <p>This class orchestrates all category-specific collectors and provides methods for
 * collecting statistics by category or all categories at once. It applies filtering
 * based on configuration settings including:
 * <ul>
 *   <li>Category enable/disable</li>
 *   <li>Material/entity whitelist/blacklist</li>
 *   <li>Excluded statistics for privacy</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * VanillaStatisticCollector collector = new VanillaStatisticCollector(
 *     config, availabilityChecker, mapper
 * );
 * 
 * // Collect all statistics for a player
 * List<QueuedStatistic> allStats = collector.collectAllForPlayer(player);
 * 
 * // Collect specific category
 * List<QueuedStatistic> blockStats = collector.collectForPlayer(player, StatisticCategory.BLOCKS);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class VanillaStatisticCollector {

    private static final Logger LOGGER = Logger.getLogger(VanillaStatisticCollector.class.getName());

    private final VanillaStatisticConfig config;
    private final StatisticAvailabilityChecker availabilityChecker;
    private final StatisticMapper mapper;

    // Category-specific collectors
    private final BlockStatisticCollector blockCollector;
    private final ItemStatisticCollector itemCollector;
    private final MobStatisticCollector mobCollector;
    private final TravelStatisticCollector travelCollector;
    private final GeneralStatisticCollector generalCollector;
    private final InteractionStatisticCollector interactionCollector;

    /**
     * Constructs a new vanilla statistic collector.
     *
     * @param config the configuration
     * @param availabilityChecker the availability checker
     * @param mapper the statistic mapper
     */
    public VanillaStatisticCollector(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticAvailabilityChecker availabilityChecker,
        final @NotNull StatisticMapper mapper
    ) {
        this.config = config;
        this.availabilityChecker = availabilityChecker;
        this.mapper = mapper;

        // Initialize category-specific collectors
        this.blockCollector = new BlockStatisticCollector(config, availabilityChecker, mapper);
        this.itemCollector = new ItemStatisticCollector(config, availabilityChecker, mapper);
        this.mobCollector = new MobStatisticCollector(config, availabilityChecker, mapper);
        this.travelCollector = new TravelStatisticCollector(config, availabilityChecker, mapper);
        this.generalCollector = new GeneralStatisticCollector(config, availabilityChecker, mapper);
        this.interactionCollector = new InteractionStatisticCollector(config, availabilityChecker, mapper);

        LOGGER.fine("Initialized VanillaStatisticCollector with " + 
                   config.getEnabledCategories().size() + " enabled categories");
    }

    /**
     * Collects statistics for a specific category from a player.
     *
     * <p>This method delegates to the appropriate category-specific collector
     * and applies filtering based on configuration.
     *
     * @param player the player to collect statistics from
     * @param category the category to collect
     * @return list of queued statistics, empty if category is disabled
     */
    public @NotNull List<QueuedStatistic> collectForPlayer(
        final @NotNull Player player,
        final @NotNull StatisticCategory category
    ) {
        // Check if category is enabled
        if (!config.isCategoryEnabled(category)) {
            LOGGER.fine("Category " + category + " is disabled, skipping collection");
            return List.of();
        }

        final List<QueuedStatistic> statistics = switch (category) {
            case BLOCKS -> blockCollector.collectBlockStatistics(player);
            case ITEMS -> itemCollector.collectItemStatistics(player);
            case MOBS -> mobCollector.collectMobStatistics(player);
            case TRAVEL -> travelCollector.collectTravelStatistics(player);
            case GENERAL -> generalCollector.collectGeneralStatistics(player);
            case INTERACTIONS -> interactionCollector.collectInteractionStatistics(player);
        };

        // Apply privacy filtering
        return applyPrivacyFiltering(statistics);
    }

    /**
     * Collects all enabled statistics from a player.
     *
     * <p>This method collects statistics from all enabled categories and combines
     * them into a single list. Categories are processed in the order defined by
     * the {@link StatisticCategory} enum.
     *
     * @param player the player to collect statistics from
     * @return list of all queued statistics from enabled categories
     */
    public @NotNull List<QueuedStatistic> collectAllForPlayer(final @NotNull Player player) {
        final List<QueuedStatistic> allStatistics = new ArrayList<>();

        for (final StatisticCategory category : StatisticCategory.values()) {
            if (config.isCategoryEnabled(category)) {
                final List<QueuedStatistic> categoryStats = collectForPlayer(player, category);
                allStatistics.addAll(categoryStats);
            }
        }

        LOGGER.fine("Collected " + allStatistics.size() + " statistics for player " + 
                   player.getName() + " across " + config.getEnabledCategories().size() + " categories");

        return allStatistics;
    }

    /**
     * Applies privacy filtering to collected statistics.
     *
     * <p>This method removes statistics that are marked as excluded in the
     * privacy configuration and anonymizes UUIDs if configured. Excluded 
     * statistics are typically sensitive information like LEAVE_GAME or 
     * TIME_SINCE_REST.
     *
     * @param statistics the statistics to filter
     * @return filtered list of statistics with privacy protections applied
     */
    private @NotNull List<QueuedStatistic> applyPrivacyFiltering(
        final @NotNull List<QueuedStatistic> statistics
    ) {
        if (!config.isPrivacyModeEnabled()) {
            return statistics;
        }

        final List<QueuedStatistic> filtered = new ArrayList<>();
        int excludedCount = 0;
        final boolean anonymizeUuids = config.isAnonymizeUuids();

        for (final QueuedStatistic statistic : statistics) {
            final String key = statistic.statisticKey();
            
            // Extract statistic name from key (e.g., "minecraft.general.deaths" -> "DEATHS")
            final String statisticName = extractStatisticName(key);
            
            if (!config.isStatisticExcluded(statisticName)) {
                // Apply UUID anonymization if enabled
                if (anonymizeUuids) {
                    final UUID originalUuid = statistic.playerUuid();
                    final UUID anonymizedUuid = UuidAnonymizer.anonymizeUuid(originalUuid);
                    
                    // Create new statistic with anonymized UUID
                    final QueuedStatistic anonymizedStatistic = new QueuedStatistic(
                        anonymizedUuid,
                        statistic.statisticKey(),
                        statistic.value(),
                        statistic.dataType(),
                        statistic.collectionTimestamp(),
                        statistic.priority(),
                        statistic.isDelta(),
                        statistic.sourcePlugin()
                    );
                    filtered.add(anonymizedStatistic);
                } else {
                    filtered.add(statistic);
                }
            } else {
                excludedCount++;
            }
        }

        if (excludedCount > 0) {
            LOGGER.fine("Excluded " + excludedCount + " statistics due to privacy settings");
        }
        
        if (anonymizeUuids && !filtered.isEmpty()) {
            LOGGER.fine("Anonymized UUIDs for " + filtered.size() + " statistics");
        }

        return filtered;
    }

    /**
     * Extracts the statistic name from a statistic key.
     *
     * <p>Converts keys like "minecraft.general.deaths" to "DEATHS" for
     * comparison with excluded statistics configuration.
     *
     * @param key the statistic key
     * @return the extracted statistic name in uppercase
     */
    private @NotNull String extractStatisticName(final @NotNull String key) {
        // Handle aggregate keys (e.g., "minecraft.blocks.total_mined")
        if (key.contains("total_")) {
            return key.substring(key.lastIndexOf('.') + 1).toUpperCase();
        }

        // Extract the action part for regular statistics
        final String[] parts = key.split("\\.");
        if (parts.length >= 3) {
            final String action = parts[2];
            
            // Try to map back to Bukkit statistic name
            return switch (action) {
                case "mined" -> "MINE_BLOCK";
                case "used" -> "USE_ITEM";
                case "broken" -> "BREAK_ITEM";
                case "crafted" -> "CRAFT_ITEM";
                case "picked_up" -> "PICKUP";
                case "dropped" -> "DROP";
                case "killed" -> "KILL_ENTITY";
                case "killed_by" -> "ENTITY_KILLED_BY";
                case "walk" -> "WALK_ONE_CM";
                case "sprint" -> "SPRINT_ONE_CM";
                case "crouch" -> "CROUCH_ONE_CM";
                case "swim" -> "SWIM_ONE_CM";
                case "fly" -> "FLY_ONE_CM";
                case "climb" -> "CLIMB_ONE_CM";
                case "fall" -> "FALL_ONE_CM";
                case "deaths" -> "DEATHS";
                case "jumps" -> "JUMP";
                default -> action.toUpperCase();
            };
        }

        return key.toUpperCase();
    }

    /**
     * Gets the configuration used by this collector.
     *
     * @return the configuration
     */
    public @NotNull VanillaStatisticConfig getConfig() {
        return config;
    }

    /**
     * Gets the availability checker used by this collector.
     *
     * @return the availability checker
     */
    public @NotNull StatisticAvailabilityChecker getAvailabilityChecker() {
        return availabilityChecker;
    }

    /**
     * Gets the statistic mapper used by this collector.
     *
     * @return the statistic mapper
     */
    public @NotNull StatisticMapper getMapper() {
        return mapper;
    }
}
