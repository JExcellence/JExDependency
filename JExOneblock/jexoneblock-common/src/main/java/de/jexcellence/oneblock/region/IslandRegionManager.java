package de.jexcellence.oneblock.region;

import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import de.jexcellence.oneblock.database.entity.region.RegionPermission;
import de.jexcellence.oneblock.region.RegionBoundaryChecker.BoundaryCheckResult;
import de.jexcellence.oneblock.region.SpiralIslandGenerator.SpiralConfiguration;
import de.jexcellence.oneblock.region.SpiralIslandGenerator.SpiralStatistics;
import de.jexcellence.oneblock.repository.IslandRegionRepository;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Central manager for island regions, handling creation, validation, and permissions.
 * Integrates spiral generation with boundary checking and permission management.
 */
public class IslandRegionManager {

    private static final Logger LOGGER = Logger.getLogger(IslandRegionManager.class.getName());
    
    private final SpiralIslandGenerator spiralGenerator;
    private final RegionBoundaryChecker boundaryChecker;
    private final ExecutorService executorService;
    
    // Repository interface for database operations (to be injected)
    private IslandRegionRepository repository;
    
    // Cache for active regions
    private final Map<UUID, IslandRegion> activeRegions = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> worldRegions = new ConcurrentHashMap<>();
    
    // Configuration
    private final RegionManagerConfiguration configuration;

    /**
     * Configuration for the region manager.
     */
    public static class RegionManagerConfiguration {
        private final int defaultIslandRadius;
        private final boolean enableAsyncOperations;
        private final boolean enableCaching;
        private final int maxConcurrentOperations;
        private final long cacheCleanupIntervalMs;

        public RegionManagerConfiguration(int defaultIslandRadius, boolean enableAsyncOperations,
                                        boolean enableCaching, int maxConcurrentOperations,
                                        long cacheCleanupIntervalMs) {
            this.defaultIslandRadius = defaultIslandRadius;
            this.enableAsyncOperations = enableAsyncOperations;
            this.enableCaching = enableCaching;
            this.maxConcurrentOperations = maxConcurrentOperations;
            this.cacheCleanupIntervalMs = cacheCleanupIntervalMs;
        }

        public static RegionManagerConfiguration defaultConfig() {
            return new RegionManagerConfiguration(100, true, true, 10, 300000); // 5 minutes
        }

        // Getters
        public int getDefaultIslandRadius() { return defaultIslandRadius; }
        public boolean isEnableAsyncOperations() { return enableAsyncOperations; }
        public boolean isEnableCaching() { return enableCaching; }
        public int getMaxConcurrentOperations() { return maxConcurrentOperations; }
        public long getCacheCleanupIntervalMs() { return cacheCleanupIntervalMs; }
    }

    public IslandRegionManager(@NotNull SpiralIslandGenerator spiralGenerator,
                              @NotNull RegionBoundaryChecker boundaryChecker,
                              @NotNull RegionManagerConfiguration configuration) {
        this.spiralGenerator = spiralGenerator;
        this.boundaryChecker = boundaryChecker;
        this.configuration = configuration;
        this.executorService = Executors.newFixedThreadPool(configuration.getMaxConcurrentOperations());
    }

    public IslandRegionManager(@NotNull SpiralIslandGenerator spiralGenerator,
                              @NotNull RegionBoundaryChecker boundaryChecker) {
        this(spiralGenerator, boundaryChecker, RegionManagerConfiguration.defaultConfig());
    }

    /**
     * Sets the repository for database operations.
     */
    public void setRepository(@NotNull IslandRegionRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new island region for the specified owner.
     */
    @NotNull
    public CompletableFuture<IslandRegion> createIslandRegion(@NotNull UUID ownerId, @NotNull World world) {
        return createIslandRegion(ownerId, world, configuration.getDefaultIslandRadius());
    }

    /**
     * Creates a new island region with a specific radius.
     */
    @NotNull
    public CompletableFuture<IslandRegion> createIslandRegion(@NotNull UUID ownerId, @NotNull World world, int radius) {
        if (configuration.isEnableAsyncOperations()) {
            return CompletableFuture.supplyAsync(() -> createIslandRegionSync(ownerId, world, radius), executorService);
        } else {
            return CompletableFuture.completedFuture(createIslandRegionSync(ownerId, world, radius));
        }
    }

    /**
     * Synchronous island region creation.
     */
    @NotNull
    private IslandRegion createIslandRegionSync(@NotNull UUID ownerId, @NotNull World world, int radius) {
        // Generate next spiral position
        Location centerLocation = spiralGenerator.generateNextPosition(world, radius);
        if (centerLocation == null) {
            throw new IllegalStateException("Cannot generate more islands in world: " + world.getName());
        }

        // Get spiral position for tracking
        SpiralIslandGenerator.SpiralPosition spiralPos = spiralGenerator.getSpiralPositionAt(centerLocation);
        int spiralPosition = spiralPos != null ? spiralPos.getPosition() : 0;

        // Create island region
        UUID islandId = UUID.randomUUID();
        IslandRegion region = new IslandRegion(islandId, ownerId, centerLocation, radius, spiralPosition);

        // Add default permissions for owner
        addDefaultPermissions(region, ownerId);

        // Cache the region
        if (configuration.isEnableCaching()) {
            cacheRegion(region);
        }

        LOGGER.info(String.format("Created island region for owner %s at position %s (spiral: %d)", 
                                ownerId, centerLocation, spiralPosition));

        return region;
    }

    /**
     * Adds default permissions for the island owner.
     */
    private void addDefaultPermissions(@NotNull IslandRegion region, @NotNull UUID ownerId) {
        // Owner gets all permissions by default (handled in IslandRegion.hasPermission)
        // But we can add explicit permissions for clarity
        String[] defaultPermissions = {
            RegionPermission.PermissionTypes.BUILD,
            RegionPermission.PermissionTypes.BREAK,
            RegionPermission.PermissionTypes.INTERACT,
            RegionPermission.PermissionTypes.MANAGE_PERMISSIONS,
            RegionPermission.PermissionTypes.ADMIN_ACCESS
        };

        for (String permissionType : defaultPermissions) {
            RegionPermission permission = new RegionPermission(region, ownerId, permissionType, true);
            region.addPermission(permission);
        }
    }

    /**
     * Checks if a location is within island boundaries.
     */
    public boolean isWithinBoundaries(@NotNull Location location, @NotNull UUID islandId) {
        IslandRegion region = getRegion(islandId);
        if (region == null) {
            return false;
        }

        BoundaryCheckResult result = boundaryChecker.checkBoundaries(location, region);
        return result.isWithinBoundaries();
    }

    /**
     * Checks if a player has permission to perform an action at a location.
     */
    public boolean hasPermission(@NotNull Player player, @NotNull Location location, @NotNull String action) {
        IslandRegion region = findRegionAt(location);
        if (region == null) {
            return false; // No region found, deny by default
        }

        return boundaryChecker.canPerformAction(player, location, region, action);
    }

    /**
     * Gets detailed boundary check result for a location and region.
     */
    @NotNull
    public BoundaryCheckResult checkBoundaries(@NotNull Location location, @NotNull UUID islandId) {
        IslandRegion region = getRegion(islandId);
        if (region == null) {
            return new BoundaryCheckResult(false, Double.MAX_VALUE, Double.MAX_VALUE, false,
                                         RegionBoundaryChecker.BoundaryViolationType.NO_REGION);
        }

        return boundaryChecker.checkBoundaries(location, region);
    }

    /**
     * Finds the region that contains the given location.
     */
    @Nullable
    public IslandRegion findRegionAt(@NotNull Location location) {
        // First check cache
        if (configuration.isEnableCaching()) {
            IslandRegion cached = boundaryChecker.findRegionAt(location);
            if (cached != null) {
                return cached;
            }
        }

        // Check all regions in the world
        String worldName = location.getWorld().getName();
        Set<UUID> worldRegionIds = worldRegions.get(worldName);
        if (worldRegionIds != null) {
            for (UUID regionId : worldRegionIds) {
                IslandRegion region = getRegion(regionId);
                if (region != null && region.isWithinBoundaries(location)) {
                    return region;
                }
            }
        }

        return null;
    }

    /**
     * Gets a region by island ID.
     */
    @Nullable
    public IslandRegion getRegion(@NotNull UUID islandId) {
        if (configuration.isEnableCaching()) {
            return activeRegions.get(islandId);
        }

        // If caching is disabled, we'd need to query the repository
        // For now, return null if not cached
        return null;
    }

    /**
     * Loads a region from the repository and caches it.
     */
    @NotNull
    public CompletableFuture<Optional<IslandRegion>> loadRegion(@NotNull UUID islandId) {
        if (repository == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return repository.findByIslandIdAsync(islandId)
                .thenApply(optionalRegion -> {
                    optionalRegion.ifPresent(this::cacheRegion);
                    return optionalRegion;
                });
    }

    /**
     * Saves a region to the repository.
     */
    @NotNull
    public CompletableFuture<IslandRegion> saveRegion(@NotNull IslandRegion region) {
        if (repository == null) {
            return CompletableFuture.completedFuture(region);
        }

        return repository.saveAsync(region)
                .thenApply(savedRegion -> {
                    if (configuration.isEnableCaching()) {
                        cacheRegion(savedRegion);
                    }
                    return savedRegion;
                });
    }

    /**
     * Deletes a region.
     */
    @NotNull
    public CompletableFuture<Void> deleteRegion(@NotNull UUID islandId) {
        // Remove from cache
        uncacheRegion(islandId);

        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return repository.deleteAsync(islandId);
    }

    /**
     * Grants permission to a player for a specific region.
     */
    @NotNull
    public CompletableFuture<Boolean> grantPermission(@NotNull UUID islandId, @NotNull UUID playerId, 
                                                     @NotNull String permissionType, @Nullable UUID grantedBy) {
        IslandRegion region = getRegion(islandId);
        if (region == null) {
            return CompletableFuture.completedFuture(false);
        }

        RegionPermission permission = new RegionPermission(region, playerId, permissionType, true, grantedBy);
        region.addPermission(permission);

        return saveRegion(region).thenApply(savedRegion -> true);
    }

    /**
     * Revokes permission from a player for a specific region.
     */
    @NotNull
    public CompletableFuture<Boolean> revokePermission(@NotNull UUID islandId, @NotNull UUID playerId, 
                                                      @NotNull String permissionType, @Nullable UUID revokedBy) {
        IslandRegion region = getRegion(islandId);
        if (region == null) {
            return CompletableFuture.completedFuture(false);
        }

        Optional<RegionPermission> existingPermission = region.getPermission(playerId, permissionType);
        if (existingPermission.isPresent()) {
            existingPermission.get().revoke(revokedBy, "Permission revoked");
            return saveRegion(region).thenApply(savedRegion -> true);
        }

        return CompletableFuture.completedFuture(false);
    }

    /**
     * Configures spiral generation for a world.
     */
    public void configureWorld(@NotNull World world, @NotNull SpiralConfiguration config) {
        spiralGenerator.configureWorld(world.getName(), config);
        worldRegions.putIfAbsent(world.getName(), ConcurrentHashMap.newKeySet());
    }

    /**
     * Gets spiral statistics for a world.
     */
    @NotNull
    public SpiralStatistics getWorldStatistics(@NotNull String worldName) {
        return spiralGenerator.getStatistics(worldName);
    }

    /**
     * Gets cache statistics.
     */
    @NotNull
    public RegionBoundaryChecker.CacheStatistics getCacheStatistics() {
        return boundaryChecker.getCacheStatistics();
    }

    /**
     * Caches a region for faster access.
     */
    private void cacheRegion(@NotNull IslandRegion region) {
        activeRegions.put(region.getIslandId(), region);
        boundaryChecker.cacheRegion(region);
        
        // Add to world regions
        worldRegions.computeIfAbsent(region.getWorldName(), k -> ConcurrentHashMap.newKeySet())
                   .add(region.getIslandId());
    }

    /**
     * Removes a region from cache.
     */
    private void uncacheRegion(@NotNull UUID islandId) {
        IslandRegion region = activeRegions.remove(islandId);
        boundaryChecker.uncacheRegion(islandId);
        
        if (region != null) {
            Set<UUID> worldRegionIds = worldRegions.get(region.getWorldName());
            if (worldRegionIds != null) {
                worldRegionIds.remove(islandId);
            }
        }
    }

    /**
     * Loads all regions for a world from the repository.
     */
    @NotNull
    public CompletableFuture<List<IslandRegion>> loadWorldRegions(@NotNull String worldName) {
        if (repository == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return repository.findByWorldAsync(worldName)
                .thenApply(regions -> {
                    if (configuration.isEnableCaching()) {
                        regions.forEach(this::cacheRegion);
                    }
                    return regions;
                });
    }

    /**
     * Performs cache cleanup to remove expired entries.
     */
    public void performCacheCleanup() {
        if (configuration.isEnableCaching()) {
            boundaryChecker.cleanupCache();
        }
    }

    /**
     * Gets the current configuration.
     */
    @NotNull
    public RegionManagerConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Gets the spiral generator.
     */
    @NotNull
    public SpiralIslandGenerator getSpiralGenerator() {
        return spiralGenerator;
    }

    /**
     * Gets the boundary checker.
     */
    @NotNull
    public RegionBoundaryChecker getBoundaryChecker() {
        return boundaryChecker;
    }

    /**
     * Shuts down the region manager and cleans up resources.
     */
    public void shutdown() {
        executorService.shutdown();
        if (configuration.isEnableCaching()) {
            boundaryChecker.clearAllCaches();
            activeRegions.clear();
            worldRegions.clear();
        }
    }
}