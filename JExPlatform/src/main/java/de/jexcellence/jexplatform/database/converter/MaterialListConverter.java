package de.jexcellence.jexplatform.database.converter;

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
 * JPA converter for {@link List lists} of {@link Material} entries stored
 * as semicolon-separated enum names.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class MaterialListConverter implements AttributeConverter<List<Material>, String> {

    private static final String DELIM = ";";

    @Override
    public String convertToDatabaseColumn(@Nullable List<Material> materials) {
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
    public List<Material> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        var parts = columnValue.split(DELIM, -1);
        var result = new ArrayList<Material>(parts.length);

        for (var raw : parts) {
            var token = raw == null ? "" : raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                result.add(Material.valueOf(token.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid Material in list: '" + raw + "'", ex);
            }
        }

        return result;
    }
}
