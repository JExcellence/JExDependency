package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Duration;

/**
 * Immutable configuration for the statistics delivery engine.
 *
 * @param endpoint HTTP endpoint to POST batches to
 * @param apiKey optional bearer token
 * @param hmacSecret optional HMAC-SHA256 secret for body signing
 * @param maxQueueSize max in-memory queue depth before backpressure
 * @param maxBatchEntries flush batch once it reaches this many entries
 * @param maxBatchBytes flush batch once the body reaches this size
 * @param flushInterval flush at least every {@code flushInterval} regardless of size
 * @param rateLimitPerSecond max deliveries per second (token bucket)
 * @param connectTimeout HTTP connect timeout
 * @param requestTimeout HTTP request timeout
 * @param retryInitial initial retry backoff
 * @param retryMax max retry backoff (exponential capped here)
 * @param retryMaxAttempts max retry attempts before spool
 * @param spoolDir directory for offline spool files
 */
public record StatisticsConfig(
        @NotNull URI endpoint,
        @Nullable String apiKey,
        @Nullable String hmacSecret,
        int maxQueueSize,
        int maxBatchEntries,
        int maxBatchBytes,
        @NotNull Duration flushInterval,
        int rateLimitPerSecond,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @NotNull Duration retryInitial,
        @NotNull Duration retryMax,
        int retryMaxAttempts,
        @NotNull String spoolDir
) {

    public static @NotNull StatisticsConfig defaults(@NotNull URI endpoint) {
        return new StatisticsConfig(
                endpoint,
                null,
                null,
                10_000,
                500,
                512 * 1024,
                Duration.ofSeconds(15),
                50,
                Duration.ofSeconds(5),
                Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                6,
                "stats-spool"
        );
    }
}
