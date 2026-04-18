package de.jexcellence.jexplatform.integration.protection;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed bridge for town/protection plugin integration.
 *
 * <p>Supports HuskTowns, RDTowns, and Towny through reflection.
 * Use {@link #detect(JavaPlugin, JExLogger)} to auto-detect the best available provider.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface ProtectionBridge
        permits HuskTownProtectionBridge, RdtProtectionBridge, TownyProtectionBridge {

    /**
     * Returns the name of the backing protection plugin.
     *
     * @return the plugin name
     */
    @NotNull String pluginName();

    /**
     * Returns whether the backing plugin is available and loaded.
     *
     * @return {@code true} if available
     */
    boolean isAvailable();

    /**
     * Checks whether a player is a member of any town.
     *
     * @param player the player
     * @return a future resolving to {@code true} if the player is in a town
     */
    @NotNull CompletableFuture<Boolean> isTownMember(@NotNull Player player);

    /**
     * Returns the player's town name.
     *
     * @param player the player
     * @return a future resolving to the town name, or empty
     */
    @NotNull CompletableFuture<Optional<String>> getTownName(@NotNull Player player);

    /**
     * Checks whether a player is the mayor of their town.
     *
     * @param player the player
     * @return a future resolving to {@code true} if the player is a mayor
     */
    @NotNull CompletableFuture<Boolean> isTownMayor(@NotNull Player player);

    /**
     * Deposits currency into the town bank.
     *
     * @param player the player making the deposit
     * @param amount the amount to deposit
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> depositToTownBank(@NotNull Player player,
                                                          double amount);

    /**
     * Withdraws currency from the town bank.
     *
     * @param player the player making the withdrawal
     * @param amount the amount to withdraw
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> withdrawFromTownBank(@NotNull Player player,
                                                             double amount);

    /**
     * Returns the town's level.
     *
     * @param player the player
     * @return a future resolving to the town level, or {@code 0} if not in a town
     */
    @NotNull CompletableFuture<Integer> getTownLevel(@NotNull Player player);

    /**
     * Detects the best available protection plugin.
     *
     * @param plugin the owning plugin
     * @param logger the platform logger
     * @return the detected bridge, or empty if none available
     */
    static @NotNull Optional<ProtectionBridge> detect(@NotNull JavaPlugin plugin,
                                                      @NotNull JExLogger logger) {
        var pm = plugin.getServer().getPluginManager();

        if (pm.isPluginEnabled("HuskTowns")) {
            logger.info("Protection bridge: HuskTowns detected");
            return Optional.of(new HuskTownProtectionBridge(logger));
        }
        if (pm.isPluginEnabled("RDTowns")) {
            logger.info("Protection bridge: RDTowns detected");
            return Optional.of(new RdtProtectionBridge(logger));
        }
        if (pm.isPluginEnabled("Towny")) {
            logger.info("Protection bridge: Towny detected");
            return Optional.of(new TownyProtectionBridge(logger));
        }

        logger.warn("Protection bridge: no provider detected");
        return Optional.empty();
    }
}
