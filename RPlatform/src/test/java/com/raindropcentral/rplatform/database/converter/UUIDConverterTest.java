package com.raindropcentral.rplatform.database.converter;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDConverterTest {

    private final UUIDConverter converter = new UUIDConverter();

    @Test
    void convertToDatabaseColumnReturnsNullForNullUuid() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnSerializesRandomUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), converter.convertToDatabaseColumn(uuid));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullOrBlank() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttributeParsesCanonicalUuidString() {
        UUID uuid = UUID.randomUUID();
        UUID reconstructed = converter.convertToEntityAttribute(uuid.toString());
        assertNotNull(reconstructed);
        assertEquals(uuid, reconstructed);
    }

    @Test
    void convertToEntityAttributeThrowsForInvalidString() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute("not-a-uuid"));
        assertTrue(exception.getMessage().contains("Invalid UUID string"));
    }
}
