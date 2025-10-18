package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * JPA attribute converter for mapping {@link Material} to its enum name (String) for storage and back.
 *
 * Behavior:
 * - null Material -> null column
 * - null/blank column -> null Material
 */
@Converter(autoApply = true)
public class BasicMaterialConverter implements AttributeConverter<Material, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable final Material material) {
        return material == null ? null : material.name();
    }

    @Override
    public Material convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        final String normalized = columnValue.trim().toUpperCase(Locale.ROOT);
        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Material value in column: '" + columnValue + "'", ex);
        }
    }
}