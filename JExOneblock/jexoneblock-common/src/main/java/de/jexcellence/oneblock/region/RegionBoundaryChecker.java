package de.jexcellence.oneblock.region;

import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * High-performance boundary checker with caching for real-time validation.
 * Provides fast location validation and boundary checking for island regions.
 */
public class RegionBoundaryChecker {

    private static final long CACHE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(5); // 5 minutes cache
    private static final int MAX_CACHE_SIZE = 10000;
    private static final double BOUNDARY_WARNING_THRESHOLD = 10.0; // Blocks from boundary

    // Cache for boundary check results
    private final Map<LocationCacheKey, BoundaryCheckResult> boundaryCache = new ConcurrentHashMap<>();
    private final Map<UUID, IslandRegion> regionCache = new ConcurrentHashMap<>();
    
    // Performance metrics
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long totalChecks = 0;

    /**
     * Cache key for location-based boundary checks.
     */
    private static class LocationCacheKey {
        private final String worldName;
        private final int blockX;
        private final int blockZ;
        private final UUID regionId;
        private final long timestamp;

        public LocationCacheKey(@NotNull Location location, @NotNull UUID regionId) {
            this.worldName = location.getWorld().getName();
            this.blockX = location.getBlockX();
            this.blockZ = location.getBlockZ();
            this.regionId = regionId;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocationCacheKey that = (LocationCacheKey) o;
            return blockX == that.blockX &&
                   blockZ == that.blockZ &&
                   worldName.equals(that.worldName) &&
                   regionId.equals(that.regionId);
        }

        @Override
        public int hashCode() {
            return worldName.hashCode() ^ (blockX * 31) ^ (blockZ * 17) ^ regionId.hashCode();
        }
    }

    /**
     * Result of a boundary check operation.
     */
    public static class BoundaryCheckResult {
        private final boolean withinBoundaries;
        private final double distanceFromCenter;
        private final double distanceFromBoundary;
        private final boolean nearBoundary;
        private final BoundaryViolationType violationType;
        private final long timestamp;

        public BoundaryCheckResult(boolean withinBoundaries, double distanceFromCenter, 
                                 double distanceFromBoundary, boolean nearBoundary,
                                 @Nullable BoundaryViolationType violationType) {
            this.withinBoundaries = withinBoundaries;
            this.distanceFromCenter = distanceFromCenter;
            this.distanceFromBoundary = distanceFromBoundary;
            this.nearBoundary = nearBoundary;
            this.violationType = violationType;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public boolean isWithinBoundaries() { return withinBoundaries; }
        public double getDistanceFromCenter() { return distanceFromCenter; }
        public double getDistanceFromBoundary() { return distanceFromBoundary; }
        public boolean isNearBoundary() { return nearBoundary; }
        public Optional<BoundaryViolationType> getViolationType() { return Optional.ofNullable(violationType); }
        public long getTimestamp() { return timestamp; }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    /**
     * Types of boundary violations.
     */
    public enum BoundaryViolationType {
        OUTSIDE_REGION("Outside island region"),
        WRONG_WORLD("Wrong world"),
        NO_REGION("No region found"),
        REGION_DISABLED("Region protection disabled");

        private final String description;

        BoundaryViolationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Checks if a location is within the boundaries of an island region.
     */
    @NotNull
    public BoundaryCheckResult checkBoundaries(@NotNull Location location, @NotNull IslandRegion region) {
        totalChecks++;

        // Check cache first
        LocationCacheKey cacheKey = new LocationCacheKey(location, region.getIslandId());
        BoundaryCheckResult cached = boundaryCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            cacheHits++;
            return cached;
        }

        cacheMisses++;

        // Perform actual boundary check
        BoundaryCheckResult result = performBoundaryCheck(location, region);
        
        // Cache the result (with size limit)
        if (boundaryCache.size() < MAX_CACHE_SIZE) {
            boundaryCache.put(cacheKey, result);
        }

        return result;
    }

    /**
     * Performs the actual boundary check without caching.
     */
    @NotNull
    private BoundaryCheckResult performBoundaryCheck(@NotNull Location location, @NotNull IslandRegion region) {
        // Check if region protection is enabled
        if (!region.isProtectionEnabled()) {
            return new BoundaryCheckResult(true, 0, 0, false, BoundaryViolationType.REGION_DISABLED);
        }

        // Check if location is in the same world
        if (!location.getWorld().getName().equals(region.getWorldName())) {
            return new BoundaryCheckResult(false, Double.MAX_VALUE, Double.MAX_VALUE, false, 
                                         BoundaryViolationType.WRONG_WORLD);
        }

        // Calculate distances
        double distanceFromCenter = region.getDistanceFromCenter(location);
        double distanceFromBoundary = Math.abs(distanceFromCenter - region.getRadius());
        boolean nearBoundary = distanceFromBoundary <= BOUNDARY_WARNING_THRESHOLD;

        // Check if within boundaries
        boolean withinBoundaries = region.isWithinBoundaries(location);
        BoundaryViolationType violationType = withinBoundaries ? null : BoundaryViolationType.OUTSIDE_REGION;

        return new BoundaryCheckResult(withinBoundaries, distanceFromCenter, distanceFromBoundary, 
                                     nearBoundary, violationType);
    }

    /**
     * Quick check if a location is within any cached region boundaries.
     */
    public boolean isWithinAnyRegion(@NotNull Location location) {
        return regionCache.values().stream()
                .anyMatch(region -> checkBoundaries(location, region).isWithinBoundaries());
    }

    /**
     * Finds the region that contains the given location.
     */
    @Nullable
    public IslandRegion findRegionAt(@NotNull Location location) {
        return regionCache.values().stream()
                .filter(region -> checkBoundaries(location, region).isWithinBoundaries())
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a player is within their own island boundaries.
     */
    @NotNull
    public BoundaryCheckResult checkPlayerBoundaries(@NotNull Player player, @NotNull IslandRegion region) {
        return checkBoundaries(player.getLocation(), region);
    }

    /**
     * Validates if a player can perform an action at a specific location.
     */
    public boolean canPerformAction(@NotNull Player player, @NotNull Location location, 
                                  @NotNull IslandRegion region, @NotNull String action) {
        BoundaryCheckResult boundaryCheck = checkBoundaries(location, region);
        
        if (!boundaryCheck.isWithinBoundaries()) {
            return false;
        }

        // Check if player has permission for this action
        return region.hasPermission(player.getUniqueId(), action);
    }

    /**
     * Gets a warning message for players near boundaries.
     */
    @Nullable
    public String getBoundaryWarning(@NotNull BoundaryCheckResult result) {
        if (!result.isWithinBoundaries()) {
            return result.getViolationType()
                    .map(type -> "You are " + type.getDescription().toLowerCase())
                    .orElse("You are outside the allowed area");
        }

        if (result.isNearBoundary()) {
            return String.format("Warning: You are %.1f blocks from the island boundary", 
                               result.getDistanceFromBoundary());
        }

        return null;
    }

    /**
     * Caches a region for faster lookups.
     */
    public void cacheRegion(@NotNull IslandRegion region) {
        regionCache.put(region.getIslandId(), region);
    }

    /**
     * Removes a region from cache.
     */
    public void uncacheRegion(@NotNull UUID islandId) {
        regionCache.remove(islandId);
        
        // Remove related boundary cache entries
        boundaryCache.entrySet().removeIf(entry -> entry.getKey().regionId.equals(islandId));
    }

    /**
     * Clears expired cache entries.
     */
    public void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        
        // Clean boundary cache
        boundaryCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > CACHE_EXPIRY_MS);
        
        // Clean location cache
        boundaryCache.entrySet().removeIf(entry -> entry.getKey().isExpired());
    }

    /**
     * Gets cache performance statistics.
     */
    @NotNull
    public CacheStatistics getCacheStatistics() {
        double hitRate = totalChecks > 0 ? (double) cacheHits / totalChecks * 100 : 0;
        return new CacheStatistics(cacheHits, cacheMisses, totalChecks, hitRate, 
                                 boundaryCache.size(), regionCache.size());
    }

    /**
     * Cache performance statistics.
     */
    public static class CacheStatistics {
        private final long cacheHits;
        private final long cacheMisses;
        private final long totalChecks;
        private final double hitRate;
        private final int boundaryCacheSize;
        private final int regionCacheSize;

        public CacheStatistics(long cacheHits, long cacheMisses, long totalChecks, double hitRate,
                             int boundaryCacheSize, int regionCacheSize) {
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.totalChecks = totalChecks;
            this.hitRate = hitRate;
            this.boundaryCacheSize = boundaryCacheSize;
            this.regionCacheSize = regionCacheSize;
        }

        // Getters
        public long getCacheHits() { return cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public long getTotalChecks() { return totalChecks; }
        public double getHitRate() { return hitRate; }
        public int getBoundaryCacheSize() { return boundaryCacheSize; }
        public int getRegionCacheSize() { return regionCacheSize; }

        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, total=%d, hitRate=%.1f%%, " +
                               "boundaryCache=%d, regionCache=%d}", 
                               cacheHits, cacheMisses, totalChecks, hitRate, 
                               boundaryCacheSize, regionCacheSize);
        }
    }

    /**
     * Resets all performance counters.
     */
    public void resetStatistics() {
        cacheHits = 0;
        cacheMisses = 0;
        totalChecks = 0;
    }

    /**
     * Clears all caches.
     */
    public void clearAllCaches() {
        boundaryCache.clear();
        regionCache.clear();
        resetStatistics();
    }

    /**
     * Gets the current cache size limits.
     */
    public int getMaxCacheSize() {
        return MAX_CACHE_SIZE;
    }

    /**
     * Gets the cache expiry time in milliseconds.
     */
    public long getCacheExpiryMs() {
        return CACHE_EXPIRY_MS;
    }

    /**
     * Gets the boundary warning threshold.
     */
    public double getBoundaryWarningThreshold() {
        return BOUNDARY_WARNING_THRESHOLD;
    }
}