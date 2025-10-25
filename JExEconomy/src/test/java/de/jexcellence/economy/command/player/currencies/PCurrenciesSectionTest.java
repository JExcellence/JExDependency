package de.jexcellence.economy.command.player.currencies;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class PCurrenciesSectionTest {

    @Test
    void itLoadsCommandMetadataFromConfiguration() {
        final PCurrenciesSection section = new PCurrenciesSection(mock(EvaluationEnvironmentBuilder.class));

        assertEquals("pcurrencies", section.getName(), "Command name should match the YAML identifier");
        assertEquals("", section.getDescription(), "Description defaults to an empty string in the YAML");
        assertEquals("currencies", section.getUsage(), "Usage string should reflect the configured help token");
        assertEquals(List.of("currencies"), section.getAliases(), "Aliases should expose the configured synonym");
    }

    @Test
    void itRegistersCurrencyPermissionsForEachAction() {
        final PCurrenciesSection section = new PCurrenciesSection(mock(EvaluationEnvironmentBuilder.class));

        final PermissionsSection permissionsSection = section.getPermissions();
        assertNotNull(permissionsSection, "Permissions section should be populated from configuration");

        final Map<String, IPermissionNode> nodes = extractPermissionNodes(permissionsSection);

        final Set<String> expectedKeys = Set.of(
            "command",
            "commandCreate",
            "commandDelete",
            "commandEdit",
            "commandOverview"
        );

        assertEquals(expectedKeys, nodes.keySet(), "All permission nodes from the YAML should be registered");
        assertEquals("currencies.command", nodes.get("command").getFallbackNode());
        assertEquals("currencies.command.create", nodes.get("commandCreate").getFallbackNode());
        assertEquals("currencies.command.delete", nodes.get("commandDelete").getFallbackNode());
        assertEquals("currencies.command.update", nodes.get("commandEdit").getFallbackNode());
        assertEquals("currencies.command.overview", nodes.get("commandOverview").getFallbackNode());
    }

    @Test
    void itExposesArgumentUsageMetadataForTabCompletion() {
        final PCurrenciesSection section = new PCurrenciesSection(mock(EvaluationEnvironmentBuilder.class));

        final Map<String, String> argumentUsages = extractArgumentUsageMetadata(section);
        assertEquals(1, argumentUsages.size(), "Expected a single argument usage entry for index 1$");
        assertEquals(
            "lut[\"prefix\"] & \"&7/\" & alias & \"&7\"",
            argumentUsages.get("1$"),
            "Argument usage metadata should mirror the configured expression"
        );
    }

    private Map<String, IPermissionNode> extractPermissionNodes(final PermissionsSection permissionsSection) {
        final Map<String, IPermissionNode> viaMethod = tryResolvePermissionNodesViaMethod(permissionsSection);
        if (!viaMethod.isEmpty()) {
            return viaMethod;
        }

        final Map<String, IPermissionNode> viaFields = new LinkedHashMap<>();
        final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        locatePermissionNodesRecursively(permissionsSection, visited, viaFields);

        if (viaFields.isEmpty()) {
            fail("Unable to locate permission nodes on PermissionsSection implementation "
                + permissionsSection.getClass().getName());
        }

        return viaFields;
    }

    private Map<String, IPermissionNode> tryResolvePermissionNodesViaMethod(final PermissionsSection permissionsSection) {
        for (final String candidate : List.of("getNodes", "nodes", "getPermissionNodes", "permissionNodes", "getAllNodes")) {
            try {
                final Method method = permissionsSection.getClass().getMethod(candidate);
                final Object value = method.invoke(permissionsSection);
                if (value instanceof Map<?, ?> map) {
                    final Map<String, IPermissionNode> cast = castPermissionMap(map);
                    if (!cast.isEmpty()) {
                        return cast;
                    }
                }
            } catch (final NoSuchMethodException ignored) {
                // Continue probing additional method names.
            } catch (final ReflectiveOperationException exception) {
                fail("Failed to inspect permission nodes via method " + candidate, exception);
            }
        }

        return Collections.emptyMap();
    }

    private void locatePermissionNodesRecursively(
        final Object current,
        final IdentityHashMap<Object, Boolean> visited,
        final Map<String, IPermissionNode> accumulator
    ) {
        if (current == null || visited.putIfAbsent(current, Boolean.TRUE) != null) {
            return;
        }

        if (current instanceof Map<?, ?> map) {
            final Map<String, IPermissionNode> cast = castPermissionMap(map);
            if (!cast.isEmpty()) {
                accumulator.putAll(cast);
            }
        }

        for (Class<?> type = current.getClass(); type != null; type = type.getSuperclass()) {
            for (final Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    locatePermissionNodesRecursively(field.get(current), visited, accumulator);
                } catch (final IllegalAccessException exception) {
                    fail("Failed to access field " + field.getName(), exception);
                }
            }
        }
    }

    private Map<String, IPermissionNode> castPermissionMap(final Map<?, ?> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, IPermissionNode> converted = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof IPermissionNode node)) {
                return Collections.emptyMap();
            }
            converted.put(key, node);
        }
        return converted;
    }

    private Map<String, String> extractArgumentUsageMetadata(final ACommandSection section) {
        final Map<String, String> viaMethod = tryResolveArgumentUsageViaMethod(section);
        if (!viaMethod.isEmpty()) {
            return viaMethod;
        }

        final Map<String, String> viaFields = new LinkedHashMap<>();
        final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        locateArgumentUsageRecursively(section, visited, viaFields);

        if (!viaFields.containsKey("1$")) {
            fail("Unable to locate argument usage metadata on section implementation "
                + section.getClass().getName());
        }

        return viaFields;
    }

    private Map<String, String> tryResolveArgumentUsageViaMethod(final ACommandSection section) {
        for (final String candidate : List.of("getArgumentUsages", "argumentUsages", "getArgumentsUsage", "arguments")) {
            try {
                final Method method = section.getClass().getMethod(candidate);
                final Object value = method.invoke(section);
                if (value instanceof Map<?, ?> map) {
                    final Map<String, String> cast = castArgumentUsageMap(map);
                    if (!cast.isEmpty()) {
                        return cast;
                    }
                }
            } catch (final NoSuchMethodException ignored) {
                // Try the next candidate method name.
            } catch (final ReflectiveOperationException exception) {
                fail("Failed to inspect argument usage metadata via method " + candidate, exception);
            }
        }

        return Collections.emptyMap();
    }

    private void locateArgumentUsageRecursively(
        final Object current,
        final IdentityHashMap<Object, Boolean> visited,
        final Map<String, String> accumulator
    ) {
        if (current == null || visited.putIfAbsent(current, Boolean.TRUE) != null) {
            return;
        }

        if (current instanceof Map<?, ?> map) {
            final Map<String, String> cast = castArgumentUsageMap(map);
            if (!cast.isEmpty()) {
                accumulator.putAll(cast);
            }
        }

        for (Class<?> type = current.getClass(); type != null; type = type.getSuperclass()) {
            for (final Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    locateArgumentUsageRecursively(field.get(current), visited, accumulator);
                } catch (final IllegalAccessException exception) {
                    fail("Failed to access field " + field.getName(), exception);
                }
            }
        }
    }

    private Map<String, String> castArgumentUsageMap(final Map<?, ?> map) {
        if (!map.containsKey("1$")) {
            return Collections.emptyMap();
        }

        final Map<String, String> converted = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                return Collections.emptyMap();
            }
            converted.put(key, Objects.toString(entry.getValue(), ""));
        }

        return converted;
    }
}

