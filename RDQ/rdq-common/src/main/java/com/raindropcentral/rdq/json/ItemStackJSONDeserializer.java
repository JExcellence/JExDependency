package com.raindropcentral.rdq.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom deserializer for Bukkit {@link ItemStack} objects.
 * <p>
 * This class reconstructs {@code ItemStack} objects from their JSON representation,
 * supporting both binary serialization (complete metadata preservation) and 
 * Map serialization (fallback compatibility).
 * </p>
 *
 * <p>
 * Handles two serialization formats:
 * 1. Binary format: Complete Bukkit serialization preserving ALL metadata
 * 2. Map format: Bukkit's Map serialization for compatibility
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class ItemStackJSONDeserializer extends JsonDeserializer<ItemStack> {

    /**
     * Deserializes a JSON representation into an {@link ItemStack} object.
     * <p>
     * Supports both binary and map serialization formats for maximum compatibility
     * and complete metadata preservation.
     * </p>
     *
     * @param jsonParser             the JSON parser
     * @param deserializationContext the deserialization context
     * @return the deserialized {@code ItemStack}
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public @NotNull ItemStack deserialize(
            @NotNull final JsonParser jsonParser,
            @NotNull final DeserializationContext deserializationContext
    ) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.has("serializationType")) {
            String serializationType = node.get("serializationType").asText();
            
            if ("binary".equals(serializationType) && node.has("binaryData")) {
                return deserializeFromBinary(node.get("binaryData").asText());
            } else if ("map".equals(serializationType) && node.has("mapData")) {
                return deserializeFromMap(node.get("mapData"));
            }
        }
        
        // Fallback to legacy format for backward compatibility
        return deserializeLegacyFormat(node);
    }

    /**
     * Deserializes an ItemStack from Base64-encoded binary data.
     * This preserves ALL metadata including complex data like persistent data containers.
     *
     * @param binaryData Base64-encoded binary data
     * @return the deserialized ItemStack
     * @throws IOException if deserialization fails
     */
    private @NotNull ItemStack deserializeFromBinary(@NotNull String binaryData) throws IOException {
        try {
            byte[] data = Base64.getDecoder().decode(binaryData);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                 BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                
                return (ItemStack) dataInput.readObject();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize ItemStack from binary data", e);
        }
    }

    /**
     * Deserializes an ItemStack from Bukkit's Map serialization format.
     *
     * @param mapNode the JSON node containing the map data
     * @return the deserialized ItemStack
     * @throws IOException if deserialization fails
     */
    private @NotNull ItemStack deserializeFromMap(@NotNull JsonNode mapNode) throws IOException {
        try {
            Map<String, Object> map = new HashMap<>();
            mapNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    map.put(entry.getKey(), value.asText());
                } else if (value.isInt()) {
                    map.put(entry.getKey(), value.asInt());
                } else if (value.isDouble()) {
                    map.put(entry.getKey(), value.asDouble());
                } else if (value.isBoolean()) {
                    map.put(entry.getKey(), value.asBoolean());
                } else if (value.isObject() || value.isArray()) {
                    // Handle nested objects/arrays - convert back to map/list
                    map.put(entry.getKey(), convertJsonNodeToObject(value));
                }
            });
            
            return ItemStack.deserialize(map);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize ItemStack from map data", e);
        }
    }

    /**
     * Converts a JsonNode to appropriate Java object for Map deserialization.
     *
     * @param node the JsonNode to convert
     * @return the converted object
     */
    private Object convertJsonNodeToObject(@NotNull JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(element -> list.add(convertJsonNodeToObject(element)));
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            node.fields().forEachRemaining(entry -> 
                map.put(entry.getKey(), convertJsonNodeToObject(entry.getValue())));
            return map;
        }
        return null;
    }

    /**
     * Fallback method for deserializing legacy format ItemStacks.
     * Maintains backward compatibility with old serialization format.
     *
     * @param node the JSON node in legacy format
     * @return the deserialized ItemStack
     * @throws IOException if deserialization fails
     */
    private @NotNull ItemStack deserializeLegacyFormat(@NotNull JsonNode node) throws IOException {
        // This would contain the old deserialization logic for backward compatibility
        // For now, we'll create a basic ItemStack and log a warning
        if (node.has("type")) {
            String typeStr = node.get("type").asText();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(typeStr);
                int amount = node.has("amount") ? node.get("amount").asInt(1) : 1;
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid material type: " + typeStr, e);
            }
        }
        
        throw new IOException("Unable to deserialize ItemStack: missing required fields");
    }
}
