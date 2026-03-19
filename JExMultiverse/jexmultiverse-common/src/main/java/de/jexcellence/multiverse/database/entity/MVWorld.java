package de.jexcellence.multiverse.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.multiverse.database.converter.LocationConverter;
import de.jexcellence.multiverse.type.MVWorldType;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity representing a managed world in the JExMultiverse system.
 * <p>
 * This entity is mapped to the {@code mv_world} table and stores
 * world configuration including spawn location, type, environment,
 * and various settings like PvP and global spawn status.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
        name = "mv_world",
        indexes = {
                @Index(name = "idx_mv_world_identifier", columnList = "world_name"),
                @Index(name = "idx_mv_world_global_spawn", columnList = "is_globalized_spawn")
        }
)
public class MVWorld extends BaseEntity {

    @Column(name = "world_name", nullable = false, unique = true, length = 64)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "world_type", nullable = false, length = 16)
    private MVWorldType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "world_environment", nullable = false, length = 16)
    private World.Environment environment;

    @Convert(converter = LocationConverter.class)
    @Column(name = "spawn_location", nullable = false, columnDefinition = "LONGTEXT")
    private Location spawnLocation;

    @Column(name = "is_globalized_spawn", nullable = false)
    private boolean globalizedSpawn;

    @Column(name = "is_pvp_enabled", nullable = false)
    private boolean pvpEnabled;

    @Column(name = "enter_permission", length = 128)
    private String enterPermission;

    /**
     * Protected no-argument constructor for JPA/Hibernate.
     */
    protected MVWorld() {}

    /**
     * Private constructor used by the Builder.
     *
     * @param builder the builder instance
     */
    private MVWorld(final Builder builder) {
        this.identifier = builder.identifier;
        this.type = builder.type;
        this.environment = builder.environment;
        this.spawnLocation = builder.spawnLocation;
        this.globalizedSpawn = builder.globalizedSpawn;
        this.pvpEnabled = builder.pvpEnabled;
        this.enterPermission = builder.enterPermission;
    }

    // ==================== Getters ====================

    /**
     * Gets the unique identifier (world name) for this world.
     *
     * @return the world identifier
     */
    public @NotNull String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the world generation type.
     *
     * @return the world type
     */
    public @NotNull MVWorldType getType() {
        return type;
    }

    /**
     * Gets the world environment (NORMAL, NETHER, THE_END).
     *
     * @return the world environment
     */
    public @NotNull World.Environment getEnvironment() {
        return environment;
    }

    /**
     * Gets the spawn location for this world.
     *
     * @return the spawn location
     */
    public @NotNull Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Checks if this world is the global spawn location.
     *
     * @return true if this is the global spawn world
     */
    public boolean isGlobalizedSpawn() {
        return globalizedSpawn;
    }

    /**
     * Checks if PvP is enabled in this world.
     *
     * @return true if PvP is enabled
     */
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    /**
     * Gets the permission required to enter this world.
     *
     * @return the enter permission, or null if no permission required
     */
    public @Nullable String getEnterPermission() {
        return enterPermission;
    }

    // ==================== Setters ====================

    /**
     * Sets the world identifier.
     *
     * @param identifier the new identifier
     */
    public void setIdentifier(@NotNull final String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the world type.
     *
     * @param type the new world type
     */
    public void setType(@NotNull final MVWorldType type) {
        this.type = type;
    }

    /**
     * Sets the world environment.
     *
     * @param environment the new environment
     */
    public void setEnvironment(@NotNull final World.Environment environment) {
        this.environment = environment;
    }

    /**
     * Sets the spawn location for this world.
     *
     * @param spawnLocation the new spawn location
     */
    public void setSpawnLocation(@NotNull final Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    /**
     * Sets whether this world is the global spawn location.
     *
     * @param globalizedSpawn true to make this the global spawn
     */
    public void setGlobalizedSpawn(final boolean globalizedSpawn) {
        this.globalizedSpawn = globalizedSpawn;
    }

    /**
     * Sets whether PvP is enabled in this world.
     *
     * @param pvpEnabled true to enable PvP
     */
    public void setPvpEnabled(final boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    /**
     * Sets the permission required to enter this world.
     *
     * @param enterPermission the enter permission, or null for no restriction
     */
    public void setEnterPermission(@Nullable final String enterPermission) {
        this.enterPermission = enterPermission;
    }

    // ==================== Utility Methods ====================

    /**
     * Gets a formatted spawn location string for display.
     *
     * @return formatted location string (e.g., "100, 64, -200")
     */
    public @NotNull String getFormattedSpawnLocation() {
        if (spawnLocation == null) return "Unknown";
        return String.format("%.0f, %.0f, %.0f",
                spawnLocation.getX(),
                spawnLocation.getY(),
                spawnLocation.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MVWorld mvWorld)) return false;
        return getId() != null && getId().equals(mvWorld.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "MVWorld{" +
                "identifier='" + identifier + '\'' +
                ", type=" + type +
                ", environment=" + environment +
                ", globalizedSpawn=" + globalizedSpawn +
                ", pvpEnabled=" + pvpEnabled +
                '}';
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for constructing MVWorld instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link MVWorld} instances.
     */
    public static class Builder {
        private String identifier;
        private MVWorldType type = MVWorldType.DEFAULT;
        private World.Environment environment = World.Environment.NORMAL;
        private Location spawnLocation;
        private boolean globalizedSpawn = false;
        private boolean pvpEnabled = true;
        private String enterPermission;

        private Builder() {}

        /**
         * Sets the world identifier.
         *
         * @param identifier the world identifier
         * @return this builder
         */
        public Builder identifier(@NotNull final String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the world type.
         *
         * @param type the world type
         * @return this builder
         */
        public Builder type(@NotNull final MVWorldType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the world environment.
         *
         * @param environment the world environment
         * @return this builder
         */
        public Builder environment(@NotNull final World.Environment environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets the spawn location.
         *
         * @param spawnLocation the spawn location
         * @return this builder
         */
        public Builder spawnLocation(@NotNull final Location spawnLocation) {
            this.spawnLocation = spawnLocation;
            return this;
        }

        /**
         * Sets whether this is the global spawn world.
         *
         * @param globalizedSpawn true for global spawn
         * @return this builder
         */
        public Builder globalizedSpawn(final boolean globalizedSpawn) {
            this.globalizedSpawn = globalizedSpawn;
            return this;
        }

        /**
         * Sets whether PvP is enabled.
         *
         * @param pvpEnabled true to enable PvP
         * @return this builder
         */
        public Builder pvpEnabled(final boolean pvpEnabled) {
            this.pvpEnabled = pvpEnabled;
            return this;
        }

        /**
         * Sets the enter permission.
         *
         * @param enterPermission the permission required to enter
         * @return this builder
         */
        public Builder enterPermission(@Nullable final String enterPermission) {
            this.enterPermission = enterPermission;
            return this;
        }

        /**
         * Builds the MVWorld instance.
         *
         * @return the constructed MVWorld
         * @throws IllegalStateException if required fields are missing
         */
        public MVWorld build() {
            if (identifier == null || identifier.isBlank()) {
                throw new IllegalStateException("World identifier is required");
            }
            if (spawnLocation == null) {
                throw new IllegalStateException("Spawn location is required");
            }
            return new MVWorld(this);
        }
    }
}
