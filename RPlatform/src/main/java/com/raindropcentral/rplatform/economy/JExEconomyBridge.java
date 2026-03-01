package com.raindropcentral.rplatform.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge to JExEconomy using reflection to avoid compile-time dependency.
 * This allows RPlatform to work with JExEconomy without creating a circular dependency.
 */
public class JExEconomyBridge {
	
	private static final Logger LOGGER = Logger.getLogger(JExEconomyBridge.class.getName());
	private static final String ADAPTER_CLASS = "de.jexcellence.economy.adapter.CurrencyAdapter";
	private static final String RESPONSE_CLASS = "de.jexcellence.economy.adapter.CurrencyResponse";
	private static final String CURRENCY_CLASS = "de.jexcellence.economy.database.entity.Currency";
	
	private final Object adapter;
	private final Class<?> adapterClass;
	private final Class<?> responseClass;
	private final Class<?> currencyClass;
	
	private JExEconomyBridge(Object adapter, Class<?> adapterClass, Class<?> responseClass, Class<?> currencyClass) {
		this.adapter = adapter;
		this.adapterClass = adapterClass;
		this.responseClass = responseClass;
		this.currencyClass = currencyClass;
	}
	
	/**
	 * Attempts to get a JExEconomy bridge if the plugin is available.
	 *
	 * @return a bridge instance, or null if JExEconomy is not available
	 */
	@Nullable
	public static JExEconomyBridge getBridge() {
		try {
			Class<?> adapterClass = Class.forName(ADAPTER_CLASS);
			Class<?> responseClass = Class.forName(RESPONSE_CLASS);
			Class<?> currencyClass = Class.forName(CURRENCY_CLASS);
			
			Object provider = Bukkit.getServicesManager().getRegistration(adapterClass);
			if (provider == null) {
				return null;
			}
			
			Method getProvider = provider.getClass().getMethod("getProvider");
			Object adapter = getProvider.invoke(provider);
			
			return new JExEconomyBridge(adapter, adapterClass, responseClass, currencyClass);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "JExEconomy not available", e);
			return null;
		}
	}
	
	/**
	 * Withdraws currency from a player.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @param amount the amount to withdraw
	 * @return a future with the response
	 */
	@NotNull
	public CompletableFuture<Boolean> withdraw(@NotNull OfflinePlayer player, @NotNull String currencyId, double amount) {
		try {
			Object currency = findCurrency(currencyId);
			if (currency == null) {
				return CompletableFuture.completedFuture(false);
			}
			
			Method withdrawMethod = adapterClass.getMethod("withdraw", OfflinePlayer.class, currencyClass, double.class);
			Object futureObj = withdrawMethod.invoke(adapter, player, currency, amount);
			
			@SuppressWarnings("unchecked")
			CompletableFuture<Object> future = (CompletableFuture<Object>) futureObj;
			
			return future.thenApply(response -> {
				try {
					Method isSuccessMethod = responseClass.getMethod("isSuccess");
					return (Boolean) isSuccessMethod.invoke(response);
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error checking response", e);
					return false;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error withdrawing currency", e);
			return CompletableFuture.completedFuture(false);
		}
	}

	/**
	 * Deposits currency to a player.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @param amount the amount to deposit
	 * @return a future with the response
	 */
	@NotNull
	public CompletableFuture<Boolean> deposit(@NotNull OfflinePlayer player, @NotNull String currencyId, double amount) {
		try {
			Object currency = findCurrency(currencyId);
			if (currency == null) {
				return CompletableFuture.completedFuture(false);
			}

			Method depositMethod = adapterClass.getMethod("deposit", OfflinePlayer.class, currencyClass, double.class);
			Object futureObj = depositMethod.invoke(adapter, player, currency, amount);

			@SuppressWarnings("unchecked")
			CompletableFuture<Object> future = (CompletableFuture<Object>) futureObj;

			return future.thenApply(response -> {
				try {
					Method isSuccessMethod = responseClass.getMethod("isSuccess");
					return (Boolean) isSuccessMethod.invoke(response);
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error checking response", e);
					return false;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error depositing currency", e);
			return CompletableFuture.completedFuture(false);
		}
	}
	
	/**
	 * Gets the balance of a currency for a player.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @return the balance
	 */
	public double getBalance(@NotNull OfflinePlayer player, @NotNull String currencyId) {
		try {
			Object currency = findCurrency(currencyId);
			if (currency == null) {
				return 0.0;
			}
			
			Method getBalanceMethod = adapterClass.getMethod("getBalance", OfflinePlayer.class, currencyClass);
			Object futureObj = getBalanceMethod.invoke(adapter, player, currency);
			
			@SuppressWarnings("unchecked")
			CompletableFuture<Double> future = (CompletableFuture<Double>) futureObj;
			Double balance = future.join();
			
			return balance != null ? balance : 0.0;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting balance", e);
			return 0.0;
		}
	}
	
	/**
	 * Checks if a player has enough currency.
	 *
	 * @param player the player
	 * @param currencyId the currency identifier
	 * @param amount the amount to check
	 * @return true if the player has enough
	 */
	public boolean has(@NotNull OfflinePlayer player, @NotNull String currencyId, double amount) {
		try {
			Object currency = findCurrency(currencyId);
			if (currency == null) {
				return false;
			}
			
			Method getBalanceMethod = adapterClass.getMethod("getBalance", OfflinePlayer.class, currencyClass);
			Object futureObj = getBalanceMethod.invoke(adapter, player, currency);
			
			@SuppressWarnings("unchecked")
			CompletableFuture<Double> future = (CompletableFuture<Double>) futureObj;
			Double balance = future.join();
			
			return balance != null && balance >= amount;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error checking balance", e);
			return false;
		}
	}
	
	/**
	 * Gets the display name of a currency.
	 *
	 * @param currencyId the currency identifier
	 * @return the display name, or the identifier if not found
	 */
	@NotNull
	public String getCurrencyDisplayName(@NotNull String currencyId) {
		try {
			Object currency = findCurrency(currencyId);
			if (currency == null) {
				return currencyId;
			}
			
			Method getDisplayNameMethod = currencyClass.getMethod("getDisplayName");
			String displayName = (String) getDisplayNameMethod.invoke(currency);
			return displayName != null ? displayName : currencyId;
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Error getting currency display name", e);
			return currencyId;
		}
	}

	/**
	 * Checks whether a currency identifier is available through JExEconomy.
	 *
	 * @param currencyId the currency identifier
	 * @return true if the currency exists
	 */
	public boolean hasCurrency(@NotNull String currencyId) {
		if (currencyId.isBlank()) {
			return false;
		}

		try {
			return findCurrency(currencyId.trim()) != null;
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Error checking currency availability", e);
			return false;
		}
	}
	
	/**
	 * Finds a currency by identifier.
	 *
	 * @param identifier the currency identifier
	 * @return the currency object, or null if not found
	 */
	@Nullable
	private Object findCurrency(@NotNull String identifier) {
		try {
			Method getAllCurrenciesMethod = adapterClass.getMethod("getAllCurrencies");
			Object mapObj = getAllCurrenciesMethod.invoke(adapter);
			
			@SuppressWarnings("unchecked")
			Map<Long, Object> currencies = (Map<Long, Object>) mapObj;
			
			for (Object currency : currencies.values()) {
				Method getIdentifierMethod = currencyClass.getMethod("getIdentifier");
				String currencyId = (String) getIdentifierMethod.invoke(currency);
				
				if (identifier.equalsIgnoreCase(currencyId)) {
					return currency;
				}
			}
			
			return null;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error finding currency", e);
			return null;
		}
	}
}
