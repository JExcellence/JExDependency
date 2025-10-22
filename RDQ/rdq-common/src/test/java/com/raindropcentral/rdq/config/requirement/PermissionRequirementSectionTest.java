package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionRequirementSectionTest {

    @Test
    void itProvidesDefaultsForEvaluationFlagsAndPermissionFallback() {
        final PermissionRequirementSection section = new PermissionRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getRequireAll(), "getRequireAll should default to true when unset");
        assertFalse(section.getCheckNegation(), "getCheckNegation should default to false when unset");
        assertEquals("", section.getRequiredPermission(), "getRequiredPermission should return an empty string when unset");
    }

    @Test
    void itCombinesAllPermissionSourcesWithoutDuplicatingSingleValues() throws Exception {
        final PermissionRequirementSection section = new PermissionRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "requiredPermissions", new ArrayList<>(List.of(
                "rdq.permission.primary"
        )));
        setField(section, "permissions", new ArrayList<>(List.of(
                "rdq.permission.secondary",
                "rdq.permission.tertiary"
        )));
        setField(section, "permission", "rdq.permission.primary");

        final List<String> requiredPermissions = section.getRequiredPermissions();
        final List<String> expectedPermissions = List.of(
                "rdq.permission.primary",
                "rdq.permission.secondary",
                "rdq.permission.tertiary"
        );

        assertEquals(expectedPermissions, requiredPermissions, "getRequiredPermissions should merge all fields without duplicating the single permission");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
