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
 * of these criteria. This requirement is useful for creating location-based challenges,
 * training areas, or access control.
 * </p>
 *
 * <ul>
 *   <li>Supports world-based location checking.</li>
 *   <li>Supports coordinate-based location checking with configurable radius.</li>
 *   <li>Supports WorldGuard region-based location checking.</li>
 *   <li>Can combine multiple location criteria (all must be met).</li>
 *   <li>Progress is calculated based on distance to target location.</li>
 *   <li>Consumption is not applicable (location is not consumed).</li>
 *   <li>Integrates with RequirementSection for flexible configuration.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class LocationRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(LocationRequirement.class.getName());

    /**
     * The required world name. If null, any world is acceptable.
     */
    @JsonProperty("requiredWorld")
    private final String requiredWorld;

    /**
     * The required WorldGuard region name. If null, region checking is disabled.
     */
    @JsonProperty("requiredRegion")
    private final String requiredRegion;

    /**
     * The required coordinates. If null, coordinate checking is disabled.
     */
    @JsonProperty("requiredCoordinates")
    private final Coordinates requiredCoordinates;

    /**
     * The maximum distance from the required coordinates. Only used if requiredCoordinates is set.
     */
    @JsonProperty("requiredDistance")
    private final double requiredDistance;

    /**
     * Optional description for this location requirement.
     */
    @JsonProperty("description")
    private final String description;

    /**
     * Whether WorldGuard integration is available.
     * This is resolved at runtime and not serialized.
     */
    @JsonIgnore
    private transient Boolean worldGuardAvailable;

    /**
     * Constructs a {@code LocationRequirement} with world and coordinates.
     *
     * @param requiredWorld The required world name.
     * @param x The required X coordinate.
     * @param y The required Y coordinate.
     * @param z The required Z coordinate.
     * @param requiredDistance The maximum distance from the coordinates.
     */
    public LocationRequirement(
            @NotNull final String requiredWorld,
            final double x,
            final double y,
            final double z,
            final double requiredDistance
    ) {
        this(requiredWorld, null, new Coordinates(x, y, z), requiredDistance, null);
    }

    /**
     * Constructs a {@code LocationRequirement} with world and region.
     *
     * @param requiredWorld The required world name.
     * @param requiredRegion The required WorldGuard region name.
     */
    public LocationRequirement(
            @NotNull final String requiredWorld,
            @NotNull final String requiredRegion
    ) {
        this(requiredWorld, requiredRegion, null, 0.0, null);
    }

    /**
     * Constructs a {@code LocationRequirement} with full configuration options.
     *
     * @param requiredWorld The required world name (can be null).
     * @param requiredRegion The required WorldGuard region name (can be null).
     * @param requiredCoordinates The required coordinates (can be null).
     * @param requiredDistance The maximum distance from coordinates (ignored if coordinates are null).
     * @param description Optional description for this requirement.
     */
    @JsonCreator
    public LocationRequirement(
            @JsonProperty("requiredWorld") @Nullable final String requiredWorld,
            @JsonProperty("requiredRegion") @Nullable final String requiredRegion,
            @JsonProperty("requiredCoordinates") @Nullable final Coordinates requiredCoordinates,
            @JsonProperty("requiredDistance") @Nullable final Double requiredDistance,
            @JsonProperty("description") @Nullable final String description
    ) {
        super(Type.LOCATION);

        // Validate that at least one location criterion is specified
        if (requiredWorld == null && requiredRegion == null && requiredCoordinates == null) {
            throw new IllegalArgumentException("At least one location criterion must be specified (world, region, or coordinates).");
        }

        // Validate coordinates and distance
        if (requiredCoordinates != null && (requiredDistance == null || requiredDistance < 0)) {
            throw new IllegalArgumentException("Required distance must be non-negative when coordinates are specified.");
        }

        // Validate world name
        if (requiredWorld != null && requiredWorld.trim().isEmpty()) {
            throw new IllegalArgumentException("Required world name cannot be empty.");
        }

        // Validate region name
        if (requiredRegion != null && requiredRegion.trim().isEmpty()) {
            throw new IllegalArgumentException("Required region name cannot be empty.");
        }

        this.requiredWorld = requiredWorld;
        this.requiredRegion = requiredRegion;
        this.requiredCoordinates = requiredCoordinates;
        this.requiredDistance = requiredDistance != null ? requiredDistance : 0.0;
        this.description = description;
    }

    /**
     * Checks if the player is at the required location based on the configured criteria.
     *
     * @param player The player whose location will be checked.
     * @return {@code true} if the player meets all location requirements, {@code false} otherwise.
     */
    @Override
    public boolean isMet(
            @NotNull final Player player
    ) {
        final Location playerLocation = player.getLocation();

        // Check world requirement
        if (this.requiredWorld != null) {
            if (!this.requiredWorld.equals(playerLocation.getWorld().getName())) {
                return false;
            }
        }

        // Check coordinate and distance requirement
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

        // Check region requirement
        if (this.requiredRegion != null) {
            if (!this.isInRegion(playerLocation, this.requiredRegion)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the progress towards fulfilling the location requirement.
     * <p>
     * Progress is calculated based on distance to the target location:
     * <ul>
     *   <li>If only world/region checking: 1.0 if in correct world/region, 0.0 otherwise.</li>
     *   <li>If coordinate checking: 1.0 - (distance / requiredDistance), clamped to [0.0, 1.0].</li>
     *   <li>If multiple criteria: minimum progress of all criteria.</li>
     * </ul>
     * </p>
     *
     * @param player The player whose location progress will be evaluated.
     * @return A double between 0.0 and 1.0 representing progress.
     */
    @Override
    public double calculateProgress(
            @NotNull final Player player
    ) {
        final Location playerLocation = player.getLocation();
        double progress = 1.0;

        // Check world progress
        if (this.requiredWorld != null) {
            final double worldProgress = this.requiredWorld.equals(playerLocation.getWorld().getName()) ? 1.0 : 0.0;
            progress = Math.min(progress, worldProgress);
        }

        // Check coordinate progress
        if (this.requiredCoordinates != null) {
            final Location targetLocation = new Location(
                playerLocation.getWorld(),
                this.requiredCoordinates.getX(),
                this.requiredCoordinates.getY(),
                this.requiredCoordinates.getZ()
            );

            final double distance = playerLocation.distance(targetLocation);
            final double coordinateProgress = this.requiredDistance > 0 ? 
                Math.max(0.0, 1.0 - (distance / this.requiredDistance)) : 
                (distance == 0 ? 1.0 : 0.0);
            progress = Math.min(progress, coordinateProgress);
        }

        // Check region progress
        if (this.requiredRegion != null) {
            final double regionProgress = this.isInRegion(playerLocation, this.requiredRegion) ? 1.0 : 0.0;
            progress = Math.min(progress, regionProgress);
        }

        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Consumes resources from the player to fulfill this requirement.
     * <p>
     * Not applicable for location requirements; this method is a no-op.
     * </p>
     *
     * @param player The player from whom resources would be consumed.
     */
    @Override
    public void consume(
            @NotNull final Player player
    ) {
        // Location is not consumed
    }

    /**
     * Returns the translation key for this requirement's description.
     * <p>
     * This key can be used for localization and user-facing descriptions.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.location";
    }

    /**
     * Gets the required world name.
     *
     * @return The required world name, or null if any world is acceptable.
     */
    @Nullable
    public String getRequiredWorld() {
        return this.requiredWorld;
    }

    /**
     * Gets the required WorldGuard region name.
     *
     * @return The required region name, or null if region checking is disabled.
     */
    @Nullable
    public String getRequiredRegion() {
        return this.requiredRegion;
    }

    /**
     * Gets the required coordinates.
     *
     * @return The required coordinates, or null if coordinate checking is disabled.
     */
    @Nullable
    public Coordinates getRequiredCoordinates() {
        return this.requiredCoordinates;
    }

    /**
     * Gets the required distance from coordinates.
     *
     * @return The maximum distance from coordinates.
     */
    public double getRequiredDistance() {
        return this.requiredDistance;
    }

    /**
     * Gets the optional description for this location requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the current distance from the player to the target coordinates.
     *
     * @param player The player whose distance will be calculated.
     * @return The distance to target coordinates, or -1 if no coordinates are set.
     */
    @JsonIgnore
    public double getCurrentDistance(
            @NotNull final Player player
    ) {
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
     * Checks if the player is in the correct world.
     *
     * @param player The player to check.
     * @return True if in correct world or no world requirement, false otherwise.
     */
    @JsonIgnore
    public boolean isInCorrectWorld(
            @NotNull final Player player
    ) {
        if (this.requiredWorld == null) {
            return true;
        }
        return this.requiredWorld.equals(player.getWorld().getName());
    }

    /**
     * Checks if the player is within the required distance of coordinates.
     *
     * @param player The player to check.
     * @return True if within distance or no coordinate requirement, false otherwise.
     */
    @JsonIgnore
    public boolean isWithinDistance(
            @NotNull final Player player
    ) {
        if (this.requiredCoordinates == null) {
            return true;
        }
        return this.getCurrentDistance(player) <= this.requiredDistance;
    }

    /**
     * Checks if the player is in the required region.
     *
     * @param player The player to check.
     * @return True if in required region or no region requirement, false otherwise.
     */
    @JsonIgnore
    public boolean isInCorrectRegion(
            @NotNull final Player player
    ) {
        if (this.requiredRegion == null) {
            return true;
        }
        return this.isInRegion(player.getLocation(), this.requiredRegion);
    }

    /**
     * Gets a formatted string showing the player's current status relative to requirements.
     *
     * @param player The player whose status will be formatted.
     * @return A formatted status string.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedStatus(
            @NotNull final Player player
    ) {
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
     * Validates the internal state of this location requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
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

        // Validate that required world exists
        if (this.requiredWorld != null) {
            final World world = Bukkit.getWorld(this.requiredWorld);
            if (world == null) {
                throw new IllegalStateException("Required world does not exist: " + this.requiredWorld);
            }
        }
    }

    /**
     * Checks if WorldGuard is available.
     *
     * @return True if WorldGuard is available, false otherwise.
     */
    private boolean isWorldGuardAvailable() {
        if (this.worldGuardAvailable == null) {
            final Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            this.worldGuardAvailable = worldGuardPlugin != null && worldGuardPlugin.isEnabled();
        }
        return this.worldGuardAvailable;
    }

    /**
     * Checks if a location is within a specific WorldGuard region.
     *
     * @param location The location to check.
     * @param regionName The region name to check.
     * @return True if the location is in the region, false otherwise.
     */
    private boolean isInRegion(
            @NotNull final Location location,
            @NotNull final String regionName
    ) {
        if (!this.isWorldGuardAvailable()) {
            LOGGER.log(Level.WARNING, "WorldGuard is not available, region check for '" + regionName + "' will always return false");
            return false;
        }
/*
        //TODO THROUGH PLUGIN SERVICE REGISTRY and WorldGuard / Region SUPPORT
   
        try {
            final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            final RegionQuery query = container.createQuery();
            final ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));

            for (final ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error checking WorldGuard region '" + regionName + "'", exception);
        }
*/

        return false;
    }

    /**
     * Represents coordinates in 3D space.
     */
    public static class Coordinates {
        @JsonProperty("x")
        private final double x;

        @JsonProperty("y")
        private final double y;

        @JsonProperty("z")
        private final double z;

        /**
         * Constructs new Coordinates.
         *
         * @param x The X coordinate.
         * @param y The Y coordinate.
         * @param z The Z coordinate.
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
         * Gets the X coordinate.
         *
         * @return The X coordinate.
         */
        public double getX() {
            return this.x;
        }

        /**
         * Gets the Y coordinate.
         *
         * @return The Y coordinate.
         */
        public double getY() {
            return this.y;
        }

        /**
         * Gets the Z coordinate.
         *
         * @return The Z coordinate.
         */
        public double getZ() {
            return this.z;
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f, %.1f)", this.x, this.y, this.z);
        }
    }
}