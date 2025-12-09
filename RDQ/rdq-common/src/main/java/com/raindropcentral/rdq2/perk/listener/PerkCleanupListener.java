/*
package com.raindropcentral.rdq2.perk.listener;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public final class PerkCleanupListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(PerkCleanupListener.class.getName());

    private final @Nullable RDQ core;
    private final @Nullable PerkRegistry perkRegistry;
    private final @Nullable com.raindropcentral.rdq2.perk.event.PerkEventBus eventBus;

    */
/**
     * Constructor for CommandFactory auto-registration.
     *//*

    public PerkCleanupListener(@NotNull RDQ core) {
        this.core = core;
        this.perkRegistry = null;
        this.eventBus = null;
    }

    public PerkCleanupListener(@NotNull PerkRegistry perkRegistry, @NotNull com.raindropcentral.rdq2.perk.event.PerkEventBus eventBus) {
        this.core = null;
        this.perkRegistry = perkRegistry;
        this.eventBus = eventBus;
    }

    private @Nullable PerkRegistry getRegistry() {
        if (perkRegistry != null) return perkRegistry;
        if (core != null) {
            try {
                return core.getPerkRegistry();
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return null;
    }

    private @Nullable com.raindropcentral.rdq2.perk.event.PerkEventBus getEventBus() {
        if (eventBus != null) return eventBus;
        if (core != null) {
            try {
                return core.getPerkEventBus();
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        // TODO: Implement when PerkRuntime API is complete
        // var registry = getRegistry();
        // var bus = getEventBus();
        // if (registry == null) return;
        // var player = event.getPlayer();
        // var playerId = player.getUniqueId();
        // var activePerks = registry.getActiveForPlayer(playerId);
        // if (bus != null) {
        //     for (var runtime : activePerks) {
        //         bus.fireDeactivation(player, runtime.perk(), PerkEventBus.DeactivationReason.DISCONNECT);
        //     }
        // }
        // registry.deactivateAllForPlayer(player);
        // registry.cleanupPlayer(playerId);
        // LOGGER.fine(() -> "Cleaned up perks for disconnecting player: " + player.getName());
    }
}
*/
