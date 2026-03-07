package com.raindropcentral.rplatform.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Custom serializer for Bukkit {@link ItemStack} objects.
 * <p>
 * Converts ItemStack objects to their JSON representation for use with Jackson.
 * Uses Bukkit's built-in serialization to preserve ALL metadata including attributes,
 * item flags, persistent data container, and other complex metadata.
 * </p>
 */
public class ItemStackJSONSerializer extends StdSerializer<ItemStack> {

    public ItemStackJSONSerializer() {
        super(ItemStack.class);
    }

    @Override
    public void serialize(
            @Nullable final ItemStack itemStack,
            @NotNull final JsonGenerator jsonGenerator,
            @NotNull final SerializerProvider serializerProvider
    ) throws IOException {
        if (itemStack == null) {
            jsonGenerator.writeNull();
            return;
        }

        final ItemStack normalizedItemStack = normalizeForSerialization(itemStack);

        jsonGenerator.writeStartObject();

        // Try binary serialization first (preserves ALL metadata)
        try {
            String binaryData = serializeToBinary(normalizedItemStack);
            jsonGenerator.writeStringField("binaryData", binaryData);
            jsonGenerator.writeStringField("serializationType", "binary");
        } catch (Exception e) {
            // Fallback to Map serialization
            Map<String, Object> serialized = normalizedItemStack.serialize();
            jsonGenerator.writeFieldName("mapData");
            serializerProvider.defaultSerializeValue(serialized, jsonGenerator);
            jsonGenerator.writeStringField("serializationType", "map");
        }

        jsonGenerator.writeEndObject();
    }

    /**
     * Serializes an ItemStack to a Base64-encoded binary string using Bukkit's serialization.
     * Uses the modern ItemStack.serializeAsBytes() API introduced in 1.21.
     *
     * @param itemStack the ItemStack to serialize
     * @return Base64-encoded binary data
     */
    private String serializeToBinary(@NotNull ItemStack itemStack) {
        byte[] data = itemStack.serializeAsBytes();
        return Base64.getEncoder().encodeToString(data);
    }

    private @NotNull ItemStack normalizeForSerialization(@NotNull ItemStack itemStack) {
        final ItemStack normalized = itemStack.clone();
        normalized.setAmount(1);
        return normalized;
    }
}
