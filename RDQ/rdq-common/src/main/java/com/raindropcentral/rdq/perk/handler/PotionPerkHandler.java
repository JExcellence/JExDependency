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

package com.raindropcentral.rdq.perk.handler;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkEffectSection;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for potion effect perks.
 *
 * <p>This handler manages the application, removal, and continuous refresh of potion effects
 * for passive perks. It maintains a scheduled task that refreshes potion effects to ensure
 * they remain active while the perk is enabled.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PotionPerkHandler {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	private static final String PERKS_PATH = "perks";
	
	// Refresh interval in ticks (10 seconds)
	private static final long REFRESH_INTERVAL_TICKS = 200L;
	
	private final RDQ plugin;
	
	// Track active potion perks by player UUID
	private final Map<UUID, Map<Long, PlayerPerk>> activePlayerPerks = new ConcurrentHashMap<>();
	
	// Track the active refresh scheduler generation because the platform scheduler does not expose task handles
	private final AtomicLong refreshTaskGeneration = new AtomicLong();
	private volatile boolean refreshTaskRunning;
	
	/**
	 * Constructs a new PotionPerkHandler.
	 *
	 * @param plugin the RDQ plugin instance
	 */
	public PotionPerkHandler(@NotNull final RDQ plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Applies a potion effect to a player based on the perk configuration.
	 *
	 * @param player the player to apply the effect to
	 * @param playerPerk the player perk containing the effect configuration
	 * @return true if the effect was applied successfully, false otherwise
	 */
	public boolean applyPotionEffect(
			@NotNull final Player player,
			@NotNull final PlayerPerk playerPerk
	) {
		PerkSection perkSection = loadPerkConfig(playerPerk.getPerk());
		if (perkSection == null || perkSection.getEffect() == null) {
			LOGGER.warning("Cannot apply potion effect for perk " + playerPerk.getPerk().getIdentifier() + " - no effect config");
			return false;
		}
		
		PerkEffectSection effect = perkSection.getEffect();
		
		// Extract potion effect configuration using getters
		String potionEffectType = effect.getPotionEffectType();
		if (potionEffectType == null || potionEffectType.isEmpty()) {
			LOGGER.warning("Perk " + playerPerk.getPerk().getIdentifier() + " has no potionEffectType configured");
			return false;
		}
		
		// Parse potion effect type using Registry API (modern approach)
		PotionEffectType effectType;
		try {
			// Convert to lowercase for namespaced key (e.g., "JUMP_BOOST" -> "jump_boost")
			String effectKey = potionEffectType.toLowerCase();
			effectType = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(effectKey));
			
			if (effectType == null) {
				LOGGER.warning("Invalid potion effect type: " + potionEffectType + " for perk " + playerPerk.getPerk().getIdentifier());
				return false;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to parse potion effect type: " + potionEffectType + " for perk " + playerPerk.getPerk().getIdentifier(), e);
			return false;
		}
		
		// Extract effect parameters using getters
		int amplifier = effect.getAmplifier();
		int durationTicks = effect.getDurationTicks();
		boolean ambient = effect.getAmbient();
		boolean particles = effect.getParticles();
		
		// Create potion effect
		PotionEffect potionEffect = new PotionEffect(
				effectType,
				durationTicks,
				amplifier,
				ambient,
				particles,
				true // icon
		);
		
		// Apply potion effect on main thread (Bukkit requirement)
		plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
			player.addPotionEffect(potionEffect);
		});
		
		// Track this perk for refresh
		activePlayerPerks
				.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
				.put(playerPerk.getId(), playerPerk);

		plugin.getPerkActivationService().recordEffectTrigger(player, playerPerk);
		
		LOGGER.info("Applied potion effect " + effectType.getKey().getKey() + " (amplifier " + amplifier + 
				") to player " + player.getName() + " from perk " + playerPerk.getPerk().getIdentifier());
		
		return true;
	}
	
	/**
	 * Removes a potion effect from a player.
	 *
	 * @param player the player to remove the effect from
	 * @param playerPerk the player perk containing the effect configuration
	 * @return true if the effect was removed successfully, false otherwise
	 */
	public boolean removePotionEffect(
			@NotNull final Player player,
			@NotNull final PlayerPerk playerPerk
	) {
		PerkSection perkSection = loadPerkConfig(playerPerk.getPerk());
		if (perkSection == null || perkSection.getEffect() == null) {
			LOGGER.warning("Cannot remove potion effect for perk " + playerPerk.getPerk().getIdentifier() + " - no effect config");
			return false;
		}
		
		PerkEffectSection effect = perkSection.getEffect();
		
		// Extract potion effect configuration using getters
		String potionEffectType = effect.getPotionEffectType();
		if (potionEffectType == null || potionEffectType.isEmpty()) {
			LOGGER.warning("Perk " + playerPerk.getPerk().getIdentifier() + " has no potionEffectType configured");
			return false;
		}
		
		// Parse potion effect type using Registry API (modern approach)
		PotionEffectType effectType;
		try {
			// Convert to lowercase for namespaced key (e.g., "JUMP_BOOST" -> "jump_boost")
			String effectKey = potionEffectType.toLowerCase();
			effectType = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(effectKey));
			
			if (effectType == null) {
				LOGGER.warning("Invalid potion effect type: " + potionEffectType + " for perk " + playerPerk.getPerk().getIdentifier());
				return false;
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to parse potion effect type: " + potionEffectType + " for perk " + playerPerk.getPerk().getIdentifier(), e);
			return false;
		}
		
		// Remove the potion effect on main thread (Bukkit requirement)
		plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
			player.removePotionEffect(effectType);
		});
		
		// Stop tracking this perk
		Map<Long, PlayerPerk> playerPerks = activePlayerPerks.get(player.getUniqueId());
		if (playerPerks != null) {
			playerPerks.remove(playerPerk.getId());
			if (playerPerks.isEmpty()) {
				activePlayerPerks.remove(player.getUniqueId());
			}
		}

		return true;
	}
	
	/**
	 * Refreshes a potion effect for a player.
	 * This is called periodically to maintain continuous effects.
	 *
	 * @param player the player to refresh the effect for
	 * @param playerPerk the player perk containing the effect configuration
	 * @return true if the effect was refreshed successfully, false otherwise
	 */
	public boolean refreshPotionEffect(
			@NotNull final Player player,
			@NotNull final PlayerPerk playerPerk
	) {
		// Simply reapply the effect
		return applyPotionEffect(player, playerPerk);
	}
	
	/**
	 * Starts the scheduled task for continuous effect refresh.
	 * This task runs periodically to refresh all active potion effects.
	 */
	public void startRefreshTask() {
		if (refreshTaskRunning) {
			LOGGER.warning("Refresh task is already running");
			return;
		}

		refreshTaskRunning = true;
		final long generation = refreshTaskGeneration.incrementAndGet();
		this.plugin.getPlatform().getScheduler().runRepeating(() -> {
			if (!this.refreshTaskRunning || this.refreshTaskGeneration.get() != generation) {
				return;
			}

			// Iterate through all active player perks
			activePlayerPerks.forEach((playerUuid, perks) -> {
				Player player = Bukkit.getPlayer(playerUuid);
				
				// Skip if player is offline
				if (player == null || !player.isOnline()) {
					return;
				}
				
				// Refresh each perk's potion effect
				perks.values().forEach(playerPerk -> {
					try {
						refreshPotionEffect(player, playerPerk);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Error refreshing potion effect for perk " + 
								playerPerk.getPerk().getIdentifier() + " for player " + player.getName(), e);
					}
				});
			});
		}, REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);

		LOGGER.info("Started potion effect refresh task (interval: " + REFRESH_INTERVAL_TICKS + " ticks)");
	}
	
	/**
	 * Stops the scheduled task for continuous effect refresh.
	 */
	public void stopRefreshTask() {
		if (!this.refreshTaskRunning) {
			return;
		}

		this.refreshTaskRunning = false;
		this.refreshTaskGeneration.incrementAndGet();
		LOGGER.info("Stopped potion effect refresh task");
	}
	
	/**
	 * Cleans up tracking data for a player.
	 * Should be called when a player logs out.
	 *
	 * @param playerUuid the player UUID
	 */
	public void cleanupPlayer(@NotNull final UUID playerUuid) {
		activePlayerPerks.remove(playerUuid);
	}
	
	/**
	 * Loads a perk configuration from YAML file.
	 *
	 * @param perk the perk
	 * @return the perk section, or null if loading failed
	 */
	@Nullable
	private PerkSection loadPerkConfig(@NotNull final Perk perk) {
		try {
			ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), PERKS_PATH);
			ConfigKeeper<PerkSection> cfgKeeper = new ConfigKeeper<>(cfgManager, perk.getIdentifier() + ".yml", PerkSection.class);
			return cfgKeeper.rootSection;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to load perk config for " + perk.getIdentifier(), e);
			return null;
		}
	}
}
