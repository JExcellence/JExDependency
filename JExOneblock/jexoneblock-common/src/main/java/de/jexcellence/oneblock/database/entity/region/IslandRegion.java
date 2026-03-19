package de.jexcellence.oneblock.database.entity.region;

import de.jexcellence.oneblock.database.converter.LocationConverter;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents an island region with boundaries and permissions.
 * Islands are positioned using a spiral algorithm for optimal spacing.
 */
@Entity
@Table(name = "oneblock_island_regions")
public class IslandRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "island_id", nullable = false, unique = true)
    private UUID islandId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "center_location", nullable = false, length = 500)
    @Convert(converter = LocationConverter.class)
    private Location centerLocation;

    @Column(name = "radius", nullable = false)
    private int radius;

    @Column(name = "spiral_position", nullable = false)
    private int spiralPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "boundary_shape", nullable = false)
    private BoundaryShape boundaryShape = BoundaryShape.SQUARE;

    @Column(name = "world_name", nullable = false)
    private String worldName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "oneblock_region_properties", joinColumns = @JoinColumn(name = "region_id"))
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value", length = 1000)
    private Map<String, String> properties = new HashMap<>();

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RegionPermission> permissions = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "protection_enabled", nullable = false)
    private boolean protectionEnabled = true;

    // Constructors
    public IslandRegion() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public IslandRegion(@NotNull UUID islandId, @NotNull UUID ownerId, @NotNull Location centerLocation, 
                       int radius, int spiralPosition) {
        this();
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.centerLocation = centerLocation;
        this.radius = radius;
        this.spiralPosition = spiralPosition;
        this.worldName = centerLocation.getWorld().getName();
    }

    // Boundary checking methods
    public boolean isWithinBoundaries(@NotNull Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        return switch (boundaryShape) {
            case SQUARE -> isWithinSquareBoundaries(location);
            case CIRCLE -> isWithinCircularBoundaries(location);
            case CUSTOM -> isWithinCustomBoundaries(location);
        };
    }

    private boolean isWithinSquareBoundaries(@NotNull Location location) {
        double deltaX = Math.abs(location.getX() - centerLocation.getX());
        double deltaZ = Math.abs(location.getZ() - centerLocation.getZ());
        return deltaX <= radius && deltaZ <= radius;
    }

    private boolean isWithinCircularBoundaries(@NotNull Location location) {
        double deltaX = location.getX() - centerLocation.getX();
        double deltaZ = location.getZ() - centerLocation.getZ();
        double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
        return distanceSquared <= radius * radius;
    }

    private boolean isWithinCustomBoundaries(@NotNull Location location) {
        // Custom boundary logic can be implemented here
        // For now, fallback to square boundaries
        return isWithinSquareBoundaries(location);
    }

    public double getDistanceFromCenter(@NotNull Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return Double.MAX_VALUE;
        }
        
        double deltaX = location.getX() - centerLocation.getX();
        double deltaZ = location.getZ() - centerLocation.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    public boolean isNearBoundary(@NotNull Location location, double threshold) {
        double distanceFromCenter = getDistanceFromCenter(location);
        return Math.abs(distanceFromCenter - radius) <= threshold;
    }

    // Utility methods
    public void addPermission(@NotNull RegionPermission permission) {
        permission.setRegion(this);
        permissions.add(permission);
    }

    public void removePermission(@NotNull RegionPermission permission) {
        permissions.remove(permission);
        permission.setRegion(null);
    }

    public Optional<RegionPermission> getPermission(@NotNull UUID playerId, @NotNull String permissionType) {
        return permissions.stream()
                .filter(p -> p.getPlayerId().equals(playerId) && p.getPermissionType().equals(permissionType))
                .findFirst();
    }

    public boolean hasPermission(@NotNull UUID playerId, @NotNull String permissionType) {
        if (ownerId.equals(playerId)) {
            return true; // Owner has all permissions
        }
        
        return getPermission(playerId, permissionType)
                .map(RegionPermission::isGranted)
                .orElse(false);
    }

    public void setProperty(@NotNull String key, @Nullable String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
        updateTimestamp();
    }

    public Optional<String> getProperty(@NotNull String key) {
        return Optional.ofNullable(properties.get(key));
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public void setIslandId(UUID islandId) {
        this.islandId = islandId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public void setCenterLocation(Location centerLocation) {
        this.centerLocation = centerLocation;
        if (centerLocation != null) {
            this.worldName = centerLocation.getWorld().getName();
        }
        updateTimestamp();
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        updateTimestamp();
    }

    public int getSpiralPosition() {
        return spiralPosition;
    }

    public void setSpiralPosition(int spiralPosition) {
        this.spiralPosition = spiralPosition;
    }

    public BoundaryShape getBoundaryShape() {
        return boundaryShape;
    }

    public void setBoundaryShape(BoundaryShape boundaryShape) {
        this.boundaryShape = boundaryShape;
        updateTimestamp();
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<RegionPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<RegionPermission> permissions) {
        this.permissions = permissions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        updateTimestamp();
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public void setProtectionEnabled(boolean protectionEnabled) {
        this.protectionEnabled = protectionEnabled;
        updateTimestamp();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IslandRegion that = (IslandRegion) o;
        return Objects.equals(islandId, that.islandId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(islandId);
    }

    @Override
    public String toString() {
        return "IslandRegion{" +
                "id=" + id +
                ", islandId=" + islandId +
                ", ownerId=" + ownerId +
                ", centerLocation=" + centerLocation +
                ", radius=" + radius +
                ", spiralPosition=" + spiralPosition +
                ", boundaryShape=" + boundaryShape +
                ", worldName='" + worldName + '\'' +
                ", active=" + active +
                ", protectionEnabled=" + protectionEnabled +
                '}';
    }

    /**
     * Enumeration of supported boundary shapes for island regions.
     */
    public enum BoundaryShape {
        SQUARE("Square", "A square-shaped boundary"),
        CIRCLE("Circle", "A circular boundary"),
        CUSTOM("Custom", "A custom-defined boundary shape");

        private final String displayName;
        private final String description;

        BoundaryShape(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}