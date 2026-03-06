package com.raindropcentral.rdr.view;

import java.util.Objects;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility helpers for PlaceholderAPI support detection and command execution.
 *
 * <p>This helper centralizes the command flow used by the PlaceholderAPI admin view so command
 * order and fallback aliases can be unit tested without Bukkit runtime dependencies.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
final class PlaceholderApiIntegrationSupport {

    static final String PLAYER_EXPANSION_NAME = "Player";

    private static final String PLACEHOLDER_API_PLUGIN_NAME = "PlaceholderAPI";
    private static final String DOWNLOAD_PLAYER_PRIMARY_COMMAND = "papi ecloud download " + PLAYER_EXPANSION_NAME;
    private static final String DOWNLOAD_PLAYER_FALLBACK_COMMAND = "placeholderapi ecloud download "
        + PLAYER_EXPANSION_NAME;
    private static final String RELOAD_PRIMARY_COMMAND = "papi reload";
    private static final String RELOAD_FALLBACK_COMMAND = "placeholderapi reload";

    private PlaceholderApiIntegrationSupport() {
    }

    /**
     * Determines whether PlaceholderAPI is present and enabled on the server.
     *
     * @param pluginManager Bukkit plugin manager
     * @return {@code true} when PlaceholderAPI is available for command execution
     */
    static boolean isPlaceholderApiSupported(
        final @Nullable PluginManager pluginManager
    ) {
        if (pluginManager == null) {
            return false;
        }

        final Plugin placeholderApiPlugin = pluginManager.getPlugin(PLACEHOLDER_API_PLUGIN_NAME);
        return placeholderApiPlugin != null && placeholderApiPlugin.isEnabled();
    }

    /**
     * Runs PlaceholderAPI commands to install the {@code Player} expansion and reload PlaceholderAPI.
     *
     * <p>The helper first attempts short aliases ({@code papi ...}) and then falls back to
     * long aliases ({@code placeholderapi ...}) when needed.</p>
     *
     * @param commandDispatcher command dispatcher bound to the active server runtime
     * @return execution result containing command success flags
     * @throws NullPointerException if {@code commandDispatcher} is {@code null}
     */
    static @NotNull ExecutionResult installPlayerExpansionAndReload(
        final @NotNull CommandDispatcher commandDispatcher
    ) {
        Objects.requireNonNull(commandDispatcher, "commandDispatcher");

        final boolean downloadSucceeded = dispatchWithFallback(
            commandDispatcher,
            DOWNLOAD_PLAYER_PRIMARY_COMMAND,
            DOWNLOAD_PLAYER_FALLBACK_COMMAND
        );
        if (!downloadSucceeded) {
            return new ExecutionResult(false, false);
        }

        final boolean reloadSucceeded = dispatchWithFallback(
            commandDispatcher,
            RELOAD_PRIMARY_COMMAND,
            RELOAD_FALLBACK_COMMAND
        );
        return new ExecutionResult(true, reloadSucceeded);
    }

    private static boolean dispatchWithFallback(
        final @NotNull CommandDispatcher commandDispatcher,
        final @NotNull String primaryCommand,
        final @NotNull String fallbackCommand
    ) {
        if (dispatchSafely(commandDispatcher, primaryCommand)) {
            return true;
        }
        return dispatchSafely(commandDispatcher, fallbackCommand);
    }

    private static boolean dispatchSafely(
        final @NotNull CommandDispatcher commandDispatcher,
        final @NotNull String command
    ) {
        try {
            return commandDispatcher.dispatch(command);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Minimal command dispatch contract for PlaceholderAPI admin actions.
     */
    @FunctionalInterface
    interface CommandDispatcher {

        /**
         * Dispatches a command as the console sender.
         *
         * @param command raw command string without leading slash
         * @return {@code true} when the command was accepted for execution
         */
        boolean dispatch(
            @NotNull String command
        );
    }

    /**
     * Result of the PlaceholderAPI install and reload command flow.
     *
     * @param expansionDownloadSucceeded whether expansion download command completed successfully
     * @param placeholderApiReloadSucceeded whether reload command completed successfully
     */
    record ExecutionResult(
        boolean expansionDownloadSucceeded,
        boolean placeholderApiReloadSucceeded
    ) {

        /**
         * Indicates whether both expansion download and reload commands succeeded.
         *
         * @return {@code true} when integration setup completed fully
         */
        boolean success() {
            return this.expansionDownloadSucceeded && this.placeholderApiReloadSucceeded;
        }
    }
}
