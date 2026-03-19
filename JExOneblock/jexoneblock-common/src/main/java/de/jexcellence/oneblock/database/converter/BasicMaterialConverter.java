package de.jexcellence.oneblock.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Converts single {@link Material} values to their string representation for persistence and restores them
 * during entity hydration.
 *
 * <p>The converter stores the {@link Material#name()} in upper-case form. {@code null} materials
 * correspond to {@code null} columns, while blank column values become {@code null} materials.
 * Invalid material names trigger an {@link IllegalArgumentException} during entity conversion.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
@Converter(autoApply = true)
public class BasicMaterialConverter implements AttributeConverter<Material, String> {

    /**
     * Serialises the provided material to its string representation.
     *
     * @param material the material being persisted; may be {@code null}
     * @return the material name or {@code null} when {@code material} is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Material material) {
        return material == null ? null : material.name();
    }

    /**
     * Rehydrates a {@link Material} value from the stored column payload.
     *
     * @param columnValue the raw database value; {@code null} yields {@code null} and blank values produce {@code null}
     * @return the reconstructed material, or {@code null} if the column value is blank or null
     * @throws IllegalArgumentException when the token cannot be resolved to a {@link Material}
     */
    @Override
    public Material convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(columnValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Material: '" + columnValue + "'", ex);
        }
    }
}