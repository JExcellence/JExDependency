/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Map;

/**
 * Custom serializer for Bukkit {@link ItemStack} objects.
 *
 * <p>Converts {@code ItemStack} objects to their JSON representation for use with Jackson.
 * Uses Bukkit's built-in serialization to preserve ALL metadata including attributes, 
 * item flags, persistent data container, and other complex metadata.
 *
 *
 * <p>The serialization uses two approaches:
 * 1. Bukkit's native serialization (base64 encoded) for complete metadata preservation
 * 2. Fallback to Bukkit's Map serialization for compatibility
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class ItemStackJSONSerializer extends StdSerializer<ItemStack> {

    /**
     * Executes ItemStackJSONSerializer.
     */
    public ItemStackJSONSerializer() {
        super(ItemStack.class);
    }

    /**
     * Serializes an {@link ItemStack} object to JSON.
 *
 * <p>Uses Bukkit's built-in serialization to preserve ALL metadata.
     * First tries binary serialization (most complete), then falls back to Map serialization.
     *
     * @param itemStack            the {@code ItemStack} to serialize (may be {@code null})
     * @param jsonGenerator        the JSON generator to write to
     * @param serializerProvider the serialization context
     */
    @Override
    public void serialize(
            @Nullable final ItemStack itemStack,
            @NotNull final JsonGenerator jsonGenerator,
            @NotNull final SerializerProvider serializerProvider
    ) throws java.io.IOException {
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

    private @NotNull ItemStack normalizeForSerialization(@NotNull ItemStack itemStack) {
        final ItemStack normalized = itemStack.clone();
        normalized.setAmount(1);
        return normalized;
    }
}
