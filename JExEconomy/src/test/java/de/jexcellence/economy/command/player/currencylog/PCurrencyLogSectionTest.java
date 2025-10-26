package de.jexcellence.economy.command.player.currencylog;

import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PCurrencyLogSectionTest {

    private PCurrencyLogSection section;

    @BeforeEach
    void setUp() {
        this.section = new PCurrencyLogSection(new EvaluationEnvironmentBuilder());
    }

    @Test
    void itLoadsCommandMetadataFromConfiguration() {
        assertEquals("pcurrencylog", this.section.getName(),
                "Command name should match the YAML identifier");
        assertEquals("", this.section.getDescription(),
                "Description defaults to an empty string in the YAML");
        assertEquals("pcurrencylog", this.section.getUsage(),
                "Usage string should reflect the configured help token");
        assertEquals(List.of("plog", "currencylog", "economylog", "ecolog"), this.section.getAliases(),
                "Aliases should expose each configured synonym");
    }

    @Test
    void itExposesHelpUsageAndPermissionMetadata() {
        final Map<?, ?> argumentUsage = locateMapContaining(this.section,
                entry -> Objects.equals("1$", entry.getKey()));
        assertNotNull(argumentUsage, "Argument usage metadata should be available for help text rendering");
        assertEquals("lut[\"prefix\"] & \"&7/\" & alias & \"&7\"",
                argumentUsage.get("1$"),
                "Argument usage metadata should mirror the configured expression");

        final Map<?, ?> errorMessages = locateMapContaining(this.section,
                entry -> Objects.equals("internalError$", entry.getKey()));
        assertNotNull(errorMessages, "Error message metadata should be hydrated from configuration");
        assertEquals("lut[\"prefix\"] & \"&4An internal error occurred\"",
                errorMessages.get("internalError$"),
                "Internal error help text should match the configured message");

        final Map<?, ?> permissionMeta = locateMapContaining(this.section.getPermissions(),
                entry -> Objects.equals("missingMessage$", entry.getKey()));
        assertNotNull(permissionMeta, "Permission metadata should expose the missing permission message template");
        assertEquals("lut[\"prefix\"] & \"You\\sre lacking the permission: \" & permission",
                permissionMeta.get("missingMessage$"),
                "Missing permission help text should match the configured message");

        final PermissionsSection permissionsSection = this.section.getPermissions();
        assertNotNull(permissionsSection, "Permissions section should be populated from configuration");

        final Map<String, IPermissionNode> nodes = extractPermissionNodes(permissionsSection);
        assertEquals(Set.of("command"), nodes.keySet(), "Currency log exposes a single base permission node");
        assertEquals("pcurrencylog.command", nodes.get("command").getFallbackNode(),
                "The base permission node should mirror the YAML fallback node");
    }

    @Test
    void itConfiguresSubcommandsWithFilterAndPaginationParameters() {
        final Map<?, ?> actions = locateMapContaining(this.section,
                entry -> entry.getKey() instanceof String key && "view".equalsIgnoreCase(key));
        assertNotNull(actions, "Command actions should be registered on the section");

        final Set<String> actionKeys = actions.keySet().stream()
                .map(Object::toString)
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(actionKeys.containsAll(Set.of("view", "filter", "clear", "stats", "export", "details", "help")),
                "All currency log actions should be present for routing");

        final Map<String, Object> viewAction = castStringKeyedMap(actions.get("view"));
        assertFalse(viewAction.isEmpty(), "View action metadata should be structured as a string keyed map");
        final Map<?, ?> paginationParameters = locateMapContaining(viewAction,
                entry -> entry.getKey() instanceof String key && "page".equalsIgnoreCase(key));
        assertNotNull(paginationParameters, "Pagination parameters should include the page token");
        assertTrue(paginationParameters.containsKey("page"),
                "Pagination metadata should expose a page parameter for navigation");

        final Map<String, Object> filterAction = castStringKeyedMap(actions.get("filter"));
        assertFalse(filterAction.isEmpty(), "Filter action metadata should be structured as a string keyed map");
        final Map<?, ?> filterParameters = locateMapContaining(filterAction,
                entry -> entry.getKey() instanceof String key && key.toLowerCase().contains("filter"));
        assertNotNull(filterParameters, "Filter parameters should expose filter type and value tokens");
        assertTrue(filterParameters.containsKey("filterType"),
                "Filter metadata should expose the filter type parameter");
        assertTrue(filterParameters.containsKey("filterValue"),
                "Filter metadata should expose the filter value parameter");
    }

    private Map<String, IPermissionNode> extractPermissionNodes(final PermissionsSection permissionsSection) {
        final Map<String, IPermissionNode> viaMethod = tryResolvePermissionNodesViaMethod(permissionsSection);
        if (!viaMethod.isEmpty()) {
            return viaMethod;
        }

        final Map<String, IPermissionNode> viaFields = new LinkedHashMap<>();
        locatePermissionNodesRecursively(permissionsSection, new IdentityHashMap<>(), viaFields);

        if (viaFields.isEmpty()) {
            throw new AssertionError("Unable to locate permission nodes on PermissionsSection implementation "
                    + permissionsSection.getClass().getName());
        }

        return viaFields;
    }

    private Map<String, IPermissionNode> tryResolvePermissionNodesViaMethod(final PermissionsSection permissionsSection) {
        for (final String candidate : List.of("getNodes", "nodes", "getPermissionNodes", "permissionNodes", "getAllNodes")) {
            try {
                final Object value = permissionsSection.getClass().getMethod(candidate).invoke(permissionsSection);
                if (value instanceof Map<?, ?> map) {
                    final Map<String, IPermissionNode> cast = castPermissionMap(map);
                    if (!cast.isEmpty()) {
                        return cast;
                    }
                }
            } catch (final ReflectiveOperationException ignored) {
                // Continue probing additional method names.
            }
        }

        return java.util.Collections.emptyMap();
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
            for (final java.lang.reflect.Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    locatePermissionNodesRecursively(field.get(current), visited, accumulator);
                } catch (final IllegalAccessException ignored) {
                    // Ignore inaccessible fields and continue scanning.
                }
            }
        }
    }

    private Map<String, IPermissionNode> castPermissionMap(final Map<?, ?> map) {
        final Map<String, IPermissionNode> converted = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof IPermissionNode node) {
                converted.put(key, node);
            } else {
                return java.util.Collections.emptyMap();
            }
        }
        return converted;
    }

    private Map<?, ?> locateMapContaining(final Object root,
            final @NotNull java.util.function.Predicate<Map.Entry<?, ?>> predicate) {
        return locateMapContaining(root, predicate, java.util.Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private Map<?, ?> locateMapContaining(final Object root,
            final @NotNull java.util.function.Predicate<Map.Entry<?, ?>> predicate,
            final Set<Object> visited) {
        if (root == null || root instanceof Class<?> || !visited.add(root)) {
            return null;
        }

        if (root instanceof Map<?, ?> map) {
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                if (predicate.test(entry)) {
                    return map;
                }
                final Map<?, ?> nested = locateMapContaining(entry.getValue(), predicate, visited);
                if (nested != null) {
                    return nested;
                }
            }
        }

        if (root instanceof Collection<?> collection) {
            for (final Object element : collection) {
                final Map<?, ?> nested = locateMapContaining(element, predicate, visited);
                if (nested != null) {
                    return nested;
                }
            }
        }

        final Class<?> type = root.getClass();
        if (type.isPrimitive() || type.getName().startsWith("java.")) {
            return null;
        }

        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            for (final java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    final Object value = field.get(root);
                    final Map<?, ?> located = locateMapContaining(value, predicate, visited);
                    if (located != null) {
                        return located;
                    }
                } catch (final IllegalAccessException ignored) {
                    // ignore inaccessible fields and continue scanning
                }
            }
            current = current.getSuperclass();
        }

        return null;
    }

    private Map<String, Object> castStringKeyedMap(final Object candidate) {
        if (!(candidate instanceof Map<?, ?> map)) {
            return java.util.Collections.emptyMap();
        }

        final Map<String, Object> converted = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                converted.put(key, entry.getValue());
            } else {
                return java.util.Collections.emptyMap();
            }
        }
        return converted;
    }
}
