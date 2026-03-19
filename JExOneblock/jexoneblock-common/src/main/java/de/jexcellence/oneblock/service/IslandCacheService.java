package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IslandCacheService {
    
    private static final Logger LOGGER = Logger.getLogger(IslandCacheService.class.getName());
    
    private final OneblockIslandRepository islandRepository;
    private final ConcurrentHashMap<UUID, OneblockIsland> playerIslandCache;
    private final ConcurrentHashMap<UUID, Boolean> dirtyFlags;
    private final ScheduledExecutorService scheduler;
    
    private final ConcurrentHashMap<UUID, Object> saveLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> savingFlags = new ConcurrentHashMap<>();
    
    private final long periodicSaveInterval;
    private final TimeUnit periodicSaveUnit;
    
    public IslandCacheService(@NotNull OneblockIslandRepository islandRepository) {
        this.islandRepository = islandRepository;
        this.playerIslandCache = new ConcurrentHashMap<>();
        this.dirtyFlags = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        this.periodicSaveInterval = 5;
        this.periodicSaveUnit = TimeUnit.MINUTES;
        
        startPeriodicSave();
    }
    
    @NotNull
    public CompletableFuture<Void> loadPlayerIsland(@NotNull UUID playerUuid) {
        return islandRepository.findByOwnerAsync(playerUuid)
            .thenAccept(islandOpt -> {
                if (islandOpt.isPresent()) {
                    OneblockIsland island = islandOpt.get();
                    playerIslandCache.put(playerUuid, island);
                    dirtyFlags.put(playerUuid, false);
                    LOGGER.log(Level.FINE, "Loaded island for player " + playerUuid + " into cache");
                } else {
                    LOGGER.log(Level.FINE, "No island found for player " + playerUuid);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Failed to load island for player " + playerUuid, throwable);
                return null;
            });
    }
    
    @NotNull
    public CompletableFuture<Void> saveAndUnloadPlayerIsland(@NotNull UUID playerUuid) {
        OneblockIsland island = playerIslandCache.get(playerUuid);
        if (island == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        Boolean isDirty = dirtyFlags.get(playerUuid);
        if (isDirty == null || !isDirty) {
            playerIslandCache.remove(playerUuid);
            dirtyFlags.remove(playerUuid);
            saveLocks.remove(playerUuid);
            LOGGER.log(Level.FINE, "Unloaded clean island for player " + playerUuid);
            return CompletableFuture.completedFuture(null);
        }
        
        return saveIslandWithRetry(island, playerUuid, 5)
            .thenRun(() -> {
                playerIslandCache.remove(playerUuid);
                dirtyFlags.remove(playerUuid);
                saveLocks.remove(playerUuid);
                LOGGER.log(Level.FINE, "Saved and unloaded island for player " + playerUuid);
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to save island for player " + playerUuid + " during logout", throwable);
                return null;
            });
    }
    
    @Nullable
    public OneblockIsland getCachedIsland(@NotNull UUID playerUuid) {
        return playerIslandCache.get(playerUuid);
    }
    
    public void markDirty(@NotNull UUID playerUuid) {
        if (playerIslandCache.containsKey(playerUuid)) {
            Boolean isSaving = savingFlags.get(playerUuid);
            if (isSaving == null || !isSaving) {
                dirtyFlags.put(playerUuid, true);
            }
        }
    }
    
    @NotNull
    public CompletableFuture<Void> forceSavePlayerIsland(@NotNull UUID playerUuid) {
        OneblockIsland island = playerIslandCache.get(playerUuid);
        if (island == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return saveIslandWithRetry(island, playerUuid, 5)
            .thenRun(() -> {
                dirtyFlags.put(playerUuid, false);
                LOGGER.log(Level.FINE, "Force saved island for player " + playerUuid);
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to force save island for player " + playerUuid, throwable);
                return null;
            });
    }
    
    public boolean isIslandCached(@NotNull UUID playerUuid) {
        return playerIslandCache.containsKey(playerUuid);
    }
    
    @NotNull
    public CompletableFuture<Void> saveAllDirtyIslands() {
        CompletableFuture<?>[] saveFutures = dirtyFlags.entrySet().stream()
            .filter(entry -> entry.getValue())
            .map(entry -> {
                UUID playerUuid = entry.getKey();
                OneblockIsland island = playerIslandCache.get(playerUuid);
                if (island != null) {
                    return saveIslandWithRetry(island, playerUuid, 3)
                        .thenRun(() -> {
                            dirtyFlags.put(playerUuid, false);
                            LOGGER.log(Level.FINE, "Periodic save completed for player " + playerUuid);
                        })
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to save island during periodic save for player " + playerUuid, throwable);
                            return null;
                        });
                }
                return CompletableFuture.completedFuture(null);
            })
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(saveFutures)
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Some islands failed to save during periodic save", throwable);
                return null;
            });
    }
    
    @NotNull
    private CompletableFuture<OneblockIsland> saveIslandWithRetry(@NotNull OneblockIsland island, @NotNull UUID playerUuid, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            Boolean isSaving = savingFlags.get(playerUuid);
            if (isSaving != null && isSaving) {
                LOGGER.fine("Island save already in progress for player " + playerUuid + ", skipping");
                return island;
            }
            
            Object lock = saveLocks.computeIfAbsent(playerUuid, k -> new Object());
            
            synchronized (lock) {
                savingFlags.put(playerUuid, true);
                
                try {
                    OneblockIsland currentIsland = island;
                    
                    synchronized (("island_save_" + currentIsland.getIdentifier()).intern()) {
                        for (int attempt = 1; attempt <= maxRetries; attempt++) {
                            try {
                                OneblockIsland cachedIsland = playerIslandCache.get(playerUuid);
                                if (cachedIsland != null && cachedIsland != currentIsland) {
                                    currentIsland = cachedIsland;
                                }
                                
                                OneblockIsland updated = islandRepository.update(currentIsland);
                                LOGGER.fine("Saved island: " + currentIsland.getIdentifier() + " for player " + playerUuid + " (attempt " + attempt + ")");
                                
                                playerIslandCache.put(playerUuid, updated);
                                
                                return updated;
                            } catch (Exception e) {
                                String errorMessage = e.getMessage();
                                boolean isOptimisticLock = errorMessage != null && 
                                    (errorMessage.contains("OptimisticLock") || 
                                     errorMessage.contains("Row was updated or deleted") ||
                                     errorMessage.contains("StaleObjectStateException"));
                                
                                if (isOptimisticLock && attempt < maxRetries) {
                                    LOGGER.warning("Optimistic lock conflict during save for player " + playerUuid + 
                                                  ", attempt " + attempt + "/" + maxRetries + ". Retrying with fresh entity...");

                                    try {
                                        Thread.sleep(100 * attempt * attempt);
                                    
                                    OneblockIsland freshIsland = islandRepository.findByIdentifier(currentIsland.getIdentifier());
                                    if (freshIsland != null) {
                                        mergeIslandChanges(freshIsland, currentIsland);
                                        currentIsland = freshIsland;
                                        playerIslandCache.put(playerUuid, freshIsland);
                                        LOGGER.info("Retrying save with fresh island entity (version: " + 
                                                   freshIsland.getVersion() + ", attempt " + (attempt + 1) + ")");
                                        continue;
                                    } else {
                                        LOGGER.warning("Could not fetch fresh island entity for retry");
                                    }
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("Save interrupted", ie);
                                } catch (Exception fetchEx) {
                                    LOGGER.warning("Failed to fetch fresh island entity for save retry: " + fetchEx.getMessage());
                                }
                            }
                            
                            if (attempt == maxRetries) {
                                LOGGER.severe("Failed to save island for player " + playerUuid + 
                                              " after " + maxRetries + " attempts: " + errorMessage);
                                throw new RuntimeException("Failed to save island after " + maxRetries + " attempts", e);
                            }
                        }
                    }
                    
                    return currentIsland;
                    }
                } finally {
                    savingFlags.remove(playerUuid);
                }
            }
        }, scheduler);
    }
    
    private void mergeIslandChanges(@NotNull OneblockIsland fresh, @NotNull OneblockIsland cached) {
        fresh.setExperience(cached.getExperience());
        fresh.setLastVisited(cached.getLastVisited());
        fresh.setIslandCoins(cached.getIslandCoins());
        fresh.setLevel(cached.getLevel());
        
        if (cached.getOneblock() != null && fresh.getOneblock() != null) {
            OneblockCore freshCore = fresh.getOneblock();
            OneblockCore cachedCore = cached.getOneblock();
            
            freshCore.setEvolutionExperience(cachedCore.getEvolutionExperience());
            freshCore.setBreakStreak(cachedCore.getBreakStreak());
            freshCore.setTotalBlocksBroken(cachedCore.getTotalBlocksBroken());
            freshCore.setLastBreakTimestamp(cachedCore.getLastBreakTimestamp());
            freshCore.setCurrentEvolution(cachedCore.getCurrentEvolution());
            freshCore.setEvolutionLevel(cachedCore.getEvolutionLevel());
            freshCore.setPrestigeLevel(cachedCore.getPrestigeLevel());
            freshCore.setPrestigePoints(cachedCore.getPrestigePoints());
        }
        
        LOGGER.fine("Merged changes from cached island (version " + cached.getVersion() + 
                   ") into fresh entity (version " + fresh.getVersion() + ")");
    }
    
    @NotNull
    public CompletableFuture<Void> saveAllCachedIslands() {
        LOGGER.log(Level.INFO, "Starting shutdown save for " + playerIslandCache.size() + " cached islands");
        
        CompletableFuture<?>[] saveFutures = playerIslandCache.entrySet().stream()
            .map(entry -> {
                UUID playerUuid = entry.getKey();
                OneblockIsland island = entry.getValue();
                Boolean isDirty = dirtyFlags.get(playerUuid);
                
                if (isDirty == null || isDirty) {
                    return saveIslandWithRetry(island, playerUuid, 2)
                        .thenRun(() -> {
                            LOGGER.log(Level.FINE, "Shutdown save completed for player " + playerUuid);
                        })
                        .exceptionally(throwable -> {
                            String errorMsg = throwable.getMessage();
                            if (errorMsg != null && (errorMsg.contains("OptimisticLock") || errorMsg.contains("Row was updated"))) {
                                LOGGER.log(Level.FINE, "Skipped concurrent save during shutdown for player " + playerUuid);
                            } else {
                                LOGGER.log(Level.WARNING, "Failed to save island during shutdown for player " + playerUuid + ": " + errorMsg);
                            }
                            return null;
                        });
                }
                return CompletableFuture.completedFuture(null);
            })
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(saveFutures)
            .thenRun(() -> {
                LOGGER.log(Level.INFO, "Shutdown save completed for all islands");
                shutdown();
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Some islands failed to save during shutdown (this is normal if concurrent saves occurred)");
                shutdown();
                return null;
            });
    }
    
    private void startPeriodicSave() {
        scheduler.scheduleAtFixedRate(
            () -> saveAllDirtyIslands().join(),
            periodicSaveInterval,
            periodicSaveInterval,
            periodicSaveUnit
        );
        LOGGER.log(Level.INFO, "Started periodic island save every " + periodicSaveInterval + " " + periodicSaveUnit.name().toLowerCase());
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        saveLocks.clear();
        savingFlags.clear();
        
        LOGGER.log(Level.INFO, "Island cache service shut down");
    }
    
    @NotNull
    public CacheStats getCacheStats() {
        int totalCached = playerIslandCache.size();
        int dirtyCount = (int) dirtyFlags.values().stream().mapToInt(dirty -> dirty ? 1 : 0).sum();
        return new CacheStats(totalCached, dirtyCount);
    }
    
    public static class CacheStats {
        private final int totalCached;
        private final int dirtyCount;
        
        public CacheStats(int totalCached, int dirtyCount) {
            this.totalCached = totalCached;
            this.dirtyCount = dirtyCount;
        }
        
        public int getTotalCached() { return totalCached; }
        public int getDirtyCount() { return dirtyCount; }
        public int getCleanCount() { return totalCached - dirtyCount; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, dirty=%d, clean=%d}", 
                totalCached, dirtyCount, getCleanCount());
        }
    }
}