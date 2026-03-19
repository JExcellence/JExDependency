package de.jexcellence.oneblock.manager.location;

import de.jexcellence.oneblock.manager.config.LocationConfiguration;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optimized utility for checking location safety with caching and comprehensive validation.
 * Implements safe location finding algorithms with performance optimizations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class LocationSafetyChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationSafetyChecker.class);
    
    private final ExecutorService executorService;
    
    @Getter
    private LocationConfiguration configuration;
    
    // Safety check result cache
    private final Map<String, SafetyCheckResult> safetyCache = new ConcurrentHashMap<>();
    
    // Unsafe materials that should be avoided
    private static final Set<Material> UNSAFE_MATERIALS = EnumSet.of(
        Material.LAVA,
        Material.FIRE,
        Material.SOUL_FIRE,
        Material.MAGMA_BLOCK,
        Material.CACTUS,
        Material.SWEET_BERRY_BUSH,
        Material.WITHER_ROSE,
        Material.POWDER_SNOW,
        Material.POINTED_DRIPSTONE
    );
    
    // Safe materials for island placement
    private static final Set<Material> SAFE_MATERIALS = EnumSet.of(
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.STONE,
        Material.DEEPSLATE,
        Material.SAND,
        Material.SANDSTONE,
        Material.WATER,
        Material.AIR
    );
    
    /**
     * Creates a new location safety checker.
     */
    public LocationSafetyChecker() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "LocationSafetyChecker");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Initializes the safety checker with configuration.
     * 
     * @param configuration the location configuration
     */
    public void initialize(@NotNull LocationConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        logger.info("LocationSafetyChecker initialized with safety checks: {}", 
                   configuration.isEnableSafetyChecks());
    }
    
    /**
     * Updates the configuration.
     * 
     * @param configuration the new configuration
     */
    public void updateConfiguration(@NotNull LocationConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        
        // Clear cache when configuration changes
        if (configuration.isEnableLocationCaching()) {
            clearExpiredCacheEntries();
        } else {
            safetyCache.clear();
        }
        
        logger.info("LocationSafetyChecker configuration updated");
    }
    
    /**
     * Checks if a location is safe for island placement asynchronously.
     * 
     * @param location the location to check
     * @return CompletableFuture with safety status
     */
    @NotNull
    public CompletableFuture<Boolean> isSafeLocation(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        
        if (!configuration.isEnableSafetyChecks()) {
            return CompletableFuture.completedFuture(true);
        }
        
        // Check cache first
        if (configuration.isEnableLocationCaching()) {
            String locationKey = locationToKey(location);
            SafetyCheckResult cached = safetyCache.get(locationKey);
            if (cached != null && !cached.isExpired(configuration.getLocationCacheExpiration())) {
                return CompletableFuture.completedFuture(cached.isSafe());
            }
        }
        
        return CompletableFuture.supplyAsync(() -> {
            boolean isSafe = performSafetyCheck(location);
            
            // Cache the result
            if (configuration.isEnableLocationCaching()) {
                String locationKey = locationToKey(location);
                safetyCache.put(locationKey, new SafetyCheckResult(isSafe, LocalDateTime.now()));
            }
            
            return isSafe;
        }, executorService);
    }
    
    /**
     * Finds a safe location near the given location.
     * 
     * @param center the center location to search around
     * @return CompletableFuture with optional safe location
     */
    @NotNull
    public CompletableFuture<Optional<Location>> findSafeLocationNear(@NotNull Location center) {
        Objects.requireNonNull(center, "Center location cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            int searchRadius = configuration.getSafetyCheckRadius();
            World world = center.getWorld();
            
            // Search in expanding circles
            for (int radius = 1; radius <= searchRadius; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        // Only check locations on the edge of the current radius
                        if (Math.abs(x) != radius && Math.abs(z) != radius) {
                            continue;
                        }
                        
                        Location testLocation = new Location(world, 
                            center.getX() + x, center.getY(), center.getZ() + z);
                        
                        if (performSafetyCheck(testLocation)) {
                            logger.debug("Found safe location at {} (offset: {}, {})", 
                                       locationToKey(testLocation), x, z);
                            return Optional.of(testLocation);
                        }
                    }
                }
            }
            
            logger.warn("Could not find safe location near {}", locationToKey(center));
            return Optional.empty();
        }, executorService);
    }
    
    /**
     * Validates multiple locations for safety in batch.
     * 
     * @param locations the locations to validate
     * @return CompletableFuture with map of location to safety status
     */
    @NotNull
    public CompletableFuture<Map<Location, Boolean>> validateLocations(@NotNull List<Location> locations) {
        Objects.requireNonNull(locations, "Locations cannot be null");
        
        if (!configuration.isEnableSafetyChecks()) {
            Map<Location, Boolean> results = new HashMap<>();
            locations.forEach(loc -> results.put(loc, true));
            return CompletableFuture.completedFuture(results);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Map<Location, Boolean> results = new HashMap<>();
            
            for (Location location : locations) {
                // Check cache first
                boolean isSafe;
                if (configuration.isEnableLocationCaching()) {
                    String locationKey = locationToKey(location);
                    SafetyCheckResult cached = safetyCache.get(locationKey);
                    if (cached != null && !cached.isExpired(configuration.getLocationCacheExpiration())) {
                        isSafe = cached.isSafe();
                    } else {
                        isSafe = performSafetyCheck(location);
                        safetyCache.put(locationKey, new SafetyCheckResult(isSafe, LocalDateTime.now()));
                    }
                } else {
                    isSafe = performSafetyCheck(location);
                }
                
                results.put(location, isSafe);
            }
            
            return results;
        }, executorService);
    }
    
    /**
     * Finds the safest location from a list of candidates.
     * 
     * @param candidates the candidate locations
     * @return CompletableFuture with the safest location
     */
    @NotNull
    public CompletableFuture<Optional<Location>> findSafestLocation(@NotNull List<Location> candidates) {
        Objects.requireNonNull(candidates, "Candidates cannot be null");
        
        if (candidates.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            Location safestLocation = null;
            int highestSafetyScore = -1;
            
            for (Location candidate : candidates) {
                int safetyScore = calculateSafetyScore(candidate);
                if (safetyScore > highestSafetyScore) {
                    highestSafetyScore = safetyScore;
                    safestLocation = candidate;
                }
            }
            
            return Optional.ofNullable(safestLocation);
        }, executorService);
    }
    
    /**
     * Clears the safety check cache.
     */
    public void clearCache() {
        safetyCache.clear();
        logger.debug("Safety check cache cleared");
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return cache statistics
     */
    @NotNull
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            safetyCache.size(),
            (int) safetyCache.values().stream()
                .filter(result -> result.isExpired(configuration.getLocationCacheExpiration()))
                .count()
        );
    }
    
    /**
     * Performs the actual safety check for a location.
     * 
     * @param location the location to check
     * @return true if the location is safe
     */
    private boolean performSafetyCheck(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Check the immediate area around the location
        int checkRadius = Math.max(1, configuration.getSafetyCheckRadius());
        
        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dy = -checkRadius; dy <= checkRadius; dy++) {
                for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    Material material = block.getType();
                    
                    // Check for unsafe materials
                    if (UNSAFE_MATERIALS.contains(material)) {
                        logger.debug("Unsafe material {} found at {}", material, 
                                   locationToKey(block.getLocation()));
                        return false;
                    }
                    
                    // Check for dangerous block states
                    if (isDangerousBlock(block)) {
                        logger.debug("Dangerous block state found at {}", 
                                   locationToKey(block.getLocation()));
                        return false;
                    }
                }
            }
        }
        
        // Check for adequate space above the location
        if (!hasAdequateSpace(location)) {
            logger.debug("Inadequate space above location {}", locationToKey(location));
            return false;
        }
        
        // Check for solid ground below
        if (!hasSolidGround(location)) {
            logger.debug("No solid ground below location {}", locationToKey(location));
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculates a safety score for a location (higher is safer).
     * 
     * @param location the location to score
     * @return the safety score
     */
    private int calculateSafetyScore(@NotNull Location location) {
        int score = 0;
        World world = location.getWorld();
        if (world == null) {
            return -1000;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Check surrounding blocks
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    Material material = block.getType();
                    
                    if (SAFE_MATERIALS.contains(material)) {
                        score += 1;
                    } else if (UNSAFE_MATERIALS.contains(material)) {
                        score -= 10;
                    }
                    
                    // Bonus for air blocks above (good for building)
                    if (dy > 0 && material == Material.AIR) {
                        score += 2;
                    }
                    
                    // Bonus for solid blocks below (good foundation)
                    if (dy < 0 && material.isSolid()) {
                        score += 3;
                    }
                }
            }
        }
        
        // Bonus for being at a reasonable height
        if (y >= 60 && y <= 80) {
            score += 10;
        }
        
        // Penalty for being too high or too low
        if (y < 10 || y > 200) {
            score -= 20;
        }
        
        return score;
    }
    
    /**
     * Checks if a block is dangerous due to its state.
     * 
     * @param block the block to check
     * @return true if the block is dangerous
     */
    private boolean isDangerousBlock(@NotNull Block block) {
        Material material = block.getType();
        
        // Check for flowing liquids
        if (material == Material.WATER || material == Material.LAVA) {
            // Check if it's a source block (safer) or flowing (dangerous)
            return block.getBlockData().getAsString().contains("level=");
        }
        
        // Check for unstable blocks that might fall
        if (material == Material.SAND || material == Material.GRAVEL || 
            material == Material.RED_SAND || material == Material.ANVIL) {
            // Check if there's support below
            Block below = block.getRelative(0, -1, 0);
            return !below.getType().isSolid();
        }
        
        return false;
    }
    
    /**
     * Checks if there's adequate space above a location.
     * 
     * @param location the location to check
     * @return true if there's adequate space
     */
    private boolean hasAdequateSpace(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        // Check for at least 3 blocks of air above
        for (int dy = 1; dy <= 3; dy++) {
            Block block = world.getBlockAt(location.getBlockX(), 
                                         location.getBlockY() + dy, 
                                         location.getBlockZ());
            if (block.getType() != Material.AIR) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if there's solid ground below a location.
     * 
     * @param location the location to check
     * @return true if there's solid ground
     */
    private boolean hasSolidGround(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        // Check for solid blocks within 5 blocks below
        for (int dy = 0; dy >= -5; dy--) {
            Block block = world.getBlockAt(location.getBlockX(), 
                                         location.getBlockY() + dy, 
                                         location.getBlockZ());
            if (block.getType().isSolid() && !UNSAFE_MATERIALS.contains(block.getType())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Converts a location to a cache key.
     * 
     * @param location the location
     * @return the cache key
     */
    @NotNull
    private String locationToKey(@NotNull Location location) {
        return String.format("%s:%d:%d:%d", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
    
    /**
     * Clears expired cache entries.
     */
    private void clearExpiredCacheEntries() {
        LocalDateTime now = LocalDateTime.now();
        safetyCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(configuration.getLocationCacheExpiration()));
    }
    
    /**
     * Represents a cached safety check result.
     */
    private static class SafetyCheckResult {
        @Getter
        private final boolean safe;
        private final LocalDateTime timestamp;
        
        public SafetyCheckResult(boolean safe, @NotNull LocalDateTime timestamp) {
            this.safe = safe;
            this.timestamp = timestamp;
        }
        
        public boolean isExpired(@NotNull java.time.Duration maxAge) {
            return LocalDateTime.now().isAfter(timestamp.plus(maxAge));
        }
    }
    
    /**
     * Cache statistics.
     */
    public static class CacheStatistics {
        @Getter
        private final int totalEntries;
        @Getter
        private final int expiredEntries;
        
        public CacheStatistics(int totalEntries, int expiredEntries) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
        }
        
        public int getActiveEntries() {
            return totalEntries - expiredEntries;
        }
        
        public double getHitRatio() {
            return totalEntries > 0 ? (double) getActiveEntries() / totalEntries : 0.0;
        }
    }
}