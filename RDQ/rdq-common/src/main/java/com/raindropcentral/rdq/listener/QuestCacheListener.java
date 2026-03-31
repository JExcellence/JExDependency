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
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
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
 * Listener for managing quest cache on player join/quit.
 * <p>
 * This listener automatically loads quest data when a player joins
 * and saves it when they quit.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCacheListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(QuestCacheListener.class.getName());

    private final PlayerQuestProgressCache progressCache;
    private final RDQ rdq;

    public QuestCacheListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.progressCache = rdq.getPlayerQuestProgressCache();
    }
    
    /**
     * Handles player join event - loads quest data.
     * <p>
     * Uses LOWEST priority to load data before other plugins access it.
     * </p>
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Load quest data asynchronously
        progressCache.loadPlayer(player.getUniqueId());
    }
    
    /**
     * Handles player quit event - saves and unloads quest data.
     * <p>
     * Uses MONITOR priority to save data after other plugins are done.
     * </p>
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        
        // Save quest data synchronously on quit
        // This blocks to ensure data is saved before player disconnects
        try {
            progressCache.savePlayer(player.getUniqueId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save quest data for " + player.getName(), e);
        }
    }
}
