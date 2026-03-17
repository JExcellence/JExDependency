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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.factory.BossBarFactory;

/**
 * Keeps the town status boss bar synchronized with player movement and session lifecycle events.
 *
 * @author RaindropCentral
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("unused")
public final class PlayerMovementListener implements Listener {

    private final RDT plugin;

    /**
     * Creates a new movement listener.
     *
     * @param plugin active RDT runtime
     */
    public PlayerMovementListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the boss bar whenever a player crosses into a different chunk.
     *
     * @param event player move event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        this.updateBossBar(
                event.getPlayer(),
                event.getTo().getChunk().getX(),
                event.getTo().getChunk().getZ()
        );
    }

    /**
     * Initializes the boss bar display after player join.
     *
     * @param event player join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        this.updateBossBar(event.getPlayer());
    }

    /**
     * Rebuilds the boss bar one tick after respawn to ensure fresh location context.
     *
     * @param event player respawn event
     */
    @EventHandler
    public void onPlayerRespawn(final @NotNull PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(this.plugin.getPlugin(), () -> this.updateBossBar(event.getPlayer()));
    }

    /**
     * Clears boss bar state when a player disconnects.
     *
     * @param event player quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final BossBarFactory bossBarFactory = this.plugin.getBossBarFactory();
        if (bossBarFactory == null) {
            return;
        }
        bossBarFactory.clear(event.getPlayer());
    }

    private void updateBossBar(final @NotNull Player player) {
        this.updateBossBar(
                player,
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ()
        );
    }

    private void updateBossBar(
            final @NotNull Player player,
            final int chunkX,
            final int chunkZ
    ) {
        final BossBarFactory bossBarFactory = this.plugin.getBossBarFactory();
        if (bossBarFactory == null) {
            return;
        }
        bossBarFactory.run(player, chunkX, chunkZ);
    }
}
