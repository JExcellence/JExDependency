package de.jexcellence.oneblock.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optimized converter for serializing Bukkit {@link Location} values to a semicolon-delimited representation
 * and rebuilding them when hydrating entities.
 *
 * <p>The converter writes the world UUID followed by the x, y, z, yaw and pitch values using a fixed order.
 * {@code null} attributes map to {@code null} columns, while blank column values return {@code null}
 * attributes. Missing world references or unparseable tokens produce an {@link IllegalArgumentException}.</p>
 *
 * <p>Performance optimizations include:</p>
 * <ul>
 *   <li>World UUID caching to reduce Bukkit.getWorld() calls</li>
 *   <li>StringBuilder for efficient string concatenation</li>
 *   <li>Thread-safe world cache with concurrent map</li>
 *   <li>Proper error handling and logging</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
@Converter(autoApply = true)
public class LocationConverter implements AttributeConverter<Location, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationConverter.class);
    
    /** Delimiter separating the world identifier and coordinate tokens. */
    private static final String DELIM = ";";
    
    /** Expected number of tokens in a serialized location. */
    private static final int EXPECTED_TOKEN_COUNT = 6;
    
    /** Thread-safe cache for world lookups to improve performance. */
    private static final ConcurrentMap<UUID, World> WORLD_CACHE = new ConcurrentHashMap<>();
    
    /** Cache size limit to prevent memory leaks. */
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * Serializes the supplied {@link Location} to the fixed-token representation.
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
            LOGGER.warn("Attempting to persist Location with null world: {}", location);
            throw new IllegalArgumentException("Location has no world; cannot persist.");
        }
        
        // Use StringBuilder for efficient string concatenation
        final StringBuilder builder = new StringBuilder(128); // Pre-allocate reasonable capacity
        builder.append(world.getUID().toString())
               .append(DELIM).append(location.getX())
               .append(DELIM).append(location.getY())
               .append(DELIM).append(location.getZ())
               .append(DELIM).append(location.getYaw())
               .append(DELIM).append(location.getPitch());
        
        return builder.toString();
    }

    /**
     * Recreates a {@link Location} from the stored column payload with optimized world lookup caching.
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

        final String[] tokens = columnValue.split(DELIM, EXPECTED_TOKEN_COUNT);
        if (tokens.length != EXPECTED_TOKEN_COUNT) {
            LOGGER.error("Invalid Location token count in '{}': expected {} but got {}", 
                        columnValue, EXPECTED_TOKEN_COUNT, tokens.length);
            throw new IllegalArgumentException("Invalid Location token count: expected " + EXPECTED_TOKEN_COUNT + " but got " + tokens.length);
        }

        try {
            final UUID worldUuid = UUID.fromString(tokens[0].trim());
            final World world = getWorldWithCaching(worldUuid);
            
            if (world == null) {
                LOGGER.error("No world found for UUID: {} in location data: {}", worldUuid, columnValue);
                throw new IllegalArgumentException("No world found for UUID: " + worldUuid);
            }
            
            final double x = Double.parseDouble(tokens[1].trim());
            final double y = Double.parseDouble(tokens[2].trim());
            final double z = Double.parseDouble(tokens[3].trim());
            final float yaw = Float.parseFloat(tokens[4].trim());
            final float pitch = Float.parseFloat(tokens[5].trim());

            return new Location(world, x, y, z, yaw, pitch);
            
        } catch (NumberFormatException ex) {
            LOGGER.error("Failed parsing numeric values from Location data: '{}'", columnValue, ex);
            throw new IllegalArgumentException("Failed parsing Location from: '" + columnValue + "'", ex);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Failed parsing UUID from Location data: '{}'", columnValue, ex);
            throw new IllegalArgumentException("Failed parsing Location from: '" + columnValue + "'", ex);
        }
    }
    
    /**
     * Gets a world by UUID with caching for improved performance.
     * 
     * @param worldUuid the world UUID to lookup
     * @return the world instance, or null if not found
     */
    private World getWorldWithCaching(final UUID worldUuid) {
        // Check cache first
        World cachedWorld = WORLD_CACHE.get(worldUuid);
        if (cachedWorld != null) {
            return cachedWorld;
        }
        
        // Lookup from Bukkit
        final World world = Bukkit.getWorld(worldUuid);
        if (world != null) {
            // Cache the result, but prevent unbounded growth
            if (WORLD_CACHE.size() < MAX_CACHE_SIZE) {
                WORLD_CACHE.put(worldUuid, world);
            } else {
                LOGGER.debug("World cache at maximum size ({}), not caching world: {}", MAX_CACHE_SIZE, worldUuid);
            }
        }
        
        return world;
    }
    
    /**
     * Clears the world cache. Useful for testing or when worlds are unloaded.
     */
    public static void clearWorldCache() {
        WORLD_CACHE.clear();
        LOGGER.debug("World cache cleared");
    }
}