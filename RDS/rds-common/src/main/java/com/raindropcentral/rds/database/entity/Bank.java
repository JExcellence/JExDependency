package com.raindropcentral.rds.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

/**
 * Represents bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(
        name = "shop_bank_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shop_bank_currency",
                        columnNames = {"shop_id", "currency_type"}
                )
        }
)
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
public class Bank extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "currency_type", nullable = false)
    private String currency_type;

    @Column(name = "amount", nullable = false)
    private double amount;

    protected Bank() {
    }

    /**
     * Creates a new bank.
     *
     * @param shop target shop
     * @param currencyType currency type
     * @param amount amount
     */
    public Bank(
            final @NotNull Shop shop,
            final @NotNull String currencyType,
            final double amount
    ) {
        this.shop = shop;
        this.currency_type = currencyType.trim();
        this.amount = Math.max(amount, 0D);
    }

    /**
     * Returns the shop.
     *
     * @return the shop
     */
    public @NotNull Shop getShop() {
        return this.shop;
    }

    public void setShop(
            final @NotNull Shop shop
    ) {
        this.shop = shop;
    }

    /**
     * Returns the currency type.
     *
     * @return the currency type
     */
    public @NotNull String getCurrencyType() {
        return this.currency_type;
    }

    /**
     * Returns the amount.
     *
     * @return the amount
     */
    public double getAmount() {
        return this.amount;
    }

    public boolean matchesCurrencyType(
            final @NotNull String currencyType
    ) {
        return this.currency_type.equalsIgnoreCase(currencyType.trim());
    }

    public double deposit(
            final double depositAmount
    ) {
        if (depositAmount <= 0D) {
            return this.amount;
        }

        this.amount += depositAmount;
        return this.amount;
    }

    public double withdraw(
            final double withdrawAmount
    ) {
        if (withdrawAmount <= 0D) {
            return this.amount;
        }

        this.amount = Math.max(0D, this.amount - withdrawAmount);
        return this.amount;
    }
}
