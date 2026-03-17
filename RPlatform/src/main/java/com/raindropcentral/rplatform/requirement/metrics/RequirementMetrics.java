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

package com.raindropcentral.rplatform.requirement.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics tracking for requirement operations.
 *
 * <p>Tracks performance, success rates, and usage statistics.
 */
public final class RequirementMetrics {

    private static final RequirementMetrics INSTANCE = new RequirementMetrics();

    private final Map<String, TypeMetrics> metricsByType = new ConcurrentHashMap<>();
    private final LongAdder totalChecks = new LongAdder();
    private final LongAdder totalConsumes = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();

    private RequirementMetrics() {}

    /**
     * Gets instance.
     */
    @NotNull
    public static RequirementMetrics getInstance() {
        return INSTANCE;
    }

    // ==================== Recording ====================

    /**
     * Executes recordCheck.
     */
    public void recordCheck(@NotNull String typeId, long durationNanos, boolean met) {
        totalChecks.increment();
        getOrCreateMetrics(typeId).recordCheck(durationNanos, met);
    }

    /**
     * Executes recordConsume.
     */
    public void recordConsume(@NotNull String typeId, long durationNanos) {
        totalConsumes.increment();
        getOrCreateMetrics(typeId).recordConsume(durationNanos);
    }

    /**
     * Executes recordError.
     */
    public void recordError(@NotNull String typeId) {
        totalErrors.increment();
        getOrCreateMetrics(typeId).recordError();
    }

    // ==================== Retrieval ====================

    /**
     * Gets metrics.
     */
    @NotNull
    public TypeMetrics getMetrics(@NotNull String typeId) {
        return metricsByType.getOrDefault(typeId, new TypeMetrics());
    }

    /**
     * Gets allMetrics.
     */
    @NotNull
    public Map<String, TypeMetrics> getAllMetrics() {
        return Map.copyOf(metricsByType);
    }

    /**
     * Gets totalChecks.
     */
    public long getTotalChecks() {
        return totalChecks.sum();
    }

    /**
     * Gets totalConsumes.
     */
    public long getTotalConsumes() {
        return totalConsumes.sum();
    }

    /**
     * Gets totalErrors.
     */
    public long getTotalErrors() {
        return totalErrors.sum();
    }

    /**
     * Executes reset.
     */
    public void reset() {
        metricsByType.clear();
        totalChecks.reset();
        totalConsumes.reset();
        totalErrors.reset();
    }

    // ==================== Internal ====================

    private TypeMetrics getOrCreateMetrics(String typeId) {
        return metricsByType.computeIfAbsent(typeId, k -> new TypeMetrics());
    }

    /**
     * Metrics for a specific requirement type.
     */
    public static final class TypeMetrics {
        private final LongAdder checkCount = new LongAdder();
        private final LongAdder metCount = new LongAdder();
        private final LongAdder consumeCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong totalCheckTimeNanos = new AtomicLong();
        private final AtomicLong totalConsumeTimeNanos = new AtomicLong();

        void recordCheck(long durationNanos, boolean met) {
            checkCount.increment();
            if (met) metCount.increment();
            totalCheckTimeNanos.addAndGet(durationNanos);
        }

        void recordConsume(long durationNanos) {
            consumeCount.increment();
            totalConsumeTimeNanos.addAndGet(durationNanos);
        }

        void recordError() {
            errorCount.increment();
        }

        /**
         * Gets checkCount.
         */
        public long getCheckCount() {
            return checkCount.sum();
        }

        /**
         * Gets metCount.
         */
        public long getMetCount() {
            return metCount.sum();
        }

        /**
         * Gets consumeCount.
         */
        public long getConsumeCount() {
            return consumeCount.sum();
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
            long checks = checkCount.sum();
            return checks > 0 ? (double) metCount.sum() / checks : 0.0;
        }

        /**
         * Gets averageCheckTimeMs.
         */
        public double getAverageCheckTimeMs() {
            long checks = checkCount.sum();
            return checks > 0 ? totalCheckTimeNanos.get() / (double) checks / 1_000_000 : 0.0;
        }

        /**
         * Gets averageConsumeTimeMs.
         */
        public double getAverageConsumeTimeMs() {
            long consumes = consumeCount.sum();
            return consumes > 0 ? totalConsumeTimeNanos.get() / (double) consumes / 1_000_000 : 0.0;
        }

        /**
         * Executes toString.
         */
        @Override
        public String toString() {
            return String.format(
                "Checks: %d (%.1f%% met, avg %.2fms), Consumes: %d (avg %.2fms), Errors: %d",
                getCheckCount(),
                getSuccessRate() * 100,
                getAverageCheckTimeMs(),
                getConsumeCount(),
                getAverageConsumeTimeMs(),
                getErrorCount()
            );
        }
    }
}
