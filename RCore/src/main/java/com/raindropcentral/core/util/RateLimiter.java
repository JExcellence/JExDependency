package com.raindropcentral.core.util;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket rate limiter for controlling authentication attempt frequency.
 * <p>
 * Implements a token bucket algorithm where each server UUID has a bucket of tokens.
 * Tokens are consumed on each authentication attempt and refilled at a fixed rate.
 * When the bucket is empty, authentication attempts are rate limited.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class RateLimiter {

    private final int bucketCapacity;
    private final long refillIntervalMillis;
    private final Map<UUID, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Constructs a new RateLimiter with specified capacity and refill rate.
     *
     * @param bucketCapacity       maximum number of tokens in a bucket
     * @param refillIntervalMillis time in milliseconds between token refills
     */
    public RateLimiter(final int bucketCapacity, final long refillIntervalMillis) {
        this.bucketCapacity = bucketCapacity;
        this.refillIntervalMillis = refillIntervalMillis;
    }

    /**
     * Attempts to consume a token for the given server UUID.
     * <p>
     * If a token is available, it is consumed and the method returns true.
     * If no tokens are available, the method returns false indicating rate limiting.
     * </p>
     *
     * @param serverUuid the server UUID to check
     * @return true if the attempt is allowed, false if rate limited
     * @throws NullPointerException if serverUuid is null
     */
    public boolean tryConsume(final @NotNull UUID serverUuid) {
        Objects.requireNonNull(serverUuid, "serverUuid cannot be null");

        final TokenBucket bucket = buckets.computeIfAbsent(
                serverUuid,
                k -> new TokenBucket(bucketCapacity, refillIntervalMillis)
        );

        return bucket.tryConsume();
    }

    /**
     * Resets the rate limit for a specific server UUID.
     * <p>
     * Useful for clearing rate limits after successful authentication.
     * </p>
     *
     * @param serverUuid the server UUID to reset
     * @throws NullPointerException if serverUuid is null
     */
    public void reset(final @NotNull UUID serverUuid) {
        Objects.requireNonNull(serverUuid, "serverUuid cannot be null");
        buckets.remove(serverUuid);
    }

    /**
     * Token bucket implementation for a single server.
     */
    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMillis;
        private int tokens;
        private long lastRefillTime;

        TokenBucket(final int capacity, final long refillIntervalMillis) {
            this.capacity = capacity;
            this.refillIntervalMillis = refillIntervalMillis;
            this.tokens = capacity;
            this.lastRefillTime = Instant.now().toEpochMilli();
        }

        synchronized boolean tryConsume() {
            refill();

            if (tokens > 0) {
                tokens--;
                return true;
            }

            return false;
        }

        private void refill() {
            final long now = Instant.now().toEpochMilli();
            final long timeSinceLastRefill = now - lastRefillTime;
            final int tokensToAdd = (int) (timeSinceLastRefill / refillIntervalMillis);

            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
