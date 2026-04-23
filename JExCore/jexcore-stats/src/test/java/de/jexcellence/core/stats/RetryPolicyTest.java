package de.jexcellence.core.stats;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    @Test
    void respectsMaxAttempts() {
        final RetryPolicy policy = new RetryPolicy(Duration.ofMillis(10), Duration.ofSeconds(1), 3);
        assertTrue(policy.hasAttemptsLeft(0));
        assertTrue(policy.hasAttemptsLeft(2));
        assertFalse(policy.hasAttemptsLeft(3));
    }

    @Test
    void delaysStayBelowCap() {
        final RetryPolicy policy = new RetryPolicy(Duration.ofMillis(10), Duration.ofMillis(100), 20);
        for (int attempt = 0; attempt < 20; attempt++) {
            final Duration delay = policy.nextDelay(attempt);
            assertTrue(delay.toMillis() > 0, "positive");
            assertTrue(delay.toMillis() <= 100, "capped at max");
        }
    }

    @Test
    void jitterProducesSpread() {
        final RetryPolicy policy = new RetryPolicy(Duration.ofMillis(50), Duration.ofSeconds(10), 10);
        boolean sawVariance = false;
        long first = policy.nextDelay(5).toMillis();
        for (int i = 0; i < 50; i++) {
            if (policy.nextDelay(5).toMillis() != first) {
                sawVariance = true;
                break;
            }
        }
        assertTrue(sawVariance, "jitter should introduce variance");
    }
}
