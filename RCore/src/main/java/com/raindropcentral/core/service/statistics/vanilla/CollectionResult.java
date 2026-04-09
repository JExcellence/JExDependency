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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the result of a vanilla statistics collection operation.
 * Contains the collected statistics along with metadata about the collection.
 *
 * @param statistics          the list of collected statistics ready for queuing
 * @param collectionTimestamp the timestamp when collection started (milliseconds since epoch)
 * @param durationMs          the duration of the collection operation in milliseconds
 * @param playerCount         the number of players included in this collection
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record CollectionResult(
    @NotNull List<QueuedStatistic> statistics,
    long collectionTimestamp,
    long durationMs,
    int playerCount
) {

    /**
     * Creates a new CollectionResult with validation.
     */
    public CollectionResult {
        if (statistics == null) {
            throw new IllegalArgumentException("statistics cannot be null");
        }
        if (collectionTimestamp <= 0) {
            throw new IllegalArgumentException("collectionTimestamp must be positive");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        if (playerCount < 0) {
            throw new IllegalArgumentException("playerCount cannot be negative");
        }
        // Make defensive copy
        statistics = List.copyOf(statistics);
    }

    /**
     * Gets the number of statistics collected.
     *
     * @return the statistic count
     */
    public int getStatisticCount() {
        return statistics.size();
    }

    /**
     * Gets the number of statistics collected.
     * Alias for getStatisticCount() for backward compatibility.
     *
     * @return the statistic count
     */
    public int statisticsCollected() {
        return statistics.size();
    }

    /**
     * Gets the collection duration in milliseconds.
     * Alias for durationMs() for backward compatibility.
     *
     * @return the duration in milliseconds
     */
    public long collectionDurationMs() {
        return durationMs;
    }

    /**
     * Checks if the collection result is empty.
     *
     * @return true if no statistics were collected
     */
    public boolean isEmpty() {
        return statistics.isEmpty();
    }

    /**
     * Creates an empty collection result.
     *
     * @return an empty collection result
     */
    public static CollectionResult empty() {
        return new CollectionResult(
            List.of(),
            System.currentTimeMillis(),
            0L,
            0
        );
    }

    /**
     * Gets the average collection time per player in milliseconds.
     *
     * @return the average time per player, or 0 if no players
     */
    public double getAverageTimePerPlayer() {
        return playerCount > 0 ? (double) durationMs / playerCount : 0.0;
    }

    /**
     * Gets the average number of statistics per player.
     *
     * @return the average statistics per player, or 0 if no players
     */
    public double getAverageStatisticsPerPlayer() {
        return playerCount > 0 ? (double) statistics.size() / playerCount : 0.0;
    }

    /**
     * Creates a builder for constructing CollectionResult instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating CollectionResult instances.
     */
    public static class Builder {
        private List<QueuedStatistic> statistics = List.of();
        private long collectionTimestamp = System.currentTimeMillis();
        private long durationMs = 0;
        private int playerCount = 0;

        /**
         * Sets the collected statistics.
         *
         * @param statistics the statistics
         * @return this builder
         */
        public Builder statistics(final @NotNull List<QueuedStatistic> statistics) {
            this.statistics = statistics;
            return this;
        }

        /**
         * Sets the collection timestamp.
         *
         * @param collectionTimestamp the timestamp in milliseconds
         * @return this builder
         */
        public Builder collectionTimestamp(final long collectionTimestamp) {
            this.collectionTimestamp = collectionTimestamp;
            return this;
        }

        /**
         * Sets the collection duration.
         *
         * @param durationMs the duration in milliseconds
         * @return this builder
         */
        public Builder durationMs(final long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * Sets the player count.
         *
         * @param playerCount the number of players
         * @return this builder
         */
        public Builder playerCount(final int playerCount) {
            this.playerCount = playerCount;
            return this;
        }

        /**
         * Builds the CollectionResult.
         *
         * @return the collection result
         */
        public CollectionResult build() {
            return new CollectionResult(statistics, collectionTimestamp, durationMs, playerCount);
        }
    }
}
