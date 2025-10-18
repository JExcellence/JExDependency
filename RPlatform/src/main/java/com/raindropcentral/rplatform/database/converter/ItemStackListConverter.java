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
 * JPA attribute converter for mapping List&lt;ItemStack&gt; to semicolon-delimited Base64 strings and back.
 *
 * Behavior:
 * - null list -> null column; empty list -> empty string
 * - null column -> null list; empty/blank column -> empty list
 */
@Converter(autoApply = true)
public class ItemStackListConverter implements AttributeConverter<List<ItemStack>, String> {

    private static final String DELIM = ";";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

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