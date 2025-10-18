package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logging.CentralLogger;
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
 * @since TBD
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

    public CurrencyRequirement(final @NotNull Map<Currency, Double> requiredCurrencies) {
        this(requiredCurrencies, null, DEFAULT_TIMEOUT_MS);
    }

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

    @NotNull
    public Map<Currency, Double> getRequiredCurrencies() {
        return Collections.unmodifiableMap(this.requiredCurrencies);
    }

    @Nullable
    public String getCurrencyPlugin() {
        return this.currencyPlugin;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

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

    @Override
    public boolean isMet(final @NotNull Player player) {
        try {
            return this.isMetAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking currency requirements", exception);
            return false;
        }
    }

    @Override
    public double calculateProgress(final @NotNull Player player) {
        try {
            return this.calculateProgressAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error calculating currency progress", exception);
            return 0.0;
        }
    }

    @Override
    public void consume(final @NotNull Player player) {
        try {
            this.consumeAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error consuming currencies", exception);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.currency";
    }

    @Nullable
    private CurrencyAdapter getCurrencyAdapter() {
        if (this.currencyPlugin == null || "jecurrency".equalsIgnoreCase(this.currencyPlugin)) {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JECurrency");
            if (plugin instanceof final JECurrency jeCurrency) {
                return jeCurrency.getImpl().getCurrencyAdapter();
            }
        }
        return null;
    }

    @NotNull
    private Map<Currency, Double> resolveCurrencies() {
        final Map<Currency, Double> resolved = new HashMap<>();
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JECurrency");
            if (!(plugin instanceof final JECurrency jeCurrencyPlugin)) {
                throw new IllegalStateException("JECurrency plugin not found");
            }

            final Map<Long, Currency> availableCurrencies = jeCurrencyPlugin.getImpl().getCurrencies();

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