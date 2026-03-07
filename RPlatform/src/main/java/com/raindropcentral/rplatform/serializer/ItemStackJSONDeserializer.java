package com.raindropcentral.rplatform.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

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
public class ItemStackJSONDeserializer extends StdDeserializer<ItemStack> {

    public ItemStackJSONDeserializer() {
        super(ItemStack.class);
    }

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
     */
    @Override
    public @NotNull ItemStack deserialize(
            @NotNull final JsonParser jsonParser,
            @NotNull final DeserializationContext deserializationContext
    ) throws IOException {
        final JsonNode node = jsonParser.readValueAsTree();

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
     * Uses the modern ItemStack.deserializeBytes() API introduced in 1.21.
     *
     * @param binaryData Base64-encoded binary data
     * @return the deserialized ItemStack
     * @throws RuntimeException if deserialization fails
     */
    private @NotNull ItemStack deserializeFromBinary(@NotNull String binaryData) {
        try {
            byte[] data = Base64.getDecoder().decode(binaryData);
            return ItemStack.deserializeBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ItemStack from binary data", e);
        }
    }

    /**
     * Deserializes an ItemStack from Bukkit's Map serialization format.
     *
     * @param mapNode the JSON node containing the map data
     * @return the deserialized ItemStack
     * @throws RuntimeException if deserialization fails
     */
    private @NotNull ItemStack deserializeFromMap(@NotNull JsonNode mapNode) {
        try {
            Map<String, Object> map = new HashMap<>();
            mapNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                map.put(entry.getKey(), convertJsonNodeToObject(value));
            });
            
            final int amount = resolveAmount(map.get("amount"));
            map.put("amount", 1);

            final ItemStack itemStack = ItemStack.deserialize(map);
            itemStack.setAmount(amount);
            return itemStack;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ItemStack from map data", e);
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
            List<Object> list = new ArrayList<>();
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
     * @throws RuntimeException if deserialization fails
     */
    private @NotNull ItemStack deserializeLegacyFormat(@NotNull JsonNode node) {
        if (node.has("type")) {
            String typeStr = node.get("type").asText();
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(typeStr);
                int amount = node.has("amount") ? node.get("amount").asInt(1) : 1;
                return createItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid material type: " + typeStr, e);
            }
        }
        
        throw new RuntimeException("Unable to deserialize ItemStack: missing required fields");
    }

    private int resolveAmount(@Nullable Object rawAmount) {
        if (rawAmount instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (rawAmount instanceof String text) {
            try {
                return Math.max(1, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private @NotNull ItemStack createItemStack(
            @NotNull org.bukkit.Material material,
            int amount
    ) {
        final ItemStack itemStack = new ItemStack(material, 1);
        itemStack.setAmount(Math.max(1, amount));
        return itemStack;
    }
}
