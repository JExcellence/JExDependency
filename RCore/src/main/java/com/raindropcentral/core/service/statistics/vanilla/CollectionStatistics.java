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

/**
 * Represents aggregate statistics about vanilla statistics collection operations.
 * Tracks performance metrics and collection health over the lifetime of the service.
 *
 * @param totalCollections   the total number of collection operations performed
 * @param totalStatistics    the total number of statistics collected
 * @param averageDurationMs  the average duration of collection operations in milliseconds
 * @param cacheSize          the current size of the statistic cache (number of cached players)
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record CollectionStatistics(
    long totalCollections,
    long totalStatistics,
    long averageDurationMs,
    int cacheSize
) {

    /**
     * Creates a new CollectionStatistics with validation.
     */
    public CollectionStatistics {
        if (totalCollections < 0) {
            throw new IllegalArgumentException("totalCollections cannot be negative");
        }
        if (totalStatistics < 0) {
            throw new IllegalArgumentException("totalStatistics cannot be negative");
        }
        if (averageDurationMs < 0) {
            throw new IllegalArgumentException("averageDurationMs cannot be negative");
        }
        if (cacheSize < 0) {
            throw new IllegalArgumentException("cacheSize cannot be negative");
        }
    }

    /**
     * Gets the average number of statistics per collection.
     *
     * @return the average statistics per collection, or 0 if no collections
     */
    public double getAverageStatisticsPerCollection() {
        return totalCollections > 0 ? (double) totalStatistics / totalCollections : 0.0;
    }

    /**
     * Gets the average duration of collections.
     * Alias for averageDurationMs() for backward compatibility.
     *
     * @return the average duration in milliseconds
     */
    public long averageDuration() {
        return averageDurationMs;
    }

    /**
     * Checks if any collections have been performed.
     *
     * @return true if at least one collection has been performed
     */
    public boolean hasCollections() {
        return totalCollections > 0;
    }

    /**
     * Creates an empty CollectionStatistics instance.
     *
     * @return an empty statistics instance
     */
    public static CollectionStatistics empty() {
        return new CollectionStatistics(0, 0, 0, 0);
    }

    /**
     * Creates a builder for constructing CollectionStatistics instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating CollectionStatistics instances.
     */
    public static class Builder {
        private long totalCollections = 0;
        private long totalStatistics = 0;
        private long averageDurationMs = 0;
        private int cacheSize = 0;

        /**
         * Sets the total number of collections.
         *
         * @param totalCollections the total collections
         * @return this builder
         */
        public Builder totalCollections(final long totalCollections) {
            this.totalCollections = totalCollections;
            return this;
        }

        /**
         * Sets the total number of statistics.
         *
         * @param totalStatistics the total statistics
         * @return this builder
         */
        public Builder totalStatistics(final long totalStatistics) {
            this.totalStatistics = totalStatistics;
            return this;
        }

        /**
         * Sets the average duration.
         *
         * @param averageDurationMs the average duration in milliseconds
         * @return this builder
         */
        public Builder averageDurationMs(final long averageDurationMs) {
            this.averageDurationMs = averageDurationMs;
            return this;
        }

        /**
         * Sets the cache size.
         *
         * @param cacheSize the cache size
         * @return this builder
         */
        public Builder cacheSize(final int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        /**
         * Builds the CollectionStatistics.
         *
         * @return the collection statistics
         */
        public CollectionStatistics build() {
            return new CollectionStatistics(
                totalCollections,
                totalStatistics,
                averageDurationMs,
                cacheSize
            );
        }
    }
}
