package com.raindropcentral.rdq.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class ItemStackJSONSerializer extends JsonSerializer<ItemStack> {

    /**
     * Serializes an {@link ItemStack} object to JSON.
     * <p>
     * Uses Bukkit's built-in serialization to preserve ALL metadata.
     * First tries binary serialization (most complete), then falls back to Map serialization.
     * </p>
     *
     * @param itemStack          the {@code ItemStack} to serialize (may be {@code null})
     * @param jsonGenerator      the JSON generator to write to
     * @param serializerProvider the serializer provider
     * @throws IOException if an I/O error occurs during serialization
     */
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

        jsonGenerator.writeStartObject();
        
        // Try binary serialization first (preserves ALL metadata)
        try {
            String binaryData = serializeToBinary(itemStack);
            jsonGenerator.writeStringField("binaryData", binaryData);
            jsonGenerator.writeStringField("serializationType", "binary");
        } catch (Exception e) {
            // Fallback to Map serialization
            Map<String, Object> serialized = itemStack.serialize();
            jsonGenerator.writeObjectField("mapData", serialized);
            jsonGenerator.writeStringField("serializationType", "map");
        }
        
        jsonGenerator.writeEndObject();
    }

    /**
     * Serializes an ItemStack to a Base64-encoded binary string using Bukkit's serialization.
     * This preserves ALL metadata including complex data like persistent data containers.
     *
     * @param itemStack the ItemStack to serialize
     * @return Base64-encoded binary data
     * @throws IOException if serialization fails
     */
    private String serializeToBinary(@NotNull ItemStack itemStack) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeObject(itemStack);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }


}
