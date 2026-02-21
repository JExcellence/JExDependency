package com.raindropcentral.core.service.statistics.delivery;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Rate limiter for statistics delivery using a sliding window algorithm.
 * Supports adaptive throttling based on error rates and backend rate limit responses.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RateLimiter {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final double ERROR_RATE_THRESHOLD = 0.10; // 10%

    private final int maxRequestsPerMinute;
    private final Deque<Long> requestTimestamps;
    private final AtomicInteger successCount;
    private final AtomicInteger errorCount;

    private volatile long pauseUntil;
    private volatile double adaptiveMultiplier;

    public RateLimiter(final int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.requestTimestamps = new ConcurrentLinkedDeque<>();
        this.successCount = new AtomicInteger(0);
        this.errorCount = new AtomicInteger(0);
        this.pauseUntil = 0;
        this.adaptiveMultiplier = 1.0;
    }

    public RateLimiter(final @NotNull StatisticsDeliveryConfig config) {
        this(config.getMaxRequestsPerMinute());
    }

    /**
     * Attempts to acquire a permit for making a request.
     *
     * @return true if a permit was acquired, false if rate limited
     */
    public boolean tryAcquire() {
        // Check if paused
        if (isPaused()) {
            return false;
        }

        // Clean old timestamps
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000; // 1 minute window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < windowStart) {
            requestTimestamps.pollFirst();
        }

        // Check rate limit with adaptive multiplier
        int effectiveLimit = (int) (maxRequestsPerMinute * adaptiveMultiplier);
        if (requestTimestamps.size() >= effectiveLimit) {
            return false;
        }

        return true;
    }

    /**
     * Records a request being made.
     */
    public void recordRequest() {
        requestTimestamps.addLast(System.currentTimeMillis());
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        successCount.incrementAndGet();
        maybeAdjustRate();
    }

    /**
     * Records a failed request.
     */
    public void recordError() {
        errorCount.incrementAndGet();
        maybeAdjustRate();
    }

    /**
     * Handles a 429 rate limit response from the backend.
     *
     * @param retryAfterSeconds seconds to wait before retrying
     */
    public void handleRateLimitResponse(final int retryAfterSeconds) {
        pauseUntil = System.currentTimeMillis() + (retryAfterSeconds * 1000L);
        LOGGER.warning("Rate limited by backend. Pausing for " + retryAfterSeconds + " seconds.");
    }

    /**
     * Adapts the rate limit based on error rate.
     *
     * @param errorRate the current error rate (0.0 to 1.0)
     */
    public void adaptToErrorRate(final double errorRate) {
        if (errorRate > ERROR_RATE_THRESHOLD) {
            // Reduce rate
            adaptiveMultiplier = Math.max(0.25, adaptiveMultiplier * 0.75);
            LOGGER.warning("High error rate (" + String.format("%.1f%%", errorRate * 100) +
                "). Reducing rate to " + String.format("%.0f%%", adaptiveMultiplier * 100));
        } else if (errorRate < ERROR_RATE_THRESHOLD / 2 && adaptiveMultiplier < 1.0) {
            // Gradually restore rate
            adaptiveMultiplier = Math.min(1.0, adaptiveMultiplier * 1.1);
            LOGGER.info("Error rate normalized. Restoring rate to " +
                String.format("%.0f%%", adaptiveMultiplier * 100));
        }
    }

    /**
     * Checks if the rate limiter is currently paused.
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return System.currentTimeMillis() < pauseUntil;
    }

    /**
     * Gets the remaining pause time in milliseconds.
     *
     * @return remaining pause time, or 0 if not paused
     */
    public long getRemainingPauseMs() {
        long remaining = pauseUntil - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets the number of available permits.
     *
     * @return available permits
     */
    public int getAvailablePermits() {
        if (isPaused()) {
            return 0;
        }

        // Clean old timestamps
        long windowStart = System.currentTimeMillis() - 60_000;
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < windowStart) {
            requestTimestamps.pollFirst();
        }

        int effectiveLimit = (int) (maxRequestsPerMinute * adaptiveMultiplier);
        return Math.max(0, effectiveLimit - requestTimestamps.size());
    }

    /**
     * Gets the current adaptive multiplier.
     *
     * @return the multiplier (0.0 to 1.0)
     */
    public double getAdaptiveMultiplier() {
        return adaptiveMultiplier;
    }

    /**
     * Resets the rate limiter state.
     */
    public void reset() {
        requestTimestamps.clear();
        successCount.set(0);
        errorCount.set(0);
        pauseUntil = 0;
        adaptiveMultiplier = 1.0;
    }

    private void maybeAdjustRate() {
        int total = successCount.get() + errorCount.get();
        if (total >= 10) { // Only adjust after sufficient samples
            double errorRate = (double) errorCount.get() / total;
            adaptToErrorRate(errorRate);

            // Reset counters periodically
            if (total >= 100) {
                successCount.set(0);
                errorCount.set(0);
            }
        }
    }
}
