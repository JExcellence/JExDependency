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

import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
import com.raindropcentral.rplatform.logging.CentralLogger;
import jakarta.persistence.OptimisticLockException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple in-memory cache for player perks.
 *
 * <p>This cache loads all player perks on join, stores them in memory,
 * and saves them back to the database on leave. This provides instant
 * access to perk data without database queries.
 * 
 * <p><strong>Design Philosophy</strong>
 * <ul>
 *   <li>Load all perks on player join</li>
 *   <li>Toggle enabled/disabled instantly in memory</li>
 *   <li>Save all changes on player leave</li>
 *   <li>Auto-save periodically for crash protection</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.0.0
 */
public class SimplePerkCache {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final PlayerPerkRepository repository;
	
	/**
	 * Cache of player perks: UUID -> List of PlayerPerks.
	 */
	private final ConcurrentHashMap<UUID, List<PlayerPerk>> cache;
	
	/**
	 * Set of players with unsaved changes.
	 */
	private final Set<UUID> dirtyPlayers;
	
	/**
	 * Whether to log performance metrics.
	 */
	private final boolean logPerformance;
	
	/**
	 * Constructs a new SimplePerkCache.
	 *
	 * @param repository the player perk repository
	 * @param logPerformance whether to log performance metrics
	 */
	public SimplePerkCache(
			@NotNull final PlayerPerkRepository repository,
			final boolean logPerformance
	) {
		this.repository = repository;
		this.cache = new ConcurrentHashMap<>();
		this.dirtyPlayers = ConcurrentHashMap.newKeySet();
		this.logPerformance = logPerformance;
	}
	
	/**
	 * Loads all perks for a player from the database into memory.
	 * Uses a custom query with JOIN FETCH to eagerly load Perk data.
	 * This prevents LazyInitializationException when accessing perks from cache.
	 * 
	 * <p><b>Thread Safety:</b> The loaded list is wrapped in a synchronized list
	 * to prevent ConcurrentModificationException when perks are modified while
	 * auto-save is running.</p>
	 *
	 * @param playerId the player's UUID
	 * @return CompletableFuture that completes when loading is done
	 */
	public CompletableFuture<Void> loadPlayerAsync(@NotNull final UUID playerId) {
		return repository.findAllByPlayerIdWithPerk(playerId).thenAcceptAsync(playerPerks -> {
			// Wrap in synchronized list to prevent ConcurrentModificationException
			// This allows safe concurrent reads/writes from GUI and auto-save
			List<PlayerPerk> synchronizedList = Collections.synchronizedList(new ArrayList<>(playerPerks));
			cache.put(playerId, synchronizedList);
		});
	}
	
	/**
	 * Loads all perks for a player synchronously (blocking).
	 * Use loadPlayerAsync() for better performance.
	 *
	 * @param playerId the player's UUID
	 */
	public void loadPlayer(@NotNull final UUID playerId) {
		try {
			loadPlayerAsync(playerId).get(5, java.util.concurrent.TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to load perks for player " + playerId, e);
			cache.put(playerId, new ArrayList<>());
		}
	}
	
	/**
	 * Saves all perks for a player to the database and removes from cache.
	 *
	 * @param playerId the player's UUID
	 */
	public void savePlayer(@NotNull final UUID playerId) {
		long startTime = System.currentTimeMillis();
		
		try {
			// Check if player has unsaved changes
			if (!dirtyPlayers.contains(playerId)) {
				LOGGER.fine("Player " + playerId + " has no unsaved changes, skipping save");
				cache.remove(playerId);
				return;
			}
			
			// Get perks from cache
			List<PlayerPerk> perks = cache.get(playerId);
			if (perks == null || perks.isEmpty()) {
				LOGGER.fine("No perks to save for player " + playerId);
				dirtyPlayers.remove(playerId);
				return;
			}
			
			// Batch update to database using the cascade-safe method
			int savedCount = 0;
			for (PlayerPerk perk : perks) {
				try {
					// CRITICAL: Use updatePlayerPerkOnly to avoid cascade to Perk entity
					// This prevents OptimisticLockException on the Perk entity
					repository.updatePlayerPerkOnly(perk).join();
					savedCount++;
				} catch (OptimisticLockException e) {
					// Log but continue - this perk was modified elsewhere
					LOGGER.fine("Optimistic lock on perk " + perk.getPerk().getIdentifier() + 
							" for player " + playerId + " - entity was updated elsewhere, skipping");
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to save perk " + perk.getPerk().getIdentifier() + 
							" for player " + playerId, e);
				}
			}
			
			// Clear dirty flag and remove from cache
			dirtyPlayers.remove(playerId);
			cache.remove(playerId);
			
			long duration = System.currentTimeMillis() - startTime;
			
			if (logPerformance) {
				LOGGER.info(String.format("Saved %d/%d perks for player %s in %dms",
						savedCount, perks.size(), playerId, duration));
			} else {
				LOGGER.fine(String.format("Saved %d perks for player %s",
						savedCount, playerId));
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to save perks for player " + playerId, e);
		}
	}
	
	/**
	 * Gets all perks for a player from cache.
	 *
	 * @param playerId the player's UUID
	 * @return list of player perks, or empty list if not loaded
	 */
	@NotNull
	public List<PlayerPerk> getPerks(@NotNull final UUID playerId) {
		List<PlayerPerk> perks = cache.get(playerId);
		return perks != null ? new ArrayList<>(perks) : Collections.emptyList();
	}
	
	/**
	 * Gets a specific perk for a player from cache.
	 *
	 * @param playerId the player's UUID
	 * @param perkId the perk ID
	 * @return the player perk, or null if not found
	 */
	@Nullable
	public PlayerPerk getPerk(@NotNull final UUID playerId, @NotNull final Long perkId) {
		List<PlayerPerk> perks = cache.get(playerId);
		if (perks == null) {
			return null;
		}
		
		return perks.stream()
				.filter(p -> p.getPerk().getId().equals(perkId))
				.findFirst()
				.orElse(null);
	}
	
	/**
	 * Updates a perk in the cache.
	 * If the perk doesn't exist in cache, it will be added.
	 * 
	 * <p><b>Thread Safety:</b> Synchronizes on the perk list to prevent
	 * concurrent modification during auto-save.</p>
	 *
	 * @param playerId the player's UUID
	 * @param perk the player perk to update
	 */
	public void updatePerk(@NotNull final UUID playerId, @NotNull final PlayerPerk perk) {
		List<PlayerPerk> perks = cache.get(playerId);
		
		if (perks == null) {
			LOGGER.warning("Attempted to update perk for player " + playerId + 
					" but cache not loaded. Loading now...");
			loadPlayer(playerId);
			perks = cache.get(playerId);
		}
		
		// Synchronize on the list to prevent concurrent modification
		synchronized (perks) {
			// Remove old version if exists
			perks.removeIf(p -> p.getPerk().getId().equals(perk.getPerk().getId()));
			
			// Add updated version
			perks.add(perk);
		}
		
		// Mark as dirty
		markDirty(playerId);
		
		LOGGER.fine("Updated perk " + perk.getPerk().getIdentifier() + " for player " + playerId);
	}
	
	/**
	 * Marks a player as having unsaved changes.
	 *
	 * @param playerId the player's UUID
	 */
	public void markDirty(@NotNull final UUID playerId) {
		dirtyPlayers.add(playerId);
		LOGGER.finest("Marked player " + playerId + " as dirty");
	}
	
	/**
	 * Checks if a player's perks are loaded in cache.
	 *
	 * @param playerId the player's UUID
	 * @return true if loaded, false otherwise
	 */
	public boolean isLoaded(@NotNull final UUID playerId) {
		return cache.containsKey(playerId);
	}
	
	/**
	 * Checks if a player has unsaved changes.
	 *
	 * @param playerId the player's UUID
	 * @return true if dirty, false otherwise
	 */
	public boolean isDirty(@NotNull final UUID playerId) {
		return dirtyPlayers.contains(playerId);
	}
	
	/**
	 * Auto-saves all players with unsaved changes.
	 * This should be called periodically for crash protection.
	 * 
	 * <p><b>Thread Safety:</b> Synchronizes on the perk list to prevent
	 * ConcurrentModificationException when perks are being modified in GUI.</p>
	 *
	 * @return number of players saved
	 */
	public int autoSaveAll() {
		long startTime = System.currentTimeMillis();
		
		// Get snapshot of dirty players
		Set<UUID> playersToSave = new HashSet<>(dirtyPlayers);
		
		if (playersToSave.isEmpty()) {
			LOGGER.fine("Auto-save: No dirty players to save");
			return 0;
		}
		
		int savedCount = 0;
		int errorCount = 0;
		
		for (UUID playerId : playersToSave) {
			try {
				List<PlayerPerk> perks = cache.get(playerId);
				if (perks == null) {
					continue;
				}
				
				// Synchronize on the list to prevent concurrent modification
				// The list is a synchronized list, so we need to sync when iterating
				synchronized (perks) {
					// Save to database without removing from cache
					for (PlayerPerk perk : perks) {
						try {
							// CRITICAL: Use updatePlayerPerkOnly to avoid cascade to Perk entity
							repository.updatePlayerPerkOnly(perk).join();
						} catch (OptimisticLockException e) {
							// Log but don't fail - this can happen if the perk was updated elsewhere
							LOGGER.fine("Optimistic lock exception for perk " + 
									perk.getPerk().getIdentifier() + " for player " + playerId + 
									" - entity was updated elsewhere");
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Auto-save failed for perk " + 
									perk.getPerk().getIdentifier() + " for player " + playerId, e);
							errorCount++;
						}
					}
				}
				
				// Clear dirty flag (but keep in cache since player is still online)
				dirtyPlayers.remove(playerId);
				savedCount++;
				
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Auto-save failed for player " + playerId, e);
				errorCount++;
			}
		}
		
		long duration = System.currentTimeMillis() - startTime;
		
		LOGGER.info(String.format("Auto-save completed: %d players saved, %d errors in %dms",
				savedCount, errorCount, duration));
		
		return savedCount;
	}
	
	/**
	 * Gets the number of players currently in cache.
	 *
	 * @return cache size
	 */
	public int getCacheSize() {
		return cache.size();
	}
	
	/**
	 * Gets the number of players with unsaved changes.
	 *
	 * @return dirty player count
	 */
	public int getDirtyCount() {
		return dirtyPlayers.size();
	}
	
	/**
	 * Gets cache statistics for monitoring.
	 *
	 * @return map of statistics
	 */
	@NotNull
	public Map<String, Object> getStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("cache_size", getCacheSize());
		stats.put("dirty_count", getDirtyCount());
		stats.put("total_perks", cache.values().stream()
				.mapToInt(List::size)
				.sum());
		return stats;
	}
	
	/**
	 * Clears all cache data. Use with caution!
	 * This will lose any unsaved changes.
	 */
	public void clearAll() {
		LOGGER.warning("Clearing all perk cache data. Unsaved changes will be lost!");
		cache.clear();
		dirtyPlayers.clear();
	}
}
