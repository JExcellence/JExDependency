package de.jexcellence.economy.command.player.currencies;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ECurrenciesActionTest {

    private static final EnumSet<ECurrenciesAction> DOCUMENTED_ACTIONS = EnumSet.of(
        ECurrenciesAction.CREATE,
        ECurrenciesAction.DELETE,
        ECurrenciesAction.EDIT,
        ECurrenciesAction.INFO,
        ECurrenciesAction.HELP,
        ECurrenciesAction.OVERVIEW
    );

    private static final Map<ECurrenciesAction, String> DOCUMENTED_DISPLAY_NAMES = Map.of(
        ECurrenciesAction.CREATE, "create",
        ECurrenciesAction.DELETE, "delete",
        ECurrenciesAction.EDIT, "edit",
        ECurrenciesAction.INFO, "info",
        ECurrenciesAction.HELP, "help",
        ECurrenciesAction.OVERVIEW, "overview"
    );

    private static final Map<ECurrenciesAction, String> DOCUMENTED_USAGE_SNIPPETS = Map.of(
        ECurrenciesAction.CREATE, "/currencies create <identifier> <symbol> [prefix] [suffix]",
        ECurrenciesAction.DELETE, "/currencies delete <identifier>",
        ECurrenciesAction.EDIT, "/currencies edit <identifier> <field> <value>",
        ECurrenciesAction.INFO, "/currencies info <identifier>",
        ECurrenciesAction.OVERVIEW, "/currencies overview"
    );

    private static final Map<ECurrenciesAction, String> DOCUMENTED_PERMISSION_NODES = Map.of(
        ECurrenciesAction.CREATE, "currencies.command.create",
        ECurrenciesAction.DELETE, "currencies.command.delete",
        ECurrenciesAction.EDIT, "currencies.command.update",
        ECurrenciesAction.OVERVIEW, "currencies.command.overview",
        ECurrenciesAction.HELP, "currencies.command"
    );

    @Test
    void shouldExposeDocumentedActionIdentifiersAndDisplayNames() {
        assertEquals(DOCUMENTED_ACTIONS, EnumSet.allOf(ECurrenciesAction.class));

        DOCUMENTED_ACTIONS.forEach(action -> assertEquals(
            DOCUMENTED_DISPLAY_NAMES.get(action),
            action.getActionName(),
            "Action " + action + " must expose the documented display name"
        ));
    }

    @Test
    void shouldDocumentRequiredArgumentsInHelpTranslations() throws IOException {
        final String englishTranslations = readResource("translations/en.yml");

        DOCUMENTED_USAGE_SNIPPETS.forEach((action, usageSnippet) -> assertTrue(
            englishTranslations.contains(usageSnippet),
            "Expected help documentation for " + action + " to mention usage snippet '" + usageSnippet + "'"
        ));
    }

    @Test
    void shouldAlignPermissionNodesWithDocumentedActions() throws IOException {
        final String commandConfiguration = readResource("commands/pcurrencies.yml");

        DOCUMENTED_PERMISSION_NODES.forEach((action, permissionNode) -> {
            assertTrue(
                commandConfiguration.contains(permissionNode),
                "Command configuration must reference permission node '" + permissionNode + "' for action " + action
            );

            assertTrue(
                ECurrenciesPermission.fromFallbackNode(permissionNode).isPresent(),
                "Permission enum should resolve documented node '" + permissionNode + "'"
            );
        });
    }

    private String readResource(final String resourcePath) throws IOException {
        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(resourceStream, "Resource '" + resourcePath + "' must exist for documentation alignment tests");
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
