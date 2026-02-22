package com.raindropcentral.rdq.perk.util;

import com.raindropcentral.rplatform.logging.CentralLogger;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for retrying database operations that may fail due to optimistic locking conflicts.
 */
public class RetryableOperation {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 50;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * Executes an operation with retry logic for optimistic lock exceptions.
     *
     * @param operation the operation to execute
     * @param operationName the name of the operation for logging
     * @param <T> the return type
     * @return a CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> operation,
            String operationName
    ) {
        return executeWithRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }
    
    /**
     * Executes an operation with retry logic for optimistic lock exceptions.
     *
     * @param operation the operation to execute
     * @param operationName the name of the operation for logging
     * @param maxRetries maximum number of retry attempts
     * @param baseDelayMs base delay in milliseconds
     * @param backoffMultiplier exponential backoff multiplier
     * @param <T> the return type
     * @return a CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> operation,
            String operationName,
            int maxRetries,
            long baseDelayMs,
            double backoffMultiplier
    ) {
        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            Throwable lastException = null;
            
            while (attempt <= maxRetries) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    lastException = e;
                    
                    if (!isRetryable(e)) {
                        LOGGER.log(Level.SEVERE, "Non-retryable exception in " + operationName, e);
                        throw e;
                    }
                    
                    if (attempt >= maxRetries) {
                        LOGGER.log(Level.SEVERE, 
                                String.format("Operation %s failed after %d retries", operationName, maxRetries), 
                                e);
                        throw e;
                    }
                    
                    attempt++;
                    long delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attempt - 1));
                    
                    LOGGER.log(Level.INFO, 
                            String.format("Retrying %s (attempt %d/%d) after %dms due to: %s", 
                                    operationName, attempt, maxRetries, delay, e.getClass().getSimpleName()));
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
            
            throw new RuntimeException("Operation failed after retries", lastException);
        });
    }
    
    /**
     * Determines if an exception is retryable.
     */
    private static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof OptimisticLockException) {
            return true;
        }
        if (throwable instanceof StaleStateException) {
            return true;
        }
        if (throwable instanceof jakarta.persistence.RollbackException) {
            Throwable cause = throwable.getCause();
            return cause != null && isRetryable(cause);
        }
        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            return isRetryable(throwable.getCause());
        }
        return false;
    }
}
