package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Represents the CurrencyReward API type.
 */
@JsonTypeName("CURRENCY")
public final class CurrencyReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CurrencyReward.class.getName());

    private final String currencyId;
    private final double amount;

    /**
     * Executes CurrencyReward.
     */
    @JsonCreator
    public CurrencyReward(
        @JsonProperty("currencyId") @NotNull String currencyId,
        @JsonProperty("amount") double amount
    ) {
        this.currencyId = currencyId;
        this.amount = amount;
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "CURRENCY";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if ("vault".equalsIgnoreCase(currencyId) || "money".equalsIgnoreCase(currencyId)) {
                return grantVaultMoney(player);
            }
            
            LOGGER.warning("Unknown currency type: " + currencyId);
            return false;
        });
    }

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
            LOGGER.warning("Failed to grant Vault currency: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return amount;
    }

    /**
     * Gets currencyId.
     */
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Gets amount.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Executes validate.
     */
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
