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

package com.raindropcentral.rplatform.reward.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Represents the RewardMetrics API type.
 */
public final class RewardMetrics {

    private static final RewardMetrics INSTANCE = new RewardMetrics();

    private final Map<String, TypeMetrics> typeMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalGrants = new LongAdder();
    private final LongAdder totalSuccesses = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final AtomicLong totalDurationNanos = new AtomicLong(0);

    private RewardMetrics() {}

    /**
     * Gets instance.
     */
    public static RewardMetrics getInstance() {
        return INSTANCE;
    }

    /**
     * Executes recordGrant.
     */
    public void recordGrant(@NotNull String typeId, long durationNanos, boolean success) {
        totalGrants.increment();
        totalDurationNanos.addAndGet(durationNanos);
        
        if (success) {
            totalSuccesses.increment();
        } else {
            totalFailures.increment();
        }

        getOrCreateMetrics(typeId).recordGrant(durationNanos, success);
    }

    /**
     * Executes recordGrant.
     */
    public void recordGrant(long durationNanos, boolean success) {
        totalGrants.increment();
        totalDurationNanos.addAndGet(durationNanos);
        
        if (success) {
            totalSuccesses.increment();
        } else {
            totalFailures.increment();
        }
    }

    /**
     * Executes recordError.
     */
    public void recordError(@NotNull String typeId) {
        totalFailures.increment();
        getOrCreateMetrics(typeId).recordError();
    }

    /**
     * Executes recordError.
     */
    public void recordError() {
        totalFailures.increment();
    }

    private TypeMetrics getOrCreateMetrics(String typeId) {
        return typeMetrics.computeIfAbsent(typeId, k -> new TypeMetrics());
    }

    /**
     * Gets metrics.
     */
    public TypeMetrics getMetrics(@NotNull String typeId) {
        return typeMetrics.getOrDefault(typeId, new TypeMetrics());
    }

    /**
     * Gets allMetrics.
     */
    public Map<String, TypeMetrics> getAllMetrics() {
        return Map.copyOf(typeMetrics);
    }

    /**
     * Gets totalGrants.
     */
    public long getTotalGrants() {
        return totalGrants.sum();
    }

    /**
     * Gets totalSuccesses.
     */
    public long getTotalSuccesses() {
        return totalSuccesses.sum();
    }

    /**
     * Gets totalFailures.
     */
    public long getTotalFailures() {
        return totalFailures.sum();
    }

    /**
     * Gets successRate.
     */
    public double getSuccessRate() {
        long total = getTotalGrants();
        return total > 0 ? (double) getTotalSuccesses() / total : 0.0;
    }

    /**
     * Gets averageGrantTimeMs.
     */
    public double getAverageGrantTimeMs() {
        long total = getTotalGrants();
        return total > 0 ? totalDurationNanos.get() / 1_000_000.0 / total : 0.0;
    }

    /**
     * Executes reset.
     */
    public void reset() {
        typeMetrics.clear();
        totalGrants.reset();
        totalSuccesses.reset();
        totalFailures.reset();
        totalDurationNanos.set(0);
    }

    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return String.format(
            "RewardMetrics{grants=%d, successes=%d, failures=%d, successRate=%.2f%%, avgTime=%.2fms}",
            getTotalGrants(),
            getTotalSuccesses(),
            getTotalFailures(),
            getSuccessRate() * 100,
            getAverageGrantTimeMs()
        );
    }

    /**
     * Represents the TypeMetrics API type.
     */
    public static class TypeMetrics {
        private final LongAdder grantCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong totalDurationNanos = new AtomicLong(0);

        void recordGrant(long durationNanos, boolean success) {
            grantCount.increment();
            totalDurationNanos.addAndGet(durationNanos);
            if (success) {
                successCount.increment();
            }
        }

        void recordError() {
            errorCount.increment();
        }

        /**
         * Gets grantCount.
         */
        public long getGrantCount() {
            return grantCount.sum();
        }

        /**
         * Gets successCount.
         */
        public long getSuccessCount() {
            return successCount.sum();
        }

        /**
         * Gets errorCount.
         */
        public long getErrorCount() {
            return errorCount.sum();
        }

        /**
         * Gets successRate.
         */
        public double getSuccessRate() {
            long total = getGrantCount();
            return total > 0 ? (double) getSuccessCount() / total : 0.0;
        }

        /**
         * Gets averageGrantTimeMs.
         */
        public double getAverageGrantTimeMs() {
            long total = getGrantCount();
            return total > 0 ? totalDurationNanos.get() / 1_000_000.0 / total : 0.0;
        }
    }
}
