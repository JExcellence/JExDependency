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
 * This reward integrates with economy plugins to deposit currency into a player's account and
 * supports both synchronous and asynchronous operations depending on the consumer's needs.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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
     * Constructs a new {@code CurrencyReward} that grants the given amount using the default
     * currency identifier and timeout configuration.
     *
     * @param amount the amount of currency to reward; must be positive
     */
    public CurrencyReward(final double amount) {
        this(amount, "VAULT", null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Constructs a new {@code CurrencyReward} with full configuration.
     *
     * @param amount             the amount of currency to reward; must be positive
     * @param currencyIdentifier the identifier of the currency to resolve, falling back to
     *                           {@code VAULT} when {@code null}
     * @param currencyPlugin     the currency plugin name or {@code null} to use the default
     *                           adapter resolution
     * @param timeoutMillis      the timeout in milliseconds to wait for asynchronous operations;
     *                           {@link #DEFAULT_TIMEOUT_MS} is used when {@code null} or
     *                           non-positive
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

    /**
     * Applies the reward synchronously by delegating to {@link #applyAsync(Player)} and blocking
     * until completion.
     *
     * @param player the player receiving the reward
     */
    @Override
    public void apply(final @NotNull Player player) {
        try {
            this.applyAsync(player).join();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to apply currency reward", exception);
        }
    }

    /**
     * Applies the reward asynchronously by depositing the configured currency amount into the
     * provided player's account.
     *
     * @param player the player receiving the reward
     * @return a future that completes when the deposit operation finishes or a timeout occurs
     */
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

    /**
     * Provides the translation key describing this reward for localization purposes.
     *
     * @return the description translation key
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.currency";
    }

    /**
     * Retrieves the configured amount of currency granted by this reward.
     *
     * @return the currency amount
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Obtains the identifier used to resolve the target currency.
     *
     * @return the configured currency identifier
     */
    @NotNull
    public String getCurrencyIdentifier() {
        return this.currencyIdentifier;
    }

    /**
     * Provides the explicit currency plugin requested for adapter resolution.
     *
     * @return the plugin name or {@code null} when the default resolution should be used
     */
    @Nullable
    public String getCurrencyPlugin() {
        return this.currencyPlugin;
    }

    /**
     * Returns the maximum duration to wait for asynchronous operations to finish before a timeout
     * is triggered.
     *
     * @return the timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return this.timeoutMillis;
    }

    /**
     * Resolves the {@link CurrencyAdapter} to use for deposit operations.
     *
     * @return the currency adapter or {@code null} when unavailable
     */
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

    /**
     * Resolves the {@link Currency} definition associated with {@link #currencyIdentifier}.
     *
     * @return the resolved currency or {@code null} when it cannot be located
     */
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