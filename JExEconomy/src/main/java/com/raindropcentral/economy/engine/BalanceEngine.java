package com.raindropcentral.economy.engine;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Stateless helpers that perform basic balance calculations for the economy module.
 *
 * <p>The engine performs validation before applying any balance mutation so that higher level
 * services can rely on deterministic arithmetic without duplicating guard clauses.</p>
 *
 * <p>All arithmetic assumes callers have normalized {@link BigDecimal} arguments to the scale
 * declared by the active currency descriptor. The engine intentionally avoids calling
 * {@code setScale} so that rounding modes remain a concern of the currency registry and its
 * configuration. Consumers should therefore harmonize both input and output values with the
 * currency metadata supplied by the surrounding service facade before persisting results.</p>
 *
 * <p>Balance deltas returned from these helpers are expected to flow immediately into the
 * transaction logging utilities housed under {@code de.jexcellence.economy.transaction} so that the
 * immutable audit log reflects the same arithmetic operators that modified the account. Downstream
 * services should also surface human readable messages through {@code JExTranslate} bundles,
 * attaching locale-aware formatting to the computed figures when notifying operators or players.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class BalanceEngine {

    /**
     * Disallows instantiation because the engine exposes only static helper methods.
     */
    private BalanceEngine() {
    }

    /**
     * Applies a deposit to the supplied {@code startingBalance} and returns the updated value.
     *
     * @param startingBalance the balance before the deposit is applied; must not be {@code null}.
     * @param amount          the deposit amount to add; must not be {@code null} and cannot be negative.
     * @return the resulting balance after the deposit is applied.
     * @throws NullPointerException     if {@code startingBalance} or {@code amount} is {@code null}.
     * @throws IllegalArgumentException if {@code amount} is negative.
     */
    public static BigDecimal applyDeposit(final BigDecimal startingBalance, final BigDecimal amount) {
        Objects.requireNonNull(startingBalance, "startingBalance");
        Objects.requireNonNull(amount, "amount");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative: " + amount);
        }

        return startingBalance.add(amount);
    }

    /**
     * Applies a withdrawal to the supplied {@code startingBalance} and returns the updated balance.
     *
     * @param startingBalance the balance before the withdrawal is processed; must not be {@code null}.
     * @param amount          the withdrawal amount; must not be {@code null}, must be non-negative, and cannot exceed {@code startingBalance}.
     * @return the resulting balance after the withdrawal is processed.
     * @throws NullPointerException     if {@code startingBalance} or {@code amount} is {@code null}.
     * @throws IllegalArgumentException if {@code amount} is negative or exceeds the {@code startingBalance}.
     */
    public static BigDecimal applyWithdrawal(final BigDecimal startingBalance, final BigDecimal amount) {
        Objects.requireNonNull(startingBalance, "startingBalance");
        Objects.requireNonNull(amount, "amount");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Withdrawal amount cannot be negative: " + amount);
        }

        if (amount.compareTo(startingBalance) > 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount cannot exceed the starting balance: " + amount + " > " + startingBalance
            );
        }

        return startingBalance.subtract(amount);
    }

    /**
     * Calculates the net change by subtracting {@code withdrawalsTotal} from {@code depositsTotal}.
     *
     * @param depositsTotal    the sum of processed deposits; must not be {@code null}.
     * @param withdrawalsTotal the sum of processed withdrawals; must not be {@code null}.
     * @return the net change to apply to an account balance.
     * @throws NullPointerException if {@code depositsTotal} or {@code withdrawalsTotal} is {@code null}.
     */
    public static BigDecimal calculateNetChange(
            final BigDecimal depositsTotal,
            final BigDecimal withdrawalsTotal
    ) {
        Objects.requireNonNull(depositsTotal, "depositsTotal");
        Objects.requireNonNull(withdrawalsTotal, "withdrawalsTotal");

        return depositsTotal.subtract(withdrawalsTotal);
    }
}
