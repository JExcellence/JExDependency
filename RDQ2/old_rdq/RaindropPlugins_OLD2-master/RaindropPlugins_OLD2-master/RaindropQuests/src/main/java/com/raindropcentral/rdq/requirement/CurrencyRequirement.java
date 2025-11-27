package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logger.CentralLogger;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Asynchronous implementation of a requirement based on player-held currencies.
 * <p>
 * This requirement checks whether a player possesses sufficient amounts of one or more currencies,
 * and supports asynchronous operations to avoid blocking the main thread during balance checks or withdrawals.
 * <br>
 * The class provides both asynchronous and synchronous (blocking) methods for checking fulfillment,
 * calculating progress, and consuming currencies, maintaining compatibility with the synchronous
 * {@link AbstractRequirement} interface.
 * </p>
 *
 * <ul>
 *   <li>Use this requirement to enforce that a player must have (and optionally spend) specific amounts of one or more currencies.</li>
 *   <li>Supports integration with asynchronous economy providers for non-blocking operations.</li>
 *   <li>Progress is calculated as the average fulfillment ratio across all required currencies.</li>
 *   <li>Supports configuration via string identifiers that are resolved to Currency objects.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class CurrencyRequirement extends AbstractRequirement {
    
    private static final Logger LOGGER = CentralLogger.getLogger(CurrencyRequirement.class.getName());
    
    /**
     * Map of currencies and their required amounts that the player must possess.
     * <p>
     * The key is the {@link Currency} object, and the value is the required amount for that currency.
     * </p>
     */
    @JsonIgnore
    private final Map<Currency, Double> requiredCurrencies;
    
    /**
     * Map of currency identifiers to required amounts for JSON serialization.
     * This is used for configuration and database storage.
     */
    @JsonProperty("requiredCurrencies")
    private final Map<String, Double> currencyIdentifiers;
    
    /**
     * Optional currency plugin identifier for future extensibility.
     */
    @JsonProperty("currencyPlugin")
    private final String currencyPlugin;
    
    /**
     * Constructs a new {@code CurrencyRequirement} with Currency objects (runtime creation).
     *
     * @param requiredCurrencies A map of {@link Currency} objects to the required amount for each.
     */
    public CurrencyRequirement(
        @NotNull final Map<Currency, Double> requiredCurrencies
    ) {
        
        super(Type.CURRENCY);
        this.requiredCurrencies = new HashMap<>(requiredCurrencies);
        this.currencyIdentifiers = requiredCurrencies.entrySet().stream()
                                                     .collect(Collectors.toMap(
                                                         entry -> entry.getKey().getIdentifier(),
                                                         Map.Entry::getValue
                                                     ));
        this.currencyPlugin = null;
    }
    
    /**
     * Constructs a new {@code CurrencyRequirement} with string identifiers (configuration creation).
     *
     * @param currencyIdentifiers A map of currency identifiers to required amounts.
     * @param currencyPlugin      Optional currency plugin identifier.
     */
    @JsonCreator
    public CurrencyRequirement(
        @JsonProperty("requiredCurrencies") @NotNull final Map<String, Double> currencyIdentifiers,
        @JsonProperty("currencyPlugin") @Nullable final String currencyPlugin
    ) {
        
        super(Type.CURRENCY);
        this.currencyIdentifiers = new HashMap<>(currencyIdentifiers);
        this.currencyPlugin = currencyPlugin;
        this.requiredCurrencies = this.resolveCurrencies();
    }
    
    /**
     * Returns an unmodifiable copy of the required currencies and their amounts.
     *
     * @return Unmodifiable map of required {@link Currency} objects and their amounts.
     */
    @NotNull
    public Map<Currency, Double> getRequiredCurrencies() {
        
        return Collections.unmodifiableMap(this.requiredCurrencies);
    }
    
    /**
     * Gets the currency plugin identifier.
     *
     * @return The currency plugin identifier, or null for default.
     */
    @Nullable
    public String getCurrencyPlugin() {
        
        return this.currencyPlugin;
    }
    
    /**
     * Asynchronously checks if the player has enough balance for every required currency.
     * <p>
     * This method is intended for use with asynchronous economy providers to avoid blocking the main thread.
     * </p>
     *
     * @param player The player whose balance will be verified.
     *
     * @return A {@link CompletableFuture} that resolves to {@code true} if the player meets all currency requirements; {@code false} otherwise.
     */
    @NotNull
    public CompletableFuture<Boolean> isMetAsync(
        @NotNull final Player player
    ) {
        
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(
                Level.WARNING,
                "Currency adapter not available"
            );
            return CompletableFuture.completedFuture(false);
        }
        
        final List<CompletableFuture<Boolean>> futures = this.requiredCurrencies.entrySet().stream()
                                                                                .map(entry -> {
                                                                                    final Currency currency       = entry.getKey();
                                                                                    final double   requiredAmount = entry.getValue();
                                                                                    
                                                                                    return adapter.getBalance(
                                                                                                      player,
                                                                                                      currency
                                                                                                  )
                                                                                                  .thenApply(balance -> balance >= requiredAmount)
                                                                                                  .exceptionally(throwable -> {
                                                                                                      LOGGER.log(
                                                                                                          Level.WARNING,
                                                                                                          "Failed to check balance for currency " + currency.getIdentifier(),
                                                                                                          throwable
                                                                                                      );
                                                                                                      return false;
                                                                                                  });
                                                                                })
                                                                                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .thenApply(ignored -> futures.stream().allMatch(CompletableFuture::join));
    }
    
    /**
     * Asynchronously calculates the progress towards fulfilling the currency requirement.
     * <p>
     * Progress for each currency is defined as the ratio between the player's current balance
     * (capped at the required amount) and the required amount. The overall progress is the average
     * of individual progress values, providing a normalized indication of how close the player is
     * to meeting all currency requirements.
     * </p>
     *
     * @param player The player whose balances will be evaluated.
     *
     * @return A {@link CompletableFuture} that resolves to a double between 0.0 and 1.0 representing overall progress.
     */
    @NotNull
    public CompletableFuture<Double> calculateProgressAsync(
        @NotNull final Player player
    ) {
        
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(
                Level.WARNING,
                "Currency adapter not available"
            );
            return CompletableFuture.completedFuture(0.0);
        }
        
        final List<CompletableFuture<Double>> futures = this.requiredCurrencies.entrySet().stream()
                                                                               .map(entry -> {
                                                                                   final Currency currency       = entry.getKey();
                                                                                   final double   requiredAmount = entry.getValue();
                                                                                   
                                                                                   return adapter.getBalance(
                                                                                                     player,
                                                                                                     currency
                                                                                                 )
                                                                                                 .thenApply(balance -> Math.min(
                                                                                                     balance,
                                                                                                     requiredAmount
                                                                                                 ) / requiredAmount)
                                                                                                 .exceptionally(throwable -> {
                                                                                                     LOGGER.log(
                                                                                                         Level.WARNING,
                                                                                                         "Failed to calculate progress for currency " + currency.getIdentifier(),
                                                                                                         throwable
                                                                                                     );
                                                                                                     return 0.0;
                                                                                                 });
                                                                               })
                                                                               .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .thenApply(ignored -> {
                                    final double totalProgress = futures.stream().mapToDouble(CompletableFuture::join).sum();
                                    final int    count         = this.requiredCurrencies.size();
                                    return count > 0 ?
                                           totalProgress / count :
                                           1.0;
                                });
    }
    
    /**
     * Asynchronously consumes the required amount of each currency from the player's account.
     * <p>
     * This method withdraws the specified amount for each currency, using asynchronous operations
     * to avoid blocking the main thread.
     * </p>
     *
     * @param player The player from whose account the currency will be deducted.
     *
     * @return A {@link CompletableFuture} that completes when all withdrawals are done.
     */
    @NotNull
    public CompletableFuture<Void> consumeAsync(
        @NotNull final Player player
    ) {
        
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(
                Level.WARNING,
                "Currency adapter not available"
            );
            return CompletableFuture.completedFuture(null);
        }
        
        final List<CompletableFuture<Object>> futures = this.requiredCurrencies.entrySet().stream()
                                                                               .map(entry -> {
                                                                                   final Currency currency       = entry.getKey();
                                                                                   final double   requiredAmount = entry.getValue();
                                                                                   
                                                                                   return adapter.withdraw(
                                                                                                     player,
                                                                                                     currency,
                                                                                                     requiredAmount
                                                                                                 )
                                                                                                 .thenApply(response -> null)
                                                                                                 .exceptionally(throwable -> {
                                                                                                     LOGGER.log(
                                                                                                         Level.WARNING,
                                                                                                         "Failed to withdraw currency " + currency.getIdentifier(),
                                                                                                         throwable
                                                                                                     );
                                                                                                     return null;
                                                                                                 });
                                                                               })
                                                                               .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Synchronously checks if the player meets all currency requirements.
     * <p>
     * This method blocks until the asynchronous check completes and should be used only when
     * synchronous logic is required.
     * </p>
     *
     * @param player The player whose balance will be verified.
     *
     * @return {@code true} if the player meets all currency requirements; {@code false} otherwise.
     */
    @Override
    public boolean isMet(
        @NotNull final Player player
    ) {
        
        try {
            return this.isMetAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(
                Level.WARNING,
                "Error checking currency requirements",
                exception
            );
            return false;
        }
    }
    
    /**
     * Synchronously calculates the progress towards fulfilling the currency requirement.
     * <p>
     * This method blocks until the asynchronous calculation completes and should be used only when
     * synchronous logic is required.
     * </p>
     *
     * @param player The player whose balances will be evaluated.
     *
     * @return A double between 0.0 and 1.0 representing overall progress.
     */
    @Override
    public double calculateProgress(
        @NotNull final Player player
    ) {
        
        try {
            return this.calculateProgressAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(
                Level.WARNING,
                "Error calculating currency progress",
                exception
            );
            return 0.0;
        }
    }
    
    /**
     * Synchronously consumes the required amount of each currency from the player's account.
     * <p>
     * This method blocks until all asynchronous withdrawals complete and should be used only when
     * synchronous logic is required.
     * </p>
     *
     * @param player The player from whose account the currency will be deducted.
     */
    @Override
    public void consume(
        @NotNull final Player player
    ) {
        
        try {
            this.consumeAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(
                Level.WARNING,
                "Error consuming currencies",
                exception
            );
        }
    }
    
    /**
     * Returns the translation key for this requirement's description.
     * <p>
     * This key can be used for localization and user-facing descriptions.
     * </p>
     *
     * @return The language key for this requirement's description, typically {@code "requirement.currency"}.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        
        return "requirement.currency";
    }
    
    /**
     * Gets the currency adapter for the configured plugin.
     *
     * @return The currency adapter, or null if not available.
     */
    @Nullable
    private CurrencyAdapter getCurrencyAdapter() {
        // For now, only support JECurrency, but extensible for future plugins
        if (this.currencyPlugin == null || "jecurrency".equalsIgnoreCase(this.currencyPlugin)) {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JECurrency");
            if (plugin instanceof JECurrency jeCurrency) {
                return jeCurrency.getImpl().getCurrencyAdapter();
            }
        }
        // Future: Add support for other currency plugins here
        return null;
    }
    
    /**
     * Resolves currency identifiers to Currency objects.
     *
     * @return Map of resolved Currency objects.
     */
    @NotNull
    private Map<Currency, Double> resolveCurrencies() {
        
        final Map<Currency, Double> resolved = new HashMap<>();
        
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JECurrency");
            if (plugin instanceof JECurrency jeCurrencyPlugin) {
                final Map<Long, Currency> availableCurrencies = jeCurrencyPlugin.getImpl().getCurrencies();
                
                for (final Map.Entry<String, Double> entry : this.currencyIdentifiers.entrySet()) {
                    final String currencyId = entry.getKey();
                    final Double amount     = entry.getValue();
                    
                    // Handle special case for VAULT currency
                    if ("VAULT".equalsIgnoreCase(currencyId)) {
                        final Currency vaultCurrency = new Currency(
                            "",
                            "",
                            "VAULT",
                            "$", Material.GOLD_NUGGET
                        );
                        resolved.put(
                            vaultCurrency,
                            amount
                        );
                        continue;
                    }
                    
                    // Find currency by identifier
                    final Currency currency = availableCurrencies.values().stream()
                                                                 .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
                                                                 .findFirst()
                                                                 .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));
                    
                    resolved.put(
                        currency,
                        amount
                    );
                }
            } else {
                throw new IllegalStateException("JECurrency plugin not found");
            }
        } catch (
            final Exception exception
        ) {
            LOGGER.log(
                Level.SEVERE,
                "Failed to resolve currencies",
                exception
            );
            throw new IllegalStateException(
                "Failed to resolve currencies",
                exception
            );
        }
        
        return resolved;
    }
    
}