package com.raindropcentral.rdq.cache.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache manager for player quest data.
 * <p>
 * This cache loads all active quests for a player on join, stores them in memory,
 * and saves them back to the database on leave. This provides instant access to
 * quest data without database queries.
 * </p>
 * <h1>Design Philosophy</h1>
 * <ul>
 *   <li>Load all active quests on player join</li>
 *   <li>Update progress instantly in memory</li>
 *   <li>Save all changes on player leave</li>
 *   <li>Auto-save periodically for crash protection (every 5 minutes)</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCacheManager {
    
    private static final Logger LOGGER = Logger.getLogger(QuestCacheManager.class.getName());
    
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20L * 60L * 5L; // 5 minutes
    
    private final RDQ plugin;
    private final QuestUserRepository repository;
    
    /**
     * Cache of player quest data: UUID -> List of QuestUser
     */
    private final ConcurrentHashMap<UUID, List<QuestUser>> cache;
    
    /**
     * Set of players with unsaved changes
     */
    private final Set<UUID> dirtyPlayers;
    
    /**
     * Auto-save task
     */
    private BukkitTask autoSaveTask;
    
    /**
     * Whether to log performance metrics
     */
    private final boolean logPerformance;
    
    /**
     * Constructs a new quest cache manager.
     *
     * @param plugin         the RDQ plugin instance
     * @param logPerformance whether to log performance metrics
     */
    public QuestCacheManager(
            @NotNull final RDQ plugin,
            final boolean logPerformance
    ) {
        this.plugin = plugin;
        this.repository = plugin.getQuestUserRepository();
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
        this.logPerformance = logPerformance;
    }
    
    /**
     * Starts the auto-save task.
     */
    public void start() {
        if (autoSaveTask != null) {
            LOGGER.warning("Quest cache auto-save task is already running");
            return;
        }
        
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin.getPlugin(),
                this::autoSaveAll,
                AUTO_SAVE_INTERVAL_TICKS,
                AUTO_SAVE_INTERVAL_TICKS
        );
        
        LOGGER.info("Quest cache auto-save task started (interval: 5 minutes)");
    }
    
    /**
     * Stops the auto-save task and saves all pending changes.
     *
     * @return a future completing when shutdown is complete
     */
    @NotNull
    public CompletableFuture<Void> shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        
        return saveAllPlayers()
                .thenRun(() -> LOGGER.info("Quest cache shutdown complete"));
    }
    
    /**
     * Loads all active quests for a player from the database into memory.
     * <p>
     * Thread Safety: The loaded list is wrapped in a synchronized list
     * to prevent ConcurrentModificationException when quests are modified
     * while auto-save is running.
     * </p>
     *
     * @param playerId the player's UUID
     * @return a future completing when loading is done
     */
    @NotNull
    public CompletableFuture<Void> loadPlayerAsync(@NotNull final UUID playerId) {
        final long startTime = logPerformance ? System.currentTimeMillis() : 0;
        
        return repository.findActiveByPlayer(playerId)
                .thenAccept(questUsers -> {
                    // Wrap in synchronized list for thread safety
                    final List<QuestUser> synchronizedList = 
                            Collections.synchronizedList(new ArrayList<>(questUsers));
                    cache.put(playerId, synchronizedList);
                    
                    if (logPerformance) {
                        final long duration = System.currentTimeMillis() - startTime;
                        LOGGER.info(String.format("Loaded %d quests for player %s in %dms",
                                questUsers.size(), playerId, duration));
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to load quests for player " + playerId, ex);
                    return null;
                });
    }
    
    /**
     * Loads all active quests for a player synchronously (blocking).
     * <p>
     * Use loadPlayerAsync() for better performance.
     * </p>
     *
     * @param playerId the player's UUID
     */
    public void loadPlayer(@NotNull final UUID playerId) {
        try {
            loadPlayerAsync(playerId).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quests for player " + playerId, e);
        }
    }
    
    /**
     * Gets all active quests for a player from cache.
     * <p>
     * Returns a copy of the list to prevent external modification.
     * To modify quest data, use the returned objects directly - they are
     * references to the cached entities, not copies.
     * </p>
     *
     * @param playerId the player's UUID
     * @return the list of active quests, or empty list if not loaded
     */
    @NotNull
    public List<QuestUser> getPlayerQuests(@NotNull final UUID playerId) {
        final List<QuestUser> quests = cache.get(playerId);
        return quests != null ? new ArrayList<>(quests) : List.of();
    }
    
    /**
     * Gets all active quests for a player from cache (direct reference).
     * <p>
     * WARNING: This returns the actual cached list. Modifications to the list
     * structure (add/remove) must be synchronized. Use this when you need to
     * modify quest data in place.
     * </p>
     *
     * @param playerId the player's UUID
     * @return the cached list of active quests, or empty list if not loaded
     */
    @NotNull
    public List<QuestUser> getPlayerQuestsDirectReference(@NotNull final UUID playerId) {
        final List<QuestUser> quests = cache.get(playerId);
        return quests != null ? quests : List.of();
    }
    
    /**
     * Gets a specific active quest for a player from cache.
     *
     * @param playerId        the player's UUID
     * @param questIdentifier the quest identifier
     * @return the quest user if found
     */
    @NotNull
    public Optional<QuestUser> getPlayerQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        final List<QuestUser> quests = cache.get(playerId);
        if (quests == null) {
            return Optional.empty();
        }
        
        synchronized (quests) {
            return quests.stream()
                    .filter(qu -> qu.getQuest().getIdentifier().equalsIgnoreCase(questIdentifier))
                    .findFirst();
        }
    }
    
    /**
     * Adds a quest to the player's cache.
     *
     * @param playerId  the player's UUID
     * @param questUser the quest user to add
     */
    public void addPlayerQuest(@NotNull final UUID playerId, @NotNull final QuestUser questUser) {
        final List<QuestUser> quests = cache.computeIfAbsent(
                playerId,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
        
        synchronized (quests) {
            quests.add(questUser);
        }
        
        markDirty(playerId);
    }
    
    /**
     * Removes a quest from the player's cache.
     *
     * @param playerId        the player's UUID
     * @param questIdentifier the quest identifier
     */
    public void removePlayerQuest(@NotNull final UUID playerId, @NotNull final String questIdentifier) {
        final List<QuestUser> quests = cache.get(playerId);
        if (quests == null) {
            return;
        }
        
        synchronized (quests) {
            quests.removeIf(qu -> qu.getQuest().getIdentifier().equalsIgnoreCase(questIdentifier));
        }
        
        markDirty(playerId);
    }
    
    /**
     * Marks a player as having unsaved changes.
     *
     * @param playerId the player's UUID
     */
    public void markDirty(@NotNull final UUID playerId) {
        dirtyPlayers.add(playerId);
    }
    
    /**
     * Checks if a player has unsaved changes.
     *
     * @param playerId the player's UUID
     * @return true if the player has unsaved changes
     */
    public boolean isDirty(@NotNull final UUID playerId) {
        return dirtyPlayers.contains(playerId);
    }
    
    /**
     * Checks if a player's quest data is loaded in cache.
     *
     * @param playerId the player's UUID
     * @return true if the player's data is loaded
     */
    public boolean isLoaded(@NotNull final UUID playerId) {
        return cache.containsKey(playerId);
    }
    
    /**
     * Saves a player's quest data to the database and removes from cache.
     *
     * @param playerId the player's UUID
     * @return a future completing when save is done
     */
    @NotNull
    public CompletableFuture<Void> savePlayer(@NotNull final UUID playerId) {
        if (!dirtyPlayers.contains(playerId)) {
            // No changes, just remove from cache
            cache.remove(playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        final List<QuestUser> quests = cache.get(playerId);
        if (quests == null || quests.isEmpty()) {
            dirtyPlayers.remove(playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        final long startTime = logPerformance ? System.currentTimeMillis() : 0;
        
        // Save all quests — use updateAsync since these are existing entities
        // Skip completed quests as they are deleted by the completion process
        final List<CompletableFuture<QuestUser>> saveFutures = new ArrayList<>();
        
        synchronized (quests) {
            for (final QuestUser questUser : quests) {
                // Skip completed quests - they are handled by the completion process
                if (!questUser.isCompleted()) {
                    saveFutures.add(repository.updateAsync(questUser));
                }
            }
        }
        
        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    dirtyPlayers.remove(playerId);
                    cache.remove(playerId);
                    
                    if (logPerformance) {
                        final long duration = System.currentTimeMillis() - startTime;
                        LOGGER.info(String.format("Saved %d quests for player %s in %dms",
                                quests.size(), playerId, duration));
                    }
                })
                .exceptionally(ex -> {
                    // Check if it's an OptimisticLockException (entity was modified elsewhere)
                    if (ex.getCause() instanceof jakarta.persistence.OptimisticLockException) {
                        LOGGER.fine("Quest entity was modified elsewhere for player " + playerId + 
                                " (likely completed) - skipping save");
                        dirtyPlayers.remove(playerId);
                        cache.remove(playerId);
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to save quests for player " + playerId, ex);
                    }
                    return null;
                });
    }
    
    /**
     * Auto-saves all players with unsaved changes.
     * <p>
     * This is called periodically for crash protection.
     * </p>
     *
     * @return the number of players saved
     */
    public int autoSaveAll() {
        if (dirtyPlayers.isEmpty()) {
            return 0;
        }
        
        final Set<UUID> playersToSave = new HashSet<>(dirtyPlayers);
        int savedCount = 0;
        
        for (final UUID playerId : playersToSave) {
            final List<QuestUser> quests = cache.get(playerId);
            if (quests == null || quests.isEmpty()) {
                dirtyPlayers.remove(playerId);
                continue;
            }
            
            try {
                synchronized (quests) {
                    for (final QuestUser questUser : quests) {
                        // Skip completed quests - they are handled by the completion process
                        if (!questUser.isCompleted()) {
                            repository.updateAsync(questUser).join();
                        }
                    }
                }
                
                dirtyPlayers.remove(playerId);
                savedCount++;
                
            } catch (jakarta.persistence.OptimisticLockException e) {
                // Entity was modified elsewhere (likely completed) - skip
                LOGGER.fine("Quest entity was modified elsewhere for player " + playerId + 
                        " during auto-save - skipping");
                dirtyPlayers.remove(playerId);
            } catch (Exception e) {
                // Check if the cause is OptimisticLockException
                if (e.getCause() instanceof jakarta.persistence.OptimisticLockException) {
                    LOGGER.fine("Quest entity was modified elsewhere for player " + playerId + 
                            " during auto-save - skipping");
                    dirtyPlayers.remove(playerId);
                } else {
                    LOGGER.log(Level.WARNING, "Auto-save failed for player " + playerId, e);
                }
            }
        }
        
        if (savedCount > 0) {
            LOGGER.info("Auto-saved quest data for " + savedCount + " players");
        }
        
        return savedCount;
    }
    
    /**
     * Saves all players' quest data.
     *
     * @return a future completing when all saves are done
     */
    @NotNull
    public CompletableFuture<Void> saveAllPlayers() {
        final List<CompletableFuture<Void>> saveFutures = new ArrayList<>();
        
        for (final UUID playerId : cache.keySet()) {
            saveFutures.add(savePlayer(playerId));
        }
        
        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> LOGGER.info("Saved quest data for all players"));
    }
    
    /**
     * Gets the number of players currently in cache.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Gets the number of players with unsaved changes.
     *
     * @return the dirty player count
     */
    public int getDirtyCount() {
        return dirtyPlayers.size();
    }
    
    /**
     * Invalidates all cached data for a player.
     *
     * @param playerId the player's UUID
     */
    public void invalidate(@NotNull final UUID playerId) {
        cache.remove(playerId);
        dirtyPlayers.remove(playerId);
    }
    
    /**
     * Clears all cached data.
     */
    public void invalidateAll() {
        cache.clear();
        dirtyPlayers.clear();
    }
}

