package de.jexcellence.oneblock.region;

import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates island positions using an Archimedean spiral algorithm.
 * This ensures optimal spacing between islands and prevents overlap.
 */
public class SpiralIslandGenerator {

    private static final double DEFAULT_SPIRAL_SPACING = 200.0; // Distance between spiral arms
    private static final double DEFAULT_ISLAND_SPACING = 300.0; // Minimum distance between islands
    private static final int DEFAULT_ISLAND_RADIUS = 100; // Default island radius
    private static final int MAX_SPIRAL_ATTEMPTS = 1000; // Maximum attempts to find a valid position

    private final Map<String, AtomicInteger> worldPositionCounters = new ConcurrentHashMap<>();
    private final Map<String, Set<SpiralPosition>> occupiedPositions = new ConcurrentHashMap<>();
    private final Map<String, SpiralConfiguration> worldConfigurations = new ConcurrentHashMap<>();

    /**
     * Configuration for spiral generation in a specific world.
     */
    public static class SpiralConfiguration {
        private final double spiralSpacing;
        private final double islandSpacing;
        private final int defaultRadius;
        private final Location centerPoint;
        private final int maxIslands;

        public SpiralConfiguration(double spiralSpacing, double islandSpacing, int defaultRadius, 
                                 @NotNull Location centerPoint, int maxIslands) {
            this.spiralSpacing = spiralSpacing;
            this.islandSpacing = islandSpacing;
            this.defaultRadius = defaultRadius;
            this.centerPoint = centerPoint;
            this.maxIslands = maxIslands;
        }

        public static SpiralConfiguration defaultConfig(@NotNull World world) {
            Location center = new Location(world, 0, 100, 0);
            return new SpiralConfiguration(DEFAULT_SPIRAL_SPACING, DEFAULT_ISLAND_SPACING, 
                                         DEFAULT_ISLAND_RADIUS, center, 10000);
        }

        // Getters
        public double getSpiralSpacing() { return spiralSpacing; }
        public double getIslandSpacing() { return islandSpacing; }
        public int getDefaultRadius() { return defaultRadius; }
        public Location getCenterPoint() { return centerPoint; }
        public int getMaxIslands() { return maxIslands; }
    }

    /**
     * Represents a position in the spiral with coordinates and metadata.
     */
    public static class SpiralPosition {
        private final int position;
        private final double x;
        private final double z;
        private final double angle;
        private final double radius;

        public SpiralPosition(int position, double x, double z, double angle, double radius) {
            this.position = position;
            this.x = x;
            this.z = z;
            this.angle = angle;
            this.radius = radius;
        }

        public int getPosition() { return position; }
        public double getX() { return x; }
        public double getZ() { return z; }
        public double getAngle() { return angle; }
        public double getRadius() { return radius; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpiralPosition that = (SpiralPosition) o;
            return position == that.position;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position);
        }

        @Override
        public String toString() {
            return String.format("SpiralPosition{pos=%d, x=%.2f, z=%.2f, angle=%.2f, radius=%.2f}", 
                               position, x, z, angle, radius);
        }
    }

    /**
     * Configures spiral generation for a specific world.
     */
    public void configureWorld(@NotNull String worldName, @NotNull SpiralConfiguration config) {
        worldConfigurations.put(worldName, config);
        worldPositionCounters.putIfAbsent(worldName, new AtomicInteger(0));
        occupiedPositions.putIfAbsent(worldName, ConcurrentHashMap.newKeySet());
    }

    /**
     * Generates the next available island position in the spiral.
     */
    @Nullable
    public Location generateNextPosition(@NotNull World world, int islandRadius) {
        String worldName = world.getName();
        SpiralConfiguration config = getOrCreateConfiguration(world);
        
        // Ensure world is initialized
        AtomicInteger counter = worldPositionCounters.computeIfAbsent(worldName, k -> new AtomicInteger(0));
        Set<SpiralPosition> occupied = occupiedPositions.computeIfAbsent(worldName, k -> ConcurrentHashMap.newKeySet());

        // Try to find a valid position
        for (int attempt = 0; attempt < MAX_SPIRAL_ATTEMPTS; attempt++) {
            int position = counter.getAndIncrement();
            
            if (position >= config.getMaxIslands()) {
                // Reached maximum islands for this world
                return null;
            }

            SpiralPosition spiralPos = calculateSpiralPosition(position, config);
            
            if (isPositionValid(spiralPos, occupied, config, islandRadius)) {
                occupied.add(spiralPos);
                
                Location location = new Location(world, 
                    config.getCenterPoint().getX() + spiralPos.getX(),
                    config.getCenterPoint().getY(),
                    config.getCenterPoint().getZ() + spiralPos.getZ());
                
                return location;
            }
        }

        return null; // Could not find a valid position
    }

    /**
     * Calculates the spiral position for a given position number.
     */
    @NotNull
    private SpiralPosition calculateSpiralPosition(int position, @NotNull SpiralConfiguration config) {
        if (position == 0) {
            // First island at center
            return new SpiralPosition(0, 0, 0, 0, 0);
        }

        // Archimedean spiral: r = a * θ
        // Where 'a' controls the spacing between spiral arms
        double a = config.getSpiralSpacing() / (2 * Math.PI);
        
        // Calculate angle based on position
        // We want roughly equal spacing along the spiral
        double theta = Math.sqrt(position * 2 * Math.PI * a / config.getIslandSpacing());
        
        // Calculate radius
        double radius = a * theta;
        
        // Convert to Cartesian coordinates
        double x = radius * Math.cos(theta);
        double z = radius * Math.sin(theta);

        return new SpiralPosition(position, x, z, theta, radius);
    }

    /**
     * Checks if a spiral position is valid (no collisions with existing islands).
     */
    private boolean isPositionValid(@NotNull SpiralPosition position, @NotNull Set<SpiralPosition> occupied,
                                  @NotNull SpiralConfiguration config, int islandRadius) {
        double minDistance = config.getIslandSpacing() + islandRadius;
        
        for (SpiralPosition occupiedPos : occupied) {
            double distance = calculateDistance(position, occupiedPos);
            if (distance < minDistance) {
                return false; // Too close to existing island
            }
        }
        
        return true;
    }

    /**
     * Calculates the distance between two spiral positions.
     */
    private double calculateDistance(@NotNull SpiralPosition pos1, @NotNull SpiralPosition pos2) {
        double deltaX = pos1.getX() - pos2.getX();
        double deltaZ = pos1.getZ() - pos2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    /**
     * Gets or creates a default configuration for a world.
     */
    @NotNull
    private SpiralConfiguration getOrCreateConfiguration(@NotNull World world) {
        return worldConfigurations.computeIfAbsent(world.getName(), 
            k -> SpiralConfiguration.defaultConfig(world));
    }

    /**
     * Reserves a specific spiral position (used when loading existing islands).
     */
    public void reservePosition(@NotNull String worldName, int spiralPosition) {
        SpiralConfiguration config = worldConfigurations.get(worldName);
        if (config == null) {
            return; // World not configured
        }

        SpiralPosition position = calculateSpiralPosition(spiralPosition, config);
        occupiedPositions.computeIfAbsent(worldName, k -> ConcurrentHashMap.newKeySet()).add(position);
        
        // Update counter to ensure we don't reuse this position
        AtomicInteger counter = worldPositionCounters.get(worldName);
        if (counter != null) {
            counter.updateAndGet(current -> Math.max(current, spiralPosition + 1));
        }
    }

    /**
     * Gets the spiral position for given coordinates.
     */
    @Nullable
    public SpiralPosition getSpiralPositionAt(@NotNull Location location) {
        String worldName = location.getWorld().getName();
        SpiralConfiguration config = worldConfigurations.get(worldName);
        if (config == null) {
            return null;
        }

        Set<SpiralPosition> occupied = occupiedPositions.get(worldName);
        if (occupied == null) {
            return null;
        }

        double targetX = location.getX() - config.getCenterPoint().getX();
        double targetZ = location.getZ() - config.getCenterPoint().getZ();

        // Find the closest spiral position
        return occupied.stream()
            .min(Comparator.comparingDouble(pos -> {
                double deltaX = pos.getX() - targetX;
                double deltaZ = pos.getZ() - targetZ;
                return deltaX * deltaX + deltaZ * deltaZ;
            }))
            .orElse(null);
    }

    /**
     * Gets statistics about spiral generation for a world.
     */
    @NotNull
    public SpiralStatistics getStatistics(@NotNull String worldName) {
        AtomicInteger counter = worldPositionCounters.get(worldName);
        Set<SpiralPosition> occupied = occupiedPositions.get(worldName);
        SpiralConfiguration config = worldConfigurations.get(worldName);

        int totalGenerated = counter != null ? counter.get() : 0;
        int occupiedCount = occupied != null ? occupied.size() : 0;
        int maxIslands = config != null ? config.getMaxIslands() : 0;

        return new SpiralStatistics(totalGenerated, occupiedCount, maxIslands);
    }

    /**
     * Statistics about spiral generation.
     */
    public static class SpiralStatistics {
        private final int totalGenerated;
        private final int occupiedPositions;
        private final int maxIslands;

        public SpiralStatistics(int totalGenerated, int occupiedPositions, int maxIslands) {
            this.totalGenerated = totalGenerated;
            this.occupiedPositions = occupiedPositions;
            this.maxIslands = maxIslands;
        }

        public int getTotalGenerated() { return totalGenerated; }
        public int getOccupiedPositions() { return occupiedPositions; }
        public int getMaxIslands() { return maxIslands; }
        public int getAvailablePositions() { return maxIslands - occupiedPositions; }
        public double getUtilizationPercentage() { 
            return maxIslands > 0 ? (double) occupiedPositions / maxIslands * 100 : 0; 
        }

        @Override
        public String toString() {
            return String.format("SpiralStatistics{generated=%d, occupied=%d, max=%d, utilization=%.1f%%}", 
                               totalGenerated, occupiedPositions, maxIslands, getUtilizationPercentage());
        }
    }

    /**
     * Clears all data for a specific world.
     */
    public void clearWorld(@NotNull String worldName) {
        worldPositionCounters.remove(worldName);
        occupiedPositions.remove(worldName);
        worldConfigurations.remove(worldName);
    }

    /**
     * Resets the position counter for a world (use with caution).
     */
    public void resetPositionCounter(@NotNull String worldName) {
        AtomicInteger counter = worldPositionCounters.get(worldName);
        if (counter != null) {
            counter.set(0);
        }
    }
}