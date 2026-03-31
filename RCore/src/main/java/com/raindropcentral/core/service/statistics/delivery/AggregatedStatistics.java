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

package com.raindropcentral.core.service.statistics.delivery;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-computed aggregate statistics included with batch payloads.
 * Provides summary information for dashboards without requiring backend computation.
 *
 * @param timestamp             when these aggregates were computed
 * @param totalPlayersTracked   total unique players with statistics
 * @param averagePlaytimeMs     average playtime across all players in milliseconds
 * @param totalEconomyVolume    total economy transaction volume
 * @param totalQuestCompletions total quests completed
 * @param customAggregates      additional custom aggregate values
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record AggregatedStatistics(
    long timestamp,
    int totalPlayersTracked,
    double averagePlaytimeMs,
    double totalEconomyVolume,
    int totalQuestCompletions,
    @NotNull Map<String, Object> customAggregates
) {

    /**
     * Creates empty aggregated statistics.
     *
     * @return empty aggregated statistics with current timestamp
     */
    public static AggregatedStatistics empty() {
        return new AggregatedStatistics(
            System.currentTimeMillis(),
            0, 0.0, 0.0, 0,
            Map.of()
        );
    }

    /**
     * Creates a builder for AggregatedStatistics.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for AggregatedStatistics.
     */
    public static class Builder {
        private long timestamp = System.currentTimeMillis();
        private int totalPlayersTracked = 0;
        private double averagePlaytimeMs = 0.0;
        private double totalEconomyVolume = 0.0;
        private int totalQuestCompletions = 0;
        private final Map<String, Object> customAggregates = new HashMap<>();

        /**
         * Executes timestamp.
         */
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        /**
         * Executes totalPlayersTracked.
         */
        public Builder totalPlayersTracked(int count) { this.totalPlayersTracked = count; return this; }
        /**
         * Executes method.
         */
        /**
         * Executes this member.
         */
        /**
         * Executes averagePlaytimeMs.
         */
        public Builder averagePlaytimeMs(double ms) { this.averagePlaytimeMs = ms; return this; }
        /**
         * Executes totalEconomyVolume.
         */
        public Builder totalEconomyVolume(double volume) { this.totalEconomyVolume = volume; return this; }
        /**
         * Executes totalQuestCompletions.
         */
        public Builder totalQuestCompletions(int count) { this.totalQuestCompletions = count; return this; }
        /**
         * Executes customAggregate.
         */
        public Builder customAggregate(String key, Object value) { this.customAggregates.put(key, value); return this; }
        /**
         * Executes customAggregates.
         */
        public Builder customAggregates(Map<String, Object> aggregates) { this.customAggregates.putAll(aggregates); return this; }

        /**
         * Executes build.
         */
        public AggregatedStatistics build() {
            return new AggregatedStatistics(
                timestamp, totalPlayersTracked, averagePlaytimeMs,
                totalEconomyVolume, totalQuestCompletions,
                Map.copyOf(customAggregates)
            );
        }
    }
}
