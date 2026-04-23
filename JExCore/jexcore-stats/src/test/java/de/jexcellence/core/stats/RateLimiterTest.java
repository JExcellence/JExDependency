package de.jexcellence.core.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void rejectsNonPositiveConfig() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(10, 0));
    }

    @Test
    void startsFull() {
        final RateLimiter limiter = new RateLimiter(5, 5);
        for (int i = 0; i < 5; i++) assertTrue(limiter.tryAcquire(), "token " + i);
        assertFalse(limiter.tryAcquire(), "bucket should be drained");
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(2, 100);
        limiter.tryAcquire();
        limiter.tryAcquire();
        assertFalse(limiter.tryAcquire(), "immediately empty");
        Thread.sleep(50);
        assertTrue(limiter.tryAcquire(), "refilled after 50ms at 100/s");
    }

    @Test
    void neverExceedsCapacity() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(3, 1000);
        Thread.sleep(20);
        assertTrue(limiter.tokens() <= 3.0 + 1e-6, "must cap at capacity");
    }
}
