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

package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Serializes Bukkit {@link Location} values to a semicolon-delimited representation and rebuilds them when.
 * hydrating entities.
 *
 * <p>The converter writes the world UUID followed by the x, y, z, yaw and pitch values using a fixed order.
 * {@code null} attributes map to {@code null} columns, while blank column values return {@code null}
 * attributes. Missing world references or unparseable tokens produce an {@link IllegalArgumentException}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class LocationConverter implements AttributeConverter<Location, String> {

    /** Delimiter separating the world identifier and coordinate tokens. */
    private static final String DELIM = ";";

    /**
     * Serialises the supplied {@link Location} to the fixed-token representation.
     *
     * @param location the location being persisted; may be {@code null}
     * @return {@code null} when the location is {@code null}, or the formatted payload otherwise
     * @throws IllegalArgumentException when the location lacks an associated world
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Location location) {
        if (location == null) {
            return null;
        }
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world; cannot persist.");
        }
        final String worldId = world.getUID().toString();
        return worldId + DELIM + location.getX() + DELIM + location.getY() + DELIM + location.getZ() + DELIM + location.getYaw() + DELIM + location.getPitch();
    }

    /**
     * Recreates a {@link Location} from the stored column payload.
     *
     * @param columnValue the raw database value; blank and {@code null} values return {@code null}
     * @return the reconstructed location, or {@code null} when the column value is blank
     * @throws IllegalArgumentException when the token count is incorrect, parsing fails, or no world matches the stored UUID
     */
    @Override
    public Location convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }

        final String[] tokens = columnValue.split(DELIM, -1);
        if (tokens.length != 6) {
            throw new IllegalArgumentException("Invalid Location token count: expected 6 but got " + tokens.length);
        }

        try {
            final UUID worldUuid = UUID.fromString(tokens[0].trim());
            final World world = Bukkit.getWorld(worldUuid);
            if (world == null) {
                throw new IllegalArgumentException("No world found for UUID: " + worldUuid);
            }
            final double x = Double.parseDouble(tokens[1].trim());
            final double y = Double.parseDouble(tokens[2].trim());
            final double z = Double.parseDouble(tokens[3].trim());
            final float yaw = Float.parseFloat(tokens[4].trim());
            final float pitch = Float.parseFloat(tokens[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
        } catch (RuntimeException ex) {
            // catches IllegalArgumentException from UUID/lookup and NumberFormatException
            throw new IllegalArgumentException("Failed parsing Location from: '" + columnValue + "'", ex);
        }
    }
}
