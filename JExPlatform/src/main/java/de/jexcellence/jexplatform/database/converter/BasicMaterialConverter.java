package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * JPA converter between Bukkit {@link Material} instances and their
 * uppercase enum-name column representation.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class BasicMaterialConverter implements AttributeConverter<Material, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable Material material) {
        return material == null ? null : material.name();
    }

    @Override
    public Material convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        var normalized = columnValue.trim().toUpperCase(Locale.ROOT);
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid Material value in column: '" + columnValue + "'", ex);
        }
    }
}
