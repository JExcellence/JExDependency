package de.jexcellence.economy.command.player.currencylog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

class ECurrencyLogActionTest {

    private static final Map<ECurrencyLogAction, String> EXPECTED_KEYWORDS = Map.ofEntries(
        entry(ECurrencyLogAction.VIEW, "view"),
        entry(ECurrencyLogAction.FILTER, "filter"),
        entry(ECurrencyLogAction.STATS, "stats"),
        entry(ECurrencyLogAction.CLEAR, "clear"),
        entry(ECurrencyLogAction.EXPORT, "export"),
        entry(ECurrencyLogAction.HELP, "help"),
        entry(ECurrencyLogAction.DETAILS, "details")
    );

    private static final Map<ECurrencyLogAction, List<String>> EXPECTED_ARGUMENTS = Map.ofEntries(
        entry(ECurrencyLogAction.VIEW, List.of()),
        entry(ECurrencyLogAction.FILTER, List.of("filter-type", "filter-value")),
        entry(ECurrencyLogAction.STATS, List.of()),
        entry(ECurrencyLogAction.CLEAR, List.of()),
        entry(ECurrencyLogAction.EXPORT, List.of()),
        entry(ECurrencyLogAction.HELP, List.of()),
        entry(ECurrencyLogAction.DETAILS, List.of("log-id"))
    );

    private static final Map<ECurrencyLogAction, String> EXPECTED_USAGE_KEYS = Map.ofEntries(
        entry(ECurrencyLogAction.VIEW, "currency_log.command_view"),
        entry(ECurrencyLogAction.FILTER, "currency_log.command_filter"),
        entry(ECurrencyLogAction.STATS, "currency_log.command_stats"),
        entry(ECurrencyLogAction.CLEAR, "currency_log.command_clear"),
        entry(ECurrencyLogAction.EXPORT, "currency_log.command_export"),
        entry(ECurrencyLogAction.HELP, "currency_log.command_help"),
        entry(ECurrencyLogAction.DETAILS, "currency_log.command_details")
    );

    private static final Map<ECurrencyLogAction, String> EXPECTED_PERMISSION_NODES = Map.ofEntries(
        entry(ECurrencyLogAction.VIEW, "pcurrencylog.command"),
        entry(ECurrencyLogAction.FILTER, "pcurrencylog.command"),
        entry(ECurrencyLogAction.STATS, "pcurrencylog.command"),
        entry(ECurrencyLogAction.CLEAR, "pcurrencylog.command"),
        entry(ECurrencyLogAction.EXPORT, "jexeconomy.admin.export"),
        entry(ECurrencyLogAction.HELP, "pcurrencylog.command"),
        entry(ECurrencyLogAction.DETAILS, "pcurrencylog.command")
    );

    private static final Map<ECurrencyLogAction, String> EXPECTED_USAGE_SNIPPETS = Map.ofEntries(
        entry(ECurrencyLogAction.VIEW, "/pcurrencylog view"),
        entry(ECurrencyLogAction.FILTER, "/pcurrencylog filter"),
        entry(ECurrencyLogAction.STATS, "/pcurrencylog stats"),
        entry(ECurrencyLogAction.CLEAR, "/pcurrencylog clear"),
        entry(ECurrencyLogAction.EXPORT, "/pcurrencylog export"),
        entry(ECurrencyLogAction.HELP, "/pcurrencylog help"),
        entry(ECurrencyLogAction.DETAILS, "/pcurrencylog details")
    );

    @Test
    void shouldExposeDocumentedCommandMetadata() {
        assertEquals(EnumSet.allOf(ECurrencyLogAction.class), EXPECTED_KEYWORDS.keySet());

        for (final ECurrencyLogAction action : ECurrencyLogAction.values()) {
            assertEquals(EXPECTED_KEYWORDS.get(action), action.getCommandKeyword());
            assertEquals(EXPECTED_ARGUMENTS.get(action), action.getRequiredArguments());
            assertEquals(EXPECTED_USAGE_KEYS.get(action), action.getUsageTranslationKey());

            final Optional<String> permissionNode = action.getRequiredPermissionNode();
            assertTrue(permissionNode.isPresent());
            assertEquals(EXPECTED_PERMISSION_NODES.get(action), permissionNode.orElseThrow());
        }
    }

    @Test
    void shouldResolveActionsFromKeywordsIgnoringCaseAndWhitespace() {
        assertAll(
            () -> assertEquals(Optional.of(ECurrencyLogAction.VIEW), ECurrencyLogAction.fromCommandKeyword("view")),
            () -> assertEquals(Optional.of(ECurrencyLogAction.FILTER), ECurrencyLogAction.fromCommandKeyword(" FILTER")),
            () -> assertEquals(Optional.of(ECurrencyLogAction.EXPORT), ECurrencyLogAction.fromCommandKeyword("Export")),
            () -> assertEquals(Optional.of(ECurrencyLogAction.DETAILS), ECurrencyLogAction.fromCommandKeyword("details  ")),
            () -> assertTrue(ECurrencyLogAction.fromCommandKeyword(null).isEmpty()),
            () -> assertTrue(ECurrencyLogAction.fromCommandKeyword("  ").isEmpty()),
            () -> assertTrue(ECurrencyLogAction.fromCommandKeyword("unknown").isEmpty())
        );
    }

    @Test
    void usageTranslationKeysShouldBeDocumented() throws IOException {
        final String englishTranslations = readResource("translations/en.yml");

        for (final ECurrencyLogAction action : ECurrencyLogAction.values()) {
            final String key = action.getUsageTranslationKey();
            final String snippet = EXPECTED_USAGE_SNIPPETS.get(action);

            assertTrue(englishTranslations.contains(key + ":"), () -> "Missing translation key for " + action);
            assertTrue(englishTranslations.contains(snippet), () -> "Translation for " + action + " should mention '" + snippet + "'");
        }
    }

    @Test
    void permissionNodesShouldMatchCommandConfiguration() throws IOException {
        final String commandConfiguration = readResource("commands/pcurrencylog.yml");
        final Set<String> documentedPermissions = Set.copyOf(EXPECTED_PERMISSION_NODES.values());

        documentedPermissions.forEach(permission -> assertTrue(
            commandConfiguration.contains(permission),
            () -> "Command configuration should declare permission node '" + permission + "'"
        ));
    }

    private String readResource(final String resourcePath) throws IOException {
        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(resourceStream, "Resource '" + resourcePath + "' must exist for documentation alignment tests");
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
