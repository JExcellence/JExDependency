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

/**
 * Requirement that checks if a player is at a specific location.
 * <p>
 * The {@code LocationRequirement} is satisfied when the player is at a specified location,
 * which can be defined by world, coordinates with radius, WorldGuard regions, or combinations
 * of these criteria.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
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

    public LocationRequirement(
            final @NotNull String requiredWorld,
            final double x,
            final double y,
            final double z,
            final double requiredDistance
    ) {
        this(requiredWorld, null, new Coordinates(x, y, z), requiredDistance, null);
    }

    public LocationRequirement(
            final @NotNull String requiredWorld,
            final @NotNull String requiredRegion
    ) {
        this(requiredWorld, requiredRegion, null, 0.0, null);
    }

    @JsonCreator
    public LocationRequirement(
            @JsonProperty("requiredWorld") final @Nullable String requiredWorld,
            @JsonProperty("requiredRegion") final @Nullable String requiredRegion,
            @JsonProperty("requiredCoordinates") final @Nullable Coordinates requiredCoordinates,
            @JsonProperty("requiredDistance") final @Nullable Double requiredDistance,
            @JsonProperty("description") final @Nullable String description
    ) {
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
    public boolean isMet(final @NotNull Player player) {
        final Location playerLocation = player.getLocation();

        if (this.requiredWorld != null) {
            if (!this.requiredWorld.equals(playerLocation.getWorld().getName())) {
                return false;
            }
        }

        if (this.requiredCoordinates != null) {
            final Location targetLocation = new Location(
                    playerLocation.getWorld(),
                    this.requiredCoordinates.getX(),
                    this.requiredCoordinates.getY(),
                    this.requiredCoordinates.getZ()
            );
            final double distance = playerLocation.distance(targetLocation);
            if (distance > this.requiredDistance) {
                return false;
            }
        }

        if (this.requiredRegion != null) {
            if (!this.isInRegion(playerLocation, this.requiredRegion)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public double calculateProgress(final @NotNull Player player) {
        final Location playerLocation = player.getLocation();
        double progress = 1.0;

        if (this.requiredWorld != null) {
            final double worldProgress = this.requiredWorld.equals(playerLocation.getWorld().getName()) ? 1.0 : 0.0;
            progress = Math.min(progress, worldProgress);
        }

        if (this.requiredCoordinates != null) {
            final Location targetLocation = new Location(
                    playerLocation.getWorld(),
                    this.requiredCoordinates.getX(),
                    this.requiredCoordinates.getY(),
                    this.requiredCoordinates.getZ()
            );
            final double distance = playerLocation.distance(targetLocation);
            final double coordinateProgress = this.requiredDistance > 0
                    ? Math.max(0.0, 1.0 - (distance / this.requiredDistance))
                    : (distance == 0 ? 1.0 : 0.0);
            progress = Math.min(progress, coordinateProgress);
        }

        if (this.requiredRegion != null) {
            final double regionProgress = this.isInRegion(playerLocation, this.requiredRegion) ? 1.0 : 0.0;
            progress = Math.min(progress, regionProgress);
        }

        return Math.max(0.0, Math.min(1.0, progress));
    }

    @Override
    public void consume(final @NotNull Player player) {
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.location";
    }

    @Nullable
    public String getRequiredWorld() {
        return this.requiredWorld;
    }

    @Nullable
    public String getRequiredRegion() {
        return this.requiredRegion;
    }

    @Nullable
    public Coordinates getRequiredCoordinates() {
        return this.requiredCoordinates;
    }

    public double getRequiredDistance() {
        return this.requiredDistance;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

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

    @JsonIgnore
    public boolean isInCorrectWorld(final @NotNull Player player) {
        if (this.requiredWorld == null) {
            return true;
        }
        return this.requiredWorld.equals(player.getWorld().getName());
    }

    @JsonIgnore
    public boolean isWithinDistance(final @NotNull Player player) {
        if (this.requiredCoordinates == null) {
            return true;
        }
        return this.getCurrentDistance(player) <= this.requiredDistance;
    }

    @JsonIgnore
    public boolean isInCorrectRegion(final @NotNull Player player) {
        if (this.requiredRegion == null) {
            return true;
        }
        return this.isInRegion(player.getLocation(), this.requiredRegion);
    }

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

    private boolean isWorldGuardAvailable() {
        if (this.worldGuardAvailable == null) {
            final Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            this.worldGuardAvailable = worldGuardPlugin != null && worldGuardPlugin.isEnabled();
        }
        return this.worldGuardAvailable;
    }

    private boolean isInRegion(final @NotNull Location location, final @NotNull String regionName) {
        if (!this.isWorldGuardAvailable()) {
            LOGGER.log(Level.WARNING,
                    "WorldGuard is not available, region check for '" + regionName + "' will always return false");
            return false;
        }
        return false;
    }

    public static final class Coordinates {
        @JsonProperty("x")
        private final double x;

        @JsonProperty("y")
        private final double y;

        @JsonProperty("z")
        private final double z;

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

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getZ() {
            return this.z;
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f, %.1f)", this.x, this.y, this.z);
        }
    }
}