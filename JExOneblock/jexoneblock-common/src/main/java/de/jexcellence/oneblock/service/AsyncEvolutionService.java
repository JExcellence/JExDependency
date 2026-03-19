package de.jexcellence.oneblock.service;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.oneblock.async.AsyncOperationManager;
import de.jexcellence.oneblock.cache.CacheManager;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Async Evolution Service
 * 
 * Provides asynchronous evolution processing with intelligent caching,
 * batch operations, and performance optimization for OneBlock evolution system.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class AsyncEvolutionService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final AsyncOperationManager asyncManager;
    private final DynamicEvolutionService evolutionService;
    private final AsyncIslandService islandService;
    
    // Simple operation counters
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // Evolution-specific caches
    private final CacheManager.Cache<String, EvolutionData> evolutionCache;
    private final CacheManager.Cache<UUID, EvolutionProgress> progressCache;
    private final CacheManager.Cache<String, List<EvolutionRequirement>> requirementCache;
    
    // Cache names
    private static final String EVOLUTION_CACHE_NAME = "async_evolution_data";
    private static final String PROGRESS_CACHE_NAME = "async_evolution_progress";
    private static final String REQUIREMENT_CACHE_NAME = "async_evolution_requirements";
    
    public AsyncEvolutionService(@NotNull DynamicEvolutionService evolutionService,
                               @NotNull AsyncIslandService islandService) {
        this.evolutionService = evolutionService;
        this.islandService = islandService;
        this.asyncManager = AsyncOperationManager.getInstance();
        
        CacheManager cacheManager = CacheManager.getInstance();
        
        // Evolution data cache - longer expiration as evolution data changes less frequently
        CacheManager.CacheConfig evolutionConfig = CacheManager.CacheConfig.builder()
            .maxSize(1000)
            .expireAfterWrite(4, TimeUnit.HOURS)
            .expireAfterAccess(2, TimeUnit.HOURS)
            .evictionPolicy(CacheManager.CacheConfig.EvictionPolicy.LFU)
            .recordStats(true)
            .build();
        
        // Progress cache - shorter expiration as progress changes frequently
        CacheManager.CacheConfig progressConfig = CacheManager.CacheConfig.builder()
            .maxSize(2000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .evictionPolicy(CacheManager.CacheConfig.EvictionPolicy.LRU)
            .recordStats(true)
            .build();
        
        // Requirement cache - medium expiration
        CacheManager.CacheConfig requirementConfig = CacheManager.CacheConfig.builder()
            .maxSize(500)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .evictionPolicy(CacheManager.CacheConfig.EvictionPolicy.LFU)
            .recordStats(true)
            .build();
        
        this.evolutionCache = cacheManager.getOrCreateCache(EVOLUTION_CACHE_NAME, evolutionConfig);
        this.progressCache = cacheManager.getOrCreateCache(PROGRESS_CACHE_NAME, progressConfig);
        this.requirementCache = cacheManager.getOrCreateCache(REQUIREMENT_CACHE_NAME, requirementConfig);
        
        LOGGER.info("AsyncEvolutionService initialized with optimized caching");
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
     * Processes evolution advancement asynchronously
     * 
     * @param playerUuid the player's UUID
     * @param experienceGained experience gained from block breaking
     * @return CompletableFuture with evolution result
     */
    @NotNull
    public CompletableFuture<EvolutionResult> processEvolutionAsync(@NotNull UUID playerUuid, int experienceGained) {
        return asyncManager.executeComputation(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Load island data
                OneblockIsland island = islandService.loadIslandByOwnerAsync(playerUuid).join();
                if (island == null) {
                    return new EvolutionResult(false, "Island not found", null, null);
                }
                
                // Get current progress
                EvolutionProgress progress = getEvolutionProgress(playerUuid, island);
                
                // Calculate new experience
                double newExperience = progress.currentExperience() + experienceGained;
                
                // Check for evolution
                EvolutionData currentEvolution = getEvolutionData(progress.currentEvolution());
                if (currentEvolution == null) {
                    return new EvolutionResult(false, "Evolution data not found", progress, null);
                }
                
                boolean evolved = false;
                EvolutionData nextEvolution = null;
                
                if (newExperience >= currentEvolution.requiredExperience()) {
                    // Check if next evolution is available
                    nextEvolution = getEvolutionData(currentEvolution.nextEvolution());
                    if (nextEvolution != null) {
                        // Validate requirements
                        if (validateEvolutionRequirements(playerUuid, nextEvolution).join()) {
                            evolved = true;
                            
                            // Update progress
                            progress = new EvolutionProgress(
                                playerUuid,
                                nextEvolution.evolutionName(),
                                0, // Reset experience for new evolution
                                progress.evolutionLevel() + 1,
                                System.currentTimeMillis()
                            );
                            
                            // Cache updated progress
                            progressCache.put(playerUuid, progress);
                            
                            incrementCounter("evolutions_completed");
                        } else {
                            // Requirements not met, cap experience
                            newExperience = currentEvolution.requiredExperience();
                        }
                    } else {
                        // No next evolution, cap experience
                        newExperience = currentEvolution.requiredExperience();
                    }
                }
                
                if (!evolved) {
                    // Update experience without evolution
                    progress = new EvolutionProgress(
                        progress.playerUuid(),
                        progress.currentEvolution(),
                        newExperience,
                        progress.evolutionLevel(),
                        System.currentTimeMillis()
                    );
                    
                    progressCache.put(playerUuid, progress);
                }
                
                incrementCounter("evolution_processing_completed");
                return new EvolutionResult(evolved, evolved ? "Evolution completed!" : "Experience gained", 
                    progress, nextEvolution);
                
            } catch (Exception e) {
                incrementCounter("evolution_processing_failed");
                LOGGER.warning("Failed to process evolution for " + playerUuid + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Validates evolution requirements asynchronously
     * 
     * @param playerUuid the player's UUID
     * @param evolution the evolution to validate
     * @return CompletableFuture with validation result
     */
    @NotNull
    public CompletableFuture<Boolean> validateEvolutionRequirements(@NotNull UUID playerUuid, 
                                                                  @NotNull EvolutionData evolution) {
        return asyncManager.executeComputation(() -> {
            try {
                List<EvolutionRequirement> requirements = getEvolutionRequirements(evolution.evolutionName());
                
                for (EvolutionRequirement requirement : requirements) {
                    if (!requirement.isMet(playerUuid)) {
                        return false;
                    }
                }
                
                return true;
            } catch (Exception e) {
                LOGGER.warning("Failed to validate evolution requirements for " + playerUuid + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Batch processes evolution for multiple players
     * 
     * @param evolutionUpdates list of evolution updates
     * @param onProgress callback for progress updates
     * @return CompletableFuture with list of results
     */
    @NotNull
    public CompletableFuture<List<EvolutionResult>> batchProcessEvolutionAsync(
            @NotNull List<EvolutionUpdate> evolutionUpdates,
            @Nullable Consumer<Integer> onProgress) {
        
        return asyncManager.executeNamed("batch_evolution_processing", () -> {
            List<EvolutionResult> results = new java.util.ArrayList<>();
            int processed = 0;
            
            for (EvolutionUpdate update : evolutionUpdates) {
                try {
                    EvolutionResult result = processEvolutionAsync(
                        update.playerUuid(),
                        update.experienceGained()
                    ).join();
                    
                    results.add(result);
                } catch (Exception e) {
                    LOGGER.warning("Failed to process evolution for " + update.playerUuid() + ": " + e.getMessage());
                    results.add(new EvolutionResult(false, "Processing failed: " + e.getMessage(), null, null));
                }
                
                processed++;
                if (onProgress != null && processed % 10 == 0) {
                    onProgress.accept(processed);
                }
            }
            
            addToCounter("batch_evolution_processed", processed);
            return results;
        });
    }
    
    /**
     * Preloads evolution data for better performance
     * 
     * @param evolutionNames list of evolution names to preload
     * @return CompletableFuture indicating completion
     */
    @NotNull
    public CompletableFuture<Void> preloadEvolutionDataAsync(@NotNull List<String> evolutionNames) {
        return asyncManager.executeComputation(() -> {
            int loaded = 0;
            
            for (String evolutionName : evolutionNames) {
                if (!evolutionCache.containsKey(evolutionName)) {
                    try {
                        EvolutionData data = loadEvolutionDataFromService(evolutionName);
                        if (data != null) {
                            evolutionCache.put(evolutionName, data);
                            loaded++;
                        }
                    } catch (Exception e) {
                        LOGGER.fine("Failed to preload evolution data for " + evolutionName + ": " + e.getMessage());
                    }
                }
            }
            
            addToCounter("evolution_data_preloaded", loaded);
            LOGGER.info("Preloaded " + loaded + "/" + evolutionNames.size() + " evolution data entries");
            return null;
        });
    }
    
    /**
     * Gets evolution progress for a player
     * 
     * @param playerUuid the player's UUID
     * @return CompletableFuture with evolution progress
     */
    @NotNull
    public CompletableFuture<EvolutionProgress> getEvolutionProgressAsync(@NotNull UUID playerUuid) {
        // Check cache first
        EvolutionProgress cached = progressCache.get(playerUuid);
        if (cached != null) {
            incrementCounter("evolution_progress_cache_hits");
            return CompletableFuture.completedFuture(cached);
        }
        
        return asyncManager.executeDatabase(() -> {
            try {
                OneblockIsland island = islandService.loadIslandByOwnerAsync(playerUuid).join();
                if (island == null) {
                    return null;
                }
                
                EvolutionProgress progress = getEvolutionProgress(playerUuid, island);
                progressCache.put(playerUuid, progress);
                
                incrementCounter("evolution_progress_cache_misses");
                return progress;
            } catch (Exception e) {
                LOGGER.warning("Failed to get evolution progress for " + playerUuid + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Gets evolution leaderboard asynchronously
     * 
     * @param limit maximum number of entries
     * @return CompletableFuture with leaderboard data
     */
    @NotNull
    public CompletableFuture<List<EvolutionLeaderboardEntry>> getEvolutionLeaderboardAsync(int limit) {
        return asyncManager.executeDatabase(() -> {
            try {
                // This would typically query the database for top evolution levels
                // For now, we'll simulate this with cached data
                List<EvolutionLeaderboardEntry> leaderboard = new java.util.ArrayList<>();
                
                // Get top islands and convert to leaderboard entries
                List<OneblockIsland> topIslands = islandService.getTopIslandsByLevelAsync(limit).join();
                
                for (int i = 0; i < topIslands.size(); i++) {
                    OneblockIsland island = topIslands.get(i);
                    EvolutionProgress progress = getEvolutionProgress(island.getOwnerUuid(), island);
                    
                    leaderboard.add(new EvolutionLeaderboardEntry(
                        i + 1, // rank
                        island.getOwnerUuid(),
                        progress.currentEvolution(),
                        progress.evolutionLevel(),
                        progress.currentExperience()
                    ));
                }
                
                incrementCounter("evolution_leaderboard_queries");
                return leaderboard;
            } catch (Exception e) {
                LOGGER.warning("Failed to get evolution leaderboard: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Invalidates evolution cache for a player
     * 
     * @param playerUuid the player's UUID
     */
    public void invalidatePlayerEvolutionCache(@NotNull UUID playerUuid) {
        progressCache.remove(playerUuid);
    }
    
    /**
     * Invalidates evolution data cache
     * 
     * @param evolutionName the evolution name
     */
    public void invalidateEvolutionDataCache(@NotNull String evolutionName) {
        evolutionCache.remove(evolutionName);
        requirementCache.remove(evolutionName);
    }
    
    /**
     * Gets cache statistics
     * 
     * @return evolution cache statistics
     */
    @NotNull
    public EvolutionCacheStats getCacheStats() {
        return new EvolutionCacheStats(
            evolutionCache.getStatistics(),
            progressCache.getStatistics(),
            requirementCache.getStatistics()
        );
    }
    
    /**
     * Performs cache cleanup
     */
    public void performCacheCleanup() {
        int evolutionCleaned = evolutionCache.cleanup();
        int progressCleaned = progressCache.cleanup();
        int requirementCleaned = requirementCache.cleanup();
        
        int totalCleaned = evolutionCleaned + progressCleaned + requirementCleaned;
        if (totalCleaned > 0) {
            LOGGER.fine("Evolution cache cleanup: removed " + totalCleaned + " expired entries");
        }
    }
    
    // Helper methods
    
    private EvolutionProgress getEvolutionProgress(@NotNull UUID playerUuid, @NotNull OneblockIsland island) {
        // Extract evolution progress from island data
        String currentEvolution = island.getOneblock() != null ? 
            String.valueOf(island.getOneblock().getCurrentEvolution()) : "1";
        double currentExperience = island.getOneblock() != null ?
            island.getOneblock().getEvolutionExperience() : 0;
        int evolutionLevel = island.getOneblock() != null ? 
            island.getOneblock().getEvolutionLevel() : 1;
        
        return new EvolutionProgress(
            playerUuid,
            currentEvolution,
            currentExperience,
            evolutionLevel,
            System.currentTimeMillis()
        );
    }
    
    private EvolutionData getEvolutionData(@NotNull String evolutionName) {
        // Check cache first
        EvolutionData cached = evolutionCache.get(evolutionName);
        if (cached != null) {
            return cached;
        }
        
        // Load from service
        EvolutionData data = loadEvolutionDataFromService(evolutionName);
        if (data != null) {
            evolutionCache.put(evolutionName, data);
        }
        
        return data;
    }
    
    private EvolutionData loadEvolutionDataFromService(@NotNull String evolutionName) {
        try {
            // Load from EvolutionFactory
            EvolutionFactory factory = EvolutionFactory.getInstance();
            OneblockEvolution evolution = factory.getCachedEvolution(evolutionName);
            
            if (evolution != null) {
                // Calculate next evolution name based on level
                int nextLevel = evolution.getLevel() + 1;
                String nextEvolution = findEvolutionByLevel(nextLevel);
                
                return new EvolutionData(
                    evolution.getEvolutionName(),
                    evolution.getDescription() != null ? evolution.getDescription() : "Evolution " + evolutionName,
                    evolution.getExperienceToPass(),
                    nextEvolution,
                    evolution.getRequirements()
                );
            }
            
            // Fallback for numeric evolution names (legacy support)
            try {
                int level = Integer.parseInt(evolutionName);
                return new EvolutionData(
                    evolutionName,
                    "Evolution " + evolutionName,
                    1000 * level,
                    String.valueOf(level + 1),
                    new ArrayList<>()
                );
            } catch (NumberFormatException e) {
                LOGGER.warning("Evolution not found: " + evolutionName);
                return null;
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load evolution data for " + evolutionName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds an evolution by its level
     * @param level the level to find
     * @return the evolution name, or null if not found
     */
    @Nullable
    private String findEvolutionByLevel(int level) {
        EvolutionFactory factory = EvolutionFactory.getInstance();
        OneblockEvolution evolution = factory.getEvolutionByLevel(level);
        return evolution != null ? evolution.getEvolutionName() : null;
    }
    
    private List<EvolutionRequirement> getEvolutionRequirements(@NotNull String evolutionName) {
        List<EvolutionRequirement> cached = requirementCache.get(evolutionName);
        if (cached != null) {
            return cached;
        }

        List<EvolutionRequirement> requirements = new ArrayList<>();
        
        // Load requirements from evolution
        EvolutionData data = getEvolutionData(evolutionName);
        if (data != null && data.requirements() != null) {
            for (AbstractRequirement req : data.requirements()) {
                requirements.add(new EvolutionRequirement(
                    req.getTypeId(),
                    req.getDescriptionKey(),
                    req
                ));
            }
        }
        
        requirementCache.put(evolutionName, requirements);
        return requirements;
    }

    public record EvolutionData(
        String evolutionName, 
        String displayName, 
        int requiredExperience, 
        String nextEvolution,
        List<AbstractRequirement> requirements
    ) { }

    public record EvolutionProgress(UUID playerUuid, String currentEvolution, double currentExperience, int evolutionLevel, long lastUpdated) { }

    public record EvolutionResult(boolean evolved, String message, EvolutionProgress progress, EvolutionData newEvolution) {}

    public record EvolutionUpdate(UUID playerUuid, int experienceGained) { }
    
    @Getter
    public static class EvolutionRequirement {
        private final String type;
        private final String description;
        private final AbstractRequirement requirement;
        
        public EvolutionRequirement(String type, String description, AbstractRequirement requirement) {
            this.type = type;
            this.description = description;
            this.requirement = requirement;
        }
        
        public boolean isMet(@NotNull UUID playerUuid) {
            if (requirement == null) {
                return true;
            }
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) {
                return false;
            }
            return requirement.isMet(player);
        }
        
        public boolean isMet(@NotNull Player player) {
            if (requirement == null) {
                return true;
            }
            return requirement.isMet(player);
        }
        
        public double calculateProgress(@NotNull UUID playerUuid) {
            if (requirement == null) {
                return 1.0;
            }
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) {
                return 0.0;
            }
            return requirement.calculateProgress(player);
        }
        
        public double calculateProgress(@NotNull Player player) {
            if (requirement == null) {
                return 1.0;
            }
            return requirement.calculateProgress(player);
        }
        
        public void consume(@NotNull UUID playerUuid) {
            if (requirement == null) {
                return;
            }
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                requirement.consume(player);
            }
        }
        
        public void consume(@NotNull Player player) {
            if (requirement == null) {
                return;
            }
            requirement.consume(player);
        }
    }

    public record EvolutionLeaderboardEntry(int rank, UUID playerUuid, String currentEvolution, int evolutionLevel, double currentExperience) { }

    public record EvolutionCacheStats(CacheManager.CacheStatistics evolutionStats, CacheManager.CacheStatistics progressStats, CacheManager.CacheStatistics requirementStats) {

        public double getOverallHitRate() {
                long totalHits = evolutionStats.getHitCount() + progressStats.getHitCount() + requirementStats.getHitCount();
                long totalRequests = totalHits + evolutionStats.getMissCount() + progressStats.getMissCount() + requirementStats.getMissCount();

            return totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
            }

        @Override
            public @NotNull String toString() {
                return String.format("EvolutionCacheStats{overallHitRate=%.2f%%, evolution=%s, progress=%s, requirements=%s}",
                        getOverallHitRate() * 100, evolutionStats, progressStats, requirementStats);
            }
        }
}