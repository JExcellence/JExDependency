package de.jexcellence.economy.command.console.withdraw;

import de.jexcellence.evaluable.section.PermissionsSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class CWithdrawSectionTest {

    @Test
    void itRegistersCanonicalCommandMetadata() {
        EvaluationEnvironmentBuilder builder = mock(EvaluationEnvironmentBuilder.class);
        CWithdrawSection section = new CWithdrawSection(builder);

        assertEquals("cwithdraw", section.getName(), "The section should expose the canonical command name");
        assertSame(builder, locateEvaluationEnvironment(section),
                "The evaluation environment builder should be retained by the superclass");
    }

    @Test
    void itExposesMappedConfigurationData() {
        EvaluationEnvironmentBuilder builder = mock(EvaluationEnvironmentBuilder.class);
        CWithdrawSection section = new CWithdrawSection(builder);

        List<String> expectedAliases = List.of("cwithdraw", "cw");
        setField(section, field -> Collection.class.isAssignableFrom(field.getType())
                && field.getName().toLowerCase().contains("alias"), expectedAliases);
        assertEquals(expectedAliases, section.getAliases(), "Aliases should reflect the mapped configuration");

        PermissionsSection permissionsSection = mock(PermissionsSection.class);
        setField(section, field -> PermissionsSection.class.isAssignableFrom(field.getType()), permissionsSection);
        assertSame(permissionsSection, section.getPermissions(), "Permissions should mirror the mapped configuration");

        Map<String, String> argumentUsages = new LinkedHashMap<>();
        argumentUsages.put("1$", "<player>");
        argumentUsages.put("2$", "<currency>");
        argumentUsages.put("3$", "<amount>");
        setField(section, field -> Map.class.isAssignableFrom(field.getType())
                && field.getName().toLowerCase().contains("argument"), argumentUsages);

        Method argumentAccessor = locateArgumentAccessor(section);
        Object resolvedArguments = invoke(section, argumentAccessor);
        assertInstanceOf(Map.class, resolvedArguments, "Argument accessor should yield a map definition");
        assertEquals(argumentUsages, resolvedArguments, "Argument usages should match the mapped configuration");

        String expectedUsage = "cwithdraw <player> <currency> <amount>";
        setField(section, field -> String.class.equals(field.getType())
                && field.getName().toLowerCase().contains("usage"), expectedUsage);
        assertEquals(expectedUsage, section.getUsage(), "Usage text should expose the mapped help message");
    }

    private Method locateArgumentAccessor(CWithdrawSection section) {
        for (Method method : section.getClass().getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getName().toLowerCase().contains("argument")
                    && Map.class.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }
        fail("Unable to locate argument accessor on CWithdrawSection");
        return null;
    }

    private Object invoke(Object target, Method method) {
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            fail("Failed to invoke method " + method.getName(), exception);
            return null;
        }
    }

    private void setField(Object target, Predicate<Field> matcher, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (matcher.test(field)) {
                    field.setAccessible(true);
                    try {
                        field.set(target, value);
                        return;
                    } catch (IllegalAccessException exception) {
                        fail("Unable to set field " + field.getName(), exception);
                    }
                }
            }
            current = current.getSuperclass();
        }
        fail("Unable to locate field matching predicate on " + target.getClass().getName());
    }

    private EvaluationEnvironmentBuilder locateEvaluationEnvironment(CWithdrawSection section) {
        Class<?> current = section.getClass();
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0
                        && EvaluationEnvironmentBuilder.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    Object value = invoke(section, method);
                    if (value instanceof EvaluationEnvironmentBuilder) {
                        return (EvaluationEnvironmentBuilder) value;
                    }
                }
            }
            current = current.getSuperclass();
        }

        current = section.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (EvaluationEnvironmentBuilder.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(section);
                        if (value instanceof EvaluationEnvironmentBuilder builder) {
                            return builder;
                        }
                    } catch (IllegalAccessException exception) {
                        fail("Failed to read evaluation environment field", exception);
                    }
                }
            }
            current = current.getSuperclass();
        }

        fail("Unable to locate evaluation environment builder on CWithdrawSection");
        return null;
    }
}
