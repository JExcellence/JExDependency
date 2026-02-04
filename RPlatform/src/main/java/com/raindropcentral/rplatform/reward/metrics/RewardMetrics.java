package com.raindropcentral.rplatform.reward.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class RewardMetrics {

    private static final RewardMetrics INSTANCE = new RewardMetrics();

    private final Map<String, TypeMetrics> typeMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalGrants = new LongAdder();
    private final LongAdder totalSuccesses = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final AtomicLong totalDurationNanos = new AtomicLong(0);

    private RewardMetrics() {}

    public static RewardMetrics getInstance() {
        return INSTANCE;
    }

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

    public void recordGrant(long durationNanos, boolean success) {
        totalGrants.increment();
        totalDurationNanos.addAndGet(durationNanos);
        
        if (success) {
            totalSuccesses.increment();
        } else {
            totalFailures.increment();
        }
    }

    public void recordError(@NotNull String typeId) {
        totalFailures.increment();
        getOrCreateMetrics(typeId).recordError();
    }

    public void recordError() {
        totalFailures.increment();
    }

    private TypeMetrics getOrCreateMetrics(String typeId) {
        return typeMetrics.computeIfAbsent(typeId, k -> new TypeMetrics());
    }

    public TypeMetrics getMetrics(@NotNull String typeId) {
        return typeMetrics.getOrDefault(typeId, new TypeMetrics());
    }

    public Map<String, TypeMetrics> getAllMetrics() {
        return Map.copyOf(typeMetrics);
    }

    public long getTotalGrants() {
        return totalGrants.sum();
    }

    public long getTotalSuccesses() {
        return totalSuccesses.sum();
    }

    public long getTotalFailures() {
        return totalFailures.sum();
    }

    public double getSuccessRate() {
        long total = getTotalGrants();
        return total > 0 ? (double) getTotalSuccesses() / total : 0.0;
    }

    public double getAverageGrantTimeMs() {
        long total = getTotalGrants();
        return total > 0 ? totalDurationNanos.get() / 1_000_000.0 / total : 0.0;
    }

    public void reset() {
        typeMetrics.clear();
        totalGrants.reset();
        totalSuccesses.reset();
        totalFailures.reset();
        totalDurationNanos.set(0);
    }

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

        public long getGrantCount() {
            return grantCount.sum();
        }

        public long getSuccessCount() {
            return successCount.sum();
        }

        public long getErrorCount() {
            return errorCount.sum();
        }

        public double getSuccessRate() {
            long total = getGrantCount();
            return total > 0 ? (double) getSuccessCount() / total : 0.0;
        }

        public double getAverageGrantTimeMs() {
            long total = getGrantCount();
            return total > 0 ? totalDurationNanos.get() / 1_000_000.0 / total : 0.0;
        }
    }
}
