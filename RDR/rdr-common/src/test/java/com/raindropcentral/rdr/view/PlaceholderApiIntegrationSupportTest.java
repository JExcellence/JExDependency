package com.raindropcentral.rdr.view;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests PlaceholderAPI admin integration support helpers.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
class PlaceholderApiIntegrationSupportTest {

    private static final String DOWNLOAD_PLAYER_PRIMARY_COMMAND = "papi ecloud download Player";
    private static final String DOWNLOAD_PLAYER_FALLBACK_COMMAND = "placeholderapi ecloud download Player";
    private static final String RELOAD_PRIMARY_COMMAND = "papi reload";
    private static final String RELOAD_FALLBACK_COMMAND = "placeholderapi reload";

    @Test
    void reportsUnsupportedWhenPluginManagerIsNull() {
        assertFalse(PlaceholderApiIntegrationSupport.isPlaceholderApiSupported(null));
    }

    @Test
    void reportsUnsupportedWhenPlaceholderApiPluginIsMissing() {
        assertFalse(PlaceholderApiIntegrationSupport.isPlaceholderApiSupported(this.pluginManagerReturning(null)));
    }

    @Test
    void reportsUnsupportedWhenPlaceholderApiPluginIsDisabled() {
        assertFalse(
            PlaceholderApiIntegrationSupport.isPlaceholderApiSupported(
                this.pluginManagerReturning(this.pluginWithEnabled(false))
            )
        );
    }

    @Test
    void reportsSupportedWhenPlaceholderApiPluginIsEnabled() {
        assertTrue(
            PlaceholderApiIntegrationSupport.isPlaceholderApiSupported(
                this.pluginManagerReturning(this.pluginWithEnabled(true))
            )
        );
    }

    @Test
    void executesPrimaryCommandsWhenAliasesAreAvailable() {
        final RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher(
            Map.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND, true,
                RELOAD_PRIMARY_COMMAND, true
            )
        );

        final PlaceholderApiIntegrationSupport.ExecutionResult result =
            PlaceholderApiIntegrationSupport.installPlayerExpansionAndReload(dispatcher);

        assertTrue(result.expansionDownloadSucceeded());
        assertTrue(result.placeholderApiReloadSucceeded());
        assertTrue(result.success());
        assertEquals(
            List.of(DOWNLOAD_PLAYER_PRIMARY_COMMAND, RELOAD_PRIMARY_COMMAND),
            dispatcher.commands()
        );
    }

    @Test
    void fallsBackToPlaceholderApiAliasWhenPapiAliasFails() {
        final RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher(
            Map.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND, false,
                DOWNLOAD_PLAYER_FALLBACK_COMMAND, true,
                RELOAD_PRIMARY_COMMAND, false,
                RELOAD_FALLBACK_COMMAND, true
            )
        );

        final PlaceholderApiIntegrationSupport.ExecutionResult result =
            PlaceholderApiIntegrationSupport.installPlayerExpansionAndReload(dispatcher);

        assertTrue(result.expansionDownloadSucceeded());
        assertTrue(result.placeholderApiReloadSucceeded());
        assertTrue(result.success());
        assertEquals(
            List.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND,
                DOWNLOAD_PLAYER_FALLBACK_COMMAND,
                RELOAD_PRIMARY_COMMAND,
                RELOAD_FALLBACK_COMMAND
            ),
            dispatcher.commands()
        );
    }

    @Test
    void abortsReloadWhenExpansionDownloadFailsForAllAliases() {
        final RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher(Map.of());

        final PlaceholderApiIntegrationSupport.ExecutionResult result =
            PlaceholderApiIntegrationSupport.installPlayerExpansionAndReload(dispatcher);

        assertFalse(result.expansionDownloadSucceeded());
        assertFalse(result.placeholderApiReloadSucceeded());
        assertFalse(result.success());
        assertEquals(
            List.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND,
                DOWNLOAD_PLAYER_FALLBACK_COMMAND
            ),
            dispatcher.commands()
        );
    }

    @Test
    void marksResultPartialWhenReloadFailsForAllAliases() {
        final RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher(
            Map.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND, true,
                RELOAD_PRIMARY_COMMAND, false,
                RELOAD_FALLBACK_COMMAND, false
            )
        );

        final PlaceholderApiIntegrationSupport.ExecutionResult result =
            PlaceholderApiIntegrationSupport.installPlayerExpansionAndReload(dispatcher);

        assertTrue(result.expansionDownloadSucceeded());
        assertFalse(result.placeholderApiReloadSucceeded());
        assertFalse(result.success());
        assertEquals(
            List.of(
                DOWNLOAD_PLAYER_PRIMARY_COMMAND,
                RELOAD_PRIMARY_COMMAND,
                RELOAD_FALLBACK_COMMAND
            ),
            dispatcher.commands()
        );
    }

    private @NotNull PluginManager pluginManagerReturning(
        final Plugin placeholderApiPlugin
    ) {
        return (PluginManager) Proxy.newProxyInstance(
            PluginManager.class.getClassLoader(),
            new Class<?>[]{PluginManager.class},
            (proxy, method, args) -> {
                if ("getPlugin".equals(method.getName()) && args != null && args.length == 1) {
                    return "PlaceholderAPI".equals(args[0]) ? placeholderApiPlugin : null;
                }
                return this.defaultValue(method.getReturnType());
            }
        );
    }

    private @NotNull Plugin pluginWithEnabled(
        final boolean enabled
    ) {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            (proxy, method, args) -> {
                if ("isEnabled".equals(method.getName())) {
                    return enabled;
                }
                if ("getName".equals(method.getName())) {
                    return "PlaceholderAPI";
                }
                return this.defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(
        final @NotNull Class<?> returnType
    ) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class RecordingCommandDispatcher implements PlaceholderApiIntegrationSupport.CommandDispatcher {

        private final Map<String, Boolean> commandResults = new HashMap<>();
        private final List<String> commands = new ArrayList<>();

        private RecordingCommandDispatcher(
            final @NotNull Map<String, Boolean> commandResults
        ) {
            this.commandResults.putAll(commandResults);
        }

        @Override
        public boolean dispatch(
            final @NotNull String command
        ) {
            this.commands.add(command);
            return this.commandResults.getOrDefault(command, false);
        }

        private @NotNull List<String> commands() {
            return this.commands;
        }
    }
}
