package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class PRQSectionTest {

    @Test
    void itPropagatesCanonicalNameAndEvaluationEnvironment() {
        final EvaluationEnvironmentBuilder builder = mock(EvaluationEnvironmentBuilder.class);
        final PRQSection section = new PRQSection(builder);

        assertEquals("prq", section.getName(), "The canonical command name should be propagated to ACommandSection");
        assertSame(builder, locateEvaluationEnvironment(section),
            "The evaluation environment builder should be stored by the superclass");
    }

    private EvaluationEnvironmentBuilder locateEvaluationEnvironment(final PRQSection section) {
        Class<?> current = section.getClass();
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0
                    && EvaluationEnvironmentBuilder.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    try {
                        final Object value = method.invoke(section);
                        if (value instanceof EvaluationEnvironmentBuilder) {
                            return (EvaluationEnvironmentBuilder) value;
                        }
                    } catch (final ReflectiveOperationException exception) {
                        fail("Failed to invoke evaluation environment accessor", exception);
                    }
                }
            }
            current = current.getSuperclass();
        }

        current = section.getClass();
        while (current != null) {
            for (final Field field : current.getDeclaredFields()) {
                if (EvaluationEnvironmentBuilder.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        final Object value = field.get(section);
                        if (value instanceof EvaluationEnvironmentBuilder) {
                            return (EvaluationEnvironmentBuilder) value;
                        }
                    } catch (final IllegalAccessException exception) {
                        fail("Failed to read evaluation environment field", exception);
                    }
                }
            }
            current = current.getSuperclass();
        }

        fail("Unable to locate evaluation environment builder on PRQSection");
        return null;
    }
}
