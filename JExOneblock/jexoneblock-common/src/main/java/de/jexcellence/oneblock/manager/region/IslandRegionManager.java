/*
package de.jexcellence.oneblock.manager.region;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockRegion;
import de.jexcellence.oneblock.manager.base.BaseManager;
import de.jexcellence.oneblock.manager.base.ManagerException;
import de.jexcellence.oneblock.manager.config.ConfigurationManager;
import de.jexcellence.oneblock.manager.config.IslandConfiguration;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockRegionRepository;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

*/
/**
 * Modern implementation of island region management with multi-dimensional support.
 * Handles region creation, expansion, player tracking, and cross-dimensional functionality.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

@Getter
public class IslandRegionManager extends BaseManager implements IIslandRegionManager {
    
    private final ConfigurationManager configurationManager;
    private final OneblockIslandRepository islandRepository;
    private final OneblockRegionRepository regionRepository;
    
    // Player tracking for performance optimization
    private final Map<UUID, OneblockIsland> playerIslandCache = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> regionPlayerTracker = new ConcurrentHashMap<>();
    
    // Multi-dimensional support
    private final Map<String, String> dimensionMapping = new HashMap<>();
    
    */
/**
     * Creates a new IslandRegionManager with modern constructor injection.
     * 
     * @param executorService the executor service for async operations
     * @param configurationManager the configuration manager
     * @param islandRepository the island repository
     * @param regionRepository the region repository
     *//*

    public IslandRegionManager(@NotNull ExecutorService executorService,
                              @NotNull ConfigurationManager configurationManager,
                              @NotNull OneblockIslandRepository islandRepository,
                              @NotNull OneblockRegionRepository regionRepository) {
        super("island-region-manager", "Island Region Manager", executorService);
        this.configurationManager = Objects.requireNonNull(configurationManager, "ConfigurationManager cannot be null");
        this.islandRepository = Objects.requireNonNull(islandRepository, "IslandRepository cannot be null");
        this.regionRepository = Objects.requireNonNull(regionRepository, "RegionRepository cannot be null");
        
        initializeDimensionMapping();
    }
    
    @Override
    protected void validateDependencies() throws ManagerException {
        if (configurationManager == null) {
            throw new ManagerException("ConfigurationManager is required", getManagerId(), 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "configurationManager");
        }
        if (islandRepository == null) {
            throw new ManagerException("IslandRepository is required", getManagerId(), 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "islandRepository");
        }
        if (regionRepository == null) {
            throw new ManagerException("RegionRepository is required", getManagerId(), 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "regionRepository");
        }
    }
    
    @Override
    protected void doInitialize() throws Exception {
        logger.info("Initializing Island Region Manager...");
        
        // Load configuration
        IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
        if (config == null) {
            throw new ManagerException("Failed to load island configuration", getManagerId(), 
                                     ManagerException.ErrorCode.CONFIGURATION_ERROR, null);
        }
        
        // Initialize player tracking
        initializePlayerTracking();
        
        logger.info("Island Region Manager initialized successfully");
    }
    
    @Override
    protected void doStart() throws Exception {
        logger.info("Starting Island Region Manager...");
        
        // Start periodic player tracking updates
        startPlayerTrackingTask();
        
        logger.info("Island Region Manager started successfully");
    }
    
    @Override
    protected void doShutdown() throws Exception {
        logger.info("Shutting down Island Region Manager...");
        
        // Clear caches
        playerIslandCache.clear();
        regionPlayerTracker.clear();
        
        logger.info("Island Region Manager shut down successfully");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> createRegion(@NotNull OneblockIsland island, @NotNull Location center, int size) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(center, "Center location cannot be null");
        
        return executeAsyncOperation(() -> {
            logger.debug("Creating region for island {} at location {}", island.getIdentifier(), center);
            
            // Validate location doesn't overlap with existing regions
            if (!validateRegionLocationSync(center, size)) {
                throw new ManagerException("Region location overlaps with existing regions", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Create embedded region for the island
            IslandRegion embeddedRegion = new IslandRegion(center, size);
            island.setRegion(embeddedRegion);
            
            // Create separate region entity for queries
            Location corner1 = new Location(center.getWorld(), 
                                          center.getBlockX() - size / 2, 
                                          center.getBlockY() - 64, 
                                          center.getBlockZ() - size / 2);
            Location corner2 = new Location(center.getWorld(), 
                                          center.getBlockX() + size / 2, 
                                          center.getBlockY() + 320, 
                                          center.getBlockZ() + size / 2);
            
            OneblockRegion regionEntity = new OneblockRegion(corner1, corner2, center.clone(), center.clone());
            regionEntity.setIsland(island);
            
            // Save both entities
            islandRepository.createAsync(island).join();
            regionRepository.createAsync(regionEntity).join();
            
            // Update tracking
            updatePlayerTracking(island);
            
            logger.info("Successfully created region for island {}", island.getIdentifier());
        }, "createRegion");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> expandRegion(@NotNull OneblockIsland island, int additionalSize) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeAsyncOperation(() -> {
            logger.debug("Expanding region for island {} by {}", island.getIdentifier(), additionalSize);
            
            IslandRegion embeddedRegion = island.getRegion();
            if (embeddedRegion == null) {
                throw new ManagerException("Island has no region to expand", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Check if expansion would exceed maximum size
            IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
            int newSize = island.getCurrentSize() + additionalSize;
            if (newSize > config.getMaxSize()) {
                throw new ManagerException("Expansion would exceed maximum island size", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Validate expanded region doesn't overlap
            Location center = embeddedRegion.getCenterLocation();
            if (!validateRegionLocationSync(center, newSize)) {
                throw new ManagerException("Expanded region would overlap with existing regions", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Expand embedded region
            embeddedRegion.expand(additionalSize / 2);
            island.setCurrentSize(newSize);
            
            // Update region entity
            Optional<OneblockRegion> regionEntityOpt = getRegionEntity(island);
            if (regionEntityOpt.isPresent()) {
                OneblockRegion regionEntity = regionEntityOpt.get();
                regionEntity.expand(additionalSize / 2);
                regionRepository.saveAsync(regionEntity).join();
            }
            
            // Save island
            islandRepository.createAsync(island).join();
            
            // Update tracking
            updatePlayerTracking(island);
            
            logger.info("Successfully expanded region for island {} to size {}", island.getIdentifier(), newSize);
        }, "expandRegion");
    }
    
    @Override
    public boolean contains(@NotNull OneblockIsland island, @NotNull Location location) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        
        IslandRegion region = island.getRegion();
        if (region == null) {
            return false;
        }
        
        return region.contains(location);
    }
    
    @Override
    @NotNull
    public List<Player> getPlayersInRegion(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        String regionKey = island.getIdentifier();
        Set<UUID> playerUUIDs = regionPlayerTracker.getOrDefault(regionKey, Collections.emptySet());
        
        return playerUUIDs.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    @Nullable
    public BoundingBox getBoundingBox(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        IslandRegion region = island.getRegion();
        if (region == null) {
            return null;
        }
        
        // Handle multi-dimensional support
        String worldName = getDimensionWorldName(region.getWorldName(), dimension);
        if (worldName == null) {
            return null;
        }
        
        return new BoundingBox(
            region.getMinX(), region.getMinY(), region.getMinZ(),
            region.getMaxX(), region.getMaxY(), region.getMaxZ()
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<OneblockIsland>> findIslandAt(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        
        return executeOperation(() -> {
            // First check cache
            for (Map.Entry<UUID, OneblockIsland> entry : playerIslandCache.entrySet()) {
                OneblockIsland island = entry.getValue();
                if (contains(island, location)) {
                    return Optional.of(island);
                }
            }
            
            // Query database for region containing the point
            Optional<OneblockRegion> regionOpt = regionRepository
                .findContainingPointAsync(location.getBlockX(), location.getBlockZ())
                .join();
            
            if (regionOpt.isPresent()) {
                OneblockRegion region = regionOpt.get();
                OneblockIsland island = region.getIsland();
                if (island != null && contains(island, location)) {
                    return Optional.of(island);
                }
            }
            
            return Optional.empty();
        }, "findIslandAt");
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findOverlappingIslands(@NotNull Location corner1, @NotNull Location corner2) {
        Objects.requireNonNull(corner1, "Corner1 cannot be null");
        Objects.requireNonNull(corner2, "Corner2 cannot be null");
        
        return executeOperation(() -> {
            int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
            int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
            int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
            int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
            
            List<OneblockRegion> overlappingRegions = regionRepository
                .findOverlappingRegionsAsync(minX, minZ, maxX, maxZ)
                .join();
            
            return overlappingRegions.stream()
                .map(OneblockRegion::getIsland)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }, "findOverlappingIslands");
    }
    
    @Override
    @NotNull
    public Optional<OneblockIsland> getPlayerCurrentIsland(@NotNull OneblockPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        
        // Check cache first
        OneblockIsland cachedIsland = playerIslandCache.get(player.getUniqueId());
        if (cachedIsland != null) {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer != null && contains(cachedIsland, bukkitPlayer.getLocation())) {
                return Optional.of(cachedIsland);
            } else {
                // Player moved out of cached island
                playerIslandCache.remove(player.getUniqueId());
            }
        }
        
        // Find current island
        Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer != null) {
            Optional<OneblockIsland> currentIsland = findIslandAt(bukkitPlayer.getLocation()).join();
            if (currentIsland.isPresent()) {
                playerIslandCache.put(player.getUniqueId(), currentIsland.get());
                return currentIsland;
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> moveRegion(@NotNull OneblockIsland island, @NotNull Location newCenter) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(newCenter, "New center cannot be null");
        
        return executeAsyncOperation(() -> {
            logger.debug("Moving region for island {} to location {}", island.getIdentifier(), newCenter);
            
            IslandRegion embeddedRegion = island.getRegion();
            if (embeddedRegion == null) {
                throw new ManagerException("Island has no region to move", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Validate new location doesn't overlap
            if (!validateRegionLocationSync(newCenter, island.getCurrentSize())) {
                throw new ManagerException("New region location overlaps with existing regions", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Move embedded region
            embeddedRegion.moveTo(newCenter);
            island.setCenterLocation(newCenter.clone());
            
            // Update region entity
            Optional<OneblockRegion> regionEntityOpt = getRegionEntity(island);
            if (regionEntityOpt.isPresent()) {
                OneblockRegion regionEntity = regionEntityOpt.get();
                
                // Calculate new corners
                int size = island.getCurrentSize();
                Location corner1 = new Location(newCenter.getWorld(), 
                                              newCenter.getBlockX() - size / 2, 
                                              newCenter.getBlockY() - 64, 
                                              newCenter.getBlockZ() - size / 2);
                Location corner2 = new Location(newCenter.getWorld(), 
                                              newCenter.getBlockX() + size / 2, 
                                              newCenter.getBlockY() + 320, 
                                              newCenter.getBlockZ() + size / 2);
                
                // Update region entity coordinates
                regionEntity.setX1(corner1.getBlockX());
                regionEntity.setY1(corner1.getBlockY());
                regionEntity.setZ1(corner1.getBlockZ());
                regionEntity.setX2(corner2.getBlockX());
                regionEntity.setY2(corner2.getBlockY());
                regionEntity.setZ2(corner2.getBlockZ());
                regionEntity.setCurrentWorld(newCenter.getWorld());
                regionEntity.setSpawnLocation(newCenter.clone());
                regionEntity.setVisitorSpawnLocation(newCenter.clone());
                
                regionRepository.saveAsync(regionEntity).join();
            }
            
            // Save island
            islandRepository.createAsync(island).join();
            
            // Update tracking
            updatePlayerTracking(island);
            
            logger.info("Successfully moved region for island {} to {}", island.getIdentifier(), newCenter);
        }, "moveRegion");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> validateRegionLocation(@NotNull Location center, int size) {
        Objects.requireNonNull(center, "Center location cannot be null");
        
        return executeOperation(() -> validateRegionLocationSync(center, size), "validateRegionLocation");
    }
    
    @Override
    @NotNull
    public Optional<Location> getSafeSpawnLocation(@NotNull OneblockIsland island, boolean isVisitor) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        IslandRegion region = island.getRegion();
        if (region == null) {
            return Optional.empty();
        }
        
        Location spawnLocation = isVisitor ? region.getVisitorSpawnLocation() : region.getSpawnLocation();
        if (spawnLocation == null) {
            spawnLocation = region.getCenterLocation();
        }
        
        // Ensure spawn location is safe (not in blocks, has air above)
        return Optional.of(findSafeLocation(spawnLocation));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> updateSpawnLocation(@NotNull OneblockIsland island, @NotNull Location newSpawn, boolean isVisitor) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(newSpawn, "New spawn location cannot be null");
        
        return executeAsyncOperation(() -> {
            IslandRegion region = island.getRegion();
            if (region == null) {
                throw new ManagerException("Island has no region", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Validate spawn location is within region
            if (!region.contains(newSpawn)) {
                throw new ManagerException("Spawn location is outside island region", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Update spawn location
            Location safeSpawn = findSafeLocation(newSpawn);
            if (isVisitor) {
                region.setVisitorSpawnLocation(safeSpawn);
            } else {
                region.setSpawnLocation(safeSpawn);
            }
            
            // Update region entity
            Optional<OneblockRegion> regionEntityOpt = getRegionEntity(island);
            if (regionEntityOpt.isPresent()) {
                OneblockRegion regionEntity = regionEntityOpt.get();
                if (isVisitor) {
                    regionEntity.setVisitorSpawnLocation(safeSpawn);
                } else {
                    regionEntity.setSpawnLocation(safeSpawn);
                }
                regionRepository.saveAsync(regionEntity).join();
            }
            
            // Save island
            islandRepository.createAsync(island).join();
            
            logger.info("Updated {} spawn location for island {} to {}", 
                       isVisitor ? "visitor" : "member", island.getIdentifier(), safeSpawn);
        }, "updateSpawnLocation");
    }
    
    @Override
    @NotNull
    public Optional<OneblockRegion> getRegionEntity(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        // Query for region entity by island relationship
        // This would need to be implemented in the repository
        // For now, return empty as this is a complex query
        return Optional.empty();
    }
    
    @Override
    public boolean regionsOverlap(@NotNull OneblockRegion region1, @NotNull OneblockRegion region2) {
        Objects.requireNonNull(region1, "Region1 cannot be null");
        Objects.requireNonNull(region2, "Region2 cannot be null");
        
        if (!region1.getCurrentWorld().equals(region2.getCurrentWorld())) {
            return false;
        }
        
        return !(region1.getX2() < region2.getX1() || region1.getX1() > region2.getX2() ||
                 region1.getY2() < region2.getY1() || region1.getY1() > region2.getY2() ||
                 region1.getZ2() < region2.getZ1() || region1.getZ1() > region2.getZ2());
    }
    
    @Override
    public double calculateRegionDistance(@NotNull OneblockRegion region1, @NotNull OneblockRegion region2) {
        Objects.requireNonNull(region1, "Region1 cannot be null");
        Objects.requireNonNull(region2, "Region2 cannot be null");
        
        if (!region1.getCurrentWorld().equals(region2.getCurrentWorld())) {
            return -1;
        }
        
        Location center1 = region1.getCenterLocation();
        Location center2 = region2.getCenterLocation();
        return center1.distance(center2);
    }
    
    // =====================================================
    // PRIVATE HELPER METHODS
    // =====================================================
    
    private void initializeDimensionMapping() {
        dimensionMapping.put("overworld", "");
        dimensionMapping.put("nether", "_nether");
        dimensionMapping.put("end", "_the_end");
    }
    
    private void initializePlayerTracking() {
        // Initialize player tracking from existing data
        logger.debug("Initializing player tracking system");
        
        // This would load existing islands and populate the cache
        // For now, we'll start with empty caches
    }
    
    private void startPlayerTrackingTask() {
        // Start a periodic task to update player tracking
        // This would run every few seconds to update player positions
        logger.debug("Starting player tracking task");
    }
    
    private void updatePlayerTracking(@NotNull OneblockIsland island) {
        String regionKey = island.getIdentifier();
        Set<UUID> playersInRegion = new HashSet<>();
        
        // Find all online players in this region
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (contains(island, player.getLocation())) {
                playersInRegion.add(player.getUniqueId());
                playerIslandCache.put(player.getUniqueId(), island);
            }
        }
        
        regionPlayerTracker.put(regionKey, playersInRegion);
    }
    
    private boolean validateRegionLocationSync(@NotNull Location center, int size) {
        try {
            // Check minimum distance from other islands
            IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
            int minDistance = config.getSpaceBetweenIslands();
            
            // Create test region bounds
            int minX = center.getBlockX() - size / 2 - minDistance;
            int minZ = center.getBlockZ() - size / 2 - minDistance;
            int maxX = center.getBlockX() + size / 2 + minDistance;
            int maxZ = center.getBlockZ() + size / 2 + minDistance;
            
            // Check for overlapping regions
            List<OneblockRegion> overlapping = regionRepository
                .findOverlappingRegionsAsync(minX, minZ, maxX, maxZ)
                .join();
            
            return overlapping.isEmpty();
        } catch (Exception e) {
            logger.error("Error validating region location", e);
            return false;
        }
    }
    
    @Nullable
    private String getDimensionWorldName(@NotNull String baseWorldName, @NotNull String dimension) {
        String suffix = dimensionMapping.get(dimension.toLowerCase());
        if (suffix == null) {
            return null;
        }
        
        String worldName = baseWorldName + suffix;
        World world = Bukkit.getWorld(worldName);
        return world != null ? worldName : null;
    }
    
    @NotNull
    private Location findSafeLocation(@NotNull Location location) {
        Location safe = location.clone();
        World world = safe.getWorld();
        
        if (world == null) {
            return safe;
        }
        
        // Ensure location is on solid ground with air above
        int y = safe.getBlockY();
        while (y > world.getMinHeight() && !world.getBlockAt(safe.getBlockX(), y, safe.getBlockZ()).getType().isSolid()) {
            y--;
        }
        
        // Move up to find air space
        while (y < world.getMaxHeight() - 2 && 
               (world.getBlockAt(safe.getBlockX(), y + 1, safe.getBlockZ()).getType().isSolid() ||
                world.getBlockAt(safe.getBlockX(), y + 2, safe.getBlockZ()).getType().isSolid())) {
            y++;
        }
        
        safe.setY(y + 1);
        return safe;
    }
    
    // =====================================================
    // ENHANCED REGION MANAGEMENT METHODS
    // =====================================================
    
    */
/**
     * Gets players in a specific dimension of the region.
     * 
     * @param island the island to check
     * @param dimension the dimension name
     * @return CompletableFuture with list of players in the dimension
     *//*

    @NotNull
    public CompletableFuture<List<Player>> getPlayersInDimension(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        return executeOperation(() -> {
            BoundingBox box = getBoundingBoxForDimension(island, dimension);
            if (box == null) return List.of();
            
            return Bukkit.getOnlinePlayers().stream()
                .filter(player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld() != null && 
                           loc.getWorld().getName().toLowerCase().contains(dimension.toLowerCase()) &&
                           box.contains(loc.toVector());
                })
                .collect(Collectors.toList());
        }, "get-players-in-dimension");
    }
    
    */
/**
     * Validates the integrity of the region.
     * 
     * @param island the island to validate
     * @return CompletableFuture with validation result
     *//*

    @NotNull
    public CompletableFuture<Boolean> validateRegionIntegrity(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            IslandRegion region = island.getRegion();
            if (region == null) return false;
            
            // Check if region bounds are valid
            BoundingBox box = region.getBoundingBox();
            return box.getMinX() < box.getMaxX() && 
                   box.getMinY() < box.getMaxY() && 
                   box.getMinZ() < box.getMaxZ();
        }, "validate-region-integrity");
    }
    
    */
/**
     * Gets the bounding box for a specific dimension.
     * 
     * @param island the island
     * @param dimension the dimension name
     * @return the bounding box or null if not found
     *//*

    @Nullable
    public BoundingBox getBoundingBoxForDimension(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        IslandRegion region = island.getRegion();
        if (region == null) return null;
        
        BoundingBox overworldBox = region.getBoundingBox();
        
        return switch (dimension.toLowerCase()) {
            case "overworld", "world" -> overworldBox;
            case "nether" -> new BoundingBox(
                overworldBox.getMinX() / 8, 0, overworldBox.getMinZ() / 8,
                overworldBox.getMaxX() / 8, 128, overworldBox.getMaxZ() / 8
            );
            case "end" -> new BoundingBox(
                overworldBox.getMinX(), 0, overworldBox.getMinZ(),
                overworldBox.getMaxX(), 256, overworldBox.getMaxZ()
            );
            default -> null;
        };
    }
    
    */
/**
     * Gets statistics about the region.
     * 
     * @param island the island
     * @return CompletableFuture with region statistics
     *//*

    @NotNull
    public CompletableFuture<RegionStatistics> getRegionStatistics(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            IslandRegion region = island.getRegion();
            if (region == null) {
                return new RegionStatistics(0, 0, 0, 0);
            }
            
            long volume = region.getVolume();
            List<Player> allPlayers = getPlayersInRegion(island);
            int playersInRegion = allPlayers.size();
            int overworldPlayers = getPlayersInDimension(island, "overworld").join().size();
            int netherPlayers = getPlayersInDimension(island, "nether").join().size();
            
            return new RegionStatistics((int) volume, playersInRegion, overworldPlayers, netherPlayers);
        }, "get-region-statistics");
    }
    
    */
/**
     * Enhanced safe location finding with better algorithms.
     * 
     * @param island the island
     * @param center the center location to search around
     * @param radius the search radius
     * @return CompletableFuture with a safe location
     *//*

    @NotNull
    public CompletableFuture<Location> findSafeLocation(@NotNull OneblockIsland island, 
                                                       @NotNull Location center, 
                                                       int radius) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(center, "Center cannot be null");
        
        return executeOperation(() -> {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -5; y <= 5; y++) {
                        Location candidate = center.clone().add(x, y, z);
                        if (contains(island, candidate) && isLocationSafe(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
            return center; // Fallback to center
        }, "find-safe-location");
    }
    
    */
/**
     * Location safety check.
     * 
     * @param location the location to check
     * @return true if location is safe
     *//*

    private boolean isLocationSafe(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return false;
        
        // Check if there's solid ground below and air above
        Location below = location.clone().subtract(0, 1, 0);
        Location above1 = location.clone().add(0, 1, 0);
        Location above2 = location.clone().add(0, 2, 0);
        
        return below.getBlock().getType().isSolid() &&
               above1.getBlock().getType().isAir() &&
               above2.getBlock().getType().isAir();
    }
    
    */
/**
     * Record for region statistics.
     *//*

    public record RegionStatistics(int volume, int totalPlayers, int overworldPlayers, int netherPlayers) {}
}*/
