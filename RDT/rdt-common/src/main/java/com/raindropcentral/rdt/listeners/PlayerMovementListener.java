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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps the town boss bar in sync with player movement and login state.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class PlayerMovementListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the movement listener.
     *
     * @param plugin active RDT runtime
     */
    public PlayerMovementListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Shows or refreshes the boss bar when a player joins.
     *
     * @param event join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        if (this.plugin.getTownBossBarService() != null) {
            this.plugin.getTownBossBarService().refreshPlayer(event.getPlayer());
        }
    }

    /**
     * Refreshes the boss bar when the player changes chunks.
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

        if (this.plugin.getTownBossBarService() != null) {
            this.plugin.getTownBossBarService().refreshPlayer(event.getPlayer());
        }
    }

    /**
     * Hides the boss bar when a player disconnects.
     *
     * @param event quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        if (this.plugin.getTownBossBarService() != null) {
            this.plugin.getTownBossBarService().clearPlayer(event.getPlayer());
        }
    }
}
