package de.jexcellence.jexplatform.serializer;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Converts {@link BoundingBox} instances to and from a comma-separated
 * textual representation for configuration storage.
 *
 * <p>Format: {@code minX,minY,minZ,maxX,maxY,maxZ}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class BoundingBoxSerializer {

    private static final String DELIMITER = ",";
    private static final int EXPECTED_PARTS = 6;

    private BoundingBoxSerializer() {
    }

    /**
     * Serializes a {@link BoundingBox} to a comma-separated string.
     *
     * @param box bounding box to convert
     * @return comma-separated representation
     */
    public static @NotNull String serialize(@NotNull BoundingBox box) {
        return String.format("%s,%s,%s,%s,%s,%s",
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    /**
     * Parses a comma-separated coordinate string into a {@link BoundingBox}.
     *
     * @param serialized comma-separated bounding box string
     * @return bounding box created from the coordinates
     * @throws IllegalArgumentException if the input cannot be parsed
     */
    public static @NotNull BoundingBox deserialize(@NotNull String serialized) {
        var parts = serialized.split(DELIMITER);
        if (parts.length != EXPECTED_PARTS) {
            throw new IllegalArgumentException("Invalid bounding box format: " + serialized);
        }

        try {
            var minX = Double.parseDouble(parts[0].trim());
            var minY = Double.parseDouble(parts[1].trim());
            var minZ = Double.parseDouble(parts[2].trim());
            var maxX = Double.parseDouble(parts[3].trim());
            var maxY = Double.parseDouble(parts[4].trim());
            var maxZ = Double.parseDouble(parts[5].trim());

            return BoundingBox.of(
                    new Vector(minX, minY, minZ),
                    new Vector(maxX, maxY, maxZ));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid number format in bounding box: " + serialized, e);
        }
    }
}
