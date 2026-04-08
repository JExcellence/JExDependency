package de.jexcellence.oneblock.manager.location;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.manager.config.LocationConfiguration;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced island location calculator with optimized spiral placement algorithm.
 * Provides efficient coordinate calculation and location availability checking.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class IslandLocationCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(IslandLocationCalculator.class);
    
    private final OneblockIslandRepository islandRepository;
    
    @Getter
    private LocationConfiguration configuration;
    
    // Spiral calculation state per world
    private final Map<String, SpiralState> worldSpiralStates = new ConcurrentHashMap<>();
    
    // Location availability cache
    private final Map<String, Boolean> availabilityCache = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final AtomicInteger calculationsPerformed = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    
    /**
     * Creates a new island location calculator.
     * 
     * @param islandRepository the island repository for checking existing islands
     */
    public IslandLocationCalculator(@NotNull OneblockIslandRepository islandRepository) {
        this.islandRepository = Objects.requireNonNull(islandRepository, "Island repository cannot be null");
    }
    
    /**
     * Initializes the calculator with configuration.
     * 
     * @param configuration the location configuration
     */
    public void initialize(@NotNull LocationConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        logger.info("IslandLocationCalculator initialized with spiral step size: {}", 
                   configuration.getSpiralStepSize());
    }
    
    /**
     * Updates the configuration.
     * 
     * @param configuration the new configuration
     */
    public void updateConfiguration(@NotNull LocationConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        
        // Clear caches when configuration changes
        availabilityCache.clear();
        
        logger.info("IslandLocationCalculator configuration updated");
    }
    
    /**
     * Finds the next available location using the optimized spiral algorithm.
     * 
     * @param world the world to find location in
     * @param startLocation the starting location for spiral calculation
     * @return the next available location
     */
    @NotNull
    public Location findNextAvailableLocation(@NotNull World world, @NotNull Location startLocation) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(startLocation, "Start location cannot be null");
        
        SpiralState spiralState = worldSpiralStates.computeIfAbsent(world.getName(), 
            k -> new SpiralState(startLocation));
        
        Location nextLocation;
        int attempts = 0;
        int maxAttempts = calculateMaxAttempts();
        
        do {
            nextLocation = calculateNextSpiralLocation(world, spiralState);
            attempts++;
            
            if (attempts > maxAttempts) {
                logger.warn("Exceeded maximum attempts ({}) to find available location in world {}", 
                           maxAttempts, world.getName());
                break;
            }
            
        } while (!isLocationAvailable(nextLocation));
        
        calculationsPerformed.incrementAndGet();
        
        logger.debug("Found available location {} after {} attempts", 
                    locationToKey(nextLocation), attempts);
        
        return nextLocation;
    }
    
    /**
     * Calculates the next location in the spiral pattern.
     * 
     * @param world the world
     * @param startLocation the starting location
     * @return the next location in the spiral
     */
    @NotNull
    public Location calculateNextLocation(@NotNull World world, @NotNull Location startLocation) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(startLocation, "Start location cannot be null");
        
        SpiralState spiralState = worldSpiralStates.computeIfAbsent(world.getName(), 
            k -> new SpiralState(startLocation));
        
        return calculateNextSpiralLocation(world, spiralState);
    }
    
    /**
     * Checks if a location is available for island placement.
     * 
     * @param location the location to check
     * @return true if the location is available
     */
    public boolean isLocationAvailable(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        
        String locationKey = locationToKey(location);
        
        // Check cache first
        if (configuration.isEnableLocationCaching()) {
            Boolean cached = availabilityCache.get(locationKey);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
        }
        
        boolean available = checkLocationAvailability(location);
        
        // Cache the result
        if (configuration.isEnableLocationCaching()) {
            availabilityCache.put(locationKey, available);
        }
        
        return available;
    }
    
    /**
     * Finds multiple available locations efficiently.
     * 
     * @param world the world to find locations in
     * @param startLocation the starting location
     * @param count the number of locations needed
     * @return list of available locations
     */
    @NotNull
    public List<Location> findMultipleAvailableLocations(@NotNull World world, 
                                                         @NotNull Location startLocation, 
                                                         int count) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(startLocation, "Start location cannot be null");
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        List<Location> locations = new ArrayList<>();
        SpiralState spiralState = worldSpiralStates.computeIfAbsent(world.getName(), 
            k -> new SpiralState(startLocation));
        
        int attempts = 0;
        int maxAttempts = calculateMaxAttempts() * count;
        
        while (locations.size() < count && attempts < maxAttempts) {
            Location nextLocation = calculateNextSpiralLocation(world, spiralState);
            attempts++;
            
            if (isLocationAvailable(nextLocation)) {
                locations.add(nextLocation);
            }
        }
        
        calculationsPerformed.addAndGet(attempts);
        
        logger.debug("Found {} available locations after {} attempts", locations.size(), attempts);
        
        return locations;
    }
    
    /**
     * Calculates the optimal location for a specific island index.
     * 
     * @param world the world
     * @param startLocation the starting location
     * @param islandIndex the island index (0-based)
     * @return the calculated location
     */
    @NotNull
    public Location calculateLocationForIndex(@NotNull World world, 
                                            @NotNull Location startLocation, 
                                            int islandIndex) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(startLocation, "Start location cannot be null");
        if (islandIndex < 0) {
            throw new IllegalArgumentException("Island index cannot be negative");
        }
        
        // Calculate spiral coordinates for the given index
        SpiralCoordinate coord = calculateSpiralCoordinate(islandIndex);
        
        int stepSize = configuration.getSpiralStepSize(world.getName());
        double x = startLocation.getX() + (coord.x * stepSize);
        double z = startLocation.getZ() + (coord.z * stepSize);
        
        // Find appropriate Y coordinate
        int y = findOptimalYCoordinate(world, (int) x, (int) z);
        
        return new Location(world, x, y, z);
    }
    
    /**
     * Gets the current spiral state for a world.
     * 
     * @param worldName the world name
     * @return the spiral state, or null if not initialized
     */
    @NotNull
    public Optional<SpiralState> getSpiralState(@NotNull String worldName) {
        Objects.requireNonNull(worldName, "World name cannot be null");
        return Optional.ofNullable(worldSpiralStates.get(worldName));
    }
    
    /**
     * Resets the spiral state for a world.
     * 
     * @param worldName the world name
     * @param startLocation the new starting location
     */
    public void resetSpiralState(@NotNull String worldName, @NotNull Location startLocation) {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(startLocation, "Start location cannot be null");
        
        worldSpiralStates.put(worldName, new SpiralState(startLocation));
        logger.info("Reset spiral state for world {} to {}", worldName, locationToKey(startLocation));
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return the performance statistics
     */
    @NotNull
    public PerformanceStatistics getPerformanceStatistics() {
        return new PerformanceStatistics(
            calculationsPerformed.get(),
            cacheHits.get(),
            availabilityCache.size(),
            worldSpiralStates.size()
        );
    }
    
    /**
     * Clears all caches and resets statistics.
     */
    public void clearCaches() {
        availabilityCache.clear();
        calculationsPerformed.set(0);
        cacheHits.set(0);
        logger.info("Cleared location calculator caches and statistics");
    }
    
    /**
     * Calculates the next spiral location based on the current state.
     * 
     * @param world the world
     * @param spiralState the current spiral state
     * @return the next location in the spiral
     */
    @NotNull
    private Location calculateNextSpiralLocation(@NotNull World world, @NotNull SpiralState spiralState) {
        SpiralCoordinate coord = spiralState.getNextCoordinate();
        
        int stepSize = configuration.getSpiralStepSize(world.getName());
        double x = spiralState.getStartLocation().getX() + (coord.x * stepSize);
        double z = spiralState.getStartLocation().getZ() + (coord.z * stepSize);
        
        // Find appropriate Y coordinate
        int y = findOptimalYCoordinate(world, (int) x, (int) z);
        
        return new Location(world, x, y, z);
    }
    
    /**
     * Calculates spiral coordinate for a given index.
     * 
     * @param index the index
     * @return the spiral coordinate
     */
    @NotNull
    private SpiralCoordinate calculateSpiralCoordinate(int index) {
        if (index == 0) {
            return new SpiralCoordinate(0, 0);
        }
        
        // Determine which ring the index is in
        int ring = (int) Math.ceil((Math.sqrt(index) - 1) / 2);
        int ringStart = (2 * ring - 1) * (2 * ring - 1);
        int positionInRing = index - ringStart;
        int sideLength = 2 * ring;
        
        int x, z;
        
        if (positionInRing < sideLength) {
            // Right side
            x = ring;
            z = -ring + positionInRing;
        } else if (positionInRing < 2 * sideLength) {
            // Top side
            x = ring - (positionInRing - sideLength);
            z = ring;
        } else if (positionInRing < 3 * sideLength) {
            // Left side
            x = -ring;
            z = ring - (positionInRing - 2 * sideLength);
        } else {
            // Bottom side
            x = -ring + (positionInRing - 3 * sideLength);
            z = -ring;
        }
        
        return new SpiralCoordinate(x, z);
    }
    
    /**
     * Checks if a location is actually available.
     * 
     * @param location the location to check
     * @return true if available
     */
    private boolean checkLocationAvailability(@NotNull Location location) {
        // Check if there's already an island at this location
        Optional<OneblockIsland> existingIsland = islandRepository.findByLocation(location);
        if (existingIsland.isPresent()) {
            return false;
        }
        
        // Check minimum distance from other islands
        int minimumDistance = configuration.getMinimumIslandDistance(location.getWorld().getName());
        List<OneblockIsland> nearbyIslands = islandRepository.findByLocationNear(location, minimumDistance);
        
        return nearbyIslands.isEmpty();
    }
    
    /**
     * Finds the optimal Y coordinate for a given X,Z position.
     * 
     * @param world the world
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return the optimal Y coordinate
     */
    private int findOptimalYCoordinate(@NotNull World world, int x, int z) {
        // Start from a reasonable height and work down to find solid ground
        int startY = 80;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        
        // Clamp start Y to world bounds
        startY = Math.max(minY, Math.min(maxY, startY));
        
        // Look for solid ground
        for (int y = startY; y >= minY; y--) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                // Found solid ground, place island a few blocks above
                return Math.min(maxY - 10, y + 3);
            }
        }
        
        // If no solid ground found, use a default height
        return Math.min(maxY - 10, 64);
    }
    
    /**
     * Calculates maximum attempts based on configuration.
     * 
     * @return the maximum attempts
     */
    private int calculateMaxAttempts() {
        int searchRadius = configuration.getMaximumSearchRadius();
        int stepSize = configuration.getSpiralStepSize();
        
        // Estimate number of positions in the search area
        int estimatedPositions = (int) Math.pow(searchRadius / stepSize, 2);
        
        // Allow up to 10% of estimated positions as attempts
        return Math.max(100, estimatedPositions / 10);
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
     * Represents a coordinate in the spiral pattern.
     */
    private static class SpiralCoordinate {
        final int x;
        final int z;
        
        SpiralCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public String toString() {
            return String.format("(%d, %d)", x, z);
        }
    }
    
    /**
     * Maintains the state of spiral calculation for a world.
     */
    public static class SpiralState {
        @Getter
        private final Location startLocation;
        private int currentIndex = 0;
        private int currentRing = 0;
        private int positionInRing = 0;
        private Direction currentDirection = Direction.RIGHT;
        private int stepsInCurrentDirection = 0;
        private int maxStepsInCurrentDirection = 1;
        
        public SpiralState(@NotNull Location startLocation) {
            this.startLocation = startLocation.clone();
        }
        
        /**
         * Gets the next coordinate in the spiral.
         * 
         * @return the next spiral coordinate
         */
        @NotNull
        public SpiralCoordinate getNextCoordinate() {
            if (currentIndex == 0) {
                currentIndex++;
                return new SpiralCoordinate(0, 0);
            }
            
            SpiralCoordinate coord = calculateCurrentCoordinate();
            advancePosition();
            
            return coord;
        }
        
        /**
         * Gets the current index in the spiral.
         * 
         * @return the current index
         */
        public int getCurrentIndex() {
            return currentIndex;
        }
        
        /**
         * Calculates the current coordinate based on the state.
         * 
         * @return the current coordinate
         */
        @NotNull
        private SpiralCoordinate calculateCurrentCoordinate() {
            int ring = (int) Math.ceil((Math.sqrt(currentIndex) - 1) / 2);
            int ringStart = (2 * ring - 1) * (2 * ring - 1);
            int positionInRing = currentIndex - ringStart;
            int sideLength = 2 * ring;
            
            int x, z;
            
            if (positionInRing < sideLength) {
                // Right side
                x = ring;
                z = -ring + positionInRing;
            } else if (positionInRing < 2 * sideLength) {
                // Top side
                x = ring - (positionInRing - sideLength);
                z = ring;
            } else if (positionInRing < 3 * sideLength) {
                // Left side
                x = -ring;
                z = ring - (positionInRing - 2 * sideLength);
            } else {
                // Bottom side
                x = -ring + (positionInRing - 3 * sideLength);
                z = -ring;
            }
            
            return new SpiralCoordinate(x, z);
        }
        
        /**
         * Advances to the next position in the spiral.
         */
        private void advancePosition() {
            currentIndex++;
            stepsInCurrentDirection++;
            
            // Check if we need to change direction
            if (stepsInCurrentDirection >= maxStepsInCurrentDirection) {
                currentDirection = currentDirection.next();
                stepsInCurrentDirection = 0;
                
                // Increase max steps every two direction changes
                if (currentDirection == Direction.RIGHT || currentDirection == Direction.LEFT) {
                    maxStepsInCurrentDirection++;
                }
            }
        }
        
        /**
         * Direction enumeration for spiral traversal.
         */
        private enum Direction {
            RIGHT, UP, LEFT, DOWN;
            
            public Direction next() {
                return values()[(ordinal() + 1) % values().length];
            }
        }
    }
    
    /**
     * Performance statistics for the location calculator.
     */
    public static class PerformanceStatistics {
        @Getter
        private final int totalCalculations;
        @Getter
        private final int cacheHits;
        @Getter
        private final int cacheSize;
        @Getter
        private final int activeWorlds;
        
        public PerformanceStatistics(int totalCalculations, int cacheHits, int cacheSize, int activeWorlds) {
            this.totalCalculations = totalCalculations;
            this.cacheHits = cacheHits;
            this.cacheSize = cacheSize;
            this.activeWorlds = activeWorlds;
        }
        
        public double getCacheHitRatio() {
            return totalCalculations > 0 ? (double) cacheHits / totalCalculations : 0.0;
        }
        
        public int getCacheMisses() {
            return totalCalculations - cacheHits;
        }
    }
}