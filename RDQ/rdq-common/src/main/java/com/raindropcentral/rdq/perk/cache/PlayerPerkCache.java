package com.raindropcentral.rdq.perk.cache;

import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.util.RetryableOperation;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Cache for player perks.
 * Loads perks on player join, caches them in memory, and persists on disconnect.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PlayerPerkCache {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    // Cache storage: UUID -> PlayerCacheEntry
    private final ConcurrentHashMap<UUID, PlayerCacheEntry> cache;
    
    // Repository for DB operations
    private final PlayerPerkRepository repository;
    
    // Configuration
    private final boolean cacheEnabled;
    private final int maxRetries;
    private final long retryDelayMs;
    private final int saveTimeoutSeconds;
    private final boolean logPerformance;
    private final long performanceThresholdMs;
    
    /**
     * Creates a new PlayerPerkCache.
     *
     * @param repository the player perk repository
     * @param cacheEnabled whether caching is enabled
     * @param maxRetries maximum retry attempts for save operations
     * @param retryDelayMs base delay for retry backoff
     * @param saveTimeoutSeconds timeout for save operations
     * @param logPerformance whether to log performance metrics
     * @param performanceThresholdMs threshold for performance warnings
     */
    public PlayerPerkCache(
            @NotNull final PlayerPerkRepository repository,
            final boolean cacheEnabled,
            final int maxRetries,
            final long retryDelayMs,
            final int saveTimeoutSeconds,
            final boolean logPerformance,
            final long performanceThresholdMs
    ) {
        this.cache = new ConcurrentHashMap<>();
        this.repository = repository;
        this.cacheEnabled = cacheEnabled;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.saveTimeoutSeconds = saveTimeoutSeconds;
        this.logPerformance = logPerformance;
        this.performanceThresholdMs = performanceThresholdMs;
        
        LOGGER.log(Level.INFO, "PlayerPerkCache initialized (enabled={0}, maxRetries={1})",
                new Object[]{cacheEnabled, maxRetries});
    }
    
    /**
     * Loads all PlayerPerks for a player into cache.
     *
     * @param playerId the player UUID
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Void> loadPlayerCache(@NotNull final UUID playerId) {
        if (!cacheEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        final long startTime = System.currentTimeMillis();
        
        return RetryableOperation.executeWithRetry(() -> {
            try {
                // Load all player perks from database
                // We need to filter by player UUID, but we must do it carefully to avoid LazyInitializationException
                List<PlayerPerk> allPlayerPerks = repository.findAll();
                List<PlayerPerk> playerPerks = new java.util.ArrayList<>();
                
                // Filter perks for this player, handling lazy initialization
                for (PlayerPerk pp : allPlayerPerks) {
                    try {
                        // Access the UUID within the same session/transaction
                        UUID ppPlayerId = pp.getPlayer().getUniqueId();
                        if (ppPlayerId != null && ppPlayerId.equals(playerId)) {
                            playerPerks.add(pp);
                        }
                    } catch (Exception e) {
                        // Skip this perk if we can't access the player
                        LOGGER.log(Level.FINE, "Skipping perk due to lazy initialization issue", e);
                    }
                }
                
                // Create cache entry
                PlayerCacheEntry entry = new PlayerCacheEntry();
                for (PlayerPerk pp : playerPerks) {
                    entry.addPerk(pp);
                }
                
                // Store in cache
                cache.put(playerId, entry);
                
                final long loadTime = System.currentTimeMillis() - startTime;
                
                if (logPerformance) {
                    LOGGER.log(Level.INFO, "Loaded {0} perks for player {1} in {2}ms",
                            new Object[]{playerPerks.size(), playerId, loadTime});
                    
                    if (loadTime > performanceThresholdMs) {
                        LOGGER.log(Level.WARNING, "Cache load time exceeded threshold: {0}ms > {1}ms",
                                new Object[]{loadTime, performanceThresholdMs});
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load cache for player " + playerId, e);
                // Create empty cache entry to allow player to join
                cache.put(playerId, new PlayerCacheEntry());
                throw e;
            }
        }, "loadPlayerCache for player " + playerId);
    }
    
    /**
     * Gets a PlayerPerk from cache.
     *
     * @param playerId the player UUID
     * @param perkId the perk ID
     * @return Optional containing the PlayerPerk, or empty if not found
     */
    public Optional<PlayerPerk> getPlayerPerk(@NotNull final UUID playerId, @NotNull final Long perkId) {
        if (!cacheEnabled) {
            return Optional.empty();
        }
        
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            if (logPerformance) {
                LOGGER.log(Level.FINE, "Cache miss for player {0}", playerId);
            }
            return Optional.empty();
        }
        
        if (logPerformance) {
            LOGGER.log(Level.FINEST, "Cache hit for player {0}, perk {1}", new Object[]{playerId, perkId});
        }
        
        return entry.getPerk(perkId);
    }
    
    /**
     * Gets all PlayerPerks for a player from cache.
     *
     * @param playerId the player UUID
     * @return List of PlayerPerks, or empty list if player not cached
     */
    public List<PlayerPerk> getAllPlayerPerks(@NotNull final UUID playerId) {
        if (!cacheEnabled) {
            return List.of();
        }
        
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            return List.of();
        }
        
        return entry.getAllPerks();
    }
    
    /**
     * Gets filtered PlayerPerks from cache.
     *
     * @param playerId the player UUID
     * @param filter the predicate to filter perks
     * @return List of filtered PlayerPerks
     */
    public List<PlayerPerk> getPlayerPerks(@NotNull final UUID playerId, @NotNull final Predicate<PlayerPerk> filter) {
        if (!cacheEnabled) {
            return List.of();
        }
        
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            return List.of();
        }
        
        return entry.getPerks(filter);
    }
    
    /**
     * Updates a PlayerPerk in cache and marks it as dirty.
     *
     * @param playerId the player UUID
     * @param playerPerk the PlayerPerk to update
     */
    public void updatePlayerPerk(@NotNull final UUID playerId, @NotNull final PlayerPerk playerPerk) {
        if (!cacheEnabled) {
            return;
        }
        
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            LOGGER.log(Level.WARNING, "Cannot update perk for player {0}: cache not loaded", playerId);
            return;
        }
        
        entry.updatePerk(playerPerk);
        
        if (logPerformance) {
            LOGGER.log(Level.FINEST, "Updated perk {0} for player {1} (marked dirty)",
                    new Object[]{playerPerk.getPerk().getId(), playerId});
        }
    }
    
    /**
     * Checks if a player's cache is loaded.
     *
     * @param playerId the player UUID
     * @return true if cache is loaded
     */
    public boolean isCacheLoaded(@NotNull final UUID playerId) {
        return cacheEnabled && cache.containsKey(playerId);
    }
    
    /**
     * Gets cache statistics.
     *
     * @return string with cache statistics
     */
    public String getCacheStats() {
        int totalPlayers = cache.size();
        int totalPerks = cache.values().stream()
                .mapToInt(PlayerCacheEntry::size)
                .sum();
        int totalDirty = cache.values().stream()
                .mapToInt(PlayerCacheEntry::dirtyCount)
                .sum();
        
        return String.format("Cache: %d players, %d perks, %d dirty", totalPlayers, totalPerks, totalDirty);
    }


    /**
     * Saves dirty PlayerPerks to DB and removes player from cache.
     *
     * @param playerId the player UUID
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Void> saveAndUnloadPlayerCache(@NotNull final UUID playerId) {
        if (!cacheEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        final long startTime = System.currentTimeMillis();
        
        PlayerCacheEntry entry = cache.get(playerId);
        if (entry == null) {
            LOGGER.log(Level.FINE, "No cache entry to save for player {0}", playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        List<PlayerPerk> dirtyPerks = entry.getDirtyPerks();
        if (dirtyPerks.isEmpty()) {
            cache.remove(playerId);
            LOGGER.log(Level.FINE, "No dirty perks to save for player {0}", playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        // Use retry logic for saving dirty perks
        return RetryableOperation.executeWithRetry(() -> {
            // Save all dirty perks
            for (PlayerPerk pp : dirtyPerks) {
                repository.update(pp);
            }
            
            // Clear dirty flags and remove from cache
            entry.clearDirtyFlags();
            cache.remove(playerId);
            
            final long saveTime = System.currentTimeMillis() - startTime;
            
            if (logPerformance) {
                LOGGER.log(Level.INFO, "Saved {0} dirty perks for player {1} in {2}ms",
                        new Object[]{dirtyPerks.size(), playerId, saveTime});
                
                if (saveTime > performanceThresholdMs) {
                    LOGGER.log(Level.WARNING, "Cache save time exceeded threshold: {0}ms > {1}ms",
                            new Object[]{saveTime, performanceThresholdMs});
                }
            }
            
            return null;
        }, "saveAndUnloadPlayerCache for player " + playerId, maxRetries, retryDelayMs, 2.0);
    }
    
    /**
     * Manually flushes cache for a player (admin command).
     *
     * @param playerId the player UUID
     * @return CompletableFuture that completes when flush is done
     */
    public CompletableFuture<Void> flushPlayerCache(@NotNull final UUID playerId) {
        if (!cacheEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.log(Level.INFO, "Manually flushing cache for player {0}", playerId);
        
        return saveAndUnloadPlayerCache(playerId)
                .thenRun(() -> {
                    LOGGER.log(Level.INFO, "Cache flushed for player {0}", playerId);
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to flush cache for player " + playerId, throwable);
                    return null;
                });
    }
    
    /**
     * Saves all caches (for server shutdown).
     *
     * @return CompletableFuture that completes when all saves are done
     */
    public CompletableFuture<Void> saveAllCaches() {
        if (!cacheEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.log(Level.INFO, "Saving all player caches ({0} players)", cache.size());
        
        List<CompletableFuture<Void>> saveFutures = cache.keySet().stream()
                .map(this::saveAndUnloadPlayerCache)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    LOGGER.log(Level.INFO, "All player caches saved successfully");
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to save all caches", throwable);
                    return null;
                });
    }
}
