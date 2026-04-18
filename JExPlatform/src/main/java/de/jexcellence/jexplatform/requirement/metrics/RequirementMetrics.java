package de.jexcellence.jexplatform.requirement.metrics;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collector for requirement operations.
 *
 * <p>Not a singleton — create one per service instance.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RequirementMetrics {

    private final Map<String, TypeCounters> counters = new ConcurrentHashMap<>();

    /**
     * Records a requirement check result.
     *
     * @param typeId      the requirement type
     * @param durationNs  check duration in nanoseconds
     * @param met         whether the requirement was met
     */
    public void recordCheck(@NotNull String typeId, long durationNs, boolean met) {
        var c = counters.computeIfAbsent(typeId, k -> new TypeCounters());
        c.checks.increment();
        if (met) {
            c.successes.increment();
        } else {
            c.failures.increment();
        }
        c.totalTimeNs.add(durationNs);
    }

    /**
     * Records a requirement consumption.
     *
     * @param typeId     the requirement type
     * @param durationNs consumption duration in nanoseconds
     */
    public void recordConsume(@NotNull String typeId, long durationNs) {
        var c = counters.computeIfAbsent(typeId, k -> new TypeCounters());
        c.consumes.increment();
        c.totalTimeNs.add(durationNs);
    }

    /**
     * Records an error for a requirement type.
     *
     * @param typeId the requirement type
     */
    public void recordError(@NotNull String typeId) {
        counters.computeIfAbsent(typeId, k -> new TypeCounters()).errors.increment();
    }

    /**
     * Returns metrics for a specific type.
     *
     * @param typeId the requirement type
     * @return the type metrics
     */
    public @NotNull TypeMetrics forType(@NotNull String typeId) {
        var c = counters.get(typeId);
        if (c == null) {
            return new TypeMetrics(0, 0, 0, 0, 0, 0.0);
        }
        var checks = c.checks.sum();
        var totalNs = c.totalTimeNs.sum();
        var avgMs = checks > 0
                ? (double) TimeUnit.NANOSECONDS.toMicros(totalNs) / checks / 1000.0
                : 0.0;
        return new TypeMetrics(
                checks, c.successes.sum(), c.failures.sum(),
                c.consumes.sum(), c.errors.sum(), avgMs);
    }

    /**
     * Returns metrics for all tracked types.
     *
     * @return an unmodifiable map of type ID to metrics
     */
    public @NotNull Map<String, TypeMetrics> all() {
        var result = new ConcurrentHashMap<String, TypeMetrics>();
        counters.forEach((id, c) -> result.put(id, forType(id)));
        return Map.copyOf(result);
    }

    /**
     * Resets all collected metrics.
     */
    public void reset() {
        counters.clear();
    }

    /**
     * Aggregated metrics for a requirement type.
     *
     * @param checks    total check operations
     * @param successes total successful checks
     * @param failures  total failed checks
     * @param consumes  total consume operations
     * @param errors    total errors
     * @param avgTimeMs average operation time in milliseconds
     */
    public record TypeMetrics(
            long checks,
            long successes,
            long failures,
            long consumes,
            long errors,
            double avgTimeMs
    ) {
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static final class TypeCounters {
        final LongAdder checks = new LongAdder();
        final LongAdder successes = new LongAdder();
        final LongAdder failures = new LongAdder();
        final LongAdder consumes = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder totalTimeNs = new LongAdder();
    }
}
