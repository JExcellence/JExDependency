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
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Persists {@link BoundingBox} instances as comma-separated coordinate rows and rebuilds them for entities.
 *
 * <p>The converter writes six comma-delimited numeric tokens representing the minimum and maximum x, y and
 * z coordinates. {@code null} attributes translate to {@code null} columns while blank or {@code null}
 * column values return {@code null} attributes. Invalid token counts or unparsable numbers raise an
 * {@link IllegalArgumentException} to protect database integrity.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class BoundingBoxConverter implements AttributeConverter<BoundingBox, String> {

    /** Delimiter used to separate individual coordinate tokens. */
    private static final String DELIM = ",";

    /**
     * Serialises a bounding box to the comma-delimited column representation.
     *
     * @param box the bounding box being persisted; may be {@code null}
     * @return the comma-separated coordinate string or {@code null} when {@code box} is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final BoundingBox box) {
        if (box == null) {
            return null;
        }

        return box.getMinX() + DELIM + box.getMinY() + DELIM + box.getMinZ() + DELIM + box.getMaxX() + DELIM + box.getMaxY() + DELIM + box.getMaxZ();
    }

    /**
     * Reconstructs a {@link BoundingBox} from a stored comma-separated coordinate set.
     *
     * @param columnValue the raw database value; blank and {@code null} values yield {@code null}
     * @return the reconstructed bounding box, or {@code null} when the column value is blank
     * @throws IllegalArgumentException if the token count is incorrect or any coordinate fails to parse
     */
    @Override
    public BoundingBox convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }

        final String[] tokens = columnValue.split(DELIM, -1);
        if (tokens.length != 6) {
            throw new IllegalArgumentException("Invalid BoundingBox token count: expected 6 but got " + tokens.length);
        }

        try {
            final double minX = Double.parseDouble(tokens[0].trim());
            final double minY = Double.parseDouble(tokens[1].trim());
            final double minZ = Double.parseDouble(tokens[2].trim());
            final double maxX = Double.parseDouble(tokens[3].trim());
            final double maxY = Double.parseDouble(tokens[4].trim());
            final double maxZ = Double.parseDouble(tokens[5].trim());
            return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Failed parsing BoundingBox numbers from: '" + columnValue + "'", ex);
        }
    }
}