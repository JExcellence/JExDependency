package com.raindropcentral.core.service.statistics.aggregation;

import com.raindropcentral.core.service.statistics.delivery.AggregatedStatistics;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Accumulates statistics over time windows for hourly and daily aggregates.
 * Maintains sliding windows of data for trend analysis.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TimeWindowedAccumulator {

    private static final Logger LOGGER = CentralLogger.getLogger(TimeWindowedAccumulator.class);
    private static final int HOURLY_BUCKETS = 24; // Keep 24 hours of hourly data
    private static final int DAILY_BUCKETS = 7;   // Keep 7 days of daily data

    // Hourly buckets: key -> hour timestamp -> accumulated value
    private final Map<String, Map<Long, AccumulatedValue>> hourlyBuckets;
    // Daily buckets: key -> day timestamp -> accumulated value
    private final Map<String, Map<Long, AccumulatedValue>> dailyBuckets;

    // Custom aggregate definitions
    private final Map<String, AggregateDefinition> customAggregates;

    public TimeWindowedAccumulator() {
        this.hourlyBuckets = new ConcurrentHashMap<>();
        this.dailyBuckets = new ConcurrentHashMap<>();
        this.customAggregates = new ConcurrentHashMap<>();
    }

    /**
     * Records a value for a statistic key.
     *
     * @param key   the statistic key
     * @param value the value to record
     */
    public void record(final @NotNull String key, final double value) {
        long now = System.currentTimeMillis();
        long hourBucket = truncateToHour(now);
        long dayBucket = truncateToDay(now);

        // Record in hourly bucket
        hourlyBuckets
            .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(hourBucket, k -> new AccumulatedValue())
            .add(value);

        // Record in daily bucket
        dailyBuckets
            .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(dayBucket, k -> new AccumulatedValue())
            .add(value);

        // Cleanup old buckets
        cleanupOldBuckets();
    }

    /**
     * Computes hourly aggregates for all tracked statistics.
     *
     * @return map of statistic key to hourly aggregate
     */
    public Map<String, HourlyAggregate> computeHourlyAggregates() {
        Map<String, HourlyAggregate> result = new HashMap<>();
        long currentHour = truncateToHour(System.currentTimeMillis());

        for (var entry : hourlyBuckets.entrySet()) {
            String key = entry.getKey();
            Map<Long, AccumulatedValue> buckets = entry.getValue();

            AccumulatedValue currentBucket = buckets.get(currentHour);
            if (currentBucket != null) {
                result.put(key, new HourlyAggregate(
                    currentHour,
                    currentBucket.getSum(),
                    currentBucket.getCount(),
                    currentBucket.getAverage(),
                    currentBucket.getMin(),
                    currentBucket.getMax()
                ));
            }
        }

        return result;
    }

    /**
     * Computes daily aggregates for all tracked statistics.
     *
     * @return map of statistic key to daily aggregate
     */
    public Map<String, DailyAggregate> computeDailyAggregates() {
        Map<String, DailyAggregate> result = new HashMap<>();
        long currentDay = truncateToDay(System.currentTimeMillis());

        for (var entry : dailyBuckets.entrySet()) {
            String key = entry.getKey();
            Map<Long, AccumulatedValue> buckets = entry.getValue();

            AccumulatedValue currentBucket = buckets.get(currentDay);
            if (currentBucket != null) {
                result.put(key, new DailyAggregate(
                    currentDay,
                    currentBucket.getSum(),
                    currentBucket.getCount(),
                    currentBucket.getAverage(),
                    currentBucket.getMin(),
                    currentBucket.getMax()
                ));
            }
        }

        return result;
    }


    /**
     * Registers a custom aggregate definition.
     *
     * @param name       the aggregate name
     * @param definition the aggregate definition
     */
    public void registerCustomAggregate(
        final @NotNull String name,
        final @NotNull AggregateDefinition definition
    ) {
        customAggregates.put(name, definition);
    }

    /**
     * Computes custom aggregates based on registered definitions.
     *
     * @return map of aggregate name to computed value
     */
    public Map<String, Object> computeCustomAggregates() {
        Map<String, Object> result = new HashMap<>();

        for (var entry : customAggregates.entrySet()) {
            String name = entry.getKey();
            AggregateDefinition def = entry.getValue();

            try {
                Object value = def.compute(this);
                result.put(name, value);
            } catch (Exception e) {
                LOGGER.warning("Failed to compute custom aggregate '" + name + "': " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Gets historical hourly data for a statistic.
     *
     * @param key   the statistic key
     * @param hours number of hours to retrieve
     * @return list of hourly values (oldest first)
     */
    public List<HourlyAggregate> getHourlyHistory(final @NotNull String key, final int hours) {
        Map<Long, AccumulatedValue> buckets = hourlyBuckets.get(key);
        if (buckets == null) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        List<HourlyAggregate> result = new ArrayList<>();

        for (int i = hours - 1; i >= 0; i--) {
            long hourBucket = truncateToHour(now - (i * 3600_000L));
            AccumulatedValue value = buckets.get(hourBucket);
            if (value != null) {
                result.add(new HourlyAggregate(
                    hourBucket, value.getSum(), value.getCount(),
                    value.getAverage(), value.getMin(), value.getMax()
                ));
            }
        }

        return result;
    }

    /**
     * Gets historical daily data for a statistic.
     *
     * @param key  the statistic key
     * @param days number of days to retrieve
     * @return list of daily values (oldest first)
     */
    public List<DailyAggregate> getDailyHistory(final @NotNull String key, final int days) {
        Map<Long, AccumulatedValue> buckets = dailyBuckets.get(key);
        if (buckets == null) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        List<DailyAggregate> result = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            long dayBucket = truncateToDay(now - (i * 86400_000L));
            AccumulatedValue value = buckets.get(dayBucket);
            if (value != null) {
                result.add(new DailyAggregate(
                    dayBucket, value.getSum(), value.getCount(),
                    value.getAverage(), value.getMin(), value.getMax()
                ));
            }
        }

        return result;
    }

    private long truncateToHour(final long timestamp) {
        return Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.HOURS).toEpochMilli();
    }

    private long truncateToDay(final long timestamp) {
        return Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
    }

    private void cleanupOldBuckets() {
        long now = System.currentTimeMillis();
        long hourlyThreshold = now - (HOURLY_BUCKETS * 3600_000L);
        long dailyThreshold = now - (DAILY_BUCKETS * 86400_000L);

        for (Map<Long, AccumulatedValue> buckets : hourlyBuckets.values()) {
            buckets.entrySet().removeIf(e -> e.getKey() < hourlyThreshold);
        }

        for (Map<Long, AccumulatedValue> buckets : dailyBuckets.values()) {
            buckets.entrySet().removeIf(e -> e.getKey() < dailyThreshold);
        }
    }

    /**
     * Clears all accumulated data.
     */
    public void clear() {
        hourlyBuckets.clear();
        dailyBuckets.clear();
    }

    // ==================== Inner Classes ====================

    /**
     * Accumulated value with sum, count, min, max tracking.
     */
    private static class AccumulatedValue {
        private double sum = 0;
        private int count = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;

        synchronized void add(double value) {
            sum += value;
            count++;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        synchronized double getSum() { return sum; }
        synchronized int getCount() { return count; }
        synchronized double getAverage() { return count > 0 ? sum / count : 0; }
        synchronized double getMin() { return count > 0 ? min : 0; }
        synchronized double getMax() { return count > 0 ? max : 0; }
    }

    /**
     * Hourly aggregate record.
     */
    public record HourlyAggregate(
        long hourTimestamp,
        double sum,
        int count,
        double average,
        double min,
        double max
    ) {}

    /**
     * Daily aggregate record.
     */
    public record DailyAggregate(
        long dayTimestamp,
        double sum,
        int count,
        double average,
        double min,
        double max
    ) {}

    /**
     * Custom aggregate definition interface.
     */
    @FunctionalInterface
    public interface AggregateDefinition {
        Object compute(TimeWindowedAccumulator accumulator);
    }
}
