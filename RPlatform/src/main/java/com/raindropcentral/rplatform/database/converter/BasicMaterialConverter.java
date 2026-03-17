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
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Converts between Bukkit {@link Material} instances and the enum name stored in the database column.
 *
 * <p>The converter writes the {@link Material#name()} value in upper-case form and attempts to resolve
 * any retrieved column value using {@link Material#valueOf(String)} after trimming and normalising the
 * case. {@code null} attributes map to {@code null} columns and blank column values are treated as
 * {@code null} attributes.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class BasicMaterialConverter implements AttributeConverter<Material, String> {

    /**
     * Serialises a {@link Material} attribute to the enum-name column representation.
     *
     * @param material the material being persisted; may be {@code null}
     * @return the enum name for the supplied material, or {@code null} when the attribute is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final Material material) {
        return material == null ? null : material.name();
    }

    /**
     * Resolves a stored enum-name back into a Bukkit {@link Material} instance.
     *
     * @param columnValue the raw database value; blank and {@code null} values yield {@code null}
     * @return the resolved material, or {@code null} when the column value is blank
     * @throws IllegalArgumentException when the stored value does not correspond to a valid material
     */
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