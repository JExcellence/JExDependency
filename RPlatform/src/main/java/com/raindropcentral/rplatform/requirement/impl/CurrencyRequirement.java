package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requirement for currency/economy checks.
 * Supports JExEconomy (multi-currency) and Vault (single currency) via reflection.
 * 
 * Example:
 * {"type": "CURRENCY", "currency": "coins", "amount": 1000}
 * {"type": "CURRENCY", "currency": "gems", "amount": 50, "consumable": true}
 */
public final class CurrencyRequirement extends AbstractRequirement {

	private static final Logger LOGGER = Logger.getLogger(CurrencyRequirement.class.getName());

	@JsonProperty("currency")
	private final String currencyId;

	@JsonProperty("amount")
	private final double amount;

	@JsonProperty("consumable")
	private final boolean consumable;

	/**
	 * Simple constructor
	 */
	public CurrencyRequirement(@NotNull String currencyId, double amount) {
		this(currencyId, amount, false);
	}

	/**
	 * Full constructor
	 */
	@JsonCreator
	public CurrencyRequirement(
		@JsonProperty("currency") @NotNull String currencyId,
		@JsonProperty("amount") double amount,
		@JsonProperty("consumable") @Nullable Boolean consumable
	) {
		super("CURRENCY", consumable != null && consumable);
		
		if (currencyId.trim().isEmpty()) {
			throw new IllegalArgumentException("Currency ID cannot be null or empty");
		}
		
		if (amount < 0) {
			throw new IllegalArgumentException("Amount must be non-negative");
		}
		
		this.currencyId = currencyId.toLowerCase();
		this.amount = amount;
		this.consumable = consumable != null && consumable;
	}

	@NotNull
	public String getCurrencyId() {
		return currencyId;
	}

	public double getAmount() {
		return amount;
	}

	public boolean isConsumable() {
		return consumable;
	}

	@Override
	public boolean isMet(@NotNull Player player) {
		return getCurrentBalance(player) >= amount;
	}

	@Override
	public double calculateProgress(@NotNull Player player) {
		if (amount <= 0) {
			return 1.0;
		}
		
		final double balance = getCurrentBalance(player);
		return Math.min(1.0, balance / amount);
	}

	@Override
	public void consume(@NotNull Player player) {
		if (!consumable) {
			return;
		}
		
		// Try JExEconomy first
		JExEconomyBridge bridge = JExEconomyBridge.getBridge();
		if (bridge != null) {
			boolean success = bridge.withdraw(player, currencyId, amount).join();
			if (!success) {
				LOGGER.log(Level.WARNING, "Failed to withdraw " + amount + " " + currencyId + 
						   " from " + player.getName());
			}
			return;
		}

		// Fall back to Vault
		try {
			if (isVaultCurrency(currencyId)) {
				Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
				RegisteredServiceProvider<?> vaultProvider =
						Bukkit.getServicesManager().getRegistration(economyClass);

				if (vaultProvider != null) {
					Object economy = vaultProvider.getProvider();
					Method withdrawMethod = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
					Object response = withdrawMethod.invoke(economy, player, amount);
					
					Object responseType = response.getClass().getField("type").get(response);
					String typeName = responseType.toString();
					
					if (!"SUCCESS".equals(typeName)) {
						String errorMessage = (String) response.getClass().getField("errorMessage").get(response);
						LOGGER.log(Level.WARNING, "Failed to withdraw " + amount + " " + currencyId + 
								   " from " + player.getName() + ": " + errorMessage);
					}
					return;
				}
			}
		} catch (ClassNotFoundException e) {
			// Vault not available
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error using Vault for " + player.getName(), e);
		}

		LOGGER.log(Level.WARNING, "Cannot consume - no economy plugin found for currency: " + currencyId);
	}

	@Override
	@NotNull
	public String getDescriptionKey() {
		return "requirement.currency." + currencyId;
	}
	
	/**
	 * Gets the display name of the currency.
	 */
	@JsonIgnore
	@NotNull
	public String getCurrencyDisplayName() {
		// Try JExEconomy first
		JExEconomyBridge bridge = JExEconomyBridge.getBridge();
		if (bridge != null) {
			return bridge.getCurrencyDisplayName(currencyId);
		}

		// Fall back to Vault display name
		if (isVaultCurrency(currencyId)) {
			return "Money";
		}

		// Final fallback: capitalize the identifier
		return currencyId.substring(0, 1).toUpperCase() + currencyId.substring(1);
	}
	
	/**
	 * Gets detailed description with current/required amounts.
	 */
	@JsonIgnore
	@NotNull
	public String getDetailedDescription(@NotNull Player player) {
		String displayName = getCurrencyDisplayName();
		double current = getCurrentBalance(player);
		
		if (current >= amount) {
			return String.format("Have %.0f/%.0f %s", current, amount, displayName);
		} else {
			return String.format("Need %.0f %s", amount, displayName);
		}
	}
	
	/**
	 * Gets the current balance for this currency
	 */
	@JsonIgnore
	public double getCurrentBalance(@NotNull Player player) {
		// Try JExEconomy first
		JExEconomyBridge bridge = JExEconomyBridge.getBridge();
		if (bridge != null) {
			return bridge.getBalance(player, currencyId);
		}

		// Fall back to Vault
		try {
			if (isVaultCurrency(currencyId)) {
				Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
				RegisteredServiceProvider<?> vaultProvider =
						Bukkit.getServicesManager().getRegistration(economyClass);

				if (vaultProvider != null) {
					Object economy = vaultProvider.getProvider();
					Method getBalanceMethod = economy.getClass().getMethod("getBalance", Player.class);
					return (Double) getBalanceMethod.invoke(economy, player);
				}
			}
		} catch (ClassNotFoundException e) {
			// Vault not available
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting Vault balance for " + player.getName(), e);
		}

		return 0.0;
	}
	
	/**
	 * Checks if this is a Vault currency identifier
	 */
	@JsonIgnore
	private boolean isVaultCurrency(String identifier) {
		return "vault".equalsIgnoreCase(identifier) || 
			   "money".equalsIgnoreCase(identifier) ||
			   "dollars".equalsIgnoreCase(identifier);
	}
	
	/**
	 * Validates this requirement
	 */
	@JsonIgnore
	public void validate() {
		JExEconomyBridge bridge = JExEconomyBridge.getBridge();
		if (bridge != null) {
			return;
		}
		
		try {
			Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
			boolean hasVault = Bukkit.getServicesManager().getRegistration(economyClass) != null;
			if (hasVault) {
				return;
			}
		} catch (ClassNotFoundException e) {
			// Vault not available
		}
		
		throw new IllegalStateException("No economy plugin found (JExEconomy or Vault required)");
	}
}
