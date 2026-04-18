package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Requires the player to be within a specified radius of a location.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class LocationRequirement extends AbstractRequirement {

    @JsonProperty("world")
    private final String world;

    @JsonProperty("x")
    private final double x;

    @JsonProperty("y")
    private final double y;

    @JsonProperty("z")
    private final double z;

    @JsonProperty("radius")
    private final double radius;

    /**
     * Creates a location requirement.
     *
     * @param world  the world name (null for any world)
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param z      the z coordinate
     * @param radius the maximum distance
     */
    public LocationRequirement(@JsonProperty("world") @Nullable String world,
                               @JsonProperty("x") double x,
                               @JsonProperty("y") double y,
                               @JsonProperty("z") double z,
                               @JsonProperty("radius") double radius) {
        super("LOCATION");
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(0, radius);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        var loc = player.getLocation();

        if (world != null && (loc.getWorld() == null
                || !loc.getWorld().getName().equals(world))) {
            return false;
        }

        var dx = loc.getX() - x;
        var dy = loc.getY() - y;
        var dz = loc.getZ() - z;
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        return isMet(player) ? 1.0 : 0.0;
    }

    @Override
    public void consume(@NotNull Player player) {
        // Location is not consumable
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.location";
    }
}
