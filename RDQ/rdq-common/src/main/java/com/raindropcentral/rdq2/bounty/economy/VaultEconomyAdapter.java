package com.raindropcentral.rdq2.bounty.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VaultEconomyAdapter implements EconomyService {

    private static final Logger LOGGER = Logger.getLogger(VaultEconomyAdapter.class.getName());

    private final ExecutorService executor;
    @Nullable
    private final Economy economy;

    public VaultEconomyAdapter(@NotNull ExecutorService executor) {
        this.executor = executor;
        this.economy = setupEconomy();

        if (economy != null) {
            LOGGER.info("Vault economy hooked: " + economy.getName());
        } else {
            LOGGER.warning("Vault economy not found - bounty system will not function properly");
        }
    }

    @Nullable
    private static Economy setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }

        return rsp.getProvider();
    }


    @Override
    public CompletableFuture<Boolean> withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        if (economy == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                var response = economy.withdrawPlayer(player, amount.doubleValue());
                return response.transactionSuccess();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to withdraw " + amount + " from " + playerId, e);
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> deposit(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        if (economy == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                var response = economy.depositPlayer(player, amount.doubleValue());
                return response.transactionSuccess();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to deposit " + amount + " to " + playerId, e);
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(@NotNull UUID playerId, @NotNull String currency) {
        if (economy == null) {
            return CompletableFuture.completedFuture(BigDecimal.ZERO);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                return BigDecimal.valueOf(economy.getBalance(player));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get balance for " + playerId, e);
                return BigDecimal.ZERO;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> hasBalance(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currency) {
        if (economy == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                return economy.has(player, amount.doubleValue());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to check balance for " + playerId, e);
                return false;
            }
        }, executor);
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    @NotNull
    public String getName() {
        return economy != null ? economy.getName() : "None";
    }
}
