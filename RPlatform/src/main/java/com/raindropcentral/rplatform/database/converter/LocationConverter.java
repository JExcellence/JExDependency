package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * JPA attribute converter for mapping {@link Location} to a semicolon-delimited string and back.
 *
 * Format: "worldUUID;x;y;z;yaw;pitch"
 *
 * Behavior:
 * - null Location -> null column
 * - null/blank column -> null Location
 */
@Converter(autoApply = true)
public class LocationConverter implements AttributeConverter<Location, String> {

    private static final String DELIM = ";";

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