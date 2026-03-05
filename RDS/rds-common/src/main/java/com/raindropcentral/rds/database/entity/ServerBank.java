package com.raindropcentral.rds.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents a server-owned bank balance for a specific currency.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(
        name = "server_bank_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_server_bank_currency",
                        columnNames = {"currency_type"}
                )
        }
)
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class ServerBank extends BaseEntity {

    @Column(name = "currency_type", nullable = false)
    private String currency_type;

    @Column(name = "amount", nullable = false)
    private double amount;

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected ServerBank() {
    }

    /**
     * Creates a new server bank entry.
     *
     * @param currencyType currency identifier
     * @param amount starting amount
     */
    public ServerBank(
            final @NotNull String currencyType,
            final double amount
    ) {
        this.currency_type = normalizeCurrencyType(currencyType);
        this.amount = Math.max(0D, amount);
    }

    /**
     * Returns the currency type tracked by this entry.
     *
     * @return normalized currency type
     */
    public @NotNull String getCurrencyType() {
        return this.currency_type;
    }

    /**
     * Returns the stored amount for this currency.
     *
     * @return stored amount
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Indicates whether this entry matches the supplied currency.
     *
     * @param currencyType currency to compare
     * @return {@code true} when both currencies match ignoring case
     */
    public boolean matchesCurrencyType(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return false;
        }

        return this.currency_type.equalsIgnoreCase(currencyType.trim());
    }

    /**
     * Deposits additional balance.
     *
     * @param depositAmount amount to add
     * @return updated balance
     */
    public double deposit(
            final double depositAmount
    ) {
        if (depositAmount <= 0D) {
            return this.amount;
        }

        this.amount += depositAmount;
        return this.amount;
    }

    /**
     * Withdraws balance.
     *
     * @param withdrawAmount amount to subtract
     * @return updated balance
     */
    public double withdraw(
            final double withdrawAmount
    ) {
        if (withdrawAmount <= 0D) {
            return this.amount;
        }

        this.amount = Math.max(0D, this.amount - withdrawAmount);
        return this.amount;
    }

    private static @NotNull String normalizeCurrencyType(
            final @NotNull String currencyType
    ) {
        if (currencyType.isBlank()) {
            return "vault";
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}
