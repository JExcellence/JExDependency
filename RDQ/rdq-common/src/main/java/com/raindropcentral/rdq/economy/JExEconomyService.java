package com.raindropcentral.rdq.economy;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JExEconomy implementation of the EconomyService.
 * Uses reflection to interact with JExEconomy's CurrencyAdapter.
 */
public class JExEconomyService implements EconomyService {
	
	private static final Logger LOGGER = CentralLogger.getLogger("RDQ-Economy");
	
	private final Object currencyAdapter;
	private Object jexEconomyImpl;
	private Method getBalanceMethod;
	private Method depositMethod;
	private Method withdrawMethod;
	private Method getCurrencyByIdMethod;
	
	public JExEconomyService(@NotNull Object currencyAdapter) {
		this.currencyAdapter = currencyAdapter;
		initializeMethods();
	}
	
	private void initializeMethods() {
		try {
			Class<?> adapterClass = currencyAdapter.getClass();
			
			// Get the JExEconomy instance from the adapter
			try {
				java.lang.reflect.Field implField = adapterClass.getDeclaredField("jexEconomyImpl");
				implField.setAccessible(true);
				jexEconomyImpl = implField.get(currencyAdapter);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Could not get JExEconomy instance", e);
			}
			
			// Find methods - CurrencyAdapter API:
			// CompletableFuture<Double> getBalance(OfflinePlayer, Currency)
			// CompletableFuture<CurrencyResponse> deposit(OfflinePlayer, Currency, double, ...)
			// CompletableFuture<CurrencyResponse> withdraw(OfflinePlayer, Currency, double, ...)
			for (Method method : adapterClass.getMethods()) {
				String name = method.getName();
				if (name.equals("getBalance") && method.getParameterCount() == 2) {
					getBalanceMethod = method;
				} else if (name.equals("deposit")) {
					depositMethod = method;
				} else if (name.equals("withdraw")) {
					withdrawMethod = method;
				}
			}
			
			// Get method to find Currency by ID from JExEconomy
			if (jexEconomyImpl != null) {
				try {
					Class<?> implClass = jexEconomyImpl.getClass();
					// Look for getCurrencyRepository or similar
					Method getCurrencyRepoMethod = implClass.getMethod("getCurrencyRepository");
					Object currencyRepo = getCurrencyRepoMethod.invoke(jexEconomyImpl);
					
					// Get the findByIdentifier method
					getCurrencyByIdMethod = currencyRepo.getClass().getMethod("getCachedByKey");
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Could not get currency repository methods", e);
				}
			}
			
			if (getBalanceMethod != null && depositMethod != null && withdrawMethod != null) {
				LOGGER.info("JExEconomy methods initialized successfully");
			} else {
				LOGGER.warning("Some JExEconomy methods could not be found: " +
						"getBalance=" + (getBalanceMethod != null) +
						", deposit=" + (depositMethod != null) +
						", withdraw=" + (withdrawMethod != null));
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize JExEconomy methods", e);
		}
	}
	
	private Object getCurrency(String currencyId) {
		try {
			if (getCurrencyByIdMethod != null) {
				// getCachedByKey() returns a Map<String, Currency>
				@SuppressWarnings("unchecked")
				Map<String, Object> currencyMap = (Map<String, Object>) getCurrencyByIdMethod.invoke(
						jexEconomyImpl.getClass().getMethod("getCurrencyRepository").invoke(jexEconomyImpl)
				);
				return currencyMap.get(currencyId);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting currency: " + currencyId, e);
		}
		return null;
	}
	
	@Override
	public CompletableFuture<Boolean> has(@NotNull Player player, @NotNull String currencyId, double amount) {
		return getBalance(player, currencyId).thenApply(balance -> balance >= amount);
	}
	
	@Override
	public CompletableFuture<Boolean> hasAll(@NotNull Player player, @NotNull Map<String, Double> requiredCurrencies) {
		return CompletableFuture.supplyAsync(() -> {
			for (Map.Entry<String, Double> entry : requiredCurrencies.entrySet()) {
				try {
					if (!has(player, entry.getKey(), entry.getValue()).join()) {
						return false;
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error checking currency " + entry.getKey(), e);
					return false;
				}
			}
			return true;
		});
	}
	
	@Override
	public CompletableFuture<Double> getBalance(@NotNull Player player, @NotNull String currencyId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (getBalanceMethod == null) {
					LOGGER.warning("getBalance method not available");
					return 0.0;
				}
				
				Object currency = getCurrency(currencyId);
				if (currency == null) {
					LOGGER.warning("Currency not found: " + currencyId);
					return 0.0;
				}
				
				@SuppressWarnings("unchecked")
				CompletableFuture<Double> balanceFuture = (CompletableFuture<Double>) 
						getBalanceMethod.invoke(currencyAdapter, player, currency);
				
				return balanceFuture.join();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting balance for " + player.getName(), e);
				return 0.0;
			}
		});
	}
	
	@Override
	public CompletableFuture<Boolean> withdraw(@NotNull Player player, @NotNull String currencyId, double amount) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (withdrawMethod == null) {
					LOGGER.warning("withdraw method not available");
					return false;
				}
				
				Object currency = getCurrency(currencyId);
				if (currency == null) {
					LOGGER.warning("Currency not found: " + currencyId);
					return false;
				}
				
				// withdraw(OfflinePlayer, Currency, double, EChangeType, String, boolean)
				// We'll use reflection to find the right overload
				@SuppressWarnings("unchecked")
				CompletableFuture<Object> responseFuture = (CompletableFuture<Object>) 
						withdrawMethod.invoke(currencyAdapter, player, currency, amount, 
								null, "RDQ Requirement", false);
				
				Object response = responseFuture.join();
				// Check if response indicates success
				Method isSuccessMethod = response.getClass().getMethod("isSuccess");
				return (Boolean) isSuccessMethod.invoke(response);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error withdrawing currency for " + player.getName(), e);
				return false;
			}
		});
	}
	
	@Override
	public CompletableFuture<Boolean> withdrawAll(@NotNull Player player, @NotNull Map<String, Double> currencies) {
		return CompletableFuture.supplyAsync(() -> {
			for (Map.Entry<String, Double> entry : currencies.entrySet()) {
				try {
					if (!withdraw(player, entry.getKey(), entry.getValue()).join()) {
						return false;
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error withdrawing currency " + entry.getKey(), e);
					return false;
				}
			}
			return true;
		});
	}
	
	@Override
	public boolean isAvailable() {
		return currencyAdapter != null && getBalanceMethod != null && withdrawMethod != null;
	}
}
