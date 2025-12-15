package com.raindropcentral.core.service.statistics.monitoring;

import com.raindropcentral.core.service.statistics.delivery.DeliveryResult;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Tracks delivery metrics for monitoring and diagnostics.
 * Maintains rolling window statistics for session analysis.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class DeliveryMetricsTracker {

    private static final Logger LOGGER = CentralLogger.getLogger(DeliveryMetricsTracker.class);

    /** Rolling window size in milliseconds (1 hour) */
    private static final long ROLLING_WINDOW_MS = 60 * 60 * 1000L;

    // Lifetime counters
    private final AtomicLong totalDeliveries = new AtomicLong(0);
    private final AtomicLong successfulDeliveries = new AtomicLong(0);
    private final AtomicLong failedDeliveries = new AtomicLong(0);
    private final AtomicLong totalStatisticsTransmitted = new AtomicLong(0);
    private final AtomicLong totalBytesTransmitted = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    // Rolling window data
    private final ConcurrentLinkedDeque<DeliveryEvent> recentDeliveries = new ConcurrentLinkedDeque<>();

    // Session tracking
    private final Instant sessionStart = Instant.now();

    public DeliveryMetricsTracker() {
    }

    /**
     * Records a delivery result.
     *
     * @param result the delivery result
     * @param bytesTransmitted bytes transmitted
     */
    public void recordDelivery(final @NotNull DeliveryResult result, final long bytesTransmitted) {
        totalDeliveries.incrementAndGet();

        if (result.success()) {
            successfulDeliveries.incrementAndGet();
            totalStatisticsTransmitted.addAndGet(result.statisticsDelivered());
        } else {
            failedDeliveries.incrementAndGet();
        }

        totalBytesTransmitted.addAndGet(bytesTransmitted);
        totalLatencyMs.addAndGet(result.latencyMs());

        // Add to rolling window
        DeliveryEvent event = new DeliveryEvent(
            System.currentTimeMillis(),
            result.success(),
            result.statisticsDelivered(),
            bytesTransmitted,
            result.latencyMs()
        );
        recentDeliveries.addLast(event);

        // Prune old events
        pruneOldEvents();
    }

    /**
     * Records a successful delivery.
     */
    public void recordSuccess(final int statisticsCount, final long bytesTransmitted, final long latencyMs) {
        totalDeliveries.incrementAndGet();
        successfulDeliveries.incrementAndGet();
        totalStatisticsTransmitted.addAndGet(statisticsCount);
        totalBytesTransmitted.addAndGet(bytesTransmitted);
        totalLatencyMs.addAndGet(latencyMs);

        DeliveryEvent event = new DeliveryEvent(
            System.currentTimeMillis(), true, statisticsCount, bytesTransmitted, latencyMs
        );
        recentDeliveries.addLast(event);
        pruneOldEvents();
    }

    /**
     * Records a failed delivery.
     */
    public void recordFailure(final long latencyMs) {
        totalDeliveries.incrementAndGet();
        failedDeliveries.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        DeliveryEvent event = new DeliveryEvent(
            System.currentTimeMillis(), false, 0, 0, latencyMs
        );
        recentDeliveries.addLast(event);
        pruneOldEvents();
    }

    /**
     * Gets the current metrics.
     *
     * @return delivery metrics
     */
    public DeliveryMetrics getMetrics() {
        long total = totalDeliveries.get();
        long successful = successfulDeliveries.get();
        long failed = failedDeliveries.get();
        long avgLatency = total > 0 ? totalLatencyMs.get() / total : 0;
        double successRate = total > 0 ? (double) successful / total : 1.0;

        return new DeliveryMetrics(
            total,
            successful,
            failed,
            totalStatisticsTransmitted.get(),
            totalBytesTransmitted.get(),
            avgLatency,
            successRate
        );
    }

    /**
     * Gets rolling window metrics for the last hour.
     *
     * @return rolling window metrics
     */
    public RollingWindowMetrics getRollingWindowMetrics() {
        pruneOldEvents();

        long windowDeliveries = 0;
        long windowSuccessful = 0;
        long windowStatistics = 0;
        long windowBytes = 0;
        long windowLatency = 0;

        for (DeliveryEvent event : recentDeliveries) {
            windowDeliveries++;
            if (event.success) {
                windowSuccessful++;
                windowStatistics += event.statisticsCount;
            }
            windowBytes += event.bytesTransmitted;
            windowLatency += event.latencyMs;
        }

        long avgLatency = windowDeliveries > 0 ? windowLatency / windowDeliveries : 0;
        double successRate = windowDeliveries > 0 ? (double) windowSuccessful / windowDeliveries : 1.0;

        return new RollingWindowMetrics(
            windowDeliveries,
            windowSuccessful,
            windowDeliveries - windowSuccessful,
            windowStatistics,
            windowBytes,
            avgLatency,
            successRate
        );
    }

    /**
     * Gets session duration in seconds.
     */
    public long getSessionDurationSeconds() {
        return Instant.now().getEpochSecond() - sessionStart.getEpochSecond();
    }

    /**
     * Resets all metrics.
     */
    public void reset() {
        totalDeliveries.set(0);
        successfulDeliveries.set(0);
        failedDeliveries.set(0);
        totalStatisticsTransmitted.set(0);
        totalBytesTransmitted.set(0);
        totalLatencyMs.set(0);
        recentDeliveries.clear();
    }

    /**
     * Prunes events older than the rolling window.
     */
    private void pruneOldEvents() {
        long cutoff = System.currentTimeMillis() - ROLLING_WINDOW_MS;
        while (!recentDeliveries.isEmpty() && recentDeliveries.peekFirst().timestamp < cutoff) {
            recentDeliveries.pollFirst();
        }
    }

    /**
     * Internal event record for rolling window tracking.
     */
    private record DeliveryEvent(
        long timestamp,
        boolean success,
        int statisticsCount,
        long bytesTransmitted,
        long latencyMs
    ) {}

    /**
     * Lifetime delivery metrics.
     */
    public record DeliveryMetrics(
        long totalDeliveries,
        long successfulDeliveries,
        long failedDeliveries,
        long totalStatisticsTransmitted,
        long totalBytesTransmitted,
        long averageLatencyMs,
        double successRate
    ) {}

    /**
     * Rolling window metrics (last hour).
     */
    public record RollingWindowMetrics(
        long deliveries,
        long successful,
        long failed,
        long statisticsTransmitted,
        long bytesTransmitted,
        long averageLatencyMs,
        double successRate
    ) {}
}
