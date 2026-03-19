package de.jexcellence.oneblock.database.entity.oneblock;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Embeddable IslandRegion component for island boundary management.
 * Defines the 3D boundaries of an island across multiple dimensions.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class IslandRegion {
    
    @Column(name = "region_min_x", nullable = false)
    private int minX;
    
    @Column(name = "region_min_y", nullable = false)
    private int minY;
    
    @Column(name = "region_min_z", nullable = false)
    private int minZ;
    
    @Column(name = "region_max_x", nullable = false)
    private int maxX;
    
    @Column(name = "region_max_y", nullable = false)
    private int maxY;
    
    @Column(name = "region_max_z", nullable = false)
    private int maxZ;
    
    @Column(name = "region_world_name", nullable = false, length = 100)
    private String worldName;
    
    @Column(name = "region_spawn_location", nullable = false)
    @Convert(converter = LocationConverter.class)
    private Location spawnLocation;
    
    @Column(name = "region_visitor_spawn_location")
    @Convert(converter = LocationConverter.class)
    private Location visitorSpawnLocation;
    
    /**
     * Constructor for creating a region from center location and size.
     * 
     * @param centerLocation the center location of the region
     * @param size the size of the region (radius)
     */
    public IslandRegion(@NotNull Location centerLocation, int size) {
        this.minX = centerLocation.getBlockX() - size / 2;
        this.minY = centerLocation.getBlockY() - 64;
        this.minZ = centerLocation.getBlockZ() - size / 2;
        this.maxX = centerLocation.getBlockX() + size / 2;
        this.maxY = centerLocation.getBlockY() + 320;
        this.maxZ = centerLocation.getBlockZ() + size / 2;
        this.worldName = centerLocation.getWorld().getName();
        this.spawnLocation = centerLocation.clone();
        this.visitorSpawnLocation = centerLocation.clone();
    }
    
    /**
     * Constructor for creating a region from two corner locations.
     * 
     * @param corner1 the first corner location
     * @param corner2 the second corner location
     * @param spawnLocation the spawn location within the region
     */
    public IslandRegion(@NotNull Location corner1, @NotNull Location corner2, @NotNull Location spawnLocation) {
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        this.worldName = corner1.getWorld().getName();
        this.spawnLocation = spawnLocation.clone();
        this.visitorSpawnLocation = spawnLocation.clone();
    }
    
    /**
     * Checks if a location is within this region's boundaries.
     * 
     * @param location the location to check
     * @return true if the location is within the region
     */
    public boolean contains(@NotNull Location location) {
        if (!location.getWorld().getName().equals(this.worldName)) {
            return false;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        return x >= this.minX && x <= this.maxX &&
               y >= this.minY && y <= this.maxY &&
               z >= this.minZ && z <= this.maxZ;
    }
    
    /**
     * Gets the minimum corner location of the region.
     * 
     * @return the minimum corner location
     */
    @NotNull
    public Location getMinLocation() {
        return new Location(org.bukkit.Bukkit.getWorld(worldName), minX, minY, minZ);
    }
    
    /**
     * Gets the maximum corner location of the region.
     * 
     * @return the maximum corner location
     */
    @NotNull
    public Location getMaxLocation() {
        return new Location(org.bukkit.Bukkit.getWorld(worldName), maxX, maxY, maxZ);
    }
    
    /**
     * Gets the center location of the region.
     * 
     * @return the center location
     */
    @NotNull
    public Location getCenterLocation() {
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        return new Location(org.bukkit.Bukkit.getWorld(worldName), centerX, centerY, centerZ);
    }
    
    /**
     * Calculates the volume of the region.
     * 
     * @return the volume in blocks
     */
    public long getVolume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
    
    /**
     * Gets the width of the region (X-axis).
     * 
     * @return the width in blocks
     */
    public int getWidth() {
        return maxX - minX + 1;
    }
    
    /**
     * Gets the height of the region (Y-axis).
     * 
     * @return the height in blocks
     */
    public int getHeight() {
        return maxY - minY + 1;
    }
    
    /**
     * Gets the depth of the region (Z-axis).
     * 
     * @return the depth in blocks
     */
    public int getDepth() {
        return maxZ - minZ + 1;
    }
    
    /**
     * Expands the region by the specified amount in all horizontal directions.
     * 
     * @param amount the amount to expand by
     */
    public void expand(int amount) {
        this.minX -= amount;
        this.minZ -= amount;
        this.maxX += amount;
        this.maxZ += amount;
    }
    
    /**
     * Expands the region by the specified amounts in each direction.
     * 
     * @param x the amount to expand in X direction
     * @param y the amount to expand in Y direction
     * @param z the amount to expand in Z direction
     */
    public void expand(int x, int y, int z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }
    
    /**
     * Contracts the region by the specified amount in all horizontal directions.
     * 
     * @param amount the amount to contract by
     */
    public void contract(int amount) {
        this.minX += amount;
        this.minZ += amount;
        this.maxX -= amount;
        this.maxZ -= amount;
        
        // Ensure region doesn't become invalid
        if (this.minX > this.maxX) {
            int center = (this.minX + this.maxX) / 2;
            this.minX = center;
            this.maxX = center;
        }
        if (this.minZ > this.maxZ) {
            int center = (this.minZ + this.maxZ) / 2;
            this.minZ = center;
            this.maxZ = center;
        }
    }
    
    /**
     * Moves the region to a new center location.
     * 
     * @param newCenter the new center location
     */
    public void moveTo(@NotNull Location newCenter) {
        int width = getWidth();
        int height = getHeight();
        int depth = getDepth();
        
        this.minX = newCenter.getBlockX() - width / 2;
        this.minY = newCenter.getBlockY() - height / 2;
        this.minZ = newCenter.getBlockZ() - depth / 2;
        this.maxX = this.minX + width - 1;
        this.maxY = this.minY + height - 1;
        this.maxZ = this.minZ + depth - 1;
        this.worldName = newCenter.getWorld().getName();
        
        // Update spawn locations
        this.spawnLocation = newCenter.clone();
        if (this.visitorSpawnLocation != null) {
            this.visitorSpawnLocation = newCenter.clone();
        }
    }
    
    /**
     * Checks if this region overlaps with another region.
     * 
     * @param other the other region to check
     * @return true if the regions overlap
     */
    public boolean overlaps(@NotNull IslandRegion other) {
        if (!this.worldName.equals(other.worldName)) {
            return false;
        }
        
        return !(this.maxX < other.minX || this.minX > other.maxX ||
                 this.maxY < other.minY || this.minY > other.maxY ||
                 this.maxZ < other.minZ || this.minZ > other.maxZ);
    }
    
    /**
     * Calculates the distance between this region's center and another region's center.
     * 
     * @param other the other region
     * @return the distance between centers, or -1 if in different worlds
     */
    public double distanceTo(@NotNull IslandRegion other) {
        if (!this.worldName.equals(other.worldName)) {
            return -1;
        }
        
        Location thisCenter = getCenterLocation();
        Location otherCenter = other.getCenterLocation();
        return thisCenter.distance(otherCenter);
    }
    
    /**
     * Checks if the region is valid (min coordinates are less than or equal to max coordinates).
     * 
     * @return true if the region is valid
     */
    public boolean isValid() {
        return minX <= maxX && minY <= maxY && minZ <= maxZ && worldName != null;
    }
    
    /**
     * Gets a BoundingBox representation of this region.
     * 
     * @return the bounding box
     */
    @NotNull
    public org.bukkit.util.BoundingBox getBoundingBox() {
        return new org.bukkit.util.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Gets the current world for this region.
     * 
     * @return the world, or null if not loaded
     */
    @org.jetbrains.annotations.Nullable
    public org.bukkit.World getCurrentWorld() {
        return org.bukkit.Bukkit.getWorld(worldName);
    }
    
    /**
     * Gets a string representation of the region boundaries.
     * 
     * @return formatted boundary string
     */
    @NotNull
    public String getBoundaryString() {
        return String.format("(%d,%d,%d) to (%d,%d,%d) in %s", 
                           minX, minY, minZ, maxX, maxY, maxZ, worldName);
    }
    
    @Override
    public String toString() {
        return "IslandRegion{" +
                "bounds=" + getBoundaryString() +
                ", volume=" + getVolume() +
                ", spawn=" + (spawnLocation != null ? 
                    String.format("(%.1f,%.1f,%.1f)", spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ()) : 
                    "null") +
                '}';
    }
}