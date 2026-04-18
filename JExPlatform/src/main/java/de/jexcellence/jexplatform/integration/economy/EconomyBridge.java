package de.jexcellence.jexplatform.integration.economy;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection-based economy bridge supporting JExEconomy with Vault fallback.
 *
 * <p>All operations are asynchronous. Use {@link #detect(JavaPlugin, JExLogger)} to
 * auto-detect the best available economy provider.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class EconomyBridge {

    private final Object economyInstance;
    private final EconomyProvider provider;
    private final JExLogger logger;

    /** The detected economy provider type. */
    public enum EconomyProvider {
        /** JExEconomy provider. */
        JEXECONOMY,
        /** Vault provider. */
        VAULT,
        /** No economy provider available. */
        NONE
    }

    private EconomyBridge(@NotNull Object economyInstance, @NotNull EconomyProvider provider,
                          @NotNull JExLogger logger) {
        this.economyInstance = economyInstance;
        this.provider = provider;
        this.logger = logger;
    }

    /**
     * Detects the best available economy provider.
     *
     * @param plugin the owning plugin
     * @param logger the platform logger
     * @return the economy bridge, or empty if none available
     */
    public static @NotNull Optional<EconomyBridge> detect(@NotNull JavaPlugin plugin,
                                                          @NotNull JExLogger logger) {
        // Try JExEconomy first
        try {
            var clazz = Class.forName("de.jexcellence.jexeconomy.api.EconomyApi");
            var method = clazz.getMethod("getInstance");
            var instance = method.invoke(null);
            if (instance != null) {
                logger.info("Economy bridge: JExEconomy detected");
                return Optional.of(new EconomyBridge(instance, EconomyProvider.JEXECONOMY,
                        logger));
            }
        } catch (Exception ignored) { }

        // Fallback to Vault
        try {
            var rsp = plugin.getServer().getServicesManager()
                    .getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"));
            if (rsp != null) {
                logger.info("Economy bridge: Vault detected");
                return Optional.of(new EconomyBridge(rsp.getProvider(), EconomyProvider.VAULT,
                        logger));
            }
        } catch (Exception ignored) { }

        logger.warn("Economy bridge: no provider detected");
        return Optional.empty();
    }

    /**
     * Returns the detected economy provider type.
     *
     * @return the provider type
     */
    public @NotNull EconomyProvider provider() {
        return provider;
    }

    /**
     * Withdraws an amount from a player's account.
     *
     * @param playerUuid the player UUID
     * @param amount     the amount to withdraw
     * @return a future resolving to {@code true} on success
     */
    public @NotNull CompletableFuture<Boolean> withdraw(@NotNull UUID playerUuid,
                                                        double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return switch (provider) {
                    case JEXECONOMY -> invokeJExEconomy("withdraw", playerUuid, amount);
                    case VAULT -> invokeVault("withdrawPlayer", playerUuid, amount);
                    case NONE -> false;
                };
            } catch (Exception e) {
                logger.error("Economy withdraw failed for {}: {}", playerUuid, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Deposits an amount into a player's account.
     *
     * @param playerUuid the player UUID
     * @param amount     the amount to deposit
     * @return a future resolving to {@code true} on success
     */
    public @NotNull CompletableFuture<Boolean> deposit(@NotNull UUID playerUuid,
                                                       double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return switch (provider) {
                    case JEXECONOMY -> invokeJExEconomy("deposit", playerUuid, amount);
                    case VAULT -> invokeVault("depositPlayer", playerUuid, amount);
                    case NONE -> false;
                };
            } catch (Exception e) {
                logger.error("Economy deposit failed for {}: {}", playerUuid, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Returns the player's current balance.
     *
     * @param playerUuid the player UUID
     * @return a future resolving to the balance
     */
    public @NotNull CompletableFuture<Double> getBalance(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return switch (provider) {
                    case JEXECONOMY -> invokeJExEconomyBalance(playerUuid);
                    case VAULT -> invokeVaultBalance(playerUuid);
                    case NONE -> 0.0;
                };
            } catch (Exception e) {
                logger.error("Economy balance check failed for {}: {}",
                        playerUuid, e.getMessage());
                return 0.0;
            }
        });
    }

    /**
     * Checks whether a player has at least the specified amount.
     *
     * @param playerUuid the player UUID
     * @param amount     the minimum required amount
     * @return a future resolving to {@code true} if the player has enough
     */
    public @NotNull CompletableFuture<Boolean> has(@NotNull UUID playerUuid, double amount) {
        return getBalance(playerUuid).thenApply(balance -> balance >= amount);
    }

    // ── Reflection helpers ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean invokeJExEconomy(String method, UUID playerUuid, double amount) {
        try {
            var m = economyInstance.getClass().getMethod(method, UUID.class, double.class);
            var result = m.invoke(economyInstance, playerUuid, amount);
            if (result instanceof Boolean b) return b;
            if (result instanceof CompletableFuture<?> f) return (Boolean) f.join();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private double invokeJExEconomyBalance(UUID playerUuid) {
        try {
            var m = economyInstance.getClass().getMethod("getBalance", UUID.class);
            var result = m.invoke(economyInstance, playerUuid);
            if (result instanceof Number n) return n.doubleValue();
            if (result instanceof CompletableFuture<?> f) {
                var val = f.join();
                return val instanceof Number n ? n.doubleValue() : 0.0;
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean invokeVault(String method, UUID playerUuid, double amount) {
        try {
            var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
            var m = economyInstance.getClass().getMethod(method,
                    org.bukkit.OfflinePlayer.class, double.class);
            var response = m.invoke(economyInstance, offlinePlayer, amount);
            var successMethod = response.getClass().getMethod("transactionSuccess");
            return (Boolean) successMethod.invoke(response);
        } catch (Exception e) {
            return false;
        }
    }

    private double invokeVaultBalance(UUID playerUuid) {
        try {
            var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
            var m = economyInstance.getClass().getMethod("getBalance",
                    org.bukkit.OfflinePlayer.class);
            return ((Number) m.invoke(economyInstance, offlinePlayer)).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }
}
