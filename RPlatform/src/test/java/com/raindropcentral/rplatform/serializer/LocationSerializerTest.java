package com.raindropcentral.rplatform.serializer;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationSerializerTest {

    private LocationSerializer serializer;
    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = server.addSimpleWorld("serializer-world");
        this.serializer = new LocationSerializer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void serializeRoundTripPreservesAllComponents() {
        Location original = new Location(world, 12.5, 64.0, -3.25, 90.0f, -30.5f);

        String serialized = serializer.serialize(original);
        Location result = serializer.deserialize(serialized);

        assertNotNull(result);
        assertSame(world, result.getWorld());
        assertEquals(original.getX(), result.getX(), 0.0001);
        assertEquals(original.getY(), result.getY(), 0.0001);
        assertEquals(original.getZ(), result.getZ(), 0.0001);
        assertEquals(original.getYaw(), result.getYaw(), 0.0001);
        assertEquals(original.getPitch(), result.getPitch(), 0.0001);
    }

    @Test
    void serializeThrowsWhenWorldNull() {
        Location location = new Location(null, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(location));
    }

    @Test
    void deserializeReturnsDefaultLocationWhenWorldUnknown() {
        World defaultWorld = server.addSimpleWorld("world");

        Location result = serializer.deserialize("missing-world,1,2,3,4,5");

        assertNotNull(result);
        assertSame(defaultWorld, result.getWorld());
        assertEquals(0.0, result.getX(), 0.0001);
        assertEquals(0.0, result.getY(), 0.0001);
        assertEquals(0.0, result.getZ(), 0.0001);
        assertEquals(0.0f, result.getYaw(), 0.0001);
        assertEquals(0.0f, result.getPitch(), 0.0001);
    }

    @Test
    void deserializeReturnsNullWhenDefaultWorldUnavailable() {
        Location result = serializer.deserialize("missing-world,1,2,3,4,5");

        assertNull(result);
    }

    @Test
    void deserializeFallsBackForMalformedCoordinates() {
        World defaultWorld = server.addSimpleWorld("world");

        Location result = serializer.deserialize(world.getName() + ",not-a-number,2,3,4,5");

        assertNotNull(result);
        assertSame(defaultWorld, result.getWorld());
        assertEquals(0.0, result.getX(), 0.0001);
        assertEquals(0.0, result.getY(), 0.0001);
        assertEquals(0.0, result.getZ(), 0.0001);
        assertEquals(0.0f, result.getYaw(), 0.0001);
        assertEquals(0.0f, result.getPitch(), 0.0001);
    }

    @Test
    void deserializeFallsBackWhenSegmentCountIncorrect() {
        World defaultWorld = server.addSimpleWorld("world");

        Location result = serializer.deserialize("serializer-world,1,2,3");

        assertNotNull(result);
        assertSame(defaultWorld, result.getWorld());
        assertEquals(0.0, result.getX(), 0.0001);
        assertEquals(0.0, result.getY(), 0.0001);
        assertEquals(0.0, result.getZ(), 0.0001);
        assertEquals(0.0f, result.getYaw(), 0.0001);
        assertEquals(0.0f, result.getPitch(), 0.0001);
    }
}
