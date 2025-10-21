package com.raindropcentral.rplatform.database.converter;

import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxConverterTest {

    private final BoundingBoxConverter converter = new BoundingBoxConverter();

    @Test
    void convertToDatabaseColumnReturnsNullForNullBoundingBox() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnSerializesCoordinates() {
        BoundingBox box = new BoundingBox(-10.5, 2.25, 3.75, 15.0, 8.5, 12.125);
        assertEquals("-10.5,2.25,3.75,15.0,8.5,12.125", converter.convertToDatabaseColumn(box));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullOrBlankColumns() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttributeReconstructsBoundingBox() {
        BoundingBox box = converter.convertToEntityAttribute("-1.5,0.0,2.0,4.5,6.75,9.0");
        assertNotNull(box);
        assertBoundingBox(box, -1.5, 0.0, 2.0, 4.5, 6.75, 9.0);
    }

    @Test
    void roundTripMaintainsBoundingBoxCoordinates() {
        BoundingBox original = new BoundingBox(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        String columnValue = converter.convertToDatabaseColumn(original);
        BoundingBox reconstructed = converter.convertToEntityAttribute(columnValue);
        assertNotNull(reconstructed);
        assertBoundingBox(reconstructed, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
    }

    @Test
    void convertToEntityAttributeThrowsWhenTokenCountIsInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute("1.0,2.0,3.0"));
        assertTrue(exception.getMessage().contains("expected 6"));
    }

    @Test
    void convertToEntityAttributeThrowsWhenTokenIsNotNumeric() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute("1.0,2.0,three,4.0,5.0,6.0"));
        assertTrue(exception.getMessage().contains("Failed parsing"));
    }

    private static void assertBoundingBox(BoundingBox box,
                                          double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ) {
        assertEquals(minX, box.getMinX());
        assertEquals(minY, box.getMinY());
        assertEquals(minZ, box.getMinZ());
        assertEquals(maxX, box.getMaxX());
        assertEquals(maxY, box.getMaxY());
        assertEquals(maxZ, box.getMaxZ());
    }
}
