package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Service for handling perk triggers based on events.
 * <p>
 * Listens to game events and triggers appropriate perks for players.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public interface PerkTriggerService {

    /**
     * Handles a game event and triggers applicable perks.
     *
     * @param event the event
     * @param player the player involved
     */
    void handleEvent(@NotNull Event event, @NotNull Player player);

    /**
     * Registers event listeners.
     */
    void registerListeners();

    /**
     * Unregisters event listeners.
     */
    void unregisterListeners();
}