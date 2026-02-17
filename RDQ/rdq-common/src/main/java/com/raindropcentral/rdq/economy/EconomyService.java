package com.raindropcentral.rdq.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for economy operations.
 * Provides async methods for checking and manipulating player currencies.
 */
public interface EconomyService {
	
	/**
	 * Checks if a player has the required amount of a specific currency.
	 *
	 * @param player the player to check
	 * @param currencyId the currency identifier
	 * @param amount the required amount
	 * @return future completing with true if player has enough currency
	 */
	CompletableFuture<Boolean> has(@NotNull Player player, @NotNull String currencyId, double amount);
	
	/**
	 * Checks if a player has all required currencies.
	 *
	 * @param player the player to check
	 * @param requiredCurrencies map of currency IDs to required amounts
	 * @return future completing with true if player has all required currencies
	 */
	CompletableFuture<Boolean> hasAll(@NotNull Player player, @NotNull Map<String, Double> requiredCurrencies);
	
	/**
	 * Gets the player's balance for a specific currency.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @return future completing with the player's balance
	 */
	CompletableFuture<Double> getBalance(@NotNull Player player, @NotNull String currencyId);
	
	/**
	 * Withdraws currency from a player's account.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @param amount the amount to withdraw
	 * @return future completing with true if withdrawal was successful
	 */
	CompletableFuture<Boolean> withdraw(@NotNull Player player, @NotNull String currencyId, double amount);
	
	/**
	 * Withdraws multiple currencies from a player's account.
	 *
	 * @param player the player
	 * @param currencies map of currency IDs to amounts to withdraw
	 * @return future completing with true if all withdrawals were successful
	 */
	CompletableFuture<Boolean> withdrawAll(@NotNull Player player, @NotNull Map<String, Double> currencies);
	
	/**
	 * Checks if the economy system is available.
	 *
	 * @return true if economy system is loaded and ready
	 */
	boolean isAvailable();
}
