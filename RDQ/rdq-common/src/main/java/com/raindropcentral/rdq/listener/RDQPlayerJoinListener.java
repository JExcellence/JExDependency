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

package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that creates or updates RDQPlayer entities when players join the server.
 * This ensures that every player has an RDQPlayer record in the database.
 */
public class RDQPlayerJoinListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private final RDQ rdq;
    
    /**
     * Executes RDQPlayerJoinListener.
     */
    public RDQPlayerJoinListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }
    
    /**
     * Executes onPlayerJoin.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Run async to avoid blocking the main thread
        rdq.getExecutor().submit(() -> {
            try {
                // Check if player already exists
                var existing = rdq.getPlayerRepository().findByAttributes(
                    java.util.Map.of("uniqueId", player.getUniqueId())
                );
                
                if (existing.isEmpty()) {
                    // Create new RDQPlayer
                    RDQPlayer rdqPlayer = new RDQPlayer(
                        player.getUniqueId(),
                        player.getName()
                    );
                    
                    rdq.getPlayerRepository().create(rdqPlayer);
                    this.restorePerkSidebarIfEnabled(player, rdqPlayer);
                    LOGGER.info("Created RDQPlayer for " + player.getName());
                } else {
                    // Update player name if it changed
                    RDQPlayer rdqPlayer = existing.get();
                    if (!rdqPlayer.getPlayerName().equals(player.getName())) {
                        rdqPlayer.setPlayerName(player.getName());
                        rdq.getPlayerRepository().update(rdqPlayer);
                        LOGGER.fine("Updated player name for " + player.getName());
                    }
                    this.restorePerkSidebarIfEnabled(player, rdqPlayer);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create/update RDQPlayer for " + player.getName(), e);
            }
        });
    }

    /**
     * Executes onPlayerQuit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        if (this.rdq.getPerkSidebarScoreboardService() == null) {
            return;
        }

        this.rdq.getPerkSidebarScoreboardService().disable(event.getPlayer());
    }

    private void restorePerkSidebarIfEnabled(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer
    ) {
        if (!rdqPlayer.isPerkSidebarScoreboardEnabled() || this.rdq.getPerkSidebarScoreboardService() == null) {
            return;
        }

        this.rdq.getPlatform().getScheduler().runDelayed(() ->
                this.rdq.getPlatform().getScheduler().runAtEntity(player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    this.rdq.getPerkSidebarScoreboardService().enable(player);
                }), 20L);
    }
}
