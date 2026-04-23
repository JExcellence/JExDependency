package de.jexcellence.core.stats;

/**
 * Token-bucket rate limiter. Thread-safe. {@code capacity} == refill rate
 * per second; bucket refills linearly in nanoseconds.
 */
public final class RateLimiter {

    private final long capacity;
    private final double refillPerNanos;

    private double tokens;
    private long lastRefillNanos;

    public RateLimiter(long capacity, double refillPerSecond) {
        if (capacity <= 0 || refillPerSecond <= 0.0) {
            throw new IllegalArgumentException("capacity and refillPerSecond must be positive");
        }
        this.capacity = capacity;
        this.refillPerNanos = refillPerSecond / 1_000_000_000.0;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (this.tokens >= 1.0) {
            this.tokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized double tokens() {
        refill();
        return this.tokens;
    }

    private void refill() {
        final long now = System.nanoTime();
        final long delta = now - this.lastRefillNanos;
        this.tokens = Math.min(this.capacity, this.tokens + delta * this.refillPerNanos);
        this.lastRefillNanos = now;
    }
}
