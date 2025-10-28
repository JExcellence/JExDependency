package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Extension of PerkRuntime for toggleable perks.
 * <p>
 * Toggleable perks can be switched on and off by the player,
 * maintaining their state until manually changed.
 * </p>
 *
 * @author qodo
 * @version 1.0.0
 * @since TBD
 */
public interface ToggleablePerkRuntime extends PerkRuntime {

    /**
     * Toggles the perk state for the player.
     *
     * @param player the player
     * @return true if toggle was successful
     */
    boolean toggle(@NotNull Player player);

    /**
     * Checks if the perk is toggled on for the player.
     *
     * @param player the player
     * @return true if toggled on
     */
    boolean isToggledOn(@NotNull Player player);
}