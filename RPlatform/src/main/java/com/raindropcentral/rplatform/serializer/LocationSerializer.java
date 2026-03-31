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

package com.raindropcentral.rplatform.serializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Bukkit {@link Location} instances to and from a comma separated textual representation.
 * suitable for configuration storage.
 *
 * <p>Serialized locations store the world name, coordinates and orientation in a fixed order to
 * maintain compatibility across modules.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class LocationSerializer {

    /**
     * Comma delimiter separating world, coordinates and orientation inside serialized locations.
     */
    private static final String DELIMITER = ",";

    /**
     * Number of segments expected when parsing a serialized location string.
     */
    private static final int EXPECTED_PARTS = 6;

    /**
     * Parses a location from a comma separated string in the form.
     * {@code world,x,y,z,yaw,pitch}.
     *
     * <p>World lookup failures, incorrect part counts, or number parsing issues fall back to the
     * default spawn location returned by {@link #createDefaultLocation()}. When no default world is
     * available the method returns {@code null}.</p>
     *
     * @param serialized serialized location string
     * @return parsed location or the configured default when parsing fails
     */
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

    /**
     * Converts a {@link Location} to a comma separated string following the.
     * {@code world,x,y,z,yaw,pitch} schema.
     *
     * <p>An {@link IllegalArgumentException} is thrown when the source location has a {@code null}
     * world reference because the format requires a world name.</p>
     *
     * @param location location to serialize
     * @return serialized representation suitable for storage
     * @throws IllegalArgumentException if the location has no associated world
     */
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

    /**
     * Provides the fallback location used when deserialization fails.
     *
     * <p>The method returns the origin in the {@code world} dimension when present, or {@code null}
     * if that world is not loaded.</p>
     *
     * @return default spawn location or {@code null}
     */
    private @Nullable Location createDefaultLocation() {
        final World world = Bukkit.getWorld("world");
        return world != null ? new Location(world, 0, 0, 0, 0, 0) : null;
    }
}
