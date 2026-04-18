package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * JPA converter that persists Bukkit {@link Location} values as semicolon-delimited
 * strings using world UUID for database robustness.
 *
 * <p>Format: {@code worldUUID;x;y;z;yaw;pitch}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class LocationConverter implements AttributeConverter<Location, String> {

    private static final String DELIM = ";";

    @Override
    public String convertToDatabaseColumn(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        var world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world; cannot persist.");
        }
        return world.getUID() + DELIM
                + location.getX() + DELIM
                + location.getY() + DELIM
                + location.getZ() + DELIM
                + location.getYaw() + DELIM
                + location.getPitch();
    }

    @Override
    public Location convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }

        var tokens = columnValue.split(DELIM, -1);
        if (tokens.length != 6) {
            throw new IllegalArgumentException(
                    "Invalid Location token count: expected 6 but got " + tokens.length);
        }

        try {
            var worldUuid = UUID.fromString(tokens[0].trim());
            var world = Bukkit.getWorld(worldUuid);
            if (world == null) {
                throw new IllegalArgumentException(
                        "No world found for UUID: " + worldUuid);
            }
            var x = Double.parseDouble(tokens[1].trim());
            var y = Double.parseDouble(tokens[2].trim());
            var z = Double.parseDouble(tokens[3].trim());
            var yaw = Float.parseFloat(tokens[4].trim());
            var pitch = Float.parseFloat(tokens[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Failed parsing Location from: '" + columnValue + "'", ex);
        }
    }
}
