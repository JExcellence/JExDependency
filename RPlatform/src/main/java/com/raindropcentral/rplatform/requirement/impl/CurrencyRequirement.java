package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Requirement for currency/economy checks.
 * Supports JExEconomy (multi-currency) and Vault (single currency) directly.
 * 
 * Example:
 * {"type": "CURRENCY", "currency": "coins", "amount": 1000}
 * {"type": "CURRENCY", "currency": "gems", "amount": 50, "consumable": true}
 */
public final class CurrencyRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(CurrencyRequirement.class.getName());
    
    // Cache for currency lookups to avoid repeated lookups
    private static final Map<String, Currency> CURRENCY_CACHE = new ConcurrentHashMap<>();

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
        
        try {
            // Try JExEconomy first (direct API call)
            RegisteredServiceProvider<CurrencyAdapter> jexProvider =
                    Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);

            if (jexProvider != null) {
                CurrencyAdapter adapter = jexProvider.getProvider();
                Currency currency = findJExCurrency(adapter, currencyId);

                if (currency != null) {
                    CompletableFuture<CurrencyResponse> withdrawFuture = 
                        adapter.withdraw((OfflinePlayer) player, currency, amount);
                    CurrencyResponse response = withdrawFuture.join();
                    
                    if (!response.isTransactionSuccessful()) {
                        LOGGER.log(Level.WARNING, "Failed to withdraw " + amount + " " + currencyId + 
                                   " from " + player.getName());
                    }
                    return;
                }
            }
        } catch (NoClassDefFoundError e) {
            // JExEconomy not available, try Vault
            LOGGER.log(Level.FINE, "JExEconomy not available, trying Vault");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error using JExEconomy for " + player.getName(), e);
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
     * Uses JExEconomy currency identifier or falls back to capitalized identifier.
     */
    @JsonIgnore
    @NotNull
    public String getCurrencyDisplayName() {
        try {
            // Try JExEconomy first (direct API call)
            RegisteredServiceProvider<CurrencyAdapter> jexProvider =
                    Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);

            if (jexProvider != null) {
                CurrencyAdapter adapter = jexProvider.getProvider();
                Currency currency = findJExCurrency(adapter, currencyId);

                if (currency != null) {
                    // Use the currency identifier as display name
                    String identifier = currency.getIdentifier();
                    if (identifier != null && !identifier.trim().isEmpty()) {
                        return identifier.substring(0, 1).toUpperCase() + identifier.substring(1);
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            // JExEconomy not available
            LOGGER.log(Level.FINE, "JExEconomy not available for display name");
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error getting currency display name: " + currencyId, e);
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
     * Format: "Have X/Y Currency" or "Need Y Currency"
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
        try {
            // Try JExEconomy first (direct API call)
            RegisteredServiceProvider<CurrencyAdapter> jexProvider =
                    Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);

            if (jexProvider != null) {
                LOGGER.log(Level.FINE, "Using JExEconomy for currency: {0}", currencyId);
                CurrencyAdapter adapter = jexProvider.getProvider();
                Currency currency = findJExCurrency(adapter, currencyId);

                if (currency != null) {
                    CompletableFuture<Double> balanceFuture = adapter.getBalance((OfflinePlayer) player, currency);
                    return balanceFuture.join();
                } else {
                    LOGGER.log(Level.WARNING, "Currency ''{0}'' not found in JExEconomy for player {1}", 
                        new Object[]{currencyId, player.getName()});
                }
            }
        } catch (NoClassDefFoundError e) {
            // JExEconomy not available, try Vault
            LOGGER.log(Level.FINE, "JExEconomy not available, trying Vault");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting JExEconomy balance for " + player.getName() + 
                    " with currency '" + currencyId + "'", e);
        }

        // Fall back to Vault
        try {
            if (isVaultCurrency(currencyId)) {
                LOGGER.log(Level.FINE, "Using Vault for currency: {0}", currencyId);
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> vaultProvider =
                        Bukkit.getServicesManager().getRegistration(economyClass);

                if (vaultProvider != null) {
                    Object economy = vaultProvider.getProvider();
                    Method getBalanceMethod = economy.getClass().getMethod("getBalance", Player.class);
                    return (Double) getBalanceMethod.invoke(economy, player);
                } else {
                    LOGGER.log(Level.WARNING, "Vault economy provider not found");
                }
            } else {
                LOGGER.log(Level.WARNING, "Currency ''{0}'' is not a valid Vault currency identifier", new Object[]{currencyId});
            }
        } catch (ClassNotFoundException e) {
            // Vault not available
            LOGGER.log(Level.WARNING, "Vault not available");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting Vault balance for " + player.getName(), e);
        }

        LOGGER.log(Level.WARNING, "No economy plugin found for currency ''{0}''", new Object[]{currencyId});
        return 0.0;
    }
    
    /**
     * Finds a currency by identifier in JExEconomy (direct API call)
     * Includes caching and detailed logging for debugging
     */
    @JsonIgnore
    @Nullable
    private Currency findJExCurrency(CurrencyAdapter adapter, String identifier) {
        // Check cache first
        String cacheKey = identifier.toLowerCase();
        if (CURRENCY_CACHE.containsKey(cacheKey)) {
            return CURRENCY_CACHE.get(cacheKey);
        }
        
        try {
            Map<Long, Currency> currencies = adapter.getAllCurrencies();
            
            // Case-insensitive search
            for (Currency currency : currencies.values()) {
                String currencyIdentifier = currency.getIdentifier();
                
                if (currencyIdentifier.equalsIgnoreCase(identifier)) {
                    CURRENCY_CACHE.put(cacheKey, currency);
                    LOGGER.log(Level.FINE, "Found JExEconomy currency: {0}", identifier);
                    return currency;
                }
            }
            
            // Currency not found - log available currencies for debugging
            List<String> availableCurrencies = currencies.values().stream()
                .map(Currency::getIdentifier)
                .collect(Collectors.toList());
            
            LOGGER.log(Level.WARNING, 
                "Currency ''{0}'' not found in JExEconomy. Available currencies: {1}", 
                new Object[]{identifier, availableCurrencies});
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding currency: " + identifier, e);
            return null;
        }
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
        try {
            RegisteredServiceProvider<CurrencyAdapter> jexProvider = 
                Bukkit.getServicesManager().getRegistration(CurrencyAdapter.class);
            if (jexProvider != null) {
                return;
            }
        } catch (NoClassDefFoundError e) {
            // JExEconomy not available
            LOGGER.log(Level.FINE, "JExEconomy not available during validation");
        }
        
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            boolean hasVault = Bukkit.getServicesManager().getRegistration(economyClass) != null;
            if (hasVault) {
                return;
            }
        } catch (ClassNotFoundException e) {
            // Vault not available
            LOGGER.log(Level.FINE, "Vault not available during validation");
        }
        
        throw new IllegalStateException("No economy plugin found (JExEconomy or Vault required)");
    }
}
