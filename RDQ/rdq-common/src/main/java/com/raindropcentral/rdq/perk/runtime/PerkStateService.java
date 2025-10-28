package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing perk state per player.
 * <p>
 * Handles activation/deactivation state, ownership, and persistence.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public interface PerkStateService {

    /**
     * Checks if a player owns a specific perk.
     *
     * @param player the player
     * @param perk the perk
     * @return true if owned
     */
    boolean playerOwnsPerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    /**
     * Grants a perk to a player.
     *
     * @param player the player
     * @param perk the perk
     * @param enabled initial enabled state
     */
    void grantPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, boolean enabled);

    /**
     * Revokes a perk from a player.
     *
     * @param player the player
     * @param perk the perk
     */
    void revokePerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    /**
     * Checks if a perk is enabled for a player.
     *
     * @param player the player
     * @param perk the perk
     * @return true if enabled
     */
    boolean isPerkEnabled(@NotNull RDQPlayer player, @NotNull RPerk perk);

    /**
     * Enables a perk for a player.
     *
     * @param player the player
     * @param perk the perk
     * @param maxEnabledPerks maximum enabled perks allowed
     * @return true if enabled successfully
     */
    boolean enablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk, int maxEnabledPerks);

    /**
     * Disables a perk for a player.
     *
     * @param player the player
     * @param perk the perk
     * @return true if disabled successfully
     */
    boolean disablePerk(@NotNull RDQPlayer player, @NotNull RPerk perk);

    /**
     * Gets all owned perks for a player.
     *
     * @param player the player
     * @return list of owned perks
     */
    @NotNull List<RPerk> getOwnedPerks(@NotNull RDQPlayer player);

    /**
     * Gets all enabled perks for a player.
     *
     * @param player the player
     * @return list of enabled perks
     */
    @NotNull List<RPerk> getEnabledPerks(@NotNull RDQPlayer player);

    /**
     * Gets the RDQPlayer entity for a Bukkit player.
     *
     * @param player the Bukkit player
     * @return the RDQPlayer entity, or null if not found
     */
    @Nullable RDQPlayer getRDQPlayer(@NotNull Player player);

    /**
     * Cleans up state for a player (on disconnect).
     *
     * @param playerId the player UUID
     */
    void cleanupPlayerState(@NotNull UUID playerId);
}