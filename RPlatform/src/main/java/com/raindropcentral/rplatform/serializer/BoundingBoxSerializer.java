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

package com.raindropcentral.rplatform.serializer;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Serializes and deserializes {@link BoundingBox} instances to a simple comma separated format.
 * understood by the RDC persistence layer.
 *
 * <p>The serializer expects six numeric components describing the minimum and maximum corners of
 * the box and converts them to and from {@link BoundingBox} instances.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class BoundingBoxSerializer {

    /**
     * Comma delimiter separating the min and max components in the serialized bounding box.
     * representation.
     */
    private static final String DELIMITER = ",";

    /**
     * Number of delimited parts expected when parsing a serialized bounding box string.
     */
    private static final int EXPECTED_PARTS = 6;

    /**
     * Converts a comma separated sequence of min/max coordinates into a {@link BoundingBox}.
     *
     * <p>The input must contain exactly six values in the order minX, minY, minZ, maxX, maxY and
     * maxZ. Each segment is trimmed prior to parsing. An {@link IllegalArgumentException} is thrown
     * when the part count is incorrect or when any value cannot be parsed into a double.</p>
     *
     * @param serialized comma separated bounding box string
     * @return bounding box created from the provided coordinates
     * @throws IllegalArgumentException if the input cannot be parsed into six doubles
     */
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

    /**
     * Serializes the provided {@link BoundingBox} into a comma separated string.
     *
     * <p>The values are written in the order minX, minY, minZ, maxX, maxY and maxZ to ensure the
     * deserializer can reconstruct the box symmetrically. The method never returns {@code null}.</p>
     *
     * @param boundingBox bounding box to convert
     * @return comma separated representation of the bounding box
     */
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
