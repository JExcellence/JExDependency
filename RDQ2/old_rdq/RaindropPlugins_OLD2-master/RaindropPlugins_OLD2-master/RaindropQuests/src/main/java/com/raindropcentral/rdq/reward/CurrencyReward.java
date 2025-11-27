package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reward that grants in-game currency to a player.
 * <p>
 * This reward assumes integration with an external economy plugin or system.
 * When applied, the specified amount of currency should be deposited into the player's account.
 * The actual deposit logic must be implemented according to the server's economy API.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     CurrencyReward reward = new CurrencyReward(100.0);
 *     reward.apply(player);
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class CurrencyReward extends AbstractReward {
	
	/**
	 * The amount of currency to grant to the player.
	 */
	private final double amount;
	
	/**
	 * Constructs a new {@code CurrencyReward} with the specified amount.
	 *
	 * @param amount The amount of currency to reward the player.
	 */
	public CurrencyReward(double amount) {
		super(Type.CURRENCY);
		this.amount = amount;
	}
	
	/**
	 * Applies the currency reward to the specified player.
	 * <p>
	 * This method should deposit the configured amount of currency into the player's account
	 * using the server's economy integration.
	 * </p>
	 *
	 * @param player The player to receive the currency reward.
	 */
	@Override
	public void apply(@NotNull Player player) {
		//TODO APPLY REWARD
		
		// Example: EconomyAPI.deposit(player, amount);
		// Replace with your actual economy integration
	}
	
	/**
	 * Returns the translation key for this reward's description.
	 * <p>
	 * Used for localization and display in user interfaces.
	 * </p>
	 *
	 * @return The language key for this reward's description.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		return "reward.currency";
	}
	
	/**
	 * Gets the amount of currency to be rewarded.
	 *
	 * @return The currency amount.
	 */
	public double getAmount() {
		return amount;
	}
}