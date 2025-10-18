package com.raindropcentral.rplatform.serializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocationSerializer {

    private static final String DELIMITER = ",";
    private static final int EXPECTED_PARTS = 6;

    public @Nullable Location deserialize(final @NotNull String serialized) {
        final String[] parts = serialized.split(DELIMITER);
        
        if (parts.length != EXPECTED_PARTS) {
            return createDefaultLocation();
        }

        final World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) {
            return createDefaultLocation();
        }

        try {
            final double x = Double.parseDouble(parts[1].trim());
            final double y = Double.parseDouble(parts[2].trim());
            final double z = Double.parseDouble(parts[3].trim());
            final float yaw = Float.parseFloat(parts[4].trim());
            final float pitch = Float.parseFloat(parts[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
        } catch (final NumberFormatException e) {
            return createDefaultLocation();
        }
    }

    public @NotNull String serialize(final @NotNull Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        return String.format("%s,%s,%s,%s,%s,%s",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    private @Nullable Location createDefaultLocation() {
        final World world = Bukkit.getWorld("world");
        return world != null ? new Location(world, 0, 0, 0, 0, 0) : null;
    }
}
