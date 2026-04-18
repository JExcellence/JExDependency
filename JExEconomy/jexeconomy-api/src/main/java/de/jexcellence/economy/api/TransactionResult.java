package de.jexcellence.economy.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable outcome of an economy transaction.
 *
 * @param amount  the amount involved in the transaction
 * @param balance the resulting balance after the transaction
 * @param status  the outcome status
 * @param error   human-readable error message, or {@code null} on success
 * @author JExcellence
 * @since 3.0.0
 */
public record TransactionResult(double amount, double balance, @NotNull Status status,
                                @Nullable String error) {

    /** Possible transaction outcomes. */
    public enum Status { SUCCESS, FAILURE, NOT_IMPLEMENTED }

    /**
     * Creates a successful result.
     *
     * @param amount  the transaction amount
     * @param balance the resulting balance
     * @return a success result
     */
    public static @NotNull TransactionResult success(double amount, double balance) {
        return new TransactionResult(amount, balance, Status.SUCCESS, null);
    }

    /**
     * Creates a failure result.
     *
     * @param amount  the attempted amount
     * @param balance the unchanged balance
     * @param error   description of why the transaction failed
     * @return a failure result
     */
    public static @NotNull TransactionResult failure(double amount, double balance,
                                                     @NotNull String error) {
        return new TransactionResult(amount, balance, Status.FAILURE, error);
    }

    /**
     * Creates a not-implemented result for unsupported operations.
     *
     * @param operation the operation that is not implemented
     * @return a not-implemented result
     */
    public static @NotNull TransactionResult notImplemented(@NotNull String operation) {
        return new TransactionResult(0, 0, Status.NOT_IMPLEMENTED, operation + " is not implemented");
    }

    /**
     * Returns whether this transaction succeeded.
     *
     * @return {@code true} if the status is {@link Status#SUCCESS}
     */
    public boolean isSuccess() { return status == Status.SUCCESS; }

    /**
     * Returns whether this transaction failed.
     *
     * @return {@code true} if the status is {@link Status#FAILURE}
     */
    public boolean isFailed() { return status == Status.FAILURE; }
}
