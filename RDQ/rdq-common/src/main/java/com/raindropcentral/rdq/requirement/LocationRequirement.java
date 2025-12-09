package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class LocationRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(LocationRequirement.class.getName());

    @JsonProperty("requiredWorld")
    private final String requiredWorld;

    @JsonProperty("requiredRegion")
    private final String requiredRegion;

    @JsonProperty("requiredCoordinates")
    private final Coordinates requiredCoordinates;

    @JsonProperty("requiredDistance")
    private final double requiredDistance;

    @JsonProperty("description")
    private final String description;

    @JsonIgnore
    private transient volatile Boolean worldGuardAvailable;

    @JsonIgnore
    private transient volatile World cachedWorld;

    public LocationRequirement(@NotNull String requiredWorld, double x, double y, double z, double requiredDistance) {
        this(requiredWorld, null, new Coordinates(x, y, z), requiredDistance, null);
    }

    public LocationRequirement(@NotNull String requiredWorld, @NotNull String requiredRegion) {
        this(requiredWorld, requiredRegion, null, 0.0, null);
    }

    @JsonCreator
    public LocationRequirement(@JsonProperty("requiredWorld") @Nullable String requiredWorld,
                              @JsonProperty("requiredRegion") @Nullable String requiredRegion,
                              @JsonProperty("requiredCoordinates") @Nullable Coordinates requiredCoordinates,
                              @JsonProperty("requiredDistance") @Nullable Double requiredDistance,
                              @JsonProperty("description") @Nullable String description) {
        super(Type.LOCATION);

        if (requiredWorld == null && requiredRegion == null && requiredCoordinates == null) {
            throw new IllegalArgumentException("At least one location criterion must be specified (world, region, or coordinates).");
        }

        if (requiredCoordinates != null && (requiredDistance == null || requiredDistance < 0)) {
            throw new IllegalArgumentException("Required distance must be non-negative when coordinates are specified.");
        }

        if (requiredWorld != null && requiredWorld.trim().isEmpty()) {
            throw new IllegalArgumentException("Required world name cannot be empty.");
        }

        if (requiredRegion != null && requiredRegion.trim().isEmpty()) {
            throw new IllegalArgumentException("Required region name cannot be empty.");
        }

        this.requiredWorld = requiredWorld;
        this.requiredRegion = requiredRegion;
        this.requiredCoordinates = requiredCoordinates;
        this.requiredDistance = requiredDistance != null ? requiredDistance : 0.0;
        this.description = description;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        var playerLocation = player.getLocation();

        if (requiredWorld != null) {
            if (!requiredWorld.equals(playerLocation.getWorld().getName())) {
                return false;
            }
        }

        if (requiredCoordinates != null) {
            var targetLocation = new Location(
                    playerLocation.getWorld(),
                    requiredCoordinates.getX(),
                    requiredCoordinates.getY(),
                    requiredCoordinates.getZ()
            );
            var distance = playerLocation.distance(targetLocation);
            if (distance > requiredDistance) {
                return false;
            }
        }

        if (requiredRegion != null) {
            if (!isInRegion(playerLocation, requiredRegion)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        var playerLocation = player.getLocation();
        var progress = 1.0;

        if (requiredWorld != null) {
            var worldProgress = requiredWorld.equals(playerLocation.getWorld().getName()) ? 1.0 : 0.0;
            progress = Math.min(progress, worldProgress);
        }

        if (requiredCoordinates != null) {
            var targetLocation = new Location(
                    playerLocation.getWorld(),
                    requiredCoordinates.getX(),
                    requiredCoordinates.getY(),
                    requiredCoordinates.getZ()
            );
            var distance = playerLocation.distance(targetLocation);
            var coordinateProgress = requiredDistance > 0
                    ? Math.max(0.0, 1.0 - (distance / requiredDistance))
                    : (distance == 0 ? 1.0 : 0.0);
            progress = Math.min(progress, coordinateProgress);
        }

        if (requiredRegion != null) {
            var regionProgress = isInRegion(playerLocation, requiredRegion) ? 1.0 : 0.0;
            progress = Math.min(progress, regionProgress);
        }

        return Math.max(0.0, Math.min(1.0, progress));
    }

    @Override
    public void consume(@NotNull Player player) {
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.location";
    }

    /**
     * Retrieves the world name that must contain the player.
     *
     * @return the required world name, or {@code null} if no world is enforced
     */
    @Nullable
    public String getRequiredWorld() {
        return this.requiredWorld;
    }

    /**
     * Retrieves the region identifier that must contain the player.
     *
     * @return the required region identifier, or {@code null} if no region is enforced
     */
    @Nullable
    public String getRequiredRegion() {
        return this.requiredRegion;
    }

    /**
     * Retrieves the coordinates that the player should remain near.
     *
     * @return the target coordinates, or {@code null} when the player position is not constrained
     */
    @Nullable
    public Coordinates getRequiredCoordinates() {
        return this.requiredCoordinates;
    }

    /**
     * Retrieves the allowed distance from the required coordinates.
     *
     * @return the maximum allowed distance when coordinates are enforced
     */
    public double getRequiredDistance() {
        return this.requiredDistance;
    }

    /**
     * Retrieves a human-readable description of the requirement.
     *
     * @return the description, or {@code null} if none was provided
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Calculates the current distance between the player and the required coordinates.
     *
     * @param player the player being evaluated
     * @return the distance to the coordinates, or {@code -1.0} when no coordinates are enforced
     */
    @JsonIgnore
    public double getCurrentDistance(final @NotNull Player player) {
        if (this.requiredCoordinates == null) {
            return -1.0;
        }

        final Location playerLocation = player.getLocation();
        final Location targetLocation = new Location(
                playerLocation.getWorld(),
                this.requiredCoordinates.getX(),
                this.requiredCoordinates.getY(),
                this.requiredCoordinates.getZ()
        );
        return playerLocation.distance(targetLocation);
    }

    /**
     * Determines whether the player is currently in the required world.
     *
     * @param player the player being evaluated
     * @return {@code true} if the requirement does not specify a world or the player matches the world
     */
    @JsonIgnore
    public boolean isInCorrectWorld(final @NotNull Player player) {
        if (this.requiredWorld == null) {
            return true;
        }
        return this.requiredWorld.equals(player.getWorld().getName());
    }

    /**
     * Determines whether the player is within the configured distance of the required coordinates.
     *
     * @param player the player being evaluated
     * @return {@code true} if coordinates are not enforced or if the player is within range
     */
    @JsonIgnore
    public boolean isWithinDistance(final @NotNull Player player) {
        if (this.requiredCoordinates == null) {
            return true;
        }
        return this.getCurrentDistance(player) <= this.requiredDistance;
    }

    /**
     * Determines whether the player is located inside the required region.
     *
     * @param player the player being evaluated
     * @return {@code true} when no region is required or when the region check succeeds
     */
    @JsonIgnore
    public boolean isInCorrectRegion(final @NotNull Player player) {
        if (this.requiredRegion == null) {
            return true;
        }
        return this.isInRegion(player.getLocation(), this.requiredRegion);
    }

    /**
     * Builds a formatted status string summarizing the player's compliance with each constraint.
     *
     * @param player the player being evaluated
     * @return the formatted status string
     */
    @JsonIgnore
    @NotNull
    public String getFormattedStatus(final @NotNull Player player) {
        final StringBuilder status = new StringBuilder();

        if (this.requiredWorld != null) {
            status.append("World: ").append(player.getWorld().getName())
                    .append(this.isInCorrectWorld(player) ? " ✓" : " ✗")
                    .append(" (Required: ").append(this.requiredWorld).append(")");
        }

        if (this.requiredCoordinates != null) {
            if (status.length() > 0) status.append(", ");
            final double distance = this.getCurrentDistance(player);
            status.append("Distance: ").append(String.format("%.1f", distance))
                    .append(this.isWithinDistance(player) ? " ✓" : " ✗")
                    .append(" (Max: ").append(String.format("%.1f", this.requiredDistance)).append(")");
        }

        if (this.requiredRegion != null) {
            if (status.length() > 0) status.append(", ");
            status.append("Region: ").append(this.requiredRegion)
                    .append(this.isInCorrectRegion(player) ? " ✓" : " ✗");
        }

        return status.toString();
    }

    /**
     * Validates that the requirement configuration is internally consistent and references existing worlds.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredWorld == null && this.requiredRegion == null && this.requiredCoordinates == null) {
            throw new IllegalStateException("At least one location criterion must be specified.");
        }

        if (this.requiredCoordinates != null && this.requiredDistance < 0) {
            throw new IllegalStateException("Required distance must be non-negative: " + this.requiredDistance);
        }

        if (this.requiredWorld != null && this.requiredWorld.trim().isEmpty()) {
            throw new IllegalStateException("Required world name cannot be empty.");
        }

        if (this.requiredRegion != null && this.requiredRegion.trim().isEmpty()) {
            throw new IllegalStateException("Required region name cannot be empty.");
        }

        if (this.requiredWorld != null) {
            final World world = this.getCachedWorld();
            if (world == null) {
                throw new IllegalStateException("Required world does not exist: " + this.requiredWorld);
            }
        }
    }

    /**
     * Retrieves the cached {@link World} reference when a required world is configured.
     *
     * @return the cached world instance, or {@code null} if no world is required or it is unavailable
     */
    @Nullable
    private World getCachedWorld() {
        if (this.requiredWorld == null) {
            return null;
        }

        if (this.cachedWorld != null) {
            return this.cachedWorld;
        }

        this.cachedWorld = Bukkit.getWorld(this.requiredWorld);
        return this.cachedWorld;
    }

    /**
     * Determines whether the WorldGuard plugin is available and enabled.
     *
     * @return {@code true} when WorldGuard can be queried for region checks
     */
    private boolean isWorldGuardAvailable() {
        if (this.worldGuardAvailable == null) {
            final Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            this.worldGuardAvailable = worldGuardPlugin != null && worldGuardPlugin.isEnabled();
        }
        return this.worldGuardAvailable;
    }

    /**
     * Determines whether a location resides within the specified WorldGuard region.
     *
     * @param location  the location to evaluate
     * @param regionName the name of the region to check
     * @return {@code true} if the region check succeeds; {@code false} otherwise
     */
    private boolean isInRegion(final @NotNull Location location, final @NotNull String regionName) {
        if (!this.isWorldGuardAvailable()) {
            LOGGER.log(Level.WARNING,
                    "WorldGuard is not available, region check for '" + regionName + "' will always return false");
            return false;
        }
        return false;
    }

    /**
     * Immutable coordinate triple used to describe a target position.
     *
     * @author JExcellence
     * @version 1.0.1
     * @since 1.0.0
     */
    public static final class Coordinates {
        @JsonProperty("x")
        private final double x;

        @JsonProperty("y")
        private final double y;

        @JsonProperty("z")
        private final double z;

        /**
         * Creates a coordinate triple.
         *
         * @param x the x-coordinate component
         * @param y the y-coordinate component
         * @param z the z-coordinate component
         */
        @JsonCreator
        public Coordinates(
                @JsonProperty("x") final double x,
                @JsonProperty("y") final double y,
                @JsonProperty("z") final double z
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Retrieves the x-coordinate.
         *
         * @return the x-coordinate component
         */
        public double getX() {
            return this.x;
        }

        /**
         * Retrieves the y-coordinate.
         *
         * @return the y-coordinate component
         */
        public double getY() {
            return this.y;
        }

        /**
         * Retrieves the z-coordinate.
         *
         * @return the z-coordinate component
         */
        public double getZ() {
            return this.z;
        }

        /**
         * Converts the coordinates into a formatted string.
         *
         * @return the formatted coordinate string
         */
        @Override
        public String toString() {
            return String.format("(%.1f, %.1f, %.1f)", this.x, this.y, this.z);
        }
    }
}