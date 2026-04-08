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

/*
 * ItemStackSlotMapConverter.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persists slot-indexed {@link ItemStack} inventories as a delimiter-safe string payload.
 *
 * <p>This converter is intended for Bukkit inventories where keys represent slot indices rather than
 * arbitrary strings. Empty or {@code null} stacks are omitted so persisted payloads remain sparse,
 * preserving only meaningful occupied slots.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
@Converter(autoApply = false)
public class ItemStackSlotMapConverter implements AttributeConverter<Map<Integer, ItemStack>, String> {

    private static final String ENTRY_DELIMITER = ";";
    private static final String VALUE_DELIMITER = ":";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    /**
     * Serializes the provided sparse slot map for database storage.
     *
     * @param inventory slot-indexed inventory contents; may be {@code null}
     * @return {@code null} when the map is {@code null}, an empty string for empty maps, or the encoded payload
     * @throws IllegalArgumentException when a slot key is negative
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Map<Integer, ItemStack> inventory) {
        if (inventory == null) {
            return null;
        }
        if (inventory.isEmpty()) {
            return "";
        }

        final StringBuilder payload = new StringBuilder(inventory.size() * 64);
        boolean firstEntry = true;

        for (final Map.Entry<Integer, ItemStack> entry : new TreeMap<>(inventory).entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null) {
                continue;
            }
            if (slot < 0) {
                throw new IllegalArgumentException("Inventory slot cannot be negative: " + slot);
            }
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (!firstEntry) {
                payload.append(ENTRY_DELIMITER);
            } else {
                firstEntry = false;
            }

            payload.append(slot)
                .append(VALUE_DELIMITER)
                .append(BASE64_ENCODER.encodeToString(itemStack.serializeAsBytes()));
        }

        return payload.toString();
    }

    /**
     * Reconstructs a sparse slot map from the stored payload.
     *
     * @param columnValue raw database payload; may be {@code null}
     * @return a decoded slot map, or {@code null} when the column value is {@code null}
     * @throws IllegalArgumentException when a slot token is invalid or the payload cannot be decoded
     */
    @Override
    public Map<Integer, ItemStack> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }

        final Map<Integer, ItemStack> inventory = new HashMap<>();
        if (columnValue.isBlank()) {
            return inventory;
        }

        final String[] entries = columnValue.split(ENTRY_DELIMITER, -1);
        for (final String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            final int separatorIndex = entry.indexOf(VALUE_DELIMITER);
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Invalid inventory entry without slot delimiter: '" + entry + "'");
            }

            final String slotToken = entry.substring(0, separatorIndex).trim();
            final String valueToken = entry.substring(separatorIndex + 1).trim();
            if (valueToken.isEmpty()) {
                continue;
            }

            try {
                final int slot = Integer.parseInt(slotToken);
                if (slot < 0) {
                    throw new IllegalArgumentException("Inventory slot cannot be negative: " + slot);
                }
                inventory.put(slot, ItemStack.deserializeBytes(BASE64_DECODER.decode(valueToken)));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Invalid slot inventory entry: '" + entry + "'", ex);
            }
        }

        return inventory;
    }
}
