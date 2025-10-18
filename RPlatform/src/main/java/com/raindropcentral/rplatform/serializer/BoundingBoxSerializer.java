package com.raindropcentral.rplatform.serializer;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BoundingBoxSerializer {

    private static final String DELIMITER = ",";
    private static final int EXPECTED_PARTS = 6;

    public @NotNull BoundingBox deserialize(final @NotNull String serialized) {
        final String[] parts = serialized.split(DELIMITER);
        
        if (parts.length != EXPECTED_PARTS) {
            throw new IllegalArgumentException("Invalid bounding box format: " + serialized);
        }

        try {
            final double minX = Double.parseDouble(parts[0].trim());
            final double minY = Double.parseDouble(parts[1].trim());
            final double minZ = Double.parseDouble(parts[2].trim());
            final double maxX = Double.parseDouble(parts[3].trim());
            final double maxY = Double.parseDouble(parts[4].trim());
            final double maxZ = Double.parseDouble(parts[5].trim());

            return BoundingBox.of(
                    new Vector(minX, minY, minZ),
                    new Vector(maxX, maxY, maxZ)
            );
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in bounding box: " + serialized, e);
        }
    }

    public @NotNull String serialize(final @NotNull BoundingBox boundingBox) {
        return String.format("%s,%s,%s,%s,%s,%s",
                boundingBox.getMinX(),
                boundingBox.getMinY(),
                boundingBox.getMinZ(),
                boundingBox.getMaxX(),
                boundingBox.getMaxY(),
                boundingBox.getMaxZ()
        );
    }
}
