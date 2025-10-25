package com.raindropcentral.rplatform.database.converter;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocationConverterTest {

    private LocationConverter converter;
    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = server.addSimpleWorld("converter-world");
        this.converter = new LocationConverter();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void convertToDatabaseColumnReturnsNullForNullLocation() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnThrowsWhenWorldMissing() {
        Location location = new Location(null, 1.0, 2.0, 3.0, 90.0f, 30.0f);
        assertThrows(IllegalArgumentException.class, () -> converter.convertToDatabaseColumn(location));
    }

    @Test
    void convertToDatabaseColumnSerializesWorldCoordinatesAndOrientation() {
        Location location = new Location(world, 12.5, -4.75, 9.0, 180.0f, -45.0f);
        String expected = world.getUID() + ";" + 12.5 + ";" + -4.75 + ";" + 9.0 + ";" + 180.0f + ";" + -45.0f;
        assertEquals(expected, converter.convertToDatabaseColumn(location));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullOrBlankColumns() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttributeRebuildsLocationUsingWorldLookup() {
        String payload = world.getUID() + ";" + 4.0 + ";" + 8.0 + ";" + -12.5 + ";" + 45.0f + ";" + 10.0f;
        Location location = converter.convertToEntityAttribute(payload);
        assertNotNull(location);
        assertSame(world, location.getWorld());
        assertEquals(4.0, location.getX());
        assertEquals(8.0, location.getY());
        assertEquals(-12.5, location.getZ());
        assertEquals(45.0f, location.getYaw());
        assertEquals(10.0f, location.getPitch());
    }

    @Test
    void roundTripMaintainsWorldAndCoordinates() {
        Location original = new Location(world, 3.25, -6.5, 7.75, -30.0f, 22.5f);
        String columnValue = converter.convertToDatabaseColumn(original);
        Location reconstructed = converter.convertToEntityAttribute(columnValue);
        assertNotNull(reconstructed);
        assertSame(world, reconstructed.getWorld());
        assertEquals(original.getX(), reconstructed.getX());
        assertEquals(original.getY(), reconstructed.getY());
        assertEquals(original.getZ(), reconstructed.getZ());
        assertEquals(original.getYaw(), reconstructed.getYaw());
        assertEquals(original.getPitch(), reconstructed.getPitch());
    }

    @Test
    void convertToEntityAttributeThrowsWhenTokenCountInvalid() {
        String payload = world.getUID() + ";1.0;2.0";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute(payload));
        assertTrue(exception.getMessage().contains("expected 6"));
    }

    @Test
    void convertToEntityAttributeThrowsWhenWorldMissing() {
        UUID missingWorldId = UUID.randomUUID();
        String payload = missingWorldId + ";1.0;2.0;3.0;4.0;5.0";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute(payload));
        assertTrue(exception.getMessage().contains("No world"));
    }

    @Test
    void convertToEntityAttributeThrowsWhenParsingFails() {
        String payload = world.getUID() + ";NaN;2.0;3.0;4.0;5.0";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                converter.convertToEntityAttribute(payload));
        assertTrue(exception.getMessage().contains("Failed parsing"));
    }
}
