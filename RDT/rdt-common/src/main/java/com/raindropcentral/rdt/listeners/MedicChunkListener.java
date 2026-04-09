/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.service.TownMedicService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Forwards relevant player movement lifecycle events into the Medic chunk runtime service.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public final class MedicChunkListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the Medic chunk listener.
     *
     * @param plugin active plugin runtime
     */
    public MedicChunkListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Refreshes medic state after a player joins.
     *
     * @param event join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final TownMedicService townMedicService = this.plugin.getTownMedicService();
        if (townMedicService != null) {
            townMedicService.handlePlayerJoin(event.getPlayer());
        }
    }

    /**
     * Refreshes medic state after a player crosses into a different chunk or world.
     *
     * @param event move event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        if (event.getTo() == null
            || event.getFrom().getWorld() == null
            || event.getTo().getWorld() == null
            || (event.getFrom().getChunk().getX() == event.getTo().getChunk().getX()
            && event.getFrom().getChunk().getZ() == event.getTo().getChunk().getZ()
            && event.getFrom().getWorld().getUID().equals(event.getTo().getWorld().getUID()))) {
            return;
        }

        final TownMedicService townMedicService = this.plugin.getTownMedicService();
        if (townMedicService != null) {
            townMedicService.handlePlayerMove(event.getPlayer(), event.getFrom(), event.getTo());
        }
    }

    /**
     * Refreshes medic state after a player teleports.
     *
     * @param event teleport event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(final @NotNull PlayerTeleportEvent event) {
        final TownMedicService townMedicService = this.plugin.getTownMedicService();
        if (townMedicService != null) {
            townMedicService.handlePlayerTeleport(event.getPlayer(), event.getTo());
        }
    }

    /**
     * Refreshes medic state after a player respawns.
     *
     * @param event respawn event
     */
    @EventHandler
    public void onPlayerRespawn(final @NotNull PlayerRespawnEvent event) {
        final TownMedicService townMedicService = this.plugin.getTownMedicService();
        if (townMedicService != null) {
            townMedicService.handlePlayerRespawn(event.getPlayer(), event.getRespawnLocation());
        }
    }

    /**
     * Clears medic runtime state after a player disconnects.
     *
     * @param event quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final TownMedicService townMedicService = this.plugin.getTownMedicService();
        if (townMedicService != null) {
            townMedicService.handlePlayerQuit(event.getPlayer());
        }
    }
}
