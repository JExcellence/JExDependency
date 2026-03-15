package com.raindropcentral.core.service.statistics.delivery;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handles retry logic with exponential backoff for failed delivery attempts.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RetryHandler {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;

    /**
     * Executes RetryHandler.
     */
    public RetryHandler(final int maxRetries, final long initialBackoffMs, final long maxBackoffMs) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    /**
     * Executes RetryHandler.
     */
    public RetryHandler(final @NotNull StatisticsDeliveryConfig config) {
        this(config.getMaxRetries(), config.getInitialBackoffMs(), config.getMaxBackoffMs());
    }

    /**
     * Executes an operation with retry logic.
     *
     * @param operation the operation to execute
     * @param <T>       the result type
     * @return a future that completes with the result or fails after all retries
     */
    public <T> CompletableFuture<T> executeWithRetry(
        final @NotNull Supplier<CompletableFuture<T>> operation
    ) {
        return executeWithRetry(operation, 0);
    }

    private <T> CompletableFuture<T> executeWithRetry(
        final Supplier<CompletableFuture<T>> operation,
        final int attemptNumber
    ) {
        return operation.get()
            .exceptionally(error -> {
                Throwable cause = unwrapException(error);

                if (!shouldRetry(cause, attemptNumber)) {
                    LOGGER.warning("Operation failed after " + (attemptNumber + 1) +
                        " attempts: " + cause.getMessage());
                    throw new CompletionException(cause);
                }

                long backoff = calculateBackoff(attemptNumber);
                LOGGER.fine("Retry attempt " + (attemptNumber + 1) + "/" + maxRetries +
                    " after " + backoff + "ms: " + cause.getMessage());

                // Schedule retry
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }

                // Recursive retry - this will be executed synchronously due to exceptionally
                return executeWithRetry(operation, attemptNumber + 1).join();
            });
    }

    /**
     * Calculates the backoff delay for a given attempt number.
     *
     * @param attemptNumber the attempt number (0-based)
     * @return the backoff delay in milliseconds
     */
    public long calculateBackoff(final int attemptNumber) {
        // Exponential backoff with jitter
        long exponentialBackoff = initialBackoffMs * (1L << attemptNumber);
        long cappedBackoff = Math.min(exponentialBackoff, maxBackoffMs);

        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5);
        return (long) (cappedBackoff * jitter);
    }

    /**
     * Determines if an error should trigger a retry.
     *
     * @param error         the error that occurred
     * @param attemptNumber the current attempt number
     * @return true if the operation should be retried
     */
    public boolean shouldRetry(final Throwable error, final int attemptNumber) {
        if (attemptNumber >= maxRetries) {
            return false;
        }

        // Retry on network errors
        if (error instanceof IOException ||
            error instanceof ConnectException ||
            error instanceof SocketTimeoutException) {
            return true;
        }

        // Retry on server errors (5xx)
        if (error instanceof DeliveryException de) {
            int statusCode = de.getStatusCode();
            return statusCode >= 500 && statusCode < 600;
        }

        // Check for wrapped exceptions
        if (error.getCause() != null && error.getCause() != error) {
            return shouldRetry(error.getCause(), attemptNumber);
        }

        return false;
    }

    private Throwable unwrapException(final Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    /**
     * Exception indicating a delivery failure with HTTP status code.
     */
    public static class DeliveryException extends RuntimeException {
        private final int statusCode;

        /**
         * Executes DeliveryException.
         */
        public DeliveryException(final int statusCode, final String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * Gets statusCode.
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}
