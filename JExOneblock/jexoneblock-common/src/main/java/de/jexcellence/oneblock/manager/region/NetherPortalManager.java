/*
package de.jexcellence.oneblock.manager.region;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.manager.base.BaseManager;
import de.jexcellence.oneblock.manager.base.ManagerException;
import de.jexcellence.oneblock.manager.config.ConfigurationManager;
import de.jexcellence.oneblock.manager.config.IslandConfiguration;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

*/
/**
 * Modern implementation of nether portal management with cross-dimensional support.
 * Handles portal creation, safe spawn location finding, and dimensional teleportation.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

@Getter
public class NetherPortalManager extends BaseManager implements INetherPortalManager {
    
    private final ConfigurationManager configurationManager;
    private final OneblockIslandRepository islandRepository;
    private final IslandRegionManager regionManager;
    
    // Portal location cache for performance
    private final Map<String, Map<String, Location>> portalLocationCache = new ConcurrentHashMap<>();
    
    // Dimension settings cache
    private final Map<String, Set<String>> enabledDimensionsCache = new ConcurrentHashMap<>();
    
    // Supported dimensions
    private static final Set<String> SUPPORTED_DIMENSIONS = Set.of("overworld", "nether", "end");
    private static final Map<String, Environment> DIMENSION_ENVIRONMENTS = Map.of(
        "overworld", Environment.NORMAL,
        "nether", Environment.NETHER,
        "end", Environment.THE_END
    );
    
    */
/**
     * Creates a new NetherPortalManager with modern constructor injection.
     * 
     * @param executorService the executor service for async operations
     * @param configurationManager the configuration manager
     * @param islandRepository the island repository
     * @param regionManager the region manager for cross-dimensional support
     *//*

    public NetherPortalManager(@NotNull ExecutorService executorService,
                              @NotNull ConfigurationManager configurationManager,
                              @NotNull OneblockIslandRepository islandRepository,
                              @NotNull IslandRegionManager regionManager) {
        super("nether-portal-manager", "Nether Portal Manager", executorService);
        this.configurationManager = Objects.requireNonNull(configurationManager, "ConfigurationManager cannot be null");
        this.islandRepository = Objects.requireNonNull(islandRepository, "IslandRepository cannot be null");
        this.regionManager = Objects.requireNonNull(regionManager, "RegionManager cannot be null");
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
        if (regionManager == null) {
            throw new ManagerException("RegionManager is required", getManagerId(), 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "regionManager");
        }
    }
    
    @Override
    protected void doInitialize() throws Exception {
        logger.info("Initializing Nether Portal Manager...");
        
        // Load configuration
        IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
        if (config == null) {
            throw new ManagerException("Failed to load island configuration", getManagerId(), 
                                     ManagerException.ErrorCode.CONFIGURATION_ERROR, null);
        }
        
        // Initialize caches
        initializeCaches();
        
        logger.info("Nether Portal Manager initialized successfully");
    }
    
    @Override
    protected void doStart() throws Exception {
        logger.info("Starting Nether Portal Manager...");
        
        // Validate world availability
        validateWorldAvailability();
        
        logger.info("Nether Portal Manager started successfully");
    }
    
    @Override
    protected void doShutdown() throws Exception {
        logger.info("Shutting down Nether Portal Manager...");
        
        // Clear caches
        portalLocationCache.clear();
        enabledDimensionsCache.clear();
        
        logger.info("Nether Portal Manager shut down successfully");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> createPortal(@NotNull OneblockIsland island, @NotNull Location location, @NotNull PortalType portalType) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        Objects.requireNonNull(portalType, "Portal type cannot be null");
        
        return executeAsyncOperation(() -> {
            logger.debug("Creating {} portal for island {} at location {}", portalType, island.getIdentifier(), location);
            
            String dimension = getDimensionFromPortalType(portalType);
            if (dimension == null) {
                throw new ManagerException("Unsupported portal type: " + portalType, 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Validate portal location
            if (!validatePortalLocation(island, location, dimension)) {
                throw new ManagerException("Invalid portal location", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Create the physical portal
            createPhysicalPortal(location, portalType);
            
            // Store portal location
            setPortalLocationSync(island, dimension, location);
            
            // Ensure target dimension region exists
            if ("nether".equals(dimension)) {
                createNetherRegion(island).join();
            } else if ("end".equals(dimension)) {
                createEndRegion(island).join();
            }
            
            logger.info("Successfully created {} portal for island {} at {}", portalType, island.getIdentifier(), location);
        }, "createPortal");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Location>> findSafeNetherSpawn(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            if (!isDimensionEnabled(island, "nether")) {
                return Optional.<Location>empty();
            }
            
            // Get nether world
            World netherWorld = getNetherWorld(island);
            if (netherWorld == null) {
                return Optional.<Location>empty();
            }
            
            // Calculate corresponding nether location
            Location overworldCenter = island.getCenterLocation();
            Location netherCenter = new Location(netherWorld, 
                                                overworldCenter.getX() / 8.0, 
                                                64, 
                                                overworldCenter.getZ() / 8.0);
            
            // Find safe location in nether
            Location safeLocation = findSafeLocationInDimension(netherCenter, netherWorld);
            return Optional.of(safeLocation);
        }, "findSafeNetherSpawn");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Location>> findSafeEndSpawn(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            if (!isDimensionEnabled(island, "end")) {
                return Optional.<Location>empty();
            }
            
            // Get end world
            World endWorld = getEndWorld(island);
            if (endWorld == null) {
                return Optional.<Location>empty();
            }
            
            // Use same coordinates as overworld for end
            Location overworldCenter = island.getCenterLocation();
            Location endCenter = new Location(endWorld, 
                                            overworldCenter.getX(), 
                                            64, 
                                            overworldCenter.getZ());
            
            // Find safe location in end
            Location safeLocation = findSafeLocationInDimension(endCenter, endWorld);
            return Optional.of(safeLocation);
        }, "findSafeEndSpawn");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> handleCrossDimensionalTeleport(@NotNull OneblockPlayer player, 
                                                                 @NotNull Location fromLocation, 
                                                                 @NotNull String targetDimension) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(fromLocation, "From location cannot be null");
        Objects.requireNonNull(targetDimension, "Target dimension cannot be null");
        
        return executeAsyncOperation(() -> {
            Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
            if (bukkitPlayer == null) {
                throw new ManagerException("Player is not online", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Find player's current island
            Optional<OneblockIsland> currentIslandOpt = regionManager.findIslandAt(fromLocation).join();
            if (currentIslandOpt.isEmpty()) {
                throw new ManagerException("Player is not in an island region", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            OneblockIsland island = currentIslandOpt.get();
            
            // Check if player has access to the island
            if (!island.canAccess(player)) {
                throw new ManagerException("Player does not have access to this island", 
                                         getManagerId(), ManagerException.ErrorCode.PERMISSION_DENIED, null);
            }
            
            // Check if target dimension is enabled
            if (!isDimensionEnabled(island, targetDimension)) {
                throw new ManagerException("Target dimension is not enabled for this island", 
                                         getManagerId(), ManagerException.ErrorCode.VALIDATION_FAILED, null);
            }
            
            // Get spawn location in target dimension
            Optional<Location> spawnLocationOpt = getPlayerSpawnLocation(player, island, targetDimension);
            if (spawnLocationOpt.isEmpty()) {
                throw new ManagerException("No safe spawn location found in target dimension", 
                                         getManagerId(), ManagerException.ErrorCode.OPERATION_FAILED, null);
            }
            
            // Teleport player
            Location spawnLocation = spawnLocationOpt.get();
            bukkitPlayer.teleport(spawnLocation);
            
            logger.info("Teleported player {} from {} to {} dimension at {}", 
                       player.getPlayerName(), fromLocation, targetDimension, spawnLocation);
        }, "handleCrossDimensionalTeleport");
    }
    
    @Override
    @Nullable
    public Location getCorrespondingLocation(@NotNull OneblockIsland island, 
                                           @NotNull Location currentLocation, 
                                           @NotNull String targetDimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(currentLocation, "Current location cannot be null");
        Objects.requireNonNull(targetDimension, "Target dimension cannot be null");
        
        World targetWorld = getWorldForDimension(island, targetDimension);
        if (targetWorld == null) {
            return null;
        }
        
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double z = currentLocation.getZ();
        
        // Apply dimension-specific coordinate transformations
        if ("nether".equals(targetDimension)) {
            x /= 8.0;
            z /= 8.0;
            y = Math.max(64, Math.min(120, y)); // Keep Y in safe nether range
        } else if (currentLocation.getWorld().getEnvironment() == Environment.NETHER && 
                   "overworld".equals(targetDimension)) {
            x *= 8.0;
            z *= 8.0;
        }
        
        return new Location(targetWorld, x, y, z);
    }
    
    @Override
    public boolean isDimensionEnabled(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        Set<String> enabledDimensions = enabledDimensionsCache.get(island.getIdentifier());
        if (enabledDimensions == null) {
            // Load from configuration or island settings
            IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
            enabledDimensions = new HashSet<>();
            
            if (config.isEnableNether() && "nether".equals(dimension)) {
                enabledDimensions.add("nether");
            }
            if (config.isEnableEnd() && "end".equals(dimension)) {
                enabledDimensions.add("end");
            }
            enabledDimensions.add("overworld"); // Always enabled
            
            enabledDimensionsCache.put(island.getIdentifier(), enabledDimensions);
        }
        
        return enabledDimensions.contains(dimension);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setDimensionEnabled(@NotNull OneblockIsland island, @NotNull String dimension, boolean enabled) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        return executeAsyncOperation(() -> {
            Set<String> enabledDimensions = enabledDimensionsCache.computeIfAbsent(
                island.getIdentifier(), k -> new HashSet<>());
            
            if (enabled) {
                enabledDimensions.add(dimension);
                
                // Create dimension region if needed
                if ("nether".equals(dimension)) {
                    createNetherRegion(island).join();
                } else if ("end".equals(dimension)) {
                    createEndRegion(island).join();
                }
            } else {
                enabledDimensions.remove(dimension);
                
                // Remove portals in disabled dimension
                removePortal(island, dimension).join();
            }
            
            // Save island settings (this would need to be implemented in the island entity)
            islandRepository.createAsync(island).join();
            
            logger.info("Set dimension {} {} for island {}", dimension, enabled ? "enabled" : "disabled", island.getIdentifier());
        }, "setDimensionEnabled");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> createNetherRegion(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeAsyncOperation(() -> {
            World netherWorld = getNetherWorld(island);
            if (netherWorld == null) {
                throw new ManagerException("Nether world is not available", 
                                         getManagerId(), ManagerException.ErrorCode.WORLD_NOT_FOUND, null);
            }
            
            // Calculate nether region location
            Location overworldCenter = island.getCenterLocation();
            Location netherCenter = new Location(netherWorld, 
                                                overworldCenter.getX() / 8.0, 
                                                64, 
                                                overworldCenter.getZ() / 8.0);
            
            // Create nether region using region manager
            regionManager.createRegion(island, netherCenter, island.getCurrentSize()).join();
            
            logger.info("Created nether region for island {} at {}", island.getIdentifier(), netherCenter);
        }, "createNetherRegion");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> createEndRegion(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeAsyncOperation(() -> {
            World endWorld = getEndWorld(island);
            if (endWorld == null) {
                throw new ManagerException("End world is not available", 
                                         getManagerId(), ManagerException.ErrorCode.WORLD_NOT_FOUND, null);
            }
            
            // Use same coordinates as overworld for end
            Location overworldCenter = island.getCenterLocation();
            Location endCenter = new Location(endWorld, 
                                            overworldCenter.getX(), 
                                            64, 
                                            overworldCenter.getZ());
            
            // Create end region using region manager
            regionManager.createRegion(island, endCenter, island.getCurrentSize()).join();
            
            logger.info("Created end region for island {} at {}", island.getIdentifier(), endCenter);
        }, "createEndRegion");
    }
    
    @Override
    public boolean handlePortalCreate(@NotNull Player player, @NotNull Location location, @NotNull PortalType portalType) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        Objects.requireNonNull(portalType, "Portal type cannot be null");
        
        try {
            // Find island at portal location
            Optional<OneblockIsland> islandOpt = regionManager.findIslandAt(location).join();
            if (islandOpt.isEmpty()) {
                return false; // Not in an island region
            }
            
            OneblockIsland island = islandOpt.get();
            
            // Check if player has permission to create portals
            // This would need to be implemented based on the permission system
            
            // Create portal asynchronously
            createPortal(island, location, portalType);
            
            return true;
        } catch (Exception e) {
            logger.error("Error handling portal creation", e);
            return false;
        }
    }
    
    @Override
    public boolean handlePortalUse(@NotNull Player player, @NotNull Location fromLocation, @NotNull PortalType portalType) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(fromLocation, "From location cannot be null");
        Objects.requireNonNull(portalType, "Portal type cannot be null");
        
        try {
            String targetDimension = getTargetDimensionFromPortalType(portalType, fromLocation.getWorld());
            if (targetDimension == null) {
                return false;
            }
            
            // Find player entity
            // This would need to be implemented to get OneblockPlayer from Player
            
            // Handle teleportation asynchronously
            // handleCrossDimensionalTeleport(oneblockPlayer, fromLocation, targetDimension);
            
            return true;
        } catch (Exception e) {
            logger.error("Error handling portal use", e);
            return false;
        }
    }
    
    @Override
    @NotNull
    public Optional<Location> getPortalLocation(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        Map<String, Location> islandPortals = portalLocationCache.get(island.getIdentifier());
        if (islandPortals == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(islandPortals.get(dimension));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setPortalLocation(@NotNull OneblockIsland island, @NotNull String dimension, @NotNull Location location) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        
        return executeAsyncOperation(() -> {
            setPortalLocationSync(island, dimension, location);
            
            // Save to database (this would need to be implemented)
            islandRepository.createAsync(island).join();
            
            logger.debug("Set portal location for island {} in dimension {} to {}", 
                        island.getIdentifier(), dimension, location);
        }, "setPortalLocation");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> removePortal(@NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        return executeAsyncOperation(() -> {
            // Remove from cache
            Map<String, Location> islandPortals = portalLocationCache.get(island.getIdentifier());
            if (islandPortals != null) {
                Location portalLocation = islandPortals.remove(dimension);
                if (portalLocation != null) {
                    // Remove physical portal
                    removePhysicalPortal(portalLocation);
                }
            }
            
            // Save to database
            islandRepository.createAsync(island).join();
            
            logger.info("Removed portal for island {} in dimension {}", island.getIdentifier(), dimension);
        }, "removePortal");
    }
    
    @Override
    public boolean validatePortalLocation(@NotNull OneblockIsland island, @NotNull Location location, @NotNull String dimension) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        // Check if location is within island region
        if (!regionManager.contains(island, location)) {
            return false;
        }
        
        // Check if dimension is enabled
        if (!isDimensionEnabled(island, dimension)) {
            return false;
        }
        
        // Check if location has enough space for portal
        return hasSpaceForPortal(location);
    }
    
    @Override
    @NotNull
    public Optional<Location> getPlayerSpawnLocation(@NotNull OneblockPlayer player, @NotNull OneblockIsland island, @NotNull String dimension) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(dimension, "Dimension cannot be null");
        
        // Check if player has access to island
        if (!island.canAccess(player)) {
            return Optional.empty();
        }
        
        // Get spawn location based on dimension
        switch (dimension) {
            case "nether":
                return findSafeNetherSpawn(island).join();
            case "end":
                return findSafeEndSpawn(island).join();
            case "overworld":
                return regionManager.getSafeSpawnLocation(island, !island.isMember(player));
            default:
                return Optional.empty();
        }
    }
    
    // =====================================================
    // PRIVATE HELPER METHODS
    // =====================================================
    
    private void initializeCaches() {
        logger.debug("Initializing portal and dimension caches");
        // Load existing portal locations and dimension settings from database
    }
    
    private void validateWorldAvailability() throws ManagerException {
        IslandConfiguration config = configurationManager.getConfiguration("island", IslandConfiguration.class);
        
        if (config.isEnableNether() && Bukkit.getWorld("world_nether") == null) {
            logger.warn("Nether is enabled in configuration but nether world is not available");
        }
        
        if (config.isEnableEnd() && Bukkit.getWorld("world_the_end") == null) {
            logger.warn("End is enabled in configuration but end world is not available");
        }
    }
    
    @Nullable
    private String getDimensionFromPortalType(@NotNull PortalType portalType) {
        switch (portalType) {
            case NETHER:
                return "nether";
            case ENDER:
                return "end";
            default:
                return null;
        }
    }
    
    @Nullable
    private String getTargetDimensionFromPortalType(@NotNull PortalType portalType, @NotNull World currentWorld) {
        if (portalType == PortalType.NETHER) {
            return currentWorld.getEnvironment() == Environment.NETHER ? "overworld" : "nether";
        } else if (portalType == PortalType.ENDER) {
            return currentWorld.getEnvironment() == Environment.THE_END ? "overworld" : "end";
        }
        return null;
    }
    
    @Nullable
    private World getNetherWorld(@NotNull OneblockIsland island) {
        String worldName = island.getCenterLocation().getWorld().getName() + "_nether";
        return Bukkit.getWorld(worldName);
    }
    
    @Nullable
    private World getEndWorld(@NotNull OneblockIsland island) {
        String worldName = island.getCenterLocation().getWorld().getName() + "_the_end";
        return Bukkit.getWorld(worldName);
    }
    
    @Nullable
    private World getWorldForDimension(@NotNull OneblockIsland island, @NotNull String dimension) {
        switch (dimension) {
            case "overworld":
                return island.getCenterLocation().getWorld();
            case "nether":
                return getNetherWorld(island);
            case "end":
                return getEndWorld(island);
            default:
                return null;
        }
    }
    
    @NotNull
    private Location findSafeLocationInDimension(@NotNull Location center, @NotNull World world) {
        Location safe = center.clone();
        
        // Find solid ground
        int y = safe.getBlockY();
        while (y > world.getMinHeight() && !world.getBlockAt(safe.getBlockX(), y, safe.getBlockZ()).getType().isSolid()) {
            y--;
        }
        
        // Find air space above
        while (y < world.getMaxHeight() - 2 && 
               (world.getBlockAt(safe.getBlockX(), y + 1, safe.getBlockZ()).getType().isSolid() ||
                world.getBlockAt(safe.getBlockX(), y + 2, safe.getBlockZ()).getType().isSolid())) {
            y++;
        }
        
        safe.setY(y + 1);
        return safe;
    }
    
    private void createPhysicalPortal(@NotNull Location location, @NotNull PortalType portalType) {
        // Create the physical portal blocks
        Material portalMaterial = portalType == PortalType.NETHER ? Material.NETHER_PORTAL : Material.END_PORTAL;
        
        // This is a simplified implementation - actual portal creation would be more complex
        Block block = location.getBlock();
        block.setType(portalMaterial);
    }
    
    private void removePhysicalPortal(@NotNull Location location) {
        // Remove portal blocks
        Block block = location.getBlock();
        if (block.getType() == Material.NETHER_PORTAL || block.getType() == Material.END_PORTAL) {
            block.setType(Material.AIR);
        }
    }
    
    private boolean hasSpaceForPortal(@NotNull Location location) {
        // Check if there's enough space for a portal (simplified check)
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        // Check 3x3 area around location
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 2; y++) {
                    Block block = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                    if (block.getType().isSolid()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private void setPortalLocationSync(@NotNull OneblockIsland island, @NotNull String dimension, @NotNull Location location) {
        portalLocationCache.computeIfAbsent(island.getIdentifier(), k -> new ConcurrentHashMap<>())
                          .put(dimension, location.clone());
    }
    
    // =====================================================
    // PORTAL MANAGEMENT METHODS
    // =====================================================
    
    */
/**
     * Links two portals together (overworld and nether).
     * 
     * @param overworldPortal the overworld portal location
     * @param netherPortal the nether portal location
     * @return CompletableFuture with success status
     *//*

    @NotNull
    public CompletableFuture<Boolean> linkPortals(@NotNull Location overworldPortal, @NotNull Location netherPortal) {
        return executeOperation(() -> {
            // Validate both locations are in correct dimensions
            if (overworldPortal.getWorld() == null || netherPortal.getWorld() == null) {
                return false;
            }
            
            // Create portals at both locations
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
                createPhysicalPortal(overworldPortal, PortalType.NETHER);
                createPhysicalPortal(netherPortal, PortalType.NETHER);
            });
            
            return true;
        }, "link-portals");
    }
    
    */
/**
     * Validates if a location is suitable for portal placement.
     * 
     * @param location the location to validate
     * @return CompletableFuture with validation result
     *//*

    @NotNull
    public CompletableFuture<PortalValidationResult> validatePortalLocation(@NotNull Location location) {
        return executeOperation(() -> {
            if (location.getWorld() == null) {
                return new PortalValidationResult(false, "World is null");
            }
            
            // Check if location is safe for portal
            if (!isSafePortalLocation(location)) {
                return new PortalValidationResult(false, "Location is not safe for portal");
            }
            
            return new PortalValidationResult(true, "Location is valid for portal");
        }, "validate-portal-location");
    }
    
    */
/**
     * Finds the optimal location for portal placement near the specified center.
     * 
     * @param center the center location to search around
     * @param searchRadius the search radius
     * @return CompletableFuture with the optimal portal location
     *//*

    @NotNull
    public CompletableFuture<Location> findOptimalPortalLocation(@NotNull Location center, int searchRadius) {
        return executeOperation(() -> {
            for (int radius = 1; radius <= searchRadius; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -2; y <= 2; y++) {
                            Location candidate = center.clone().add(x, y, z);
                            if (isSafePortalLocation(candidate)) {
                                return candidate;
                            }
                        }
                    }
                }
            }
            return center; // Fallback to center if no optimal location found
        }, "find-optimal-portal-location");
    }
    
    */
/**
     * Safe platform creation with better safety checks.
     * 
     * @param center the center location for the platform
     *//*

    private void createSafePlatform(@NotNull Location center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Create platform
                Location platformLoc = center.clone().add(x, -1, z);
                if (platformLoc.getBlock().getType().isAir()) {
                    platformLoc.getBlock().setType(Material.BEDROCK, false);
                }
                
                // Clear air space
                for (int y = 0; y <= 2; y++) {
                    Location clearLoc = center.clone().add(x, y, z);
                    if (!clearLoc.getBlock().getType().isAir()) {
                        clearLoc.getBlock().setType(Material.AIR, false);
                    }
                }
            }
        }
    }
    
    */
/**
     * Portal location safety check.
     * 
     * @param location the location to check
     * @return true if location is safe for portal
     *//*

    private boolean isSafePortalLocation(@NotNull Location location) {
        // Check for sufficient space (3x3x3 area)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location check = location.clone().add(x, y, z);
                    Material material = check.getBlock().getType();
                    
                    // Center column should be air or replaceable
                    if (x == 0 && z == 0 && y > 0 && !material.isAir() && material.isSolid()) {
                        return false;
                    }
                    
                    // Base should be solid
                    if (y == 0 && x == 0 && z == 0 && material.isAir()) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    */
/**
     * Result record for portal validation.
     *//*

    public record PortalValidationResult(boolean valid, String message) {}
}*/
