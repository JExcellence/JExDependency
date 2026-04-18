package de.jexcellence.economy.database.entity;

import de.jexcellence.economy.api.AccountSnapshot;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

/**
 * A player's balance for a specific currency.
 *
 * <p>Business logic (deposit, withdraw, balance checks) lives in
 * {@code EconomyService}, not here. This entity is pure data.
 *
 * @author JExcellence
 * @since 3.0.0
 */
@Entity
@Table(name = "economy_account")
public class Account extends LongIdEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private EconomyPlayer player;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(name = "balance", nullable = false, columnDefinition = "DECIMAL(64,2) DEFAULT '0.00'")
    private double balance;

    /** JPA constructor. */
    protected Account() {
    }

    /**
     * Creates a zero-balance account.
     *
     * @param player   the account owner
     * @param currency the currency
     */
    public Account(@NotNull EconomyPlayer player, @NotNull Currency currency) {
        this(player, currency, 0.0);
    }

    /**
     * Creates an account with an initial balance.
     *
     * @param player         the account owner
     * @param currency       the currency
     * @param initialBalance the starting balance (must be non-negative)
     */
    public Account(@NotNull EconomyPlayer player,
                   @NotNull Currency currency,
                   double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        this.player = player;
        this.currency = currency;
        this.balance = initialBalance;
    }

    /**
     * Returns the account owner.
     *
     * @return the player
     */
    public @NotNull EconomyPlayer getPlayer() {
        return player;
    }

    /**
     * Sets the account owner.
     *
     * @param player the player
     */
    public void setPlayer(@NotNull EconomyPlayer player) {
        this.player = player;
    }

    /**
     * Returns the currency.
     *
     * @return the currency
     */
    public @NotNull Currency getCurrency() {
        return currency;
    }

    /**
     * Sets the currency.
     *
     * @param currency the currency
     */
    public void setCurrency(@NotNull Currency currency) {
        this.currency = currency;
    }

    /**
     * Returns the current balance.
     *
     * @return the balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Sets the balance directly.
     *
     * @param balance the new balance
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Creates an API-safe snapshot of this account.
     *
     * @return a lightweight, immutable snapshot
     */
    public @NotNull AccountSnapshot toSnapshot() {
        return new AccountSnapshot(
                player.getUniqueId(), player.getPlayerName(),
                currency.getIdentifier(), balance);
    }

    @Override
    public String toString() {
        return "Account[" + player.getPlayerName() + "/" + currency.getIdentifier()
                + "=" + balance + "]";
    }
}
