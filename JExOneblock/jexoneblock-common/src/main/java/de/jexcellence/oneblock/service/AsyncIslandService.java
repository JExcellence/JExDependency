package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.async.AsyncOperationManager;
import de.jexcellence.oneblock.cache.CacheManager;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Async Island Service
 * 
 * Provides asynchronous operations for island management with intelligent
 * caching, performance monitoring, and optimized database operations.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class AsyncIslandService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final AsyncOperationManager asyncManager;
    private final OneblockIslandRepository islandRepository;
    
    // Simple operation counters
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // Caches
    private final CacheManager.Cache<UUID, OneblockIsland> ownerCache;
    private final CacheManager.Cache<Long, OneblockIsland> idCache;
    private final CacheManager.Cache<String, List<OneblockIsland>> queryCache;
    
    // Cache names
    private static final String OWNER_CACHE_NAME = "async_island_by_owner";
    private static final String ID_CACHE_NAME = "async_island_by_id";
    private static final String QUERY_CACHE_NAME = "async_island_queries";
    
    public AsyncIslandService(@NotNull OneblockIslandRepository islandRepository) {
        this.islandRepository = islandRepository;
        this.asyncManager = AsyncOperationManager.getInstance();
        
        CacheManager cacheManager = CacheManager.getInstance();
        
        // Configure caches for async operations
        CacheManager.CacheConfig config = CacheManager.CacheConfig.builder()
            .maxSize(2000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .evictionPolicy(CacheManager.CacheConfig.EvictionPolicy.LRU)
            .recordStats(true)
            .build();
        
        this.ownerCache = cacheManager.getOrCreateCache(OWNER_CACHE_NAME, config);
        this.idCache = cacheManager.getOrCreateCache(ID_CACHE_NAME, config);
        
        // Query cache with shorter expiration
        CacheManager.CacheConfig queryConfig = CacheManager.CacheConfig.builder()
            .maxSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .evictionPolicy(CacheManager.CacheConfig.EvictionPolicy.LFU)
            .recordStats(true)
            .build();
        
        this.queryCache = cacheManager.getOrCreateCache(QUERY_CACHE_NAME, queryConfig);
        
        LOGGER.info("AsyncIslandService initialized with optimized caching");
    }
    
    /**
     * Increments a counter
     */
    private void incrementCounter(@NotNull String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Adds to a counter
     */
    private void addToCounter(@NotNull String name, long value) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * Loads an island by owner UUID asynchronously
     * 
     * @param ownerUuid the owner's UUID
     * @return CompletableFuture with the island or null if not found
     */
    @NotNull
    public CompletableFuture<OneblockIsland> loadIslandByOwnerAsync(@NotNull UUID ownerUuid) {
        // Check cache first
        OneblockIsland cached = ownerCache.get(ownerUuid);
        if (cached != null) {
            incrementCounter("island_cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        incrementCounter("island_cache_misses");
        
        return asyncManager.executeDatabase(() -> {
            try {
                OneblockIsland island = islandRepository.findByOwner(ownerUuid);
                if (island != null) {
                    cacheIsland(island);
                }
                return island;
            } catch (Exception e) {
                LOGGER.warning("Failed to load island for owner " + ownerUuid + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Loads an island by ID asynchronously
     * 
     * @param islandId the island ID
     * @return CompletableFuture with the island or null if not found
     */
    @NotNull
    public CompletableFuture<OneblockIsland> loadIslandByIdAsync(@NotNull Long islandId) {
        // Check cache first
        OneblockIsland cached = idCache.get(islandId);
        if (cached != null) {
            incrementCounter("island_cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        incrementCounter("island_cache_misses");
        
        return asyncManager.executeDatabase(() -> {
            try {
                OneblockIsland island = islandRepository.findById(islandId).orElse(null);
                if (island != null) {
                    cacheIsland(island);
                }
                return island;
            } catch (Exception e) {
                LOGGER.warning("Failed to load island by ID " + islandId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Saves an island asynchronously
     * 
     * @param island the island to save
     * @return CompletableFuture with the saved island
     */
    @NotNull
    public CompletableFuture<OneblockIsland> saveIslandAsync(@NotNull OneblockIsland island) {
        return asyncManager.executeDatabase(() -> {
            try {
                OneblockIsland saved = islandRepository.save(island);
                cacheIsland(saved);
                incrementCounter("island_saves");
                return saved;
            } catch (Exception e) {
                LOGGER.warning("Failed to save island " + island.getId() + ": " + e.getMessage());
                incrementCounter("island_save_errors");
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Updates an island asynchronously
     * 
     * @param island the island to update
     * @return CompletableFuture with the updated island
     */
    @NotNull
    public CompletableFuture<OneblockIsland> updateIslandAsync(@NotNull OneblockIsland island) {
        return asyncManager.executeDatabase(() -> {
            try {
                OneblockIsland updated = islandRepository.update(island);
                cacheIsland(updated);
                incrementCounter("island_updates");
                return updated;
            } catch (Exception e) {
                LOGGER.warning("Failed to update island " + island.getId() + ": " + e.getMessage());
                incrementCounter("island_update_errors");
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Deletes an island asynchronously
     * 
     * @param islandId the island ID to delete
     * @return CompletableFuture indicating completion
     */
    @NotNull
    public CompletableFuture<Void> deleteIslandAsync(@NotNull Long islandId) {
        return asyncManager.executeDatabase(() -> {
            try {
                // Remove from cache first
                OneblockIsland cached = idCache.get(islandId);
                if (cached != null) {
                    removeCachedIsland(cached);
                }
                
                islandRepository.delete(islandId);
                incrementCounter("island_deletions");
                return null;
            } catch (Exception e) {
                LOGGER.warning("Failed to delete island " + islandId + ": " + e.getMessage());
                incrementCounter("island_deletion_errors");
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Loads multiple islands by owner UUIDs asynchronously
     * 
     * @param ownerUuids list of owner UUIDs
     * @return CompletableFuture with list of islands
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> loadIslandsByOwnersAsync(@NotNull List<UUID> ownerUuids) {
        // Check cache for each UUID
        List<UUID> uncachedUuids = ownerUuids.stream()
            .filter(uuid -> !ownerCache.containsKey(uuid))
            .collect(Collectors.toList());
        
        if (uncachedUuids.isEmpty()) {
            // All islands are cached
            List<OneblockIsland> cachedIslands = ownerUuids.stream()
                .map(ownerCache::get)
                .filter(island -> island != null)
                .collect(Collectors.toList());
            
            incrementCounter("island_cache_hits");
            return CompletableFuture.completedFuture(cachedIslands);
        }
        
        return asyncManager.executeDatabase(() -> {
            try {
                List<OneblockIsland> islands = islandRepository.findByMember(uncachedUuids.get(0));
                
                // Cache loaded islands
                islands.forEach(this::cacheIsland);
                
                // Combine with cached islands
                List<OneblockIsland> allIslands = ownerUuids.stream()
                    .map(ownerCache::get)
                    .filter(island -> island != null)
                    .collect(Collectors.toList());
                
                addToCounter("island_batch_loads", islands.size());
                return allIslands;
            } catch (Exception e) {
                LOGGER.warning("Failed to load islands by owners: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Searches islands by criteria asynchronously
     * 
     * @param searchCriteria search criteria
     * @param limit maximum number of results
     * @return CompletableFuture with list of matching islands
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> searchIslandsAsync(@NotNull String searchCriteria, int limit) {
        String cacheKey = "search_" + searchCriteria + "_" + limit;
        
        // Check query cache
        List<OneblockIsland> cached = queryCache.get(cacheKey);
        if (cached != null) {
            incrementCounter("island_search_cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        return asyncManager.executeDatabase(() -> {
            try {
                // Simple search implementation - in a real scenario you'd have a proper search method
                List<OneblockIsland> results = islandRepository.findAll().stream()
                    .filter(island -> island.getIdentifier().contains(searchCriteria) || 
                                    (island.getOwner() != null && island.getOwner().getUniqueId().toString().contains(searchCriteria)))
                    .limit(limit)
                    .collect(Collectors.toList());
                
                // Cache results
                queryCache.put(cacheKey, results);
                
                // Also cache individual islands
                results.forEach(this::cacheIsland);
                
                incrementCounter("island_searches");
                return results;
            } catch (Exception e) {
                LOGGER.warning("Failed to search islands: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Gets top islands by level asynchronously
     * 
     * @param limit maximum number of results
     * @return CompletableFuture with list of top islands
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> getTopIslandsByLevelAsync(int limit) {
        String cacheKey = "top_by_level_" + limit;
        
        // Check query cache
        List<OneblockIsland> cached = queryCache.get(cacheKey);
        if (cached != null) {
            incrementCounter("island_leaderboard_cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        return asyncManager.executeDatabase(() -> {
            try {
                // Simple top islands implementation - in a real scenario you'd have a proper method
                List<OneblockIsland> results = islandRepository.findAll().stream()
                    .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                    .limit(limit)
                    .collect(Collectors.toList());
                
                // Cache results with shorter expiration for leaderboards
                queryCache.put(cacheKey, results);
                
                // Also cache individual islands
                results.forEach(this::cacheIsland);
                
                incrementCounter("island_leaderboard_queries");
                return results;
            } catch (Exception e) {
                LOGGER.warning("Failed to get top islands by level: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Preloads islands for a list of players (useful for events)
     * 
     * @param playerUuids list of player UUIDs
     * @param onProgress callback for progress updates
     * @return CompletableFuture indicating completion
     */
    @NotNull
    public CompletableFuture<Void> preloadIslandsAsync(@NotNull List<UUID> playerUuids, 
                                                     @Nullable Consumer<Integer> onProgress) {
        return asyncManager.executeNamed("island_preload", () -> {
            int total = playerUuids.size();
            int processed = 0;
            
            for (UUID playerUuid : playerUuids) {
                if (!ownerCache.containsKey(playerUuid)) {
                    try {
                        OneblockIsland island = islandRepository.findByOwner(playerUuid);
                        if (island != null) {
                            cacheIsland(island);
                        }
                    } catch (Exception e) {
                        LOGGER.fine("Failed to preload island for " + playerUuid + ": " + e.getMessage());
                    }
                }
                
                processed++;
                if (onProgress != null && processed % 10 == 0) {
                    onProgress.accept(processed);
                }
            }
            
            addToCounter("island_preloads", processed);
            LOGGER.info("Preloaded " + processed + "/" + total + " islands");
            return null;
        }).thenRun(() -> {
            if (onProgress != null) {
                onProgress.accept(playerUuids.size());
            }
        });
    }
    
    /**
     * Batch saves multiple islands asynchronously
     * 
     * @param islands list of islands to save
     * @return CompletableFuture with list of saved islands
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> batchSaveIslandsAsync(@NotNull List<OneblockIsland> islands) {
        return asyncManager.executeDatabase(() -> {
            try {
                // Save all islands individually since there's no batch save method
                List<OneblockIsland> saved = new ArrayList<>();
                for (OneblockIsland island : islands) {
                    saved.add(islandRepository.save(island));
                }
                
                // Cache all saved islands
                saved.forEach(this::cacheIsland);
                
                addToCounter("island_batch_saves", saved.size());
                return saved;
            } catch (Exception e) {
                LOGGER.warning("Failed to batch save islands: " + e.getMessage());
                incrementCounter("island_batch_save_errors");
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Invalidates cache for a specific island
     * 
     * @param islandId the island ID
     */
    public void invalidateIslandCache(@NotNull Long islandId) {
        OneblockIsland cached = idCache.get(islandId);
        if (cached != null) {
            removeCachedIsland(cached);
        }
        
        // Also clear query cache as it might contain this island
        queryCache.clear();
    }
    
    /**
     * Invalidates cache for a specific owner
     * 
     * @param ownerUuid the owner UUID
     */
    public void invalidateOwnerCache(@NotNull UUID ownerUuid) {
        OneblockIsland cached = ownerCache.get(ownerUuid);
        if (cached != null) {
            removeCachedIsland(cached);
        }
    }
    
    /**
     * Gets cache statistics
     * 
     * @return cache statistics
     */
    @NotNull
    public AsyncCacheStats getCacheStats() {
        return new AsyncCacheStats(
            ownerCache.getStatistics(),
            idCache.getStatistics(),
            queryCache.getStatistics()
        );
    }
    
    /**
     * Performs cache cleanup
     */
    public void performCacheCleanup() {
        int ownerCleaned = ownerCache.cleanup();
        int idCleaned = idCache.cleanup();
        int queryCleaned = queryCache.cleanup();
        
        int totalCleaned = ownerCleaned + idCleaned + queryCleaned;
        if (totalCleaned > 0) {
            LOGGER.fine("Async island cache cleanup: removed " + totalCleaned + " expired entries");
        }
    }
    
    /**
     * Caches an island in both owner and ID caches
     */
    private void cacheIsland(@NotNull OneblockIsland island) {
        ownerCache.put(island.getOwnerUuid(), island);
        idCache.put(island.getId(), island);
    }
    
    /**
     * Removes an island from all caches
     */
    private void removeCachedIsland(@NotNull OneblockIsland island) {
        ownerCache.remove(island.getOwnerUuid());
        idCache.remove(island.getId());
        
        // Clear query cache as it might contain this island
        queryCache.clear();
    }
    
    /**
     * Async cache statistics
     */
    public static class AsyncCacheStats {
        private final CacheManager.CacheStatistics ownerStats;
        private final CacheManager.CacheStatistics idStats;
        private final CacheManager.CacheStatistics queryStats;
        
        public AsyncCacheStats(CacheManager.CacheStatistics ownerStats,
                             CacheManager.CacheStatistics idStats,
                             CacheManager.CacheStatistics queryStats) {
            this.ownerStats = ownerStats;
            this.idStats = idStats;
            this.queryStats = queryStats;
        }
        
        public CacheManager.CacheStatistics getOwnerStats() { return ownerStats; }
        public CacheManager.CacheStatistics getIdStats() { return idStats; }
        public CacheManager.CacheStatistics getQueryStats() { return queryStats; }
        
        public double getOverallHitRate() {
            long totalHits = ownerStats.getHitCount() + idStats.getHitCount() + queryStats.getHitCount();
            long totalRequests = totalHits + ownerStats.getMissCount() + idStats.getMissCount() + queryStats.getMissCount();
            
            return totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
        }
        
        public int getTotalCacheSize() {
            return ownerStats.getCurrentSize() + idStats.getCurrentSize() + queryStats.getCurrentSize();
        }
        
        @Override
        public String toString() {
            return String.format("AsyncIslandCacheStats{overallHitRate=%.2f%%, totalSize=%d, owner=%s, id=%s, query=%s}",
                getOverallHitRate() * 100, getTotalCacheSize(), ownerStats, idStats, queryStats);
        }
    }
}