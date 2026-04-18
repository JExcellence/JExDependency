package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * JPA converter that persists {@link BoundingBox} instances as
 * comma-separated coordinate strings.
 *
 * <p>Format: {@code minX,minY,minZ,maxX,maxY,maxZ}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class BoundingBoxConverter implements AttributeConverter<BoundingBox, String> {

    private static final String DELIM = ",";

    @Override
    public String convertToDatabaseColumn(@Nullable BoundingBox box) {
        if (box == null) {
            return null;
        }
        return box.getMinX() + DELIM + box.getMinY() + DELIM + box.getMinZ()
                + DELIM + box.getMaxX() + DELIM + box.getMaxY() + DELIM + box.getMaxZ();
    }

    @Override
    public BoundingBox convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }

        var tokens = columnValue.split(DELIM, -1);
        if (tokens.length != 6) {
            throw new IllegalArgumentException(
                    "Invalid BoundingBox token count: expected 6 but got " + tokens.length);
        }

        try {
            var minX = Double.parseDouble(tokens[0].trim());
            var minY = Double.parseDouble(tokens[1].trim());
            var minZ = Double.parseDouble(tokens[2].trim());
            var maxX = Double.parseDouble(tokens[3].trim());
            var maxY = Double.parseDouble(tokens[4].trim());
            var maxZ = Double.parseDouble(tokens[5].trim());
            return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Failed parsing BoundingBox numbers from: '" + columnValue + "'", ex);
        }
    }
}
