package com.raindropcentral.rplatform.database.converter;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicMaterialConverterTest {

    private final BasicMaterialConverter converter = new BasicMaterialConverter();

    @Test
    void convertToDatabaseColumnSerializesEnumName() {
        assertEquals("STONE", converter.convertToDatabaseColumn(Material.STONE));
        assertEquals("DIAMOND_SWORD", converter.convertToDatabaseColumn(Material.DIAMOND_SWORD));
    }

    @Test
    void convertToDatabaseColumnReturnsNullForNullAttribute() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeResolvesNormalizedValues() {
        assertEquals(Material.STONE, converter.convertToEntityAttribute("stone"));
        assertEquals(Material.GOLDEN_APPLE, converter.convertToEntityAttribute("  Golden_Apple  "));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullOrBlankColumns() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttributeThrowsForInvalidMaterial() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute("not-a-material"));
        assertTrue(exception.getMessage().contains("not-a-material"));
    }

    @Test
    void convertRoundTripPreservesMaterialIdentity() {
        Material material = Material.NETHERITE_PICKAXE;
        String columnValue = converter.convertToDatabaseColumn(material);
        assertEquals(material, converter.convertToEntityAttribute(columnValue));
    }
}
