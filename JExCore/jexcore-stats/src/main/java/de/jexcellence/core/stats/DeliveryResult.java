package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of one batch delivery attempt. Three shapes:
 * {@link Success}, {@link Retryable}, {@link Fatal}.
 */
public sealed interface DeliveryResult {

    static @NotNull Success success(int statusCode) {
        return new Success(statusCode);
    }

    static @NotNull Retryable retry(int statusCode, @NotNull String reason) {
        return new Retryable(statusCode, reason);
    }

    static @NotNull Fatal fatal(int statusCode, @NotNull String reason) {
        return new Fatal(statusCode, reason);
    }

    /**
     * Delivered and acknowledged.
     *
     * @param statusCode HTTP status code
     */
    record Success(int statusCode) implements DeliveryResult {
    }

    /**
     * Transport-level failure worth retrying.
     *
     * @param statusCode HTTP status code, or {@code -1} when no response
     * @param reason human-readable cause for logs
     */
    record Retryable(int statusCode, @NotNull String reason) implements DeliveryResult {
    }

    /**
     * Non-retryable failure (auth rejected, payload malformed). The batch is
     * spooled and not re-attempted until config change.
     *
     * @param statusCode HTTP status code, or {@code -1} when no response
     * @param reason human-readable cause for logs
     */
    record Fatal(int statusCode, @NotNull String reason) implements DeliveryResult {
    }

    default @Nullable String describe() {
        return switch (this) {
            case Success s -> "ok(" + s.statusCode() + ")";
            case Retryable r -> "retry(" + r.statusCode() + "): " + r.reason();
            case Fatal f -> "fatal(" + f.statusCode() + "): " + f.reason();
        };
    }
}
