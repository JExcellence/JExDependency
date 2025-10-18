package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a reward that grants in-game currency to a player.
 * <p>
 * This reward integrates with economy plugins to deposit currency into a player's account.
 * Supports both synchronous and asynchronous operations.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public final class CurrencyReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CurrencyReward.class.getName());
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    @JsonProperty("amount")
    private final double amount;

    @JsonProperty("currencyIdentifier")
    private final String currencyIdentifier;

    @JsonProperty("currencyPlugin")
    private final String currencyPlugin;

    @JsonProperty("timeoutMillis")
    private final long timeoutMillis;

    /**
     * Constructs a new {@code CurrencyReward} with the specified amount (default currency).
     *
     * @param amount The amount of currency to reward.
     */
    public CurrencyReward(final double amount) {
        this(amount, "VAULT", null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Constructs a new {@code CurrencyReward} with full configuration.
     *
     * @param amount              The amount of currency to reward.
     * @param currencyIdentifier  The identifier of the currency.
     * @param currencyPlugin      The currency plugin name (null for default).
     * @param timeoutMillis       Timeout for async operations.
     */
    @JsonCreator
    public CurrencyReward(
            @JsonProperty("amount") final double amount,
            @JsonProperty("currencyIdentifier") final @Nullable String currencyIdentifier,
            @JsonProperty("currencyPlugin") final @Nullable String currencyPlugin,
            @JsonProperty("timeoutMillis") final @Nullable Long timeoutMillis
    ) {
        super(Type.CURRENCY);

        if (amount <= 0) {
            throw new IllegalArgumentException("Currency amount must be positive: " + amount);
        }

        this.amount = amount;
        this.currencyIdentifier = currencyIdentifier != null ? currencyIdentifier : "VAULT";
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    @Override
    public void apply(final @NotNull Player player) {
        try {
            this.applyAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to apply currency reward", exception);
        }
    }

    @NotNull
    public CompletableFuture<Void> applyAsync(final @NotNull Player player) {
        final CurrencyAdapter adapter = this.getCurrencyAdapter();
        if (adapter == null) {
            LOGGER.log(Level.WARNING, "Currency adapter not available");
            return CompletableFuture.completedFuture(null);
        }

        final Currency currency = this.resolveCurrency();
        if (currency == null) {
            LOGGER.log(Level.WARNING, "Currency not found: " + this.currencyIdentifier);
            return CompletableFuture.completedFuture(null);
        }

        return adapter.deposit(player, currency, this.amount)
                .thenAccept(response -> {
                    // No-op: we only care about completion, not the response
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to deposit currency", throwable);
                    return null; // recover with Void
                })
                .orTimeout(this.timeoutMillis, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Timeout depositing currency");
                    return null; // recover with Void
                });
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.currency";
    }

    public double getAmount() {
        return this.amount;
    }

    @NotNull
    public String getCurrencyIdentifier() {
        return this.currencyIdentifier;
    }

    @Nullable
    public String getCurrencyPlugin() {
        return this.currencyPlugin;
    }

    public long getTimeoutMillis() {
        return this.timeoutMillis;
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

    @Nullable
    private Currency resolveCurrency() {
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("JECurrency");
            if (!(plugin instanceof final JECurrency jeCurrencyPlugin)) {
                return null;
            }

            if ("VAULT".equalsIgnoreCase(this.currencyIdentifier)) {
                return new Currency("", "", "VAULT", "$", Material.GOLD_NUGGET);
            }

            final Map<Long, Currency> availableCurrencies = jeCurrencyPlugin.getImpl().getCurrencies();
            return availableCurrencies.values().stream()
                    .filter(c -> c.getIdentifier().equalsIgnoreCase(this.currencyIdentifier))
                    .findFirst()
                    .orElse(null);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to resolve currency", exception);
            return null;
        }
    }
}