package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Asynchronous implementation of a requirement based on player-held currencies.
 * <p>
 * This requirement checks whether a player possesses sufficient amounts of one or more currencies,
 * and supports asynchronous operations to avoid blocking the main thread during balance checks or withdrawals.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since 1.0.0
 */
public final class CurrencyRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(CurrencyRequirement.class.getName());
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    @JsonIgnore
    private final Map<Currency, Double> requiredCurrencies;

    @JsonProperty("requiredCurrencies")
    private final Map<String, Double> currencyIdentifiers;

    @JsonProperty("currencyPlugin")
    private final String currencyPlugin;

    @JsonProperty("timeoutMillis")
    private final long timeoutMillis;

    /**
     * Creates a requirement with the given currencies and the default timeout.
     *
     * @param requiredCurrencies the currencies and required amounts that must be satisfied
     */
    public CurrencyRequirement(final @NotNull Map<Currency, Double> requiredCurrencies) {
        this(requiredCurrencies, null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Creates a requirement that optionally targets a specific currency plugin and custom timeout.
     *
     * @param requiredCurrencies the currencies and required amounts that must be satisfied
     * @param currencyPlugin     the explicit currency plugin identifier to target, or {@code null} for the default
     * @param timeoutMillis      the maximum duration, in milliseconds, to wait for currency operations before timing out
     */
    public CurrencyRequirement(
            final @NotNull Map<Currency, Double> requiredCurrencies,
            final @Nullable String currencyPlugin,
            final long timeoutMillis
    ) {
        super(Type.CURRENCY);
        this.requiredCurrencies = new HashMap<>(requiredCurrencies);
        this.currencyIdentifiers = requiredCurrencies.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getIdentifier(),
                        Map.Entry::getValue
                ));
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    /**
     * Jackson-backed constructor that restores serialized currency requirements.
     *
     * @param currencyIdentifiers the serialized currency identifiers mapped to their required amounts
     * @param currencyPlugin      the explicit currency plugin identifier to target, or {@code null} for the default
     * @param timeoutMillis       the maximum duration, in milliseconds, to wait for currency operations before timing out
     */
    @JsonCreator
    public CurrencyRequirement(
            @JsonProperty("requiredCurrencies") final @NotNull Map<String, Double> currencyIdentifiers,
            @JsonProperty("currencyPlugin") final @Nullable String currencyPlugin,
            @JsonProperty("timeoutMillis") final @Nullable Long timeoutMillis
    ) {
        super(Type.CURRENCY);
        this.currencyIdentifiers = new HashMap<>(currencyIdentifiers);
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
        this.requiredCurrencies = this.resolveCurrencies();
    }

    /**
     * Retrieves an immutable view of the required currencies.
     *
     * @return the currencies and required amounts that must be satisfied
     */
    @NotNull
    public Map<Currency, Double> getRequiredCurrencies() {
        return Collections.unmodifiableMap(this.requiredCurrencies);
    }

    /**
     * Obtains the preferred currency plugin identifier.
     *
     * @return the identifier of the currency plugin to use, or {@code null} if the default plugin should be used
     */
    @Nullable
    public String getCurrencyPlugin() {
        return this.currencyPlugin;
    }

    /**
     * Provides the maximum duration to wait for asynchronous currency operations.
     *
     * @return the timeout duration in milliseconds
     */
    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    /**
     * Checks asynchronously whether the requirement is satisfied for the provided player.
     *
     * @param player the player whose balances should be evaluated
     * @return a future that completes with {@code true} when all currency conditions are met
     */
    @NotNull
    public CompletableFuture<Boolean> isMetAsync(final @NotNull Player player) {
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(Level.WARNING, "Currency adapter not available");
            return CompletableFuture.completedFuture(false);
        }

        final List<CompletableFuture<Boolean>> futures = this.requiredCurrencies.entrySet().stream()
                .map(entry -> {
                    final Currency currency = entry.getKey();
                    final double requiredAmount = entry.getValue();
                    return adapter.getBalance(player, currency)
                            .thenApply(balance -> balance >= requiredAmount)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Failed to check balance for currency " + currency.getIdentifier(),
                                        throwable);
                                return false;
                            })
                            .orTimeout(this.timeoutMillis, TimeUnit.MILLISECONDS)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Timeout checking balance for currency " + currency.getIdentifier());
                                return false;
                            });
                })
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(ignored -> futures.stream().allMatch(CompletableFuture::join));
    }

    /**
     * Calculates asynchronously how close the player is to fulfilling the requirement.
     *
     * @param player the player whose progress should be evaluated
     * @return a future that completes with a value between {@code 0.0} and {@code 1.0} representing the completion percentage
     */
    @NotNull
    public CompletableFuture<Double> calculateProgressAsync(final @NotNull Player player) {
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(Level.WARNING, "Currency adapter not available");
            return CompletableFuture.completedFuture(0.0);
        }

        final List<CompletableFuture<Double>> futures = this.requiredCurrencies.entrySet().stream()
                .map(entry -> {
                    final Currency currency = entry.getKey();
                    final double requiredAmount = entry.getValue();
                    return adapter.getBalance(player, currency)
                            .thenApply(balance -> Math.min(balance, requiredAmount) / requiredAmount)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Failed to calculate progress for currency " + currency.getIdentifier(),
                                        throwable);
                                return 0.0;
                            })
                            .orTimeout(this.timeoutMillis, TimeUnit.MILLISECONDS)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Timeout calculating progress for currency " + currency.getIdentifier());
                                return 0.0;
                            });
                })
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    final double totalProgress = futures.stream()
                            .mapToDouble(CompletableFuture::join)
                            .sum();
                    final int count = this.requiredCurrencies.size();
                    return count > 0 ? totalProgress / count : 1.0;
                });
    }

    /**
     * Withdraws the required currencies from the player asynchronously.
     *
     * @param player the player whose balances should be reduced
     * @return a future that completes when all withdrawals finish, even if some operations fail
     */
    @NotNull
    public CompletableFuture<Void> consumeAsync(final @NotNull Player player) {
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(Level.WARNING, "Currency adapter not available");
            return CompletableFuture.completedFuture(null);
        }

        final List<CompletableFuture<Void>> futures = this.requiredCurrencies.entrySet().stream()
                .map(entry -> {
                    final Currency currency = entry.getKey();
                    final double requiredAmount = entry.getValue();
                    return adapter.withdraw(player, currency, requiredAmount)
                            .thenApply(response -> (Void) null)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Failed to withdraw currency " + currency.getIdentifier(),
                                        throwable);
                                return null;
                            })
                            .orTimeout(this.timeoutMillis, TimeUnit.MILLISECONDS)
                            .exceptionally(throwable -> {
                                LOGGER.log(Level.WARNING,
                                        "Timeout withdrawing currency " + currency.getIdentifier());
                                return null;
                            });
                })
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        try {
            return this.isMetAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking currency requirements", exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        try {
            return this.calculateProgressAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error calculating currency progress", exception);
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consume(final @NotNull Player player) {
        try {
            this.consumeAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error consuming currencies", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.currency";
    }

    @Nullable
    private CurrencyAdapter getCurrencyAdapter() {
        if (this.currencyPlugin == null || "jexeconomy".equalsIgnoreCase(this.currencyPlugin)) {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
            if (plugin instanceof final JExEconomy jexEconomy) {
                return jexEconomy.getImpl().getCurrencyAdapter();
            }
        }
        return null;
    }

    @NotNull
    private Map<Currency, Double> resolveCurrencies() {
        final Map<Currency, Double> resolved = new HashMap<>();
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
            if (!(plugin instanceof final JExEconomy jexEconomyPlugin)) {
                throw new IllegalStateException("JExEconomy plugin not found");
            }

            final Map<Long, Currency> availableCurrencies = jexEconomyPlugin.getImpl().getCurrencies();

            for (final Map.Entry<String, Double> entry : this.currencyIdentifiers.entrySet()) {
                final String currencyId = entry.getKey();
                final Double amount = entry.getValue();

                if ("VAULT".equalsIgnoreCase(currencyId)) {
                    final Currency vaultCurrency = new Currency("", "", "VAULT", "$", Material.GOLD_NUGGET);
                    resolved.put(vaultCurrency, amount);
                    continue;
                }

                final Currency currency = availableCurrencies.values().stream()
                        .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));

                resolved.put(currency, amount);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to resolve currencies", exception);
            throw new IllegalStateException("Failed to resolve currencies", exception);
        }
        return resolved;
    }
}