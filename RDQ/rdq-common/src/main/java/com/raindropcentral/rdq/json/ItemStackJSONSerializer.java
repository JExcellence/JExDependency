package com.raindropcentral.rdq.json;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Base64;
import java.util.Map;

/**
 * Custom serializer for Bukkit {@link ItemStack} objects.
 * <p>
 * Converts {@code ItemStack} objects to their JSON representation for use with Jackson.
 * Uses Bukkit's built-in serialization to preserve ALL metadata including attributes, 
 * item flags, persistent data container, and other complex metadata.
 * </p>
 *
 * <p>
 * The serialization uses two approaches:
 * 1. Bukkit's native serialization (base64 encoded) for complete metadata preservation
 * 2. Fallback to Bukkit's Map serialization for compatibility
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class ItemStackJSONSerializer extends StdSerializer<ItemStack> {

    public ItemStackJSONSerializer() {
        super(ItemStack.class);
    }

    /**
     * Serializes an {@link ItemStack} object to JSON.
     * <p>
     * Uses Bukkit's built-in serialization to preserve ALL metadata.
     * First tries binary serialization (most complete), then falls back to Map serialization.
     * </p>
     *
     * @param itemStack            the {@code ItemStack} to serialize (may be {@code null})
     * @param jsonGenerator        the JSON generator to write to
     * @param serializationContext the serialization context
     */
    @Override
    public void serialize(
            @Nullable final ItemStack itemStack,
            @NotNull final JsonGenerator jsonGenerator,
            @NotNull final SerializationContext serializationContext
    ) {
        if (itemStack == null) {
            jsonGenerator.writeNull();
            return;
        }

        jsonGenerator.writeStartObject();
        
        // Try binary serialization first (preserves ALL metadata)
        try {
            String binaryData = serializeToBinary(itemStack);
            jsonGenerator.writeStringProperty("binaryData", binaryData);
            jsonGenerator.writeStringProperty("serializationType", "binary");
        } catch (Exception e) {
            // Fallback to Map serialization
            Map<String, Object> serialized = itemStack.serialize();
            jsonGenerator.writeName("mapData");
            serializationContext.writeValue(jsonGenerator, serialized);
            jsonGenerator.writeStringProperty("serializationType", "map");
        }
        
        jsonGenerator.writeEndObject();
    }

    /**
     * Serializes an ItemStack to a Base64-encoded binary string using Bukkit's serialization.
     * This preserves ALL metadata including complex data like persistent data containers.
     * Uses the modern ItemStack.serializeAsBytes() API introduced in 1.21.
     *
     * @param itemStack the ItemStack to serialize
     * @return Base64-encoded binary data
     */
    private String serializeToBinary(@NotNull ItemStack itemStack) {
        byte[] data = itemStack.serializeAsBytes();
        return Base64.getEncoder().encodeToString(data);
    }
}
