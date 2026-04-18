package de.jexcellence.jexplatform.serializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Bukkit {@link Location} instances to and from a comma-separated
 * textual representation suitable for configuration storage.
 *
 * <p>Format: {@code world,x,y,z,yaw,pitch}. Uses world names for
 * human-readable configs (unlike the JPA converter which uses world UUIDs
 * for database robustness).
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class LocationSerializer {

    private static final String DELIMITER = ",";
    private static final int EXPECTED_PARTS = 6;

    private LocationSerializer() {
    }

    /**
     * Serializes a {@link Location} to a comma-separated string.
     *
     * @param location location to serialize
     * @return serialized representation
     * @throws IllegalArgumentException if the location has no associated world
     */
    public static @NotNull String serialize(@NotNull Location location) {
        var world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        return String.format("%s,%s,%s,%s,%s,%s",
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Parses a location from a comma-separated string.
     *
     * <p>Falls back to the default spawn location when parsing fails.
     *
     * @param serialized serialized location string
     * @return parsed location, or the default when parsing fails
     */
    public static @Nullable Location deserialize(@NotNull String serialized) {
        var parts = serialized.split(DELIMITER);
        if (parts.length != EXPECTED_PARTS) {
            return createDefaultLocation();
        }

        var world = Bukkit.getWorld(parts[0].trim());
        if (world == null) {
            return createDefaultLocation();
        }

        try {
            var x = Double.parseDouble(parts[1].trim());
            var y = Double.parseDouble(parts[2].trim());
            var z = Double.parseDouble(parts[3].trim());
            var yaw = Float.parseFloat(parts[4].trim());
            var pitch = Float.parseFloat(parts[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return createDefaultLocation();
        }
    }

    private static @Nullable Location createDefaultLocation() {
        var world = Bukkit.getWorld("world");
        return world != null ? new Location(world, 0, 0, 0, 0, 0) : null;
    }
}
