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
 * JPA attribute converter for mapping Map&lt;String, ItemStack&gt; to a delimiter-safe String and back.
 *
 * Encoding:
 * - Each entry is Base64(key UTF-8) + ":" + Base64(ItemStack bytes), entries joined with ";"
 * - Null/empty ItemStack is encoded as empty value token to represent AIR
 *
 * Behavior:
 * - null map -> null column; empty map -> empty string
 * - null column -> null map; blank column -> empty map
 */
@Converter(autoApply = false)
public class ItemStackMapConverter implements AttributeConverter<Map<String, ItemStack>, String> {

    private static final String ENTRY_DELIM = ";";
    private static final String KV_DELIM = ":";

    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

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