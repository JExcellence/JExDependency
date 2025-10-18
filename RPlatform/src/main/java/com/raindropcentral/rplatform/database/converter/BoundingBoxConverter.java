package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * JPA attribute converter for mapping {@link BoundingBox} to a comma-delimited string and back.
 *
 * Format: "minX,minY,minZ,maxX,maxY,maxZ"
 *
 * Behavior:
 * - null bbox -> null column
 * - null/blank column -> null bbox
 */
@Converter(autoApply = true)
public class BoundingBoxConverter implements AttributeConverter<BoundingBox, String> {

    private static final String DELIM = ",";

    @Override
    public String convertToDatabaseColumn(@Nullable final BoundingBox box) {
        if (box == null) {
            return null;
        }

        return box.getMinX() + DELIM + box.getMinY() + DELIM + box.getMinZ() + DELIM + box.getMaxX() + DELIM + box.getMaxY() + DELIM + box.getMaxZ();
    }

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