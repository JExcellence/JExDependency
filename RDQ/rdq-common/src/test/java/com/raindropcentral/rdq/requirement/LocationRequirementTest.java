package com.raindropcentral.rdq.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationRequirementTest {

    private static final Unsafe UNSAFE = lookupUnsafe();

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.player = this.server.addPlayer("LocationRequirementTest");
        this.player.teleport(new Location(this.world, 0, 64, 0));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldEvaluateWorldOnlyRequirement() {
        final LocationRequirement requirement = new LocationRequirement("world", null, null, null, "Reach world");

        assertTrue(requirement.isMet(this.player), "Player in required world should satisfy requirement");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Progress should be complete when the player is in the required world");

        final World otherWorld = this.server.addSimpleWorld("other_world");
        this.player.teleport(new Location(otherWorld, 0, 64, 0));

        assertFalse(requirement.isMet(this.player), "Player in different world should fail requirement");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Progress should be zero when the player is in the wrong world");
    }

    @Test
    void shouldEvaluateCoordinateRequirement() {
        final LocationRequirement requirement = new LocationRequirement(null, null,
                new LocationRequirement.Coordinates(10.0, 64.0, -5.0), 5.0, null);

        this.player.teleport(new Location(this.world, 10.0, 64.0, -5.0));
        assertTrue(requirement.isMet(this.player), "Player exactly at coordinates should satisfy requirement");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Progress should be full when player is within the radius");

        this.player.teleport(new Location(this.world, 20.0, 64.0, -5.0));
        assertFalse(requirement.isMet(this.player), "Player outside radius should fail requirement");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Progress should clamp to zero when player is out of range");
    }

    @Test
    void shouldEvaluateCombinedConfiguration() {
        final LocationRequirement requirement = new LocationRequirement("world", null,
                new LocationRequirement.Coordinates(5.0, 70.0, 5.0), 3.0, "Spawn");

        this.player.teleport(new Location(this.world, 5.0, 70.0, 5.0));
        assertTrue(requirement.isMet(this.player), "Player matching world and coordinates should satisfy requirement");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Progress should be full when all constraints are satisfied");

        this.player.teleport(new Location(this.world, 10.0, 70.0, 5.0));
        assertFalse(requirement.isMet(this.player), "Player outside coordinate radius should fail requirement");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Progress should be zero when coordinate constraint fails");

        final World otherWorld = this.server.addSimpleWorld("combined_other");
        this.player.teleport(new Location(otherWorld, 5.0, 70.0, 5.0));
        assertFalse(requirement.isMet(this.player), "Player in wrong world should fail requirement even with matching coordinates");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Progress should be zero when world constraint fails");
    }

    @Test
    void shouldReportDistanceAndFormattedStatus() {
        final LocationRequirement requirement = new LocationRequirement("world", "spawn",
                new LocationRequirement.Coordinates(0.0, 64.0, 0.0), 5.0, null);

        assertEquals(-1.0, new LocationRequirement("world", null, null, null, null).getCurrentDistance(this.player),
                "Current distance should be -1 when coordinates are not configured");

        this.player.teleport(new Location(this.world, 2.0, 64.0, 2.0));
        final double expectedDistance = Math.sqrt(8.0);
        assertEquals(expectedDistance, requirement.getCurrentDistance(this.player), 1.0E-6,
                "Current distance should match Euclidean distance to required coordinates");
        assertFalse(requirement.isWithinDistance(this.player),
                "Player outside required distance should return false");

        this.player.teleport(new Location(this.world, 0.0, 64.0, 0.0));
        assertTrue(requirement.isWithinDistance(this.player),
                "Player at coordinates should be within distance");

        final String formatted = requirement.getFormattedStatus(this.player);
        assertEquals("World: world ✓ (Required: world), Distance: 0.0 ✓ (Max: 5.0), Region: spawn ✗", formatted,
                "Formatted status should include world, distance, and region summaries");
    }

    @Test
    void shouldRequireWorldGuardForRegionChecks() {
        final LocationRequirement noRegionRequirement = new LocationRequirement("world", null, null, null, null);
        assertTrue(noRegionRequirement.isInCorrectRegion(this.player),
                "When no region is configured, region checks should pass");

        final LocationRequirement regionRequirement = new LocationRequirement("world", "market");
        this.player.teleport(new Location(this.world, 0.0, 64.0, 0.0));
        assertFalse(regionRequirement.isMet(this.player),
                "Region requirement should fail without WorldGuard availability");
        assertFalse(regionRequirement.isInCorrectRegion(this.player),
                "Region check should be false when WorldGuard is unavailable");
    }

    @Test
    void validateShouldDetectMissingCriteria() {
        final LocationRequirement requirement = new LocationRequirement(null, null,
                new LocationRequirement.Coordinates(1.0, 2.0, 3.0), 1.0, null);

        setField(requirement, "requiredCoordinates", null);
        setField(requirement, "requiredWorld", null);
        setField(requirement, "requiredRegion", null);

        assertThrows(IllegalStateException.class, requirement::validate,
                "validate should reject configurations without any criteria");
    }

    @Test
    void validateShouldDetectNegativeDistances() {
        final LocationRequirement requirement = new LocationRequirement(null, null,
                new LocationRequirement.Coordinates(4.0, 5.0, 6.0), 1.0, null);

        setField(requirement, "requiredDistance", -5.0);
        assertThrows(IllegalStateException.class, requirement::validate,
                "validate should reject negative distance values");
    }

    @Test
    void validateShouldDetectBlankNames() {
        final LocationRequirement worldRequirement = new LocationRequirement("world", null, null, null, null);
        setField(worldRequirement, "requiredWorld", "   ");
        assertThrows(IllegalStateException.class, worldRequirement::validate,
                "validate should reject blank world names");

        final LocationRequirement regionRequirement = new LocationRequirement("world", "region");
        setField(regionRequirement, "requiredRegion", "");
        assertThrows(IllegalStateException.class, regionRequirement::validate,
                "validate should reject blank region names");
    }

    @Test
    void validateShouldDetectMissingWorlds() {
        final WorldMock temporaryWorld = this.server.addSimpleWorld("temporary");
        final LocationRequirement requirement = new LocationRequirement("temporary", null, null, null, null);

        this.server.getWorlds().remove(temporaryWorld);
        assertThrows(IllegalStateException.class, requirement::validate,
                "validate should reject references to worlds that no longer exist");
    }

    @Test
    void shouldExposeCoordinateAccessors() {
        final LocationRequirement.Coordinates coordinates = new LocationRequirement.Coordinates(1.25, 64.0, -3.5);

        assertEquals(1.25, coordinates.getX(), 1.0E-9);
        assertEquals(64.0, coordinates.getY(), 1.0E-9);
        assertEquals(-3.5, coordinates.getZ(), 1.0E-9);
        assertEquals("(1.3, 64.0, -3.5)", coordinates.toString(),
                "Coordinate toString should format values to one decimal place");
    }

    private static Unsafe lookupUnsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Unsafe", exception);
        }
    }

    private static void setField(final LocationRequirement requirement, final String fieldName, final Object value) {
        try {
            final Field field = LocationRequirement.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            try {
                if (field.getType() == double.class) {
                    field.setDouble(requirement, (Double) value);
                } else {
                    field.set(requirement, value);
                }
                return;
            } catch (final IllegalAccessException ignored) {
                final long offset = UNSAFE.objectFieldOffset(field);
                if (field.getType() == double.class) {
                    UNSAFE.putDouble(requirement, offset, (Double) value);
                } else {
                    UNSAFE.putObject(requirement, offset, value);
                }
            }
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set field '" + fieldName + "'", exception);
        }
    }
}
