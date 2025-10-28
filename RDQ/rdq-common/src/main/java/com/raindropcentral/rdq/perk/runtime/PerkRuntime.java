package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime abstraction for perk behavior, separating persistence from execution.
 * <p>
 * This class defines the contract for perk implementations that handle
 * activation, deactivation, triggering, and state management at runtime.
 * Concrete implementations are built from PerkSection configurations.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public interface PerkRuntime {

    /**
     * Gets the unique identifier for this perk.
     *
     * @return the perk identifier
     */
    @NotNull String getId();

    /**
     * Gets the type of this perk.
     *
     * @return the perk type
     */
    @NotNull EPerkType getType();

    /**
     * Checks if the perk can be activated for the given player.
     *
     * @param player the player
     * @return true if activation is possible
     */
    boolean canActivate(@NotNull Player player);

    /**
     * Activates the perk for the given player.
     *
     * @param player the player
     * @return true if activation was successful
     */
    boolean activate(@NotNull Player player);

    /**
     * Deactivates the perk for the given player.
     *
     * @param player the player
     * @return true if deactivation was successful
     */
    boolean deactivate(@NotNull Player player);

    /**
     * Triggers the perk effect for event-based perks.
     *
     * @param player the player
     */
    void trigger(@NotNull Player player);

    /**
     * Triggers the perk effect while providing contextual information about the source.
     *
     * @param player the player
     * @param source description of the trigger source, typically an event name
     */
    default void trigger(@NotNull Player player, @NotNull String source) {
        trigger(player);
    }

    /**
     * Checks if the player is currently on cooldown for this perk.
     *
     * @param player the player
     * @return true if on cooldown
     */
    boolean isOnCooldown(@NotNull Player player);

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param player the player
     * @return remaining cooldown in seconds, 0 if not on cooldown
     */
    long getRemainingCooldown(@NotNull Player player);

    /**
     * Sets a cooldown for the player.
     *
     * @param player the player
     * @param seconds cooldown duration in seconds
     */
    void setCooldown(@NotNull Player player, long seconds);

    /**
     * Checks if the perk is currently active for the player.
     *
     * @param player the player
     * @return true if active
     */
    boolean isActive(@NotNull Player player);

    /**
     * Indicates whether this runtime responds to the supplied event type.
     *
     * @param event the event being processed
     * @return {@code true} when the event should be propagated to {@link #trigger(Player)}
     */
    default boolean supports(@NotNull Event event) {
        return true;
    }
}