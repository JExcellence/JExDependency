package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class
CurrencyRequirement extends AbstractRequirement {

    private static final Logger LOGGER = CentralLogger.getLogger(CurrencyRequirement.class.getName());
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    @JsonProperty("requiredCurrencies")
    private final Map<String, Double> currencyIdentifiers;

    @JsonProperty("currencyPlugin")
    private final String currencyPlugin;

    @JsonProperty("timeoutMillis")
    private final long timeoutMillis;

    public CurrencyRequirement(@NotNull Map<String, Double> requiredCurrencies) {
        this(requiredCurrencies, null, DEFAULT_TIMEOUT_MS, true);
    }

    public CurrencyRequirement(@NotNull Map<String, Double> requiredCurrencies, @Nullable String currencyPlugin, long timeoutMillis) {
        this(requiredCurrencies, currencyPlugin, timeoutMillis, true);
    }

    public CurrencyRequirement(@NotNull Map<String, Double> requiredCurrencies, @Nullable String currencyPlugin, long timeoutMillis, boolean consumeOnComplete) {
        super("CURRENCY", consumeOnComplete);
        this.currencyIdentifiers = new HashMap<>(requiredCurrencies);
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    @JsonCreator
    public CurrencyRequirement(@JsonProperty("requiredCurrencies") @NotNull Map<String, Double> currencyIdentifiers,
                              @JsonProperty("currencyPlugin") @Nullable String currencyPlugin,
                              @JsonProperty("timeoutMillis") @Nullable Long timeoutMillis,
                              @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete) {
        super("CURRENCY", consumeOnComplete != null ? consumeOnComplete : true);
        this.currencyIdentifiers = new HashMap<>(currencyIdentifiers);
        this.currencyPlugin = currencyPlugin;
        this.timeoutMillis = timeoutMillis != null && timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MS;
    }

    public @Nullable String getCurrencyPlugin() { return currencyPlugin; }
    public long getTimeoutMillis() { return timeoutMillis; }

    public CompletableFuture<Boolean> isMetAsync(@NotNull Player player) {
        // Use reflection to access EconomyService to avoid compile-time dependency on RDQ module.
        // This allows RPlatform to remain a standalone library that works with or without RDQ.
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> provider = 
                    player.getServer().getServicesManager().getRegistration(
                            Class.forName("com.raindropcentral.rdq.economy.EconomyService")
                    );
            
            if (provider != null) {
                Object service = provider.getProvider();
                // Use reflection to call hasAll method
                return (CompletableFuture<Boolean>) service.getClass()
                        .getMethod("hasAll", Player.class, Map.class)
                        .invoke(service, player, currencyIdentifiers);
            }
        } catch (ClassNotFoundException e) {
            // RDQ not loaded, this is expected
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error accessing economy service", e);
        }

        LOGGER.warning("Currency requirement checking not implemented - economy system unavailable");
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<Double> calculateProgressAsync(@NotNull Player player) {
        // Try to get economy service from Bukkit's ServicesManager
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> provider = 
                    player.getServer().getServicesManager().getRegistration(
                            Class.forName("com.raindropcentral.rdq.economy.EconomyService")
                    );
            
            if (provider != null) {
                Object service = provider.getProvider();
                
                // Calculate progress as percentage of currencies owned
                double totalRequired = currencyIdentifiers.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalOwned = 0.0;
                
                for (Map.Entry<String, Double> entry : currencyIdentifiers.entrySet()) {
                    CompletableFuture<Double> balanceFuture = (CompletableFuture<Double>) service.getClass()
                            .getMethod("getBalance", Player.class, String.class)
                            .invoke(service, player, entry.getKey());
                    double balance = balanceFuture.join();
                    totalOwned += Math.min(balance, entry.getValue());
                }
                
                return CompletableFuture.completedFuture(totalRequired > 0 ? totalOwned / totalRequired : 0.0);
            }
        } catch (ClassNotFoundException e) {
            // RDQ not loaded, this is expected
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calculating currency progress", e);
        }
        
        LOGGER.warning("Currency progress calculation not implemented - economy system unavailable");
        return CompletableFuture.completedFuture(0.0);
    }

    public CompletableFuture<Void> consumeAsync(@NotNull Player player) {
        // Try to get economy service from Bukkit's ServicesManager
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> provider = 
                    player.getServer().getServicesManager().getRegistration(
                            Class.forName("com.raindropcentral.rdq.economy.EconomyService")
                    );
            
            if (provider != null) {
                Object service = provider.getProvider();
                // Use reflection to call withdrawAll method
                CompletableFuture<Boolean> withdrawFuture = (CompletableFuture<Boolean>) service.getClass()
                        .getMethod("withdrawAll", Player.class, Map.class)
                        .invoke(service, player, currencyIdentifiers);
                
                return withdrawFuture.thenAccept(success -> {
                    if (!success) {
                        LOGGER.warning("Failed to withdraw currencies from " + player.getName());
                    }
                });
            }
        } catch (ClassNotFoundException e) {
            // RDQ not loaded, this is expected
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error consuming currencies", e);
        }
        
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
}
