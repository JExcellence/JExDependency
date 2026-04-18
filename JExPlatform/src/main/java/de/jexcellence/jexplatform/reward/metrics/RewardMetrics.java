package de.jexcellence.jexplatform.reward.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collector for reward operations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardMetrics {

    private final Map<String, TypeCounters> counters = new ConcurrentHashMap<>();

    /** Records a grant operation. */
    public void recordGrant(@NotNull String typeId, long durationNs, boolean success) {
        var c = counters.computeIfAbsent(typeId, k -> new TypeCounters());
        c.grants.increment();
        if (success) c.successes.increment(); else c.failures.increment();
        c.totalTimeNs.add(durationNs);
    }

    /** Records an error. */
    public void recordError(@NotNull String typeId) {
        counters.computeIfAbsent(typeId, k -> new TypeCounters()).errors.increment();
    }

    /** Returns metrics for a type. */
    public @NotNull TypeMetrics forType(@NotNull String typeId) {
        var c = counters.get(typeId);
        if (c == null) return new TypeMetrics(0, 0, 0, 0, 0.0);
        var grants = c.grants.sum();
        var totalNs = c.totalTimeNs.sum();
        var avgMs = grants > 0 ? (double) TimeUnit.NANOSECONDS.toMicros(totalNs) / grants / 1000.0 : 0.0;
        return new TypeMetrics(grants, c.successes.sum(), c.failures.sum(), c.errors.sum(), avgMs);
    }

    /** Returns metrics for all types. */
    public @NotNull Map<String, TypeMetrics> all() {
        var result = new ConcurrentHashMap<String, TypeMetrics>();
        counters.forEach((id, c) -> result.put(id, forType(id)));
        return Map.copyOf(result);
    }

    /** Resets all metrics. */
    public void reset() { counters.clear(); }

    /** Aggregated metrics for a reward type. */
    public record TypeMetrics(long grants, long successes, long failures,
                              long errors, double avgTimeMs) { }

    private static final class TypeCounters {
        final LongAdder grants = new LongAdder();
        final LongAdder successes = new LongAdder();
        final LongAdder failures = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder totalTimeNs = new LongAdder();
    }
}
