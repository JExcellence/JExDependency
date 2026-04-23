package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff with full jitter. {@code nextDelay(attempt)} returns
 * a random duration in {@code [0, min(max, initial * 2^attempt))}.
 */
public final class RetryPolicy {

    private final long initialMillis;
    private final long maxMillis;
    private final int maxAttempts;

    public RetryPolicy(@NotNull Duration initial, @NotNull Duration max, int maxAttempts) {
        this.initialMillis = initial.toMillis();
        this.maxMillis = max.toMillis();
        this.maxAttempts = maxAttempts;
    }

    public boolean hasAttemptsLeft(int attempt) {
        return attempt < this.maxAttempts;
    }

    public @NotNull Duration nextDelay(int attempt) {
        final long cap = Math.min(this.maxMillis, this.initialMillis << Math.min(attempt, 30));
        final long jittered = ThreadLocalRandom.current().nextLong(1, Math.max(2, cap));
        return Duration.ofMillis(jittered);
    }
}
