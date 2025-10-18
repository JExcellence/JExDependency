package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JPA attribute converter for mapping List&lt;Material&gt; to semicolon-separated strings and back.
 *
 * Behavior:
 * - null list -> null column; empty list -> empty string
 * - null column -> null list; empty/blank column -> empty list
 */
@Converter(autoApply = true)
public class MaterialListConverter implements AttributeConverter<List<Material>, String> {

    private static final String DELIM = ";";

    @Override
    public String convertToDatabaseColumn(@Nullable final List<Material> materials) {
        if (materials == null) {
            return null;
        }
        if (materials.isEmpty()) {
            return "";
        }
        return materials.stream()
                .filter(Objects::nonNull)
                .map(Material::name)
                .collect(Collectors.joining(DELIM));
    }

    @Override
    public List<Material> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        final String[] parts = columnValue.split(DELIM, -1);
        final List<Material> result = new ArrayList<>(parts.length);

        for (String raw : parts) {
            final String token = raw == null ? "" : raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                result.add(Material.valueOf(token.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid Material in list: '" + raw + "'", ex);
            }
        }

        return result;
    }
}