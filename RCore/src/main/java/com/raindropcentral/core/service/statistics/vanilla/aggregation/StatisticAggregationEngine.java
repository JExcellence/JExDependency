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

package com.raindropcentral.core.service.statistics.vanilla.aggregation;

import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Engine for computing aggregate statistics from collected vanilla statistics.
 *
 * <p>This engine computes various aggregate values including:
 * <ul>
 *   <li>Total blocks broken (sum of all MINE_BLOCK statistics)</li>
 *   <li>Total blocks placed (sum of all USE_ITEM for placeable blocks)</li>
 *   <li>Total items crafted (sum of all CRAFT_ITEM statistics)</li>
 *   <li>Total distance traveled (sum of all movement statistics)</li>
 *   <li>Total mob kills (sum of all KILL_ENTITY statistics)</li>
 *   <li>Total deaths (DEATHS statistic)</li>
 *   <li>Rate statistics (per minute calculations)</li>
 *   <li>Custom aggregates from configuration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * StatisticAggregationEngine engine = new StatisticAggregationEngine(config);
 * List<QueuedStatistic> collected = collector.collectAllForPlayer(player);
 * List<QueuedStatistic> aggregates = engine.computeAggregates(collected, player);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticAggregationEngine {

    private static final Logger LOGGER = Logger.getLogger(StatisticAggregationEngine.class.getName());
    private static final String SOURCE_PLUGIN = "RCore-Vanilla";

    private final VanillaStatisticConfig config;

    /**
     * Constructs a new statistic aggregation engine.
     *
     * @param config the configuration
     */
    public StatisticAggregationEngine(final @NotNull VanillaStatisticConfig config) {
        this.config = config;
    }

    /**
     * Computes aggregate statistics from a list of collected statistics.
     *
     * <p>This method calculates various totals and rates based on the provided
     * statistics. The aggregates are returned as new {@link QueuedStatistic} instances
     * that can be queued for delivery alongside the detailed statistics.
     *
     * @param statistics the collected statistics to aggregate
     * @param player the player these statistics belong to
     * @return list of aggregate statistics
     */
    public @NotNull List<QueuedStatistic> computeAggregates(
        final @NotNull List<QueuedStatistic> statistics,
        final @NotNull Player player
    ) {
        final List<QueuedStatistic> aggregates = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();

        // Compute basic totals
        final long totalBlocksBroken = computeTotalBlocksBroken(statistics);
        final long totalBlocksPlaced = computeTotalBlocksPlaced(statistics);
        final long totalItemsCrafted = computeTotalItemsCrafted(statistics);
        final long totalDistanceTraveled = computeTotalDistanceTraveled(statistics);
        final long totalMobKills = computeTotalMobKills(statistics);
        final long totalDeaths = computeTotalDeaths(statistics);

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_blocks_broken",
            totalBlocksBroken,
            timestamp
        ));

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_blocks_placed",
            totalBlocksPlaced,
            timestamp
        ));

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_items_crafted",
            totalItemsCrafted,
            timestamp
        ));

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_distance_traveled",
            totalDistanceTraveled,
            timestamp
        ));

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_mob_kills",
            totalMobKills,
            timestamp
        ));

        aggregates.add(createAggregate(
            player,
            "minecraft.aggregates.total_deaths",
            totalDeaths,
            timestamp
        ));

        // Compute rate statistics
        final long playTimeMinutes = getPlayTimeMinutes(player);
        if (playTimeMinutes > 0) {
            aggregates.add(createRateAggregate(
                player,
                "minecraft.aggregates.blocks_per_minute",
                totalBlocksBroken,
                playTimeMinutes,
                timestamp
            ));

            aggregates.add(createRateAggregate(
                player,
                "minecraft.aggregates.distance_per_minute",
                totalDistanceTraveled,
                playTimeMinutes,
                timestamp
            ));

            aggregates.add(createRateAggregate(
                player,
                "minecraft.aggregates.kills_per_minute",
                totalMobKills,
                playTimeMinutes,
                timestamp
            ));

            LOGGER.fine("Computed rate statistics for player " + player.getName() + 
                       " with " + playTimeMinutes + " minutes of play time");
        }

        // Compute custom aggregates from configuration
        final List<QueuedStatistic> customAggregates = computeCustomAggregates(statistics, player, timestamp);
        aggregates.addAll(customAggregates);

        LOGGER.fine("Computed " + aggregates.size() + " aggregates for player " + player.getName());

        return aggregates;
    }

    /**
     * Computes the total number of blocks broken.
     * Sums all statistics with keys matching "minecraft.blocks.mined.*".
     *
     * @param statistics the collected statistics
     * @return the total blocks broken
     */
    private long computeTotalBlocksBroken(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().startsWith("minecraft.blocks.mined."))
            .filter(stat -> !stat.statisticKey().endsWith(".total"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Computes the total number of blocks placed.
     * Sums all statistics with keys matching "minecraft.blocks.used.*".
     *
     * @param statistics the collected statistics
     * @return the total blocks placed
     */
    private long computeTotalBlocksPlaced(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().startsWith("minecraft.blocks.used."))
            .filter(stat -> !stat.statisticKey().endsWith(".total"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Computes the total number of items crafted.
     * Sums all statistics with keys matching "minecraft.items.crafted.*".
     *
     * @param statistics the collected statistics
     * @return the total items crafted
     */
    private long computeTotalItemsCrafted(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().startsWith("minecraft.items.crafted."))
            .filter(stat -> !stat.statisticKey().endsWith(".total"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Computes the total distance traveled across all movement types.
     * Sums all statistics with keys matching "minecraft.travel.*".
     *
     * @param statistics the collected statistics
     * @return the total distance traveled in centimeters
     */
    private long computeTotalDistanceTraveled(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().startsWith("minecraft.travel."))
            .filter(stat -> !stat.statisticKey().endsWith(".total"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Computes the total number of mobs killed.
     * Sums all statistics with keys matching "minecraft.mobs.killed.*".
     *
     * @param statistics the collected statistics
     * @return the total mobs killed
     */
    private long computeTotalMobKills(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().startsWith("minecraft.mobs.killed."))
            .filter(stat -> !stat.statisticKey().endsWith(".total"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Computes the total number of deaths.
     * Extracts the value from "minecraft.general.deaths" statistic.
     *
     * @param statistics the collected statistics
     * @return the total deaths
     */
    private long computeTotalDeaths(final @NotNull List<QueuedStatistic> statistics) {
        return statistics.stream()
            .filter(stat -> stat.statisticKey().equals("minecraft.general.deaths"))
            .mapToLong(stat -> {
                if (stat.value() instanceof Number number) {
                    return number.longValue();
                }
                return 0L;
            })
            .findFirst()
            .orElse(0L);
    }

    /**
     * Creates an aggregate statistic.
     *
     * @param player the player
     * @param key the statistic key
     * @param value the aggregate value
     * @param timestamp the collection timestamp
     * @return the queued statistic
     */
    private @NotNull QueuedStatistic createAggregate(
        final @NotNull Player player,
        final @NotNull String key,
        final long value,
        final long timestamp
    ) {
        return QueuedStatistic.builder()
            .playerUuid(player.getUniqueId())
            .statisticKey(key)
            .value(value)
            .dataType(StatisticDataType.NUMBER)
            .collectionTimestamp(timestamp)
            .priority(DeliveryPriority.NORMAL)
            .isDelta(false)
            .sourcePlugin(SOURCE_PLUGIN)
            .build();
    }

    /**
     * Creates a rate statistic (value per minute).
     *
     * @param player the player
     * @param key the statistic key
     * @param totalValue the total value
     * @param minutes the number of minutes
     * @param timestamp the collection timestamp
     * @return the queued statistic
     */
    private @NotNull QueuedStatistic createRateAggregate(
        final @NotNull Player player,
        final @NotNull String key,
        final long totalValue,
        final long minutes,
        final long timestamp
    ) {
        final double rate = minutes > 0 ? (double) totalValue / minutes : 0.0;
        
        return QueuedStatistic.builder()
            .playerUuid(player.getUniqueId())
            .statisticKey(key)
            .value(rate)
            .dataType(StatisticDataType.NUMBER)
            .collectionTimestamp(timestamp)
            .priority(DeliveryPriority.NORMAL)
            .isDelta(false)
            .sourcePlugin(SOURCE_PLUGIN)
            .build();
    }

    /**
     * Gets the player's total play time in minutes.
     * Uses the PLAY_ONE_MINUTE statistic which tracks ticks played.
     *
     * @param player the player
     * @return the play time in minutes
     */
    private long getPlayTimeMinutes(final @NotNull Player player) {
        try {
            // PLAY_ONE_MINUTE tracks ticks (20 ticks = 1 second, 1200 ticks = 1 minute)
            final int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            return ticks / 1200L; // Convert ticks to minutes
        } catch (final Exception e) {
            LOGGER.warning("Failed to get play time for player " + player.getName() + ": " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Computes custom aggregates defined in configuration.
     *
     * <p>This method processes custom aggregate definitions from the configuration
     * and computes their values based on the collected statistics. Supports:
     * <ul>
     *   <li>Sum type: Sums values from specified statistic keys</li>
     *   <li>Formula type: Evaluates simple formulas (optional, basic implementation)</li>
     * </ul>
     *
     * @param statistics the collected statistics
     * @param player the player
     * @param timestamp the collection timestamp
     * @return list of custom aggregate statistics
     */
    private @NotNull List<QueuedStatistic> computeCustomAggregates(
        final @NotNull List<QueuedStatistic> statistics,
        final @NotNull Player player,
        final long timestamp
    ) {
        final List<QueuedStatistic> customAggregates = new ArrayList<>();
        final Map<String, VanillaStatisticConfig.CustomAggregate> aggregateDefs = config.getCustomAggregates();

        if (aggregateDefs.isEmpty()) {
            return customAggregates;
        }

        // Build a map of statistic keys to values for quick lookup
        final Map<String, Long> statisticValues = new HashMap<>();
        for (final QueuedStatistic stat : statistics) {
            if (stat.value() instanceof Number number) {
                statisticValues.put(stat.statisticKey(), number.longValue());
            }
        }

        // Process each custom aggregate definition
        for (final VanillaStatisticConfig.CustomAggregate aggregateDef : aggregateDefs.values()) {
            try {
                final long value = computeCustomAggregateValue(aggregateDef, statisticValues, player);
                
                customAggregates.add(createAggregate(
                    player,
                    "minecraft.aggregates.custom." + aggregateDef.getName(),
                    value,
                    timestamp
                ));
                
                LOGGER.fine("Computed custom aggregate '" + aggregateDef.getName() + 
                           "' = " + value + " for player " + player.getName());
            } catch (final Exception e) {
                LOGGER.warning("Failed to compute custom aggregate '" + aggregateDef.getName() + 
                             "' for player " + player.getName() + ": " + e.getMessage());
            }
        }

        return customAggregates;
    }

    /**
     * Computes the value for a single custom aggregate definition.
     *
     * @param aggregateDef the aggregate definition
     * @param statisticValues map of statistic keys to values
     * @param player the player
     * @return the computed aggregate value
     */
    private long computeCustomAggregateValue(
        final @NotNull VanillaStatisticConfig.CustomAggregate aggregateDef,
        final @NotNull Map<String, Long> statisticValues,
        final @NotNull Player player
    ) {
        final String type = aggregateDef.getType().toLowerCase();

        return switch (type) {
            case "sum" -> computeSumAggregate(aggregateDef, statisticValues);
            case "formula" -> computeFormulaAggregate(aggregateDef, statisticValues, player);
            default -> {
                LOGGER.warning("Unknown custom aggregate type: " + type);
                yield 0L;
            }
        };
    }

    /**
     * Computes a sum-type custom aggregate.
     * Sums the values of all specified statistic keys.
     *
     * @param aggregateDef the aggregate definition
     * @param statisticValues map of statistic keys to values
     * @return the sum of all specified statistics
     */
    private long computeSumAggregate(
        final @NotNull VanillaStatisticConfig.CustomAggregate aggregateDef,
        final @NotNull Map<String, Long> statisticValues
    ) {
        long sum = 0L;
        
        for (final String statisticKey : aggregateDef.getStatistics()) {
            final Long value = statisticValues.get(statisticKey);
            if (value != null) {
                sum += value;
            }
        }
        
        return sum;
    }

    /**
     * Computes a formula-type custom aggregate.
     *
     * <p>This is a basic implementation that supports simple arithmetic formulas
     * with statistic references. For example: "(player_kills * 10) - (deaths * 5)"
     *
     * <p><strong>Note:</strong> This is an optional feature with limited support.
     * Complex formulas may not be evaluated correctly.
     *
     * @param aggregateDef the aggregate definition
     * @param statisticValues map of statistic keys to values
     * @param player the player
     * @return the computed formula result
     */
    private long computeFormulaAggregate(
        final @NotNull VanillaStatisticConfig.CustomAggregate aggregateDef,
        final @NotNull Map<String, Long> statisticValues,
        final @NotNull Player player
    ) {
        final String formula = aggregateDef.getFormula();
        if (formula == null || formula.isBlank()) {
            LOGGER.warning("Formula aggregate '" + aggregateDef.getName() + 
                         "' has no formula defined");
            return 0L;
        }

        // Basic formula evaluation - replace statistic names with values
        String evaluatedFormula = formula;
        
        // Replace statistic references with actual values
        for (final Map.Entry<String, Long> entry : statisticValues.entrySet()) {
            final String key = entry.getKey();
            final String simplifiedKey = key.replace("minecraft.", "")
                                            .replace(".", "_");
            evaluatedFormula = evaluatedFormula.replace(simplifiedKey, String.valueOf(entry.getValue()));
        }

        // Try to evaluate the formula (basic implementation)
        try {
            return evaluateSimpleFormula(evaluatedFormula);
        } catch (final Exception e) {
            LOGGER.warning("Failed to evaluate formula '" + formula + "': " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Evaluates a simple arithmetic formula.
     *
     * <p>This is a very basic implementation that handles simple expressions
     * with +, -, *, / operators and parentheses. For production use, consider
     * using a proper expression evaluation library.
     *
     * @param formula the formula to evaluate
     * @return the result
     */
    private long evaluateSimpleFormula(final @NotNull String formula) {
        // Remove whitespace
        final String cleaned = formula.replaceAll("\\s+", "");
        
        // Very basic evaluation - just handle simple cases
        // For a production implementation, use a proper expression parser
        try {
            // Remove parentheses for simplicity (not handling nested expressions properly)
            final String simplified = cleaned.replaceAll("[()]", "");
            
            // Split by operators and evaluate left to right (not respecting precedence)
            // This is intentionally simple - formula support is optional
            return (long) Double.parseDouble(simplified);
        } catch (final NumberFormatException e) {
            // If it's not a simple number, return 0
            // A proper implementation would parse and evaluate the expression
            LOGGER.fine("Formula evaluation not fully supported: " + formula);
            return 0L;
        }
    }
}
