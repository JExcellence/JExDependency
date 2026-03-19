package com.raindropcentral.rdq.cache.quest;

import com.raindropcentral.rdq.database.entity.quest.PlayerQuestProgress;
import com.raindropcentral.rdq.database.repository.quest.PlayerQuestProgressRepository;
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
 * In-memory cache for player quest progress.
 * <p>
 * This cache loads all active quest progress on player join, stores it in memory,
 * and saves it back to the database on player quit. This provides instant access
 * to quest progress data without database queries during gameplay.
 * </p>
 * 
 * <h3>Design Philosophy</h3>
 * <ul>
 *   <li>Load all active quest progress on player join</li>
 *   <li>Update progress instantly in memory</li>
 *   <li>Save all changes on player quit</li>
 *   <li>Auto-save periodically for crash protection (every 5 minutes)</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * This cache uses synchronized lists to prevent ConcurrentModificationException
 * when progress is modified while auto-save is running. All public methods are
 * thread-safe and can be called from any thread.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PlayerQuestProgressCache {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final PlayerQuestProgressRepository repository;
    
    /**
     * Cache of player quest progress: UUID -> List of PlayerQuestProgress
     */
    private final ConcurrentHashMap<UUID, List<PlayerQuestProgress>> cache;
    
    /**
     * Set of players with unsaved changes
     */
    private final Set<UUID> dirtyPlayers;
    
    /**
     * Whether to log performance metrics
     */
    private final boolean logPerformance;
    
    /**
     * Constructs a new PlayerQuestProgressCache.
     *
     * @param repository the player quest progress repository
     * @param logPerformance whether to log performance metrics
     */
    public PlayerQuestProgressCache(
        @NotNull final PlayerQuestProgressRepository repository,
        final boolean logPerformance
    ) {
        this.repository = repository;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
        this.logPerformance = logPerformance;
    }
    
    /**
     * Loads all active quest progress for a player from the database into memory.
     * Uses a custom query with JOIN FETCH to eagerly load task progress.
     * This prevents LazyInitializationException when accessing tasks from cache.
     * 
     * <p><b>Thread Safety:</b> The loaded list is wrapped in a synchronized list
     * to prevent ConcurrentModificationException when progress is modified while
     * auto-save is running.</p>
     *
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when loading is done
     */
    @NotNull
    public CompletableFuture<Void> loadPlayerAsync(@NotNull final UUID playerId) {
        return repository.findActiveByPlayerWithTasks(playerId).thenAcceptAsync(progressList -> {
            // Wrap in synchronized list to prevent ConcurrentModificationException
            // This allows safe concurrent reads/writes from GUI and auto-save
            List<PlayerQuestProgress> synchronizedList = Collections.synchronizedList(new ArrayList<>(progressList));
            cache.put(playerId, synchronizedList);
            
            if (logPerformance) {
                LOGGER.info(String.format("Loaded %d active quests for player %s", 
                    progressList.size(), playerId));
            }
        });
    }
    
    /**
     * Loads all quest progress for a player synchronously (blocking).
     * Use loadPlayerAsync() for better performance.
     *
     * @param playerId the player's UUID
     */
    public void loadPlayer(@NotNull final UUID playerId) {
        try {
            loadPlayerAsync(playerId).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quest progress for player " + playerId, e);
            cache.put(playerId, Collections.synchronizedList(new ArrayList<>()));
        }
    }
    
    /**
     * Saves all quest progress for a player to the database and removes from cache.
     * <p>
     * This method should be called when a player quits the server to persist
     * all progress changes and free memory.
     * </p>
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
            
            // Get progress from cache
            List<PlayerQuestProgress> progressList = cache.get(playerId);
            if (progressList == null || progressList.isEmpty()) {
                LOGGER.fine("No quest progress to save for player " + playerId);
                dirtyPlayers.remove(playerId);
                return;
            }
            
            // Batch update to database
            int savedCount = 0;
            synchronized (progressList) {
                for (PlayerQuestProgress progress : progressList) {
                    try {
                        repository.createAsync(progress).join();
                        savedCount++;
                    } catch (OptimisticLockException e) {
                        // Log but continue - this progress was modified elsewhere
                        LOGGER.fine("Optimistic lock on quest progress " + progress.getQuest().getIdentifier() + 
                            " for player " + playerId + " - entity was updated elsewhere, skipping");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to save quest progress " + 
                            progress.getQuest().getIdentifier() + " for player " + playerId, e);
                    }
                }
            }
            
            // Clear dirty flag and remove from cache
            dirtyPlayers.remove(playerId);
            cache.remove(playerId);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (logPerformance) {
                LOGGER.info(String.format("Saved %d/%d quest progress for player %s in %dms",
                    savedCount, progressList.size(), playerId, duration));
            } else {
                LOGGER.fine(String.format("Saved %d quest progress for player %s",
                    savedCount, playerId));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save quest progress for player " + playerId, e);
        }
    }
    
    /**
     * Gets all quest progress for a player from cache.
     *
     * @param playerId the player's UUID
     * @return list of quest progress, or empty list if not loaded
     */
    @NotNull
    public List<PlayerQuestProgress> getProgress(@NotNull final UUID playerId) {
        List<PlayerQuestProgress> progress = cache.get(playerId);
        return progress != null ? new ArrayList<>(progress) : Collections.emptyList();
    }
    
    /**
     * Gets progress for a specific quest from cache.
     *
     * @param playerId the player's UUID
     * @param questId the quest ID
     * @return the quest progress, or null if not found
     */
    @Nullable
    public PlayerQuestProgress getQuestProgress(@NotNull final UUID playerId, @NotNull final Long questId) {
        List<PlayerQuestProgress> progressList = cache.get(playerId);
        if (progressList == null) {
            return null;
        }
        
        synchronized (progressList) {
            return progressList.stream()
                .filter(p -> p.getQuest().getId().equals(questId))
                .findFirst()
                .orElse(null);
        }
    }
    
    /**
     * Updates quest progress in the cache.
     * If the progress doesn't exist in cache, it will be added.
     * 
     * <p><b>Thread Safety:</b> Synchronizes on the progress list to prevent
     * concurrent modification during auto-save.</p>
     *
     * @param playerId the player's UUID
     * @param progress the quest progress to update
     */
    public void updateProgress(@NotNull final UUID playerId, @NotNull final PlayerQuestProgress progress) {
        List<PlayerQuestProgress> progressList = cache.get(playerId);
        
        if (progressList == null) {
            LOGGER.warning("Attempted to update quest progress for player " + playerId + 
                " but cache not loaded. Loading now...");
            loadPlayer(playerId);
            progressList = cache.get(playerId);
        }
        
        // Synchronize on the list to prevent concurrent modification
        synchronized (progressList) {
            // Remove old version if exists
            progressList.removeIf(p -> p.getQuest().getId().equals(progress.getQuest().getId()));
            
            // Add updated version
            progressList.add(progress);
        }
        
        // Mark as dirty
        markDirty(playerId);
        
        LOGGER.fine("Updated quest progress " + progress.getQuest().getIdentifier() + 
            " for player " + playerId);
    }
    
    /**
     * Removes quest progress from the cache.
     * This should be called when a quest is completed or abandoned.
     *
     * @param playerId the player's UUID
     * @param questId the quest ID
     */
    public void removeProgress(@NotNull final UUID playerId, @NotNull final Long questId) {
        List<PlayerQuestProgress> progressList = cache.get(playerId);
        if (progressList == null) {
            return;
        }
        
        synchronized (progressList) {
            boolean removed = progressList.removeIf(p -> p.getQuest().getId().equals(questId));
            if (removed) {
                markDirty(playerId);
                LOGGER.fine("Removed quest progress for quest " + questId + " from player " + playerId);
            }
        }
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
     * Checks if a player's quest progress is loaded in cache.
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
     * Gets the number of active quests for a player from cache.
     *
     * @param playerId the player's UUID
     * @return number of active quests
     */
    public int getActiveQuestCount(@NotNull final UUID playerId) {
        List<PlayerQuestProgress> progressList = cache.get(playerId);
        return progressList != null ? progressList.size() : 0;
    }
    
    /**
     * Auto-saves all players with unsaved changes.
     * This should be called periodically for crash protection (every 5 minutes).
     * 
     * <p><b>Thread Safety:</b> Synchronizes on the progress list to prevent
     * ConcurrentModificationException when progress is being modified.</p>
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
                List<PlayerQuestProgress> progressList = cache.get(playerId);
                if (progressList == null) {
                    continue;
                }
                
                // Synchronize on the list to prevent concurrent modification
                synchronized (progressList) {
                    // Save to database without removing from cache
                    for (PlayerQuestProgress progress : progressList) {
                        try {
                            repository.createAsync(progress).join();
                        } catch (OptimisticLockException e) {
                            // Log but don't fail - this can happen if progress was updated elsewhere
                            LOGGER.fine("Optimistic lock exception for quest " + 
                                progress.getQuest().getIdentifier() + " for player " + playerId + 
                                " - entity was updated elsewhere");
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Auto-save failed for quest " + 
                                progress.getQuest().getIdentifier() + " for player " + playerId, e);
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
        
        LOGGER.info(String.format("Quest progress auto-save completed: %d players saved, %d errors in %dms",
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
        stats.put("total_quests", cache.values().stream()
            .mapToInt(List::size)
            .sum());
        return stats;
    }
    
    /**
     * Clears all cache data. Use with caution!
     * This will lose any unsaved changes.
     */
    public void clearAll() {
        LOGGER.warning("Clearing all quest progress cache data. Unsaved changes will be lost!");
        cache.clear();
        dirtyPlayers.clear();
    }
}
