package de.jexcellence.multiverse.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JPA AttributeConverter for serializing Bukkit {@link Location} objects to JSON format.
 * <p>
 * This converter stores location data as a JSON object containing:
 * <ul>
 *   <li>worldUuid - The UUID of the world</li>
 *   <li>worldName - The name of the world (for reference/debugging)</li>
 *   <li>x, y, z - The coordinates</li>
 *   <li>yaw, pitch - The rotation values</li>
 * </ul>
 * </p>
 * <p>
 * Handles null locations gracefully and provides fallback behavior when worlds
 * are not loaded during deserialization.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Converter
public class LocationConverter implements AttributeConverter<Location, String> {

    private static final Logger LOGGER = Logger.getLogger(LocationConverter.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Converts a {@link Location} to its JSON string representation.
     *
     * @param location the location to convert; may be {@code null}
     * @return the JSON string representation, or {@code null} if location is null
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Location location) {
        if (location == null) {
            return null;
        }

        final World world = location.getWorld();
        if (world == null) {
            LOGGER.log(Level.WARNING, "Cannot serialize location without world reference");
            return null;
        }

        try {
            final ObjectNode node = MAPPER.createObjectNode();
            node.put("worldUuid", world.getUID().toString());
            node.put("worldName", world.getName());
            node.put("x", location.getX());
            node.put("y", location.getY());
            node.put("z", location.getZ());
            node.put("yaw", location.getYaw());
            node.put("pitch", location.getPitch());

            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize location to JSON", e);
            return null;
        }
    }

    /**
     * Converts a JSON string back to a {@link Location} object.
     *
     * @param json the JSON string to convert; may be {@code null} or blank
     * @return the reconstructed location, or {@code null} if JSON is null/blank or world not found
     */
    @Override
    public Location convertToEntityAttribute(@Nullable final String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            final JsonNode node = MAPPER.readTree(json);

            final String worldUuidStr = node.has("worldUuid") ? node.get("worldUuid").asText() : null;
            final String worldName = node.has("worldName") ? node.get("worldName").asText() : null;

            World world = null;

            // Try to find world by UUID first
            if (worldUuidStr != null && !worldUuidStr.isBlank()) {
                try {
                    final UUID worldUuid = UUID.fromString(worldUuidStr);
                    world = Bukkit.getWorld(worldUuid);
                } catch (IllegalArgumentException ignored) {
                    // Invalid UUID format, try by name
                }
            }

            // Fallback to world name if UUID lookup failed
            if (world == null && worldName != null && !worldName.isBlank()) {
                world = Bukkit.getWorld(worldName);
            }

            if (world == null) {
                LOGGER.log(Level.WARNING, "World not found for location: UUID={0}, Name={1}",
                        new Object[]{worldUuidStr, worldName});
                return null;
            }

            final double x = node.get("x").asDouble();
            final double y = node.get("y").asDouble();
            final double z = node.get("z").asDouble();
            final float yaw = (float) node.get("yaw").asDouble();
            final float pitch = (float) node.get("pitch").asDouble();

            return new Location(world, x, y, z, yaw, pitch);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize location from JSON: " + json, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deserializing location: " + json, e);
            return null;
        }
    }
}
