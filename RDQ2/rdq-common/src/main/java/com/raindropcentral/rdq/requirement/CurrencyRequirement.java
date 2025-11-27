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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    public CurrencyRequirement(@NotNull Map<Currency, Double> requiredCurrencies) {
        this(requiredCurrencies, null, DEFAULT_TIMEOUT_MS);
    }

    public CurrencyRequirement(@NotNull Map<Currency, Double> requiredCurrencies, @Nullable String currencyPlugin, long timeoutMillis) {
        super(Type.CURRENCY);
        this.requiredCurrencies = new HashMap<>(requiredCurrencies);
        this.currencyIdentifiers = requiredCurrencies.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getIdentifier(), Map.Entry::getValue));
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    @JsonCreator
    public CurrencyRequirement(@JsonProperty("requiredCurrencies") @NotNull Map<String, Double> currencyIdentifiers,
                              @JsonProperty("currencyPlugin") @Nullable String currencyPlugin,
                              @JsonProperty("timeoutMillis") @Nullable Long timeoutMillis) {
        super(Type.CURRENCY);
        this.currencyIdentifiers = new HashMap<>(currencyIdentifiers);
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
        this.requiredCurrencies = resolveCurrencies();
    }

    public Map<Currency, Double> getRequiredCurrencies() { return Collections.unmodifiableMap(requiredCurrencies); }
    public @Nullable String getCurrencyPlugin() { return currencyPlugin; }
    public long getTimeoutMillis() { return timeoutMillis; }

    public CompletableFuture<Boolean> isMetAsync(@NotNull Player player) {
        var adapter = getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.warning("Currency adapter not available");
            return CompletableFuture.completedFuture(false);
        }

        var futures = requiredCurrencies.entrySet().stream()
            .map(entry -> {
                var currency = entry.getKey();
                var requiredAmount = entry.getValue();
                return adapter.getBalance(player, currency)
                    .thenApply(balance -> balance >= requiredAmount)
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.WARNING, "Failed to check balance for currency " + currency.getIdentifier(), throwable);
                        return false;
                    })
                    .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        LOGGER.warning("Timeout checking balance for currency " + currency.getIdentifier());
                        return false;
                    });
            })
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> futures.stream().allMatch(CompletableFuture::join));
    }

    public CompletableFuture<Double> calculateProgressAsync(@NotNull Player player) {
        var adapter = getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.warning("Currency adapter not available");
            return CompletableFuture.completedFuture(0.0);
        }

        var futures = requiredCurrencies.entrySet().stream()
            .map(entry -> {
                var currency = entry.getKey();
                var requiredAmount = entry.getValue();
                return adapter.getBalance(player, currency)
                    .thenApply(balance -> Math.min(balance, requiredAmount) / requiredAmount)
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.WARNING, "Failed to calculate progress for currency " + currency.getIdentifier(), throwable);
                        return 0.0;
                    })
                    .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        LOGGER.warning("Timeout calculating progress for currency " + currency.getIdentifier());
                        return 0.0;
                    });
            })
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> {
                var totalProgress = futures.stream().mapToDouble(CompletableFuture::join).sum();
                var count = requiredCurrencies.size();
                return count > 0 ? totalProgress / count : 1.0;
            });
    }

    public CompletableFuture<Void> consumeAsync(@NotNull Player player) {
        var adapter = getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.warning("Currency adapter not available");
            return CompletableFuture.completedFuture(null);
        }

        var futures = requiredCurrencies.entrySet().stream()
            .map(entry -> {
                var currency = entry.getKey();
                var requiredAmount = entry.getValue();
                return adapter.withdraw(player, currency, requiredAmount)
                    .thenApply(response -> (Void) null)
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.WARNING, "Failed to withdraw currency " + currency.getIdentifier(), throwable);
                        return null;
                    })
                    .orTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        LOGGER.warning("Timeout withdrawing currency " + currency.getIdentifier());
                        return null;
                    });
            })
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        try {
            return isMetAsync(player).join();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking currency requirements", exception);
            return false;
        }
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        try {
            return calculateProgressAsync(player).join();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error calculating currency progress", exception);
            return 0.0;
        }
    }

    @Override
    public void consume(@NotNull Player player) {
        try {
            consumeAsync(player).join();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Error consuming currencies", exception);
        }
    }

    @Override
    public @NotNull String getDescriptionKey() {
        return "requirement.currency";
    }

    private @Nullable CurrencyAdapter getCurrencyAdapter() {
        if (currencyPlugin == null || "jexeconomy".equalsIgnoreCase(currencyPlugin)) {
            var plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
            if (plugin instanceof JExEconomy jexEconomy) {
                return jexEconomy.getImpl().getCurrencyAdapter();
            }
        }
        return null;
    }

    private Map<Currency, Double> resolveCurrencies() {
        var resolved = new HashMap<Currency, Double>();
        try {
            var plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
            if (!(plugin instanceof JExEconomy jexEconomyPlugin)) {
                throw new IllegalStateException("JExEconomy plugin not found");
            }

            var availableCurrencies = jexEconomyPlugin.getImpl().getCurrencies();

            for (var entry : currencyIdentifiers.entrySet()) {
                var currencyId = entry.getKey();
                var amount = entry.getValue();

                if ("VAULT".equalsIgnoreCase(currencyId)) {
                    var vaultCurrency = new Currency("", "", "VAULT", "$", Material.GOLD_NUGGET);
                    resolved.put(vaultCurrency, amount);
                    continue;
                }

                var currency = availableCurrencies.values().stream()
                    .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));

                resolved.put(currency, amount);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to resolve currencies", exception);
            throw new IllegalStateException("Failed to resolve currencies", exception);
        }
        return resolved;
    }
}