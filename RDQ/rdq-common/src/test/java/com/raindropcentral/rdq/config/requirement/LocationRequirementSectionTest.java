package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocationRequirementSectionTest {

    @Test
    void itProvidesDefaultValuesWhenUnset() {
        final LocationRequirementSection section = new LocationRequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("", section.getRequiredWorld(), "getRequiredWorld should default to empty string when unset");
        assertEquals("", section.getRequiredRegion(), "getRequiredRegion should default to empty string when unset");
        assertEquals(0.0D, section.getRequiredDistance(), "getRequiredDistance should default to zero when unset");
        assertFalse(section.getExactLocation(), "getExactLocation should default to false when unset");
    }

    @Test
    void itMergesCoordinateSources() throws Exception {
        final LocationRequirementSection section = new LocationRequirementSection(new EvaluationEnvironmentBuilder());

        final Map<String, Double> requiredCoordinates = new HashMap<>();
        requiredCoordinates.put("x", 1.5D);
        requiredCoordinates.put("z", 2.5D);

        final Map<String, Double> coordinates = new HashMap<>();
        coordinates.put("y", 3.5D);
        coordinates.put("x", 4.5D);

        setField(section, "requiredCoordinates", requiredCoordinates);
        setField(section, "coordinates", coordinates);
        setField(section, "x", 5.5D);
        setField(section, "y", 6.5D);
        setField(section, "z", 7.5D);

        final Map<String, Double> mergedCoordinates = section.getRequiredCoordinates();
        assertEquals(3, mergedCoordinates.size(), "getRequiredCoordinates should merge map and scalar sources");
        assertEquals(5.5D, mergedCoordinates.get("x"), "scalar x should override map entries");
        assertEquals(6.5D, mergedCoordinates.get("y"), "scalar y should override map entries");
        assertEquals(7.5D, mergedCoordinates.get("z"), "scalar z should override map entries");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field locateField(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
