package de.jexcellence.home.database.entity;

import com.raindropcentral.rplatform.database.converter.LocationConverter;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.home.config.utility.IconSection;
import de.jexcellence.home.database.converter.IconSectionConverter;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a player's home location with enhanced metadata.
 * <p>
 * This entity is mapped to the {@code jexhome_home} table and stores
 * home locations with coordinates, owner information, and metadata
 * for categories, favorites, and visit tracking.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
        name = "jexhome_home",
        indexes = {
                @Index(name = "idx_home_player_uuid", columnList = "player_uuid"),
                @Index(name = "idx_home_player_name", columnList = "player_uuid, home_name"),
                @Index(name = "idx_home_category", columnList = "player_uuid, category")
        }
)
public class Home extends BaseEntity {

    @Column(name = "home_name", nullable = false, length = 64)
    private String homeName;

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = LocationConverter.class)
    private Location location;

    // Enhanced metadata fields
    @Column(name = "category", length = 32)
    private String category = "default";

    @Column(name = "favorite")
    private boolean favorite = false;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon", columnDefinition = "TEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    @Column(name = "visit_count")
    private int visitCount = 0;

    @Column(name = "last_visited")
    private LocalDateTime lastVisited;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Protected no-argument constructor for JPA/Hibernate.
     */
    protected Home() {}

    /**
     * Constructs a new Home entity with the specified name and player.
     *
     * @param homeName the name of the home
     * @param player   the player who owns this home
     */
    public Home(@NotNull String homeName, @NotNull Player player) {
        this.homeName = homeName;
        this.playerUuid = player.getUniqueId();
        this.location = player.getLocation();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructs a new Home entity with the specified name, player UUID, and location.
     *
     * @param homeName   the name of the home
     * @param playerUuid the UUID of the player who owns this home
     * @param location   the location of the home
     */
    public Home(@NotNull String homeName, @NotNull UUID playerUuid, @NotNull Location location) {
        this.homeName = homeName;
        this.location = location;
        this.playerUuid = playerUuid;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Core Getters/Setters ====================

    public String getHomeName() {
        return homeName;
    }

    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    // ==================== Metadata Getters/Setters ====================

    public @NotNull String getCategory() {
        return category != null ? category : "default";
    }

    public void setCategory(@Nullable String category) {
        this.category = category != null ? category : "default";
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @NotNull IconSection getIcon() {
        if (icon == null) {
            icon = createDefaultIcon();
        }
        return icon;
    }

    public void setIcon(@Nullable IconSection icon) {
        this.icon = icon != null ? icon : createDefaultIcon();
    }

    /**
     * Creates a default icon with PLAYER_HEAD material.
     *
     * @return the default IconSection
     */
    private IconSection createDefaultIcon() {
        var defaultIcon = new IconSection(new EvaluationEnvironmentBuilder());
        defaultIcon.setMaterial("PLAYER_HEAD");
        defaultIcon.setDisplayNameKey("home.icon.default.name");
        defaultIcon.setDescriptionKey("home.icon.default.lore");
        return defaultIcon;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public @Nullable LocalDateTime getLastVisited() {
        return lastVisited;
    }

    public void setLastVisited(@Nullable LocalDateTime lastVisited) {
        this.lastVisited = lastVisited;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== Utility Methods ====================

    /**
     * Records a visit to this home, incrementing visit count and updating last visited.
     */
    public void recordVisit() {
        this.visitCount++;
        this.lastVisited = LocalDateTime.now();
    }

    /**
     * Gets a formatted location string for display.
     *
     * @return formatted location string (e.g., "100, 64, -200")
     */
    public @NotNull String getFormattedLocation() {
        if (location == null) return "Unknown";
        return String.format("%.0f, %.0f, %.0f", location.getX(), location.getY(), location.getZ());
    }

    /**
     * Gets the world name from the stored location.
     *
     * @return the world name, or "unknown" if location is null
     */
    public @NotNull String getWorldName() {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName();
    }

    /**
     * Converts the stored location to a Bukkit Location.
     * <p>
     * Returns null if the world is not loaded or the location is invalid.
     * </p>
     *
     * @return the location, or null if the world is not loaded
     */
    public @Nullable Location toLocation() {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Home home)) return false;
        return getId() != null && getId().equals(home.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Home{" +
                "homeName='" + homeName + '\'' +
                ", playerUuid=" + playerUuid +
                ", location=" + (location != null ? location.toString() : "null") +
                ", category='" + category + '\'' +
                ", favorite=" + favorite +
                ", visitCount=" + visitCount +
                '}';
    }
}
