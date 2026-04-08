package de.jexcellence.oneblock.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optimized converter for {@link List lists} of {@link Material} entries to a semicolon-separated 
 * column string and rebuilds them for entity hydration.
 *
 * <p>The converter stores each {@link Material#name()} token in upper-case form, skipping {@code null}
 * entries. {@code null} lists correspond to {@code null} columns, while blank column values become empty
 * lists. Invalid tokens are handled gracefully with fallback mechanisms and comprehensive logging.</p>
 *
 * <p>Performance optimizations include:</p>
 * <ul>
 *   <li>Pre-allocated collections with appropriate initial capacity</li>
 *   <li>Efficient string building with StringBuilder</li>
 *   <li>Material validation caching to reduce enum lookups</li>
 *   <li>Graceful fallback handling for invalid materials</li>
 *   <li>Thread-safe validation cache</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
@Converter(autoApply = true)
public class MaterialListConverter implements AttributeConverter<List<Material>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialListConverter.class);
    
    /** Delimiter separating material tokens within the stored column string. */
    private static final String DELIM = ";";
    
    /** Thread-safe cache for material validation results to improve performance. */
    private static final ConcurrentMap<String, Material> MATERIAL_CACHE = new ConcurrentHashMap<>();
    
    /** Set of valid material names for quick validation. */
    private static final Set<String> VALID_MATERIAL_NAMES = EnumSet.allOf(Material.class)
            .stream()
            .map(Material::name)
            .collect(java.util.stream.Collectors.toSet());
    
    /** Maximum cache size to prevent memory leaks. */
    private static final int MAX_CACHE_SIZE = 200;
    
    /** Fallback material for invalid entries. */
    private static final Material FALLBACK_MATERIAL = Material.STONE;

    /**
     * Serializes the provided materials to the joined string representation with optimized string building.
     *
     * @param materials the list being persisted; may be {@code null}
     * @return {@code null} when the list is {@code null}, an empty string for empty lists, or the joined payload otherwise
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final List<Material> materials) {
        if (materials == null) {
            return null;
        }
        if (materials.isEmpty()) {
            return "";
        }
        
        // Pre-allocate StringBuilder with reasonable capacity
        final StringBuilder result = new StringBuilder(materials.size() * 16);
        boolean first = true;
        
        for (final Material material : materials) {
            if (material == null) {
                continue; // Skip null materials
            }
            
            if (!first) {
                result.append(DELIM);
            }
            first = false;
            
            result.append(material.name());
        }
        
        return result.toString();
    }

    /**
     * Rehydrates a list of {@link Material} values from the stored column payload with graceful error handling.
     *
     * @param columnValue the raw database value; {@code null} yields {@code null} and blank values produce an empty list
     * @return the reconstructed list, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when critical parsing errors occur that cannot be recovered
     */
    @Override
    public List<Material> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        final String[] parts = columnValue.split(DELIM, -1);
        // Pre-allocate with exact capacity for better performance
        final List<Material> result = new ArrayList<>(parts.length);
        int invalidCount = 0;

        for (int i = 0; i < parts.length; i++) {
            final String token = parts[i] == null ? "" : parts[i].trim();
            if (token.isEmpty()) {
                continue; // Skip empty tokens
            }
            
            final Material material = parseMaterialWithFallback(token, i);
            if (material != null) {
                result.add(material);
                if (material == FALLBACK_MATERIAL && !token.equalsIgnoreCase(FALLBACK_MATERIAL.name())) {
                    invalidCount++;
                }
            }
        }
        
        if (invalidCount > 0) {
            LOGGER.warn("Converted {} invalid materials to fallback ({}) from column: '{}'", 
                       invalidCount, FALLBACK_MATERIAL.name(), columnValue);
        }

        return result;
    }
    
    /**
     * Parses a material token with caching and fallback handling.
     * 
     * @param token the material name token
     * @param index the index in the list for error reporting
     * @return the parsed material, fallback material, or null if token should be skipped
     */
    private Material parseMaterialWithFallback(final String token, final int index) {
        final String upperToken = token.toUpperCase(Locale.ROOT);
        
        // Check cache first
        Material cachedMaterial = MATERIAL_CACHE.get(upperToken);
        if (cachedMaterial != null) {
            return cachedMaterial;
        }
        
        try {
            // Quick validation check before expensive valueOf
            if (!VALID_MATERIAL_NAMES.contains(upperToken)) {
                LOGGER.warn("Invalid Material at index {}: '{}' - using fallback: {}", 
                           index, token, FALLBACK_MATERIAL.name());
                cacheMaterial(upperToken, FALLBACK_MATERIAL);
                return FALLBACK_MATERIAL;
            }
            
            final Material material = Material.valueOf(upperToken);
            cacheMaterial(upperToken, material);
            return material;
            
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Failed to parse Material at index {}: '{}' - using fallback: {}", 
                       index, token, FALLBACK_MATERIAL.name(), ex);
            cacheMaterial(upperToken, FALLBACK_MATERIAL);
            return FALLBACK_MATERIAL;
        }
    }
    
    /**
     * Caches a material lookup result if cache size permits.
     * 
     * @param token the material name token
     * @param material the resolved material
     */
    private void cacheMaterial(final String token, final Material material) {
        if (MATERIAL_CACHE.size() < MAX_CACHE_SIZE) {
            MATERIAL_CACHE.put(token, material);
        } else {
            LOGGER.debug("Material cache at maximum size ({}), not caching token: {}", MAX_CACHE_SIZE, token);
        }
    }
    
    /**
     * Clears the material cache. Useful for testing or memory management.
     */
    public static void clearMaterialCache() {
        MATERIAL_CACHE.clear();
        LOGGER.debug("Material cache cleared");
    }
    
    /**
     * Gets the current cache size for monitoring purposes.
     * 
     * @return the current number of cached material lookups
     */
    public static int getCacheSize() {
        return MATERIAL_CACHE.size();
    }
}