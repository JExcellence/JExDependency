package com.raindropcentral.rplatform.serializer;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundingBoxSerializerTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void serializeAndDeserializePreservesCoordinates() {
        final BoundingBoxSerializer serializer = new BoundingBoxSerializer();
        final List<BoundingBox> boundingBoxes = List.of(
                BoundingBox.of(new Vector(0.0, 0.0, 0.0), new Vector(5.0, 5.0, 5.0)),
                BoundingBox.of(new Vector(-10.5, -20.25, -30.75), new Vector(-1.5, -2.25, -3.75)),
                BoundingBox.of(new Vector(1.5, 2.5, 3.5), new Vector(100.25, 200.5, 300.75))
        );

        for (final BoundingBox original : boundingBoxes) {
            final String serialized = serializer.serialize(original);
            final BoundingBox deserialized = serializer.deserialize(serialized);

            assertEquals(original.getMinX(), deserialized.getMinX(), DELTA);
            assertEquals(original.getMinY(), deserialized.getMinY(), DELTA);
            assertEquals(original.getMinZ(), deserialized.getMinZ(), DELTA);
            assertEquals(original.getMaxX(), deserialized.getMaxX(), DELTA);
            assertEquals(original.getMaxY(), deserialized.getMaxY(), DELTA);
            assertEquals(original.getMaxZ(), deserialized.getMaxZ(), DELTA);
        }
    }

    @Test
    void deserializeTrimsWhitespaceBetweenCoordinates() {
        final BoundingBoxSerializer serializer = new BoundingBoxSerializer();

        final BoundingBox boundingBox = serializer.deserialize(" 1 , 2 , 3 , 4 , 5 , 6 ");

        assertEquals(1.0, boundingBox.getMinX(), DELTA);
        assertEquals(2.0, boundingBox.getMinY(), DELTA);
        assertEquals(3.0, boundingBox.getMinZ(), DELTA);
        assertEquals(4.0, boundingBox.getMaxX(), DELTA);
        assertEquals(5.0, boundingBox.getMaxY(), DELTA);
        assertEquals(6.0, boundingBox.getMaxZ(), DELTA);
    }

    @Test
    void deserializeRejectsInputsWithIncorrectPartCount() {
        final BoundingBoxSerializer serializer = new BoundingBoxSerializer();

        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("1,2,3,4"));
        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("1,2,3,4,5,6,7"));
    }

    @Test
    void deserializeRejectsNonNumericValues() {
        final BoundingBoxSerializer serializer = new BoundingBoxSerializer();

        assertThrows(IllegalArgumentException.class, () -> serializer.deserialize("1,2,three,4,5,6"));
    }
}
