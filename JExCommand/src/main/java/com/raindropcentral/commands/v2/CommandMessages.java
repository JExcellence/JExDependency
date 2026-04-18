package com.raindropcentral.commands.v2;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Service Provider Interface for command-related messaging.
 *
 * <p>JExCommand 2.0 is deliberately decoupled from any specific i18n framework.
 * Plugins supply an implementation of this interface so the dispatcher can surface
 * parse errors, permission denials, and usage output without knowing anything about
 * the plugin's translation layer.
 *
 * <p>A typical implementation delegates to the plugin's own translator (e.g.
 * {@code R18nManager}, {@code JExTranslate}, or direct {@code MiniMessage}).
 *
 * <p><b>Built-in error keys.</b> The framework emits the following keys — plugins
 * should supply translations for each. Placeholders are passed through the
 * {@code placeholders} map.
 *
 * <table>
 *   <caption>Framework-emitted message keys</caption>
 *   <tr><th>Key</th><th>Trigger</th><th>Placeholders</th></tr>
 *   <tr><td>{@code jexcommand.error.missing-argument}</td>
 *       <td>Required argument absent</td><td>{@code name}</td></tr>
 *   <tr><td>{@code jexcommand.error.too-many-arguments}</td>
 *       <td>Extra tokens after schema exhausted</td><td>{@code value}</td></tr>
 *   <tr><td>{@code jexcommand.error.unknown-subcommand}</td>
 *       <td>No subcommand matched</td><td>{@code value}, {@code options}</td></tr>
 *   <tr><td>{@code jexcommand.error.no-permission}</td>
 *       <td>Permission check failed</td><td>{@code permission}</td></tr>
 *   <tr><td>{@code jexcommand.error.not-a-player}</td>
 *       <td>Player-only command invoked from console</td><td>—</td></tr>
 *   <tr><td>{@code jexcommand.error.not-a-console}</td>
 *       <td>Console-only command invoked by player</td><td>—</td></tr>
 *   <tr><td>{@code jexcommand.error.unknown-type}</td>
 *       <td>YAML references an unregistered type id</td><td>{@code type}</td></tr>
 *   <tr><td>{@code jexcommand.usage}</td>
 *       <td>Usage hint after a parse error</td><td>{@code usage}</td></tr>
 * </table>
 *
 * <p>Argument types additionally emit their own keys (see
 * {@link com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry}).
 *
 * @author JExcellence
 * @since 2.0.0
 */
public interface CommandMessages {

    /**
     * Sends a message to the given sender, identified by an i18n key with optional
     * placeholders.
     *
     * @param sender       the recipient
     * @param key          the i18n key (never {@code null})
     * @param placeholders placeholder map (may be empty, never {@code null})
     */
    void send(@NotNull CommandSender sender, @NotNull String key,
              @NotNull Map<String, String> placeholders);

    /** Convenience helper — no placeholders. */
    default void send(@NotNull CommandSender sender, @NotNull String key) {
        send(sender, key, Map.of());
    }

    /**
     * Returns a no-op implementation that silently swallows every message. Useful
     * for tests or for commands that handle their own error output.
     */
    static @NotNull CommandMessages noop() {
        return (sender, key, placeholders) -> {};
    }

    /**
     * Returns a fallback implementation that echoes the raw key and placeholders
     * back to the sender. Useful for bootstrapping before a real translator is
     * wired up.
     */
    static @NotNull CommandMessages debug() {
        return (sender, key, placeholders) -> {
            var sb = new StringBuilder(key);
            if (!placeholders.isEmpty()) {
                sb.append(' ').append(placeholders);
            }
            sender.sendMessage(sb.toString());
        };
    }
}
