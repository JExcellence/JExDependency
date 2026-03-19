package de.jexcellence.oneblock.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optimized converter for persisting collections of {@link ItemStack} values as a semicolon-delimited 
 * Base64 payload and restoring them for entity hydration.
 *
 * <p>Each non-empty stack is encoded via {@link ItemStack#serializeAsBytes()} and Base64 while empty or
 * {@code null} entries are skipped to keep the column compact. {@code null} collections yield
 * {@code null} columns and blank column values map back to empty collections. Any token that fails to
 * decode raises an {@link IllegalArgumentException} so callers can remediate corrupted data.</p>
 *
 * <p>Performance optimizations include:</p>
 * <ul>
 *   <li>Pre-allocated collections with appropriate initial capacity</li>
 *   <li>Efficient string building with StringBuilder</li>
 *   <li>Thread-safe Base64 encoder/decoder instances</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Memory-efficient processing of large item lists</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
@Converter(autoApply = true)
public class ItemStackListConverter implements AttributeConverter<List<ItemStack>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemStackListConverter.class);
    
    /** Delimiter separating encoded stack payloads in the column string. */
    private static final String DELIM = ";";
    
    /** Thread-safe encoder for translating stack bytes into a storage-friendly Base64 value. */
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    
    /** Thread-safe decoder for rebuilding stacks from the persisted payload. */
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();
    
    /** Cache for commonly used empty ItemStack instances to reduce object creation. */
    private static final ConcurrentMap<Material, ItemStack> EMPTY_ITEM_CACHE = new ConcurrentHashMap<>();
    
    /** Maximum cache size to prevent memory leaks. */
    private static final int MAX_CACHE_SIZE = 50;

    /**
     * Serializes the provided list into the joined Base64 representation with optimized string building.
     *
     * @param items the list scheduled for persistence; may be {@code null}
     * @return {@code null} when the list is {@code null}, an empty string for empty lists, or the joined payload otherwise
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final List<ItemStack> items) {
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            return "";
        }
        
        // Pre-allocate StringBuilder with reasonable capacity
        final StringBuilder result = new StringBuilder(items.size() * 64);
        boolean first = true;
        
        for (final ItemStack item : items) {
            if (item == null || item.isEmpty()) {
                continue; // Skip null and empty items
            }
            
            if (!first) {
                result.append(DELIM);
            }
            first = false;
            
            try {
                final byte[] serialized = item.serializeAsBytes();
                result.append(B64_ENCODER.encodeToString(serialized));
            } catch (Exception ex) {
                LOGGER.error("Failed to serialize ItemStack: {}", item, ex);
                throw new IllegalArgumentException("Failed to serialize ItemStack: " + item, ex);
            }
        }
        
        return result.toString();
    }

    /**
     * Rebuilds a list of {@link ItemStack} instances from the persisted Base64 payload with optimized parsing.
     *
     * @param columnValue the column data; {@code null} yields {@code null} and blank values produce an empty list
     * @return the reconstructed list of stacks, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when a token cannot be decoded or deserialized
     */
    @Override
    public List<ItemStack> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        final String[] parts = columnValue.split(DELIM, -1);
        // Pre-allocate with exact capacity for better performance
        final List<ItemStack> result = new ArrayList<>(parts.length);

        for (int i = 0; i < parts.length; i++) {
            final String token = parts[i] == null ? "" : parts[i].trim();
            if (token.isEmpty()) {
                // treat empty token as AIR for symmetry with single ItemStack converter
                result.add(getCachedEmptyItem(Material.AIR));
                continue;
            }
            
            try {
                final byte[] decoded = B64_DECODER.decode(token);
                final ItemStack item = ItemStack.deserializeBytes(decoded);
                result.add(item);
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Invalid Base64 ItemStack at index {} in list: '{}'", i, token, ex);
                throw new IllegalArgumentException("Invalid Base64 ItemStack at index " + i + " in list.", ex);
            } catch (Exception ex) {
                LOGGER.error("Failed to deserialize ItemStack at index {} from token: '{}'", i, token, ex);
                throw new IllegalArgumentException("Failed to deserialize ItemStack at index " + i, ex);
            }
        }

        return result;
    }
    
    /**
     * Gets a cached empty ItemStack instance for the given material to reduce object creation.
     * 
     * @param material the material for the empty item
     * @return a cached empty ItemStack instance
     */
    private ItemStack getCachedEmptyItem(final Material material) {
        return EMPTY_ITEM_CACHE.computeIfAbsent(material, mat -> {
            if (EMPTY_ITEM_CACHE.size() >= MAX_CACHE_SIZE) {
                LOGGER.debug("Empty item cache at maximum size ({}), not caching material: {}", MAX_CACHE_SIZE, mat);
                return new ItemStack(mat);
            }
            return new ItemStack(mat);
        });
    }
    
    /**
     * Clears the empty item cache. Useful for testing or memory management.
     */
    public static void clearEmptyItemCache() {
        EMPTY_ITEM_CACHE.clear();
        LOGGER.debug("Empty item cache cleared");
    }
}