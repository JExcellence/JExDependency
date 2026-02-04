package com.raindropcentral.rplatform.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * Custom deserializer for Bukkit {@link ItemStack} objects.
 * <p>
 * Reconstructs ItemStack objects from their JSON representation,
 * supporting both binary serialization (complete metadata preservation) and
 * Map serialization (fallback compatibility).
 * </p>
 */
public class ItemStackJSONDeserializer extends StdDeserializer<ItemStack> {

    public ItemStackJSONDeserializer() {
        super(ItemStack.class);
    }

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
     * Uses the modern ItemStack.deserializeBytes() API introduced in 1.21.
     *
     * @param binaryData Base64-encoded binary data
     * @return the deserialized ItemStack
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
     */
    private @NotNull ItemStack deserializeFromMap(@NotNull JsonNode mapNode) {
        try {
            Map<String, Object> map = new HashMap<>();
            mapNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                map.put(entry.getKey(), convertJsonNodeToObject(value));
            });

            return ItemStack.deserialize(map);
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
     *
     * @param node the JSON node in legacy format
     * @return the deserialized ItemStack
     */
    private @NotNull ItemStack deserializeLegacyFormat(@NotNull JsonNode node) {
        if (node.has("type")) {
            String typeStr = node.get("type").asText();
            try {
                Material material = Material.valueOf(typeStr);
                int amount = node.has("amount") ? node.get("amount").asInt(1) : 1;
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid material type: " + typeStr, e);
            }
        }

        throw new RuntimeException("Unable to deserialize ItemStack: missing required fields");
    }
}
