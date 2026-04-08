package de.jexcellence.economy.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Entity representing the association between a user (player) and a currency, including the user's balance.
 *
 * <p>This entity is mapped to the {@code join_p_player_currency} table in the database and models
 * the many-to-many relationship between users and currencies, with an additional balance field.
 *
 *
 * <p>Example usages include tracking how much of each currency a player owns, performing deposits and withdrawals,
 * and managing player balances for different currencies in the system.
 *
 * @author JExcellence
 */
@Table(name = "join_p_player_currency")
@Entity
public class UserCurrency extends BaseEntity {
	
	/**
	 * The user (player) associated with this balance.
	 */
	@ManyToOne
	@JoinColumn(
		name = "p_player_id",
		nullable = false
	)
	private User player;
	
	/**
	 * The currency associated with this balance.
	 */
	@ManyToOne
	@JoinColumn(
		name = "p_currency_id",
		nullable = false
	)
	private Currency currency;
	
	/**
	 * The balance of the user for this currency.
	 */
	@Column(
		name = "balance",
		nullable = false,
		columnDefinition = "DECIMAL(64, 2) DEFAULT '0.00'"
	)
	private double balance;
	
	/**
	 * Protected no-args constructor for JPA/Hibernate.
	 */
	protected UserCurrency() {
	
	}
	
	/**
	 * Constructs a new {@code UserCurrency} entity with a zero balance.
	 *
	 * @param player the user (player) associated with this balance, must not be null
	 * @param currency the currency associated with this balance, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public UserCurrency(
		final @NotNull User player,
		final @NotNull Currency currency
	) {
		this(
			player,
			currency,
			0.00
		);
	}
	
	/**
	 * Constructs a new {@code UserCurrency} entity with the specified balance.
	 *
	 * @param player the user (player) associated with this balance, must not be null
	 * @param currency the currency associated with this balance, must not be null
	 * @param initialBalance the initial balance for this currency
	 * @throws IllegalArgumentException if any parameter is null or balance is negative
	 */
	public UserCurrency(
		final @NotNull User player,
		final @NotNull Currency currency,
		final double initialBalance
	) {
		
		if (
			initialBalance < 0
		) {
			throw new IllegalArgumentException("Initial balance cannot be negative");
		}
		
		this.player = player;
		this.currency = currency;
		this.balance = initialBalance;
	}
	
	/**
	 * Gets the user (player) associated with this balance.
	 *
	 * @return the user entity, never null
	 */
	public @NotNull User getPlayer() {
		return player;
	}
	
	/**
	 * Sets the user (player) associated with this balance.
	 *
	 * @param newPlayer the new player to associate with this balance, must not be null
	 * @throws IllegalArgumentException if the player is null
	 */
	public void setPlayer(final @NotNull User newPlayer) {
		
		this.player = newPlayer;
	}
	
	/**
	 * Gets the currency associated with this balance.
	 *
	 * @return the currency entity, never null
	 */
	public @NotNull Currency getCurrency() {
		return currency;
	}
	
	/**
	 * Sets the currency associated with this balance.
	 *
	 * @param newCurrency the new currency to associate with this balance, must not be null
	 * @throws IllegalArgumentException if the currency is null
	 */
	public void setCurrency(final @NotNull Currency newCurrency) {
		
		this.currency = newCurrency;
	}
	
	/**
	 * Gets the current balance of the user for this currency.
	 *
	 * @return the current balance amount
	 */
	public double getBalance() {
		return balance;
	}
	
	/**
	 * Sets the balance of the user for this currency.
	 *
	 * @param newBalance the new balance to set
	 * @throws IllegalArgumentException if the balance is negative
	 */
	public void setBalance(final double newBalance) {
		if (
			newBalance < 0
		) {
			throw new IllegalArgumentException("Balance cannot be negative");
		}
		
		this.balance = newBalance;
	}
	
	/**
	 * Deposits the specified amount into the user's balance for this currency.
	 *
	 * @param depositAmount the amount to deposit, must not be null and must be positive
	 * @return {@code true} if the deposit was successful, otherwise {@code false}
	 * @throws IllegalArgumentException if the amount is null or negative
	 */
	public boolean deposit(final @NotNull Double depositAmount) {
		
		if (
			depositAmount < 0
		) {
			throw new IllegalArgumentException("Deposit amount cannot be negative");
		}
		if (
			depositAmount == 0
		) {
			return true;
		}
		
		try {
			this.balance += depositAmount;
			return true;
		} catch (
			  final Exception ignoredException
		) {
			return false;
		}
	}
	
	/**
	 * Withdraws the specified amount from the user's balance for this currency.
 *
 * <p>The withdrawal will only occur if the current balance is sufficient.
	 *
	 * @param withdrawalAmount the amount to withdraw, must not be null and must be positive
	 * @return {@code true} if the withdrawal was successful, otherwise {@code false}
	 * @throws IllegalArgumentException if the amount is null or negative
	 */
	public boolean withdraw(final @NotNull Double withdrawalAmount) {
		
		if (
			withdrawalAmount < 0
		) {
			throw new IllegalArgumentException("Withdrawal amount cannot be negative");
		}
		if (
			withdrawalAmount == 0
		) {
			return true;
		}
		
		try {
			if (
				this.balance < withdrawalAmount
			) {
				return false;
			}
			
			this.balance -= withdrawalAmount;
			return true;
		} catch (
			  final Exception ignoredException
		) {
			return false;
		}
	}
	
	/**
	 * Checks if the user has sufficient balance for the specified amount.
	 *
	 * @param requiredAmount the amount to check against the current balance
	 * @return {@code true} if the balance is sufficient, otherwise {@code false}
	 * @throws IllegalArgumentException if the amount is null or negative
	 */
	public boolean hasSufficientBalance(final @NotNull Double requiredAmount) {
		
		if (
			requiredAmount < 0
		) {
			throw new IllegalArgumentException("Required amount cannot be negative");
		}
		
		return this.balance >= requiredAmount;
	}
	
	/**
	 * Checks if this user currency is equal to another object.
 *
 * <p>Two UserCurrency entities are considered equal if they have the same player and currency.
	 *
	 * @param comparisonObject the object to compare with, can be null
	 * @return {@code true} if the objects are equal, otherwise {@code false}
	 */
	@Override
	public boolean equals(final @Nullable Object comparisonObject) {
		if (
			this == comparisonObject
		) {
			return true;
		}
		if (
			comparisonObject == null || getClass() != comparisonObject.getClass()
		) {
			return false;
		}
		
		final UserCurrency otherUserCurrency = (UserCurrency) comparisonObject;
		return Objects.equals(player, otherUserCurrency.player) &&
		       Objects.equals(currency, otherUserCurrency.currency);
	}
	
	/**
	 * Returns the hash code for this user currency, based on the player and currency.
	 *
	 * @return the hash code of this user currency
	 */
	@Override
	public int hashCode() {
		return Objects.hash(player, currency);
	}
	
	/**
	 * Returns a string representation of this user currency.
	 *
	 * @return a string containing the player, currency, and balance information
	 */
	@Override
	public @NotNull String toString() {
		return String.format(
			"UserCurrency{player=%s, currency=%s, balance=%.2f}",
			player != null ? player.getPlayerName() : "null",
			currency != null ? currency.getIdentifier() : "null",
			balance
		);
	}
}
