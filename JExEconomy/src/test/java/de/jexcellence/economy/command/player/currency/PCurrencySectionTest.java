package de.jexcellence.economy.command.player.currency;

import de.jexcellence.evaluable.section.PermissionsSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PCurrencySectionTest {

        private PCurrencySection section;

        @BeforeEach
        void setUp() {
                this.section = new PCurrencySection(new EvaluationEnvironmentBuilder());
        }

        @Test
        void itRegistersActionsWithCurrencyPermissions() {
                assertEquals("pcurrency", this.section.getName(),
                        "The canonical command name should be propagated to ACommandSection");
                assertEquals(List.of("currency", "balance", "bal", "money"), this.section.getAliases(),
                        "Aliases declared in configuration should surface for tab completion metadata");

                final PermissionsSection permissionsSection = this.section.getPermissions();
                assertNotNull(permissionsSection, "A permissions section should be exposed for the command");

                final Map<?, ?> permissionNodes = locateMapContaining(permissionsSection,
                        entry -> entry.getValue() instanceof ECurrencyPermission);

                assertNotNull(permissionNodes, "Currency permission map should be discoverable on the permissions section");
                assertSame(ECurrencyPermission.CURRENCY, permissionNodes.get("command"),
                        "Base command access should require the currency command permission");
                assertSame(ECurrencyPermission.CURRENCY_OTHER, permissionNodes.get("commandOther"),
                        "Cross-player operations should require the elevated currency permission");

                final Map<?, ?> actionPermissions = locateMapContaining(this.section,
                        entry -> entry.getKey() instanceof String && "help".equalsIgnoreCase(entry.getKey().toString())
                                && entry.getValue() instanceof ECurrencyPermission);

                assertNotNull(actionPermissions,
                        "The command section should register actions mapped to their required permissions");
                assertSame(ECurrencyPermission.CURRENCY, actionPermissions.get("help"),
                        "The help subcommand should fall back to the base currency permission");
        }

        @Test
        void itExposesHelpTextAndTabCompletionMetadata() {
                final Map<?, ?> argumentUsage = locateMapContaining(this.section,
                        entry -> Objects.equals("1$", entry.getKey()));
                assertNotNull(argumentUsage, "Argument usage metadata should be available for help text rendering");
                assertEquals("lut[\"prefix\"] & \"&7/\" & alias & \"&7\"",
                        argumentUsage.get("1$"),
                        "The first argument usage should mirror the configured help text template");

                final Map<?, ?> errorMessages = locateMapContaining(this.section,
                        entry -> Objects.equals("internalError$", entry.getKey()));
                assertNotNull(errorMessages, "Error message metadata should be hydrated from configuration");
                assertTrue(errorMessages.get("internalError$") instanceof String,
                        "Internal error help text should remain a string template");

                final Map<?, ?> permissionMeta = locateMapContaining(this.section.getPermissions(),
                        entry -> Objects.equals("missingMessage$", entry.getKey()));
                assertNotNull(permissionMeta, "Permission metadata should expose the missing permission message template");
                assertEquals("lut[\"prefix\"] & \"You\\sre lacking the permission: \" & permission",
                        permissionMeta.get("missingMessage$"),
                        "Missing permission help text should match the configured message");
        }

        private Map<?, ?> locateMapContaining(final Object root,
                                               final @NotNull java.util.function.Predicate<Map.Entry<?, ?>> predicate) {
                return locateMapContaining(root, predicate, Set.newSetFromMap(new IdentityHashMap<>()));
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

                final Class<?> type = root.getClass();
                if (type.isPrimitive() || type.getName().startsWith("java.")) {
                        return null;
                }

                Class<?> current = type;
                while (current != null && !Object.class.equals(current)) {
                        for (final Field field : current.getDeclaredFields()) {
                                if (Modifier.isStatic(field.getModifiers())) {
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
}
