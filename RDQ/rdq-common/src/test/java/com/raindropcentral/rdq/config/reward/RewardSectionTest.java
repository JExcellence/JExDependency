package com.raindropcentral.rdq.config.reward;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RewardSectionTest {

    @Test
    void itDefaultsFieldsToNull() throws Exception {
        final RewardSection section = new RewardSection(new EvaluationEnvironmentBuilder());

        assertNull(readField(section, "type"), "type should default to null");
        assertNull(readField(section, "target"), "target should default to null");
        assertNull(readField(section, "amount"), "amount should default to null");
    }

    @Test
    void itStoresAndReturnsConfiguredValues() throws Exception {
        final RewardSection section = new RewardSection(new EvaluationEnvironmentBuilder());

        final String expectedType = "currency";
        final String expectedTarget = "player";
        final Long expectedAmount = 5L;

        writeField(section, "type", expectedType);
        writeField(section, "target", expectedTarget);
        writeField(section, "amount", expectedAmount);

        assertEquals(expectedType, readField(section, "type"), "type should match the configured value");
        assertEquals(expectedTarget, readField(section, "target"), "target should match the configured value");
        assertEquals(expectedAmount, readField(section, "amount"), "amount should match the configured value");
    }

    private static Object readField(final Object target, final String name) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void writeField(final Object target, final String name, final Object value) throws Exception {
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
