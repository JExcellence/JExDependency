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
 * Converts {@link List lists} of {@link Material} entries to a semicolon-separated column string and rebuilds.
 * them for entity hydration.
 *
 * <p>The converter stores each {@link Material#name()} token in upper-case form, skipping {@code null}
 * entries. {@code null} lists correspond to {@code null} columns, while blank column values become empty
 * lists. Invalid tokens trigger an {@link IllegalArgumentException} during entity conversion.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class MaterialListConverter implements AttributeConverter<List<Material>, String> {

    /** Delimiter separating material tokens within the stored column string. */
    private static final String DELIM = ";";

    /**
     * Serialises the provided materials to the joined string representation.
     *
     * @param materials the list being persisted; may be {@code null}
     * @return {@code null} when the list is {@code null}, an empty string for empty lists, or the joined payload otherwise
     */
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

    /**
     * Rehydrates a list of {@link Material} values from the stored column payload.
     *
     * @param columnValue the raw database value; {@code null} yields {@code null} and blank values produce an empty list
     * @return the reconstructed list, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when a token cannot be resolved to a {@link Material}
     */
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
