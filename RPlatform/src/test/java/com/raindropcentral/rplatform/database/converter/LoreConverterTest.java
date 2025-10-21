package com.raindropcentral.rplatform.database.converter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoreConverterTest {

    private final LoreConverter converter = new LoreConverter();

    @Test
    void convertToDatabaseColumnReturnsNullWhenLoreNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnReturnsEmptyStringForEmptyLore() {
        assertEquals("", converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void convertToEntityAttributeReturnsNullWhenColumnValueNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttributeReturnsEmptyListForBlankColumnValue() {
        List<String> result = converter.convertToEntityAttribute("   ");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void roundTripPreservesOrderingAndSpecialCharacters() {
        List<String> original = List.of(
                "First line",
                "",
                "Symbols: ☂️✨",
                "Line with\nnewline",
                "Semicolon ; inside",
                "汉字 and emoji 🌧️"
        );

        String column = converter.convertToDatabaseColumn(original);
        assertNotNull(column);
        assertFalse(column.isBlank());

        List<String> reconstructed = converter.convertToEntityAttribute(column);
        assertNotNull(reconstructed);
        assertEquals(original, reconstructed);
    }

    @Test
    void roundTripConvertsNullEntriesToEmptyStrings() {
        List<String> original = Arrays.asList("Line one", null, "Line three");

        String column = converter.convertToDatabaseColumn(original);
        assertNotNull(column);
        assertTrue(column.contains(";;"));

        List<String> reconstructed = converter.convertToEntityAttribute(column);
        assertNotNull(reconstructed);
        assertEquals(3, reconstructed.size());
        assertEquals("Line one", reconstructed.get(0));
        assertEquals("", reconstructed.get(1));
        assertEquals("Line three", reconstructed.get(2));
    }
}
