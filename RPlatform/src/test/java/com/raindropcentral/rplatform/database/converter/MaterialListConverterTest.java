package com.raindropcentral.rplatform.database.converter;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaterialListConverterTest {

    private final MaterialListConverter converter = new MaterialListConverter();

    @Test
    void convertToDatabaseColumnReturnsNullForNullList() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnReturnsEmptyStringForEmptyList() {
        assertEquals("", converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void convertToDatabaseColumnMaintainsOrderAndDuplicates() {
        List<Material> materials = Arrays.asList(
                Material.STONE,
                null,
                Material.DIRT,
                Material.STONE,
                Material.ACACIA_LOG
        );

        String column = converter.convertToDatabaseColumn(materials);
        assertEquals("STONE;DIRT;STONE;ACACIA_LOG", column);
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullColumn() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttributeReturnsEmptyListForBlankColumn() {
        List<Material> result = converter.convertToEntityAttribute("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttributePreservesOrderAndDuplicates() {
        String column = "stone;dirt;STONE;diamond";
        List<Material> result = converter.convertToEntityAttribute(column);

        assertNotNull(result);
        assertEquals(List.of(
                Material.STONE,
                Material.DIRT,
                Material.STONE,
                Material.DIAMOND
        ), result);
    }

    @Test
    void convertToEntityAttributeThrowsForInvalidToken() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("STONE;INVALID;DIAMOND")
        );

        assertTrue(exception.getMessage().contains("Invalid Material in list"));
    }
}
