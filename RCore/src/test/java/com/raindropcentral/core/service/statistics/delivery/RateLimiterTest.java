package com.raindropcentral.core.service.statistics.delivery;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests sliding-window and adaptive behavior in {@link RateLimiter}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RateLimiterTest {

    @Test
    void tryAcquireRespectsConfiguredLimitWithinWindow() {
        final RateLimiter limiter = new RateLimiter(2);

        assertTrue(limiter.tryAcquire());
        limiter.recordRequest();
        assertTrue(limiter.tryAcquire());
        limiter.recordRequest();
        assertFalse(limiter.tryAcquire());
        assertEquals(0, limiter.getAvailablePermits());
    }

    @Test
    void expiredTimestampsArePrunedBeforePermitCheck() {
        final RateLimiter limiter = new RateLimiter(1);
        final Deque<Long> timestamps = requestTimestamps(limiter);

        timestamps.addLast(System.currentTimeMillis() - 61_000L);

        assertTrue(limiter.tryAcquire());
        assertEquals(1, limiter.getAvailablePermits());
    }

    @Test
    void pauseBlocksRequestsUntilDeadlinePasses() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(1);

        limiter.handleRateLimitResponse(1);

        assertTrue(limiter.isPaused());
        assertFalse(limiter.tryAcquire());
        assertTrue(limiter.getRemainingPauseMs() > 0L);

        Thread.sleep(1_100L);

        assertFalse(limiter.isPaused());
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void adaptiveMultiplierCanReduceAndRecover() {
        final RateLimiter limiter = new RateLimiter(10);

        limiter.adaptToErrorRate(0.30D);
        final double reduced = limiter.getAdaptiveMultiplier();
        limiter.adaptToErrorRate(0.0D);

        assertTrue(reduced < 1.0D);
        assertTrue(reduced >= 0.25D);
        assertTrue(limiter.getAdaptiveMultiplier() > reduced);
    }

    @Test
    void recordErrorAdjustsRateAfterSampleThreshold() {
        final RateLimiter limiter = new RateLimiter(10);

        for (int index = 0; index < 10; index++) {
            limiter.recordError();
        }

        assertTrue(limiter.getAdaptiveMultiplier() < 1.0D);
    }

    @Test
    void resetRestoresBaselineState() {
        final RateLimiter limiter = new RateLimiter(5);
        limiter.recordRequest();
        limiter.recordError();
        limiter.handleRateLimitResponse(1);
        limiter.adaptToErrorRate(0.20D);

        limiter.reset();

        assertEquals(1.0D, limiter.getAdaptiveMultiplier(), 0.000_1D);
        assertFalse(limiter.isPaused());
        assertEquals(5, limiter.getAvailablePermits());
    }

    @SuppressWarnings("unchecked")
    private static Deque<Long> requestTimestamps(final RateLimiter limiter) {
        try {
            final Field field = RateLimiter.class.getDeclaredField("requestTimestamps");
            field.setAccessible(true);
            return (Deque<Long>) field.get(limiter);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inspect request timestamps", exception);
        }
    }
}
