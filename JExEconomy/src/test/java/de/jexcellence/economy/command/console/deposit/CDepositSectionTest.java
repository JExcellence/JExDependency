package de.jexcellence.economy.command.console.deposit;

import com.raindropcentral.commands.BukkitCommand;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class CDepositSectionTest {

        private EvaluationEnvironmentBuilder environmentBuilder;
        private CDepositSection section;

        @BeforeEach
        void setUp() {
                this.environmentBuilder = mock(EvaluationEnvironmentBuilder.class, withSettings().stubOnly());
                this.section = new CDepositSection(this.environmentBuilder);
        }

        @Test
        void itExposesCommandMetadataFromConfiguration() {
                assertEquals("cdeposit", this.section.getName(), "The canonical command name should match the YAML metadata");
                assertEquals(
                        "Deposit a certain amount of currencies to a player.",
                        this.section.getDescription(),
                        "The description should be sourced from commands/cdeposit.yml"
                );
                assertEquals(
                        "cdeposit <player_name> <currency_name> <amount>",
                        this.section.getUsage(),
                        "The usage string should mirror the console help entry"
                );
                assertEquals(List.of("cdeposit"), this.section.getAliases(), "Command alias should only contain the canonical name");
                assertSame(
                        this.environmentBuilder,
                        locateEvaluationEnvironment(this.section),
                        "The evaluation environment builder should be retained by the section"
                );
        }

        @Test
        void itProvidesArgumentMetadataAndConsolePermissions() {
                final Map<String, String> argumentUsages = extractArgumentUsages(this.section);

                assertEquals(3, argumentUsages.size(), "Three argument usage entries should be exposed");
                assertEquals(
                        "lut[\"prefix\"] & \"&7/\" & alias & \" &c<player> &7<currency> &7<amount>\"",
                        argumentUsages.get("1$"),
                        "First argument usage should highlight the player token"
                );
                assertEquals(
                        "lut[\"prefix\"] & \"&7/\" & alias & \" &7<player> &c<currency> &7<amount>\"",
                        argumentUsages.get("2$"),
                        "Second argument usage should highlight the currency token"
                );
                assertEquals(
                        "lut[\"prefix\"] & \"&7/\" & alias & \" &7<player> &7<currency> &c<amount>\"",
                        argumentUsages.get("3$"),
                        "Third argument usage should highlight the amount token"
                );

                final Object permissions = extractPermissions(this.section);
                assertNotNull(permissions, "Console commands should expose a permissions facade");
                assertTrue(
                        locateConsoleOnlyFlag(permissions),
                        "Console-only flag should be enabled for the deposit command"
                );
        }

        @Test
        void itBuildsCommandDelegateForCDeposit() throws NoSuchFieldException, IllegalAccessException {
                final JExEconomy plugin = mock(JExEconomy.class, withSettings().defaultAnswer(invocation -> null));
                final JExEconomyImpl implementation = mock(JExEconomyImpl.class, withSettings().stubOnly());

                when(plugin.getImpl()).thenReturn(implementation);

                final CDeposit command = new CDeposit(this.section, plugin);

                assertEquals(this.section.getName(), command.getName(), "Command should expose section command name");
                assertEquals(this.section.getDescription(), command.getDescription(), "Command should surface section description");
                assertEquals(this.section.getUsage(), command.getUsage(), "Command should surface section usage");
                assertEquals(this.section.getAliases(), command.getAliases(), "Command should inherit section aliases");

                final Field commandSectionField = BukkitCommand.class.getDeclaredField("commandSection");
                commandSectionField.setAccessible(true);
                assertSame(this.section, commandSectionField.get(command), "Command should retain the supplied section instance");
        }

        private EvaluationEnvironmentBuilder locateEvaluationEnvironment(final CDepositSection target) {
                Class<?> current = target.getClass();
                while (current != null) {
                        for (final Method method : current.getDeclaredMethods()) {
                                if (method.getParameterCount() == 0
                                        && EvaluationEnvironmentBuilder.class.isAssignableFrom(method.getReturnType())) {
                                        method.setAccessible(true);
                                        try {
                                                final Object value = method.invoke(target);
                                                if (value instanceof EvaluationEnvironmentBuilder builder) {
                                                        return builder;
                                                }
                                        } catch (final IllegalAccessException | InvocationTargetException exception) {
                                                fail("Failed to invoke evaluation environment accessor", exception);
                                        }
                                }
                        }
                        current = current.getSuperclass();
                }

                current = target.getClass();
                while (current != null) {
                        for (final Field field : current.getDeclaredFields()) {
                                if (EvaluationEnvironmentBuilder.class.isAssignableFrom(field.getType())) {
                                        field.setAccessible(true);
                                        try {
                                                final Object value = field.get(target);
                                                if (value instanceof EvaluationEnvironmentBuilder builder) {
                                                        return builder;
                                                }
                                        } catch (final IllegalAccessException exception) {
                                                fail("Failed to read evaluation environment field", exception);
                                        }
                                }
                        }
                        current = current.getSuperclass();
                }

                fail("Unable to locate evaluation environment builder on CDepositSection");
                return null;
        }

        @SuppressWarnings("unchecked")
        private Map<String, String> extractArgumentUsages(final CDepositSection target) {
                for (final Method method : target.getClass().getMethods()) {
                        if (method.getParameterCount() == 0
                                && Map.class.isAssignableFrom(method.getReturnType())
                                && method.getName().toLowerCase().contains("argument")) {
                                try {
                                        final Object value = method.invoke(target);
                                        if (value instanceof Map<?, ?> map) {
                                                return (Map<String, String>) map;
                                        }
                                } catch (final IllegalAccessException | InvocationTargetException exception) {
                                        fail("Failed to read argument usage metadata", exception);
                                }
                        }
                }
                fail("Unable to locate argument usage metadata on CDepositSection");
                return Map.of();
        }

        private Object extractPermissions(final CDepositSection target) {
                for (final Method method : target.getClass().getMethods()) {
                        if (method.getParameterCount() == 0
                                && !CharSequence.class.isAssignableFrom(method.getReturnType())
                                && method.getName().toLowerCase().contains("permission")) {
                                try {
                                        final Object value = method.invoke(target);
                                        if (value != null) {
                                                return value;
                                        }
                                } catch (final IllegalAccessException | InvocationTargetException exception) {
                                        fail("Failed to resolve permissions metadata", exception);
                                }
                        }
                }
                return null;
        }

        private boolean locateConsoleOnlyFlag(final Object permissions) {
                if (permissions == null) {
                        return false;
                }
                for (final Method method : permissions.getClass().getMethods()) {
                        if (method.getParameterCount() == 0
                                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)
                                && method.getName().toLowerCase().contains("console")) {
                                try {
                                        return Boolean.TRUE.equals(method.invoke(permissions));
                                } catch (final IllegalAccessException | InvocationTargetException exception) {
                                        fail("Failed to evaluate console-only flag", exception);
                                }
                        }
                }
                // If no explicit flag is present, fall back to checking absence of player nodes
                final Collection<?> nodes = locatePermissionNodes(permissions);
                return nodes != null && nodes.isEmpty();
        }

        private Collection<?> locatePermissionNodes(final Object permissions) {
                if (permissions == null) {
                        return null;
                }
                for (final Method method : permissions.getClass().getMethods()) {
                        if (method.getParameterCount() == 0
                                && Collection.class.isAssignableFrom(method.getReturnType())
                                && method.getName().toLowerCase().contains("node")) {
                                try {
                                        final Object value = method.invoke(permissions);
                                        if (value instanceof Collection<?>) {
                                                return (Collection<?>) value;
                                        }
                                } catch (final IllegalAccessException | InvocationTargetException exception) {
                                        fail("Failed to read permission nodes", exception);
                                }
                        }
                }
                return null;
        }
}
