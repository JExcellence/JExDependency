package de.jexcellence.glow.service;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing player glow effects.
 * <p>
 * Defines the contract for glow operations including enabling/disabling glow,
 * checking glow status, and applying/removing visual effects.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public interface IGlowService {

    /**
     * Enables the glow effect for a player.
     *
     * @param playerId the UUID of the player to enable glow for
     * @param adminId  the UUID of the admin enabling the glow (nullable)
     * @return a CompletableFuture containing true if the operation was successful
     */
    @NotNull CompletableFuture<Boolean> enableGlow(@NotNull UUID playerId, @Nullable UUID adminId);

    /**
     * Disables the glow effect for a player.
     *
     * @param playerId the UUID of the player to disable glow for
     * @return a CompletableFuture containing true if the operation was successful
     */
    @NotNull CompletableFuture<Boolean> disableGlow(@NotNull UUID playerId);

    /**
     * Checks if a player has the glow effect enabled.
     *
     * @param playerId the UUID of the player to check
     * @return a CompletableFuture containing true if glow is enabled
     */
    @NotNull CompletableFuture<Boolean> isGlowEnabled(@NotNull UUID playerId);

    /**
     * Applies the visual glow effect to a player.
     * <p>
     * This method schedules the glow application on the main thread.
     * </p>
     *
     * @param player the player to apply the glow effect to
     * @return a CompletableFuture that completes when the effect is applied
     */
    @NotNull CompletableFuture<Void> applyGlowEffect(@NotNull Player player);

    /**
     * Removes the visual glow effect from a player.
     * <p>
     * This method schedules the glow removal on the main thread.
     * </p>
     *
     * @param player the player to remove the glow effect from
     * @return a CompletableFuture that completes when the effect is removed
     */
    @NotNull CompletableFuture<Void> removeGlowEffect(@NotNull Player player);
}
