package com.raindropcentral.rplatform.database.converter;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldConverterTest {

    private WorldConverter converter;
    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = server.addSimpleWorld("world-converter");
        this.converter = new WorldConverter();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void convertToDatabaseColumnReturnsNullWhenWorldNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnReturnsWorldName() {
        assertEquals(world.getName(), converter.convertToDatabaseColumn(world));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullOrBlankValues() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToEntityAttributeResolvesWorldByName() {
        World resolved = converter.convertToEntityAttribute(world.getName());
        assertNotNull(resolved);
        assertSame(world, resolved);
    }

    @Test
    void convertToEntityAttributeReturnsNullForUnknownWorld() {
        assertNull(converter.convertToEntityAttribute("missing-world"));
    }
}
