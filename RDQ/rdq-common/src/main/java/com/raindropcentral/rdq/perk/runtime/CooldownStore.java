package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for managing perk cooldowns.
 * <p>
 * Provides methods to check, set, and clear cooldowns for players and perks.
 * Implementations may use in-memory storage or persistent storage.
 * </p>
 *
 * @author qodo
 * @version 1.0.0
 * @since TBD
 */
public interface CooldownStore {

    /**
     * Checks if a player is on cooldown for a specific perk.
     *
     * @param player the player
     * @param perkId the perk identifier
     * @return true if on cooldown
     */
    boolean isOnCooldown(@NotNull Player player, @NotNull String perkId);

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param player the player
     * @param perkId the perk identifier
     * @return remaining cooldown in seconds, 0 if not on cooldown
     */
    long getRemainingCooldown(@NotNull Player player, @NotNull String perkId);

    /**
     * Sets a cooldown for a player and perk.
     *
     * @param player the player
     * @param perkId the perk identifier
     * @param seconds cooldown duration in seconds
     */
    void setCooldown(@NotNull Player player, @NotNull String perkId, long seconds);

    /**
     * Clears the cooldown for a player and perk.
     *
     * @param player the player
     * @param perkId the perk identifier
     */
    void clearCooldown(@NotNull Player player, @NotNull String perkId);

    /**
     * Clears all cooldowns for a player.
     *
     * @param player the player
     */
    void clearAllCooldowns(@NotNull Player player);
}