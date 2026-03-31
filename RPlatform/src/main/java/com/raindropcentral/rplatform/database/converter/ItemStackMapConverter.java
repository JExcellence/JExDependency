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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores {@link Map} entries of string keys to {@link ItemStack} values using a delimiter-safe Base64 format.
 * and restores them for entity hydration.
 *
 * <p>Each key is encoded as UTF-8 before applying Base64 and combined with the value payload using
 * {@link #KV_DELIM}. Empty or {@code null} stacks are represented by an empty value token that rebuilds to
 * {@link Material#AIR}. {@code null} maps yield {@code null} columns while blank column values produce empty
 * maps. Any malformed entry or Base64 payload results in an {@link IllegalArgumentException}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = false)
public class ItemStackMapConverter implements AttributeConverter<Map<String, ItemStack>, String> {

    /** Delimiter separating individual key/value pairs within the column payload. */
    private static final String ENTRY_DELIM = ";";
    /** Delimiter separating encoded keys from encoded values within a pair. */
    private static final String KV_DELIM = ":";

    /** Encoder for transforming keys and stack payloads into Base64 text. */
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    /** Decoder for reconstructing keys and stack payloads from Base64 text. */
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    /**
     * Serialises the supplied map into the delimiter-safe Base64 column representation.
     *
     * @param map the map being persisted; may be {@code null}
     * @return {@code null} when the map is {@code null}, an empty string when the map is empty, or the encoded payload otherwise
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Map<String, ItemStack> map) {
        if (map == null) {
            return null;
        }
        if (map.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder(map.size() * 64);
        boolean first = true;
        for (Map.Entry<String, ItemStack> e : map.entrySet()) {
            final String key = e.getKey();
            if (key == null) {
                continue; // skip null keys
            }
            final String keyToken = B64_ENCODER.encodeToString(key.getBytes(StandardCharsets.UTF_8));
            final ItemStack value = e.getValue();
            final String valueToken;
            if (value == null || value.isEmpty()) {
                valueToken = ""; // represent AIR / empty
            } else {
                valueToken = B64_ENCODER.encodeToString(value.serializeAsBytes());
            }
            if (!first) {
                sb.append(ENTRY_DELIM);
            } else {
                first = false;
            }
            sb.append(keyToken).append(KV_DELIM).append(valueToken);
        }
        return sb.toString();
    }

    /**
     * Reconstructs a map of {@link ItemStack} entries from the delimiter-safe Base64 payload.
     *
     * @param columnValue the raw column value; {@code null} produces {@code null} and blank values yield an empty map
     * @return the decoded map, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when an entry lacks delimiters or contains invalid Base64 data
     */
    @Override
    public Map<String, ItemStack> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        final Map<String, ItemStack> result = new HashMap<>();
        if (columnValue.isBlank()) {
            return result;
        }

        final String[] entries = columnValue.split(ENTRY_DELIM, -1);
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            final int sep = entry.indexOf(KV_DELIM);
            if (sep < 0) {
                throw new IllegalArgumentException("Invalid map entry (missing key/value delimiter): '" + entry + "'");
            }
            final String keyToken = entry.substring(0, sep);
            final String valueToken = entry.substring(sep + 1);

            try {
                final String key = new String(B64_DECODER.decode(keyToken), StandardCharsets.UTF_8);
                final ItemStack item;
                if (valueToken.isEmpty()) {
                    item = new ItemStack(Material.AIR);
                } else {
                    item = ItemStack.deserializeBytes(B64_DECODER.decode(valueToken));
                }
                result.put(key, item);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid Base64 in map entry: '" + entry + "'", ex);
            }
        }

        return result;
    }
}
