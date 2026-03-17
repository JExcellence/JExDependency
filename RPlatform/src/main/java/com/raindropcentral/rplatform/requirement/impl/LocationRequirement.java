/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
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
 * Represents the LocationRequirement API type.
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

    /**
     * Executes LocationRequirement.
     */
    public LocationRequirement(@NotNull String requiredWorld, double x, double y, double z, double requiredDistance) {
        this(requiredWorld, null, new Coordinates(x, y, z), requiredDistance, null);
    }

    /**
     * Executes LocationRequirement.
     */
    public LocationRequirement(@NotNull String requiredWorld, @NotNull String requiredRegion) {
        this(requiredWorld, requiredRegion, null, 0.0, null);
    }

    /**
     * Executes LocationRequirement.
     */
    @JsonCreator
    public LocationRequirement(@JsonProperty("requiredWorld") @Nullable String requiredWorld,
                              @JsonProperty("requiredRegion") @Nullable String requiredRegion,
                              @JsonProperty("requiredCoordinates") @Nullable Coordinates requiredCoordinates,
                              @JsonProperty("requiredDistance") @Nullable Double requiredDistance,
                              @JsonProperty("description") @Nullable String description) {
        super("LOCATION");

        if (requiredWorld == null && requiredRegion == null && requiredCoordinates == null) {
            throw new IllegalArgumentException("At least one location criterion must be specified.");
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

    /**
     * Returns whether met.
     */
    @Override
    public boolean isMet(@NotNull Player player) {
        var playerLocation = player.getLocation();

        if (requiredWorld != null && !requiredWorld.equals(playerLocation.getWorld().getName())) return false;

        if (requiredCoordinates != null) {
            var targetLocation = new Location(playerLocation.getWorld(),
                    requiredCoordinates.getX(), requiredCoordinates.getY(), requiredCoordinates.getZ());
            if (playerLocation.distance(targetLocation) > requiredDistance) return false;
        }

        if (requiredRegion != null && !isInRegion(playerLocation, requiredRegion)) return false;

        return true;
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(@NotNull Player player) {
        var playerLocation = player.getLocation();
        var progress = 1.0;

        if (requiredWorld != null) {
            progress = Math.min(progress, requiredWorld.equals(playerLocation.getWorld().getName()) ? 1.0 : 0.0);
        }

        if (requiredCoordinates != null) {
            var targetLocation = new Location(playerLocation.getWorld(),
                    requiredCoordinates.getX(), requiredCoordinates.getY(), requiredCoordinates.getZ());
            var distance = playerLocation.distance(targetLocation);
            var coordinateProgress = requiredDistance > 0 ? Math.max(0.0, 1.0 - (distance / requiredDistance)) : (distance == 0 ? 1.0 : 0.0);
            progress = Math.min(progress, coordinateProgress);
        }

        if (requiredRegion != null) {
            progress = Math.min(progress, isInRegion(playerLocation, requiredRegion) ? 1.0 : 0.0);
        }

        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(@NotNull Player player) {}

    /**
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() { return "requirement.location"; }

    /**
     * Gets requiredWorld.
     */
    @Nullable
    public String getRequiredWorld() { return this.requiredWorld; }

    /**
     * Gets requiredRegion.
     */
    @Nullable
    public String getRequiredRegion() { return this.requiredRegion; }

    /**
     * Gets requiredCoordinates.
     */
    @Nullable
    public Coordinates getRequiredCoordinates() { return this.requiredCoordinates; }

    /**
     * Gets requiredDistance.
     */
    public double getRequiredDistance() { return this.requiredDistance; }

    /**
     * Gets description.
     */
    @Nullable
    public String getDescription() { return this.description; }

    /**
     * Gets currentDistance.
     */
    @JsonIgnore
    public double getCurrentDistance(final @NotNull Player player) {
        if (this.requiredCoordinates == null) return -1.0;
        final Location playerLocation = player.getLocation();
        final Location targetLocation = new Location(playerLocation.getWorld(),
                this.requiredCoordinates.getX(), this.requiredCoordinates.getY(), this.requiredCoordinates.getZ());
        return playerLocation.distance(targetLocation);
    }

    /**
     * Returns whether inCorrectWorld.
     */
    @JsonIgnore
    public boolean isInCorrectWorld(final @NotNull Player player) {
        return this.requiredWorld == null || this.requiredWorld.equals(player.getWorld().getName());
    }

    /**
     * Returns whether withinDistance.
     */
    @JsonIgnore
    public boolean isWithinDistance(final @NotNull Player player) {
        return this.requiredCoordinates == null || this.getCurrentDistance(player) <= this.requiredDistance;
    }

    /**
     * Returns whether inCorrectRegion.
     */
    @JsonIgnore
    public boolean isInCorrectRegion(final @NotNull Player player) {
        return this.requiredRegion == null || this.isInRegion(player.getLocation(), this.requiredRegion);
    }

    /**
     * Gets formattedStatus.
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
            status.append("Distance: ").append(String.format("%.1f", this.getCurrentDistance(player)))
                    .append(this.isWithinDistance(player) ? " ✓" : " ✗")
                    .append(" (Max: ").append(String.format("%.1f", this.requiredDistance)).append(")");
        }
        if (this.requiredRegion != null) {
            if (status.length() > 0) status.append(", ");
            status.append("Region: ").append(this.requiredRegion).append(this.isInCorrectRegion(player) ? " ✓" : " ✗");
        }
        return status.toString();
    }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredWorld == null && this.requiredRegion == null && this.requiredCoordinates == null)
            throw new IllegalStateException("At least one location criterion must be specified.");
        if (this.requiredCoordinates != null && this.requiredDistance < 0)
            throw new IllegalStateException("Required distance must be non-negative.");
        if (this.requiredWorld != null && this.requiredWorld.trim().isEmpty())
            throw new IllegalStateException("Required world name cannot be empty.");
        if (this.requiredRegion != null && this.requiredRegion.trim().isEmpty())
            throw new IllegalStateException("Required region name cannot be empty.");
        if (this.requiredWorld != null && this.getCachedWorld() == null)
            throw new IllegalStateException("Required world does not exist: " + this.requiredWorld);
    }

    @Nullable
    private World getCachedWorld() {
        if (this.requiredWorld == null) return null;
        if (this.cachedWorld != null) return this.cachedWorld;
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
            LOGGER.log(Level.WARNING, "WorldGuard is not available, region check for '" + regionName + "' will always return false");
            return false;
        }
        return false;
    }

    /**
     * Represents the Coordinates API type.
     */
    public static final class Coordinates {
        @JsonProperty("x")
        private final double x;
        @JsonProperty("y")
        private final double y;
        @JsonProperty("z")
        private final double z;

        /**
         * Executes Coordinates.
         */
        @JsonCreator
        public Coordinates(@JsonProperty("x") final double x, @JsonProperty("y") final double y, @JsonProperty("z") final double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Gets x.
         */
        public double getX() { return this.x; }
        /**
         * Gets y.
         */
        public double getY() { return this.y; }
        /**
         * Gets z.
         */
        public double getZ() { return this.z; }

        /**
         * Executes toString.
         */
        @Override
        public String toString() { return String.format("(%.1f, %.1f, %.1f)", this.x, this.y, this.z); }
    }
}
