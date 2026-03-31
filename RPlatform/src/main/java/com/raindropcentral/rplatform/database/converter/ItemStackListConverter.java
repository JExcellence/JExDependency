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

package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists collections of {@link ItemStack} values as a semicolon-delimited Base64 payload and restores them.
 * for entity hydration.
 *
 * <p>Each non-empty stack is encoded via {@link ItemStack#serializeAsBytes()} and Base64 while empty or
 * {@code null} entries are skipped to keep the column compact. {@code null} collections yield
 * {@code null} columns and blank column values map back to empty collections. Any token that fails to
 * decode raises an {@link IllegalArgumentException} so callers can remediate corrupted data.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class ItemStackListConverter implements AttributeConverter<List<ItemStack>, String> {

    /** Delimiter separating encoded stack payloads in the column string. */
    private static final String DELIM = ";";
    /** Encoder for translating stack bytes into a storage-friendly Base64 value. */
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    /** Decoder for rebuilding stacks from the persisted payload. */
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    /**
     * Serialises the provided list into the joined Base64 representation.
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
        return items.stream()
                .filter(item -> item != null && !item.isEmpty())
                .map(item -> B64_ENCODER.encodeToString(item.serializeAsBytes()))
                .collect(Collectors.joining(DELIM));
    }

    /**
     * Rebuilds a list of {@link ItemStack} instances from the persisted Base64 payload.
     *
     * @param columnValue the column data; {@code null} yields {@code null} and blank values produce an empty list
     * @return the reconstructed list of stacks, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when a token cannot be decoded or deserialised
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
        final List<ItemStack> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            final String token = part == null ? "" : part.trim();
            if (token.isEmpty()) {
                // treat empty token as AIR for symmetry with single ItemStack converter
                result.add(new ItemStack(Material.AIR));
                continue;
            }
            try {
                result.add(ItemStack.deserializeBytes(B64_DECODER.decode(token)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid Base64 ItemStack in list.", ex);
            }
        }

        return result;
    }
}
