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

package com.raindropcentral.rdq.perk.cache;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Event listener for managing simple perk cache lifecycle.
 *
 * <p>Loads all player perks on join (async) and saves all changes on quit (blocking).
 *
 * @author JExcellence
 * @version 2.0.0
 */
public class PerkCacheListener implements Listener {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

	private final RDQ rdq;

	/**
	 * Executes PerkCacheListener.
	 */
	public PerkCacheListener(@NotNull final RDQ rdq) {
		this.rdq = rdq;
	}
	
	/**
	 * Handles player join event by loading their perks asynchronously.
	 *
	 * @param event the player join event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(@NotNull final AsyncPlayerPreLoginEvent event) {
		rdq.getPlayerPerkCache().loadPlayerAsync(event.getUniqueId()).exceptionally(throwable -> {LOGGER.log(java.util.logging.Level.SEVERE, "Failed to load perks for player " + event.getUniqueId(), throwable);return null;});
	}
	
	/**
	 * Handles player quit event by saving their perks synchronously.
	 * This blocks to ensure data is saved before player disconnects.
	 *
	 * @param event the player quit event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();
		
		LOGGER.fine("Saving perks for player " + player.getName());
		rdq.getPlayerPerkCache().savePlayer(playerId);
	}
}
