package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CurrencyRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(CurrencyRequirement.class.getName());
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    // TODO: Implement when economy system is available
    // @JsonIgnore
    // private final Map<Currency, Double> requiredCurrencies;

    @JsonProperty("requiredCurrencies")
    private final Map<String, Double> currencyIdentifiers;

    @JsonProperty("currencyPlugin")
    private final String currencyPlugin;

    @JsonProperty("timeoutMillis")
    private final long timeoutMillis;

    public CurrencyRequirement(@NotNull Map<String, Double> requiredCurrencies) {
        this(requiredCurrencies, null, DEFAULT_TIMEOUT_MS);
    }

    public CurrencyRequirement(@NotNull Map<String, Double> requiredCurrencies, @Nullable String currencyPlugin, long timeoutMillis) {
        super(Type.CURRENCY);
        // this.requiredCurrencies = new HashMap<>(requiredCurrencies);
        this.currencyIdentifiers = new HashMap<>(requiredCurrencies);
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
        // this.requiredCurrencies = resolveCurrencies();
    }

    // TODO: Implement when economy system is available
    // public Map<Currency, Double> getRequiredCurrencies() { return Collections.unmodifiableMap(requiredCurrencies); }
    public @Nullable String getCurrencyPlugin() { return currencyPlugin; }
    public long getTimeoutMillis() { return timeoutMillis; }

    public CompletableFuture<Boolean> isMetAsync(@NotNull Player player) {
        // TODO: Implement when economy system is available
        LOGGER.warning("Currency requirement checking not implemented - economy system unavailable");
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<Double> calculateProgressAsync(@NotNull Player player) {
        // TODO: Implement when economy system is available
        LOGGER.warning("Currency progress calculation not implemented - economy system unavailable");
        return CompletableFuture.completedFuture(0.0);
    }

    public CompletableFuture<Void> consumeAsync(@NotNull Player player) {
        // TODO: Implement when economy system is available
        LOGGER.warning("Currency consumption not implemented - economy system unavailable");
        return CompletableFuture.completedFuture(null);
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

    private @Nullable Object getCurrencyAdapter() {
        // if (currencyPlugin == null || "jexeconomy".equalsIgnoreCase(currencyPlugin)) {
        //     var plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
        //     if (plugin instanceof JExEconomy jexEconomy) {
        //         return jexEconomy.getImpl().getCurrencyAdapter();
        //     }
        // }
        return null;
    }

    private Map<Object, Double> resolveCurrencies() {
        var resolved = new HashMap<Object, Double>();
        // try {
        //     var plugin = Bukkit.getPluginManager().getPlugin("JExEconomy");
        //     if (!(plugin instanceof JExEconomy jexEconomyPlugin)) {
        //         throw new IllegalStateException("JExEconomy plugin not found");
        //     }

        //     var availableCurrencies = jexEconomyPlugin.getImpl().getCurrencies();

        //     for (var entry : currencyIdentifiers.entrySet()) {
        //         var currencyId = entry.getKey();
        //         var amount = entry.getValue();

        //         if ("VAULT".equalsIgnoreCase(currencyId)) {
        //             var vaultCurrency = new Currency("", "", "VAULT", "$", Material.GOLD_NUGGET);
        //             resolved.put(vaultCurrency, amount);
        //             continue;
        //         }

        //         var currency = availableCurrencies.values().stream()
        //             .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyId))
        //             .findFirst()
        //             .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + currencyId));

        //         resolved.put(currency, amount);
        //     }
        // } catch (Exception exception) {
        //     LOGGER.log(Level.SEVERE, "Failed to resolve currencies", exception);
        //     throw new IllegalStateException("Failed to resolve currencies", exception);
        // }
        return resolved;
    }
}