package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reward that grants currency to a player.
 * <p>
 * This reward supports both Vault economy and JExEconomy with multiple currency types.
 * It includes retry logic for transient failures.
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since TBD
 */
@JsonTypeName("CURRENCY")
public final class CurrencyReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CurrencyReward.class.getName());
    private static final int MAX_RETRIES = 1;
    private static final long RETRY_DELAY_MS = 100;

    @Nullable
    private static JExEconomyBridge economyBridge;

    private final String currencyId;
    private final double amount;

    /**
     * Sets the JExEconomy bridge for currency operations.
     *
     * @param bridge the economy bridge
     */
    public static void setEconomyBridge(@Nullable final JExEconomyBridge bridge) {
        economyBridge = bridge;
    }

    @JsonCreator
    public CurrencyReward(
        @JsonProperty("currencyId") @NotNull String currencyId,
        @JsonProperty("amount") double amount
    ) {
        this.currencyId = currencyId;
        this.amount = amount;
    }

    @Override
    public @NotNull String getTypeId() {
        return "CURRENCY";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return grantWithRetry(player, 0);
    }

    /**
     * Grants currency with retry logic.
     *
     * @param player  the player
     * @param attempt the current attempt number
     * @return a future completing with true if successful
     */
    @NotNull
    private CompletableFuture<Boolean> grantWithRetry(@NotNull final Player player, final int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            // Try JExEconomy first if available
            if (economyBridge != null && economyBridge.hasCurrency(currencyId)) {
                return grantJExEconomy(player);
            }

            // Fall back to Vault for standard currencies
            if ("vault".equalsIgnoreCase(currencyId) || "money".equalsIgnoreCase(currencyId)) {
                return grantVaultMoney(player);
            }
            
            LOGGER.warning("Unknown currency type: " + currencyId);
            return false;
        }).thenCompose(success -> {
            if (success || attempt >= MAX_RETRIES) {
                if (success) {
                    LOGGER.fine("Granted " + amount + " " + currencyId + " to " + player.getName());
                } else if (attempt >= MAX_RETRIES) {
                    LOGGER.warning("Failed to grant currency after " + (attempt + 1) + " attempts");
                }
                return CompletableFuture.completedFuture(success);
            }

            // Retry after delay
            LOGGER.fine("Retrying currency grant (attempt " + (attempt + 2) + ")");
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }).thenCompose(v -> grantWithRetry(player, attempt + 1));
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Error granting currency reward", ex);
            return false;
        });
    }

    /**
     * Grants currency using JExEconomy.
     *
     * @param player the player
     * @return true if successful
     */
    private boolean grantJExEconomy(@NotNull final Player player) {
        try {
            return economyBridge.deposit(player, currencyId, amount).join();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to grant JExEconomy currency: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Grants currency using Vault economy.
     *
     * @param player the player
     * @return true if successful
     */
    private boolean grantVaultMoney(@NotNull Player player) {
        try {
            var economyProvider = Bukkit.getServicesManager().getRegistration(
                Class.forName("net.milkbowl.vault.economy.Economy")
            );
            
            if (economyProvider != null) {
                Object economy = economyProvider.getProvider();
                economy.getClass()
                    .getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount);
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to grant Vault currency: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public double getEstimatedValue() {
        return amount;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public void validate() {
        if (currencyId == null || currencyId.isEmpty()) {
            throw new IllegalArgumentException("Currency ID cannot be empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Currency amount must be positive");
        }
    }
}
