package com.raindropcentral.rplatform.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base {@link PlaceholderExpansion} implementation that caches the resolved placeholder
 * identifiers and drives the lifecycle for registering and handling PlaceholderAPI requests.
 * Implementations provide the concrete placeholder keys and the logic to resolve each key for
 * players. Construction builds the placeholder cache so repeated registration cycles reuse the
 * resolved identifiers while {@link #register()} and {@link #unregister()} invocations are
 * delegated to PlaceholderAPI. Subclasses should focus purely on resolution logic while this
 * class coordinates lifecycle wiring with the owning {@link Plugin}.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class AbstractPlaceholderExpansion extends PlaceholderExpansion {

    /**
     * Primary plugin reference used for identifier resolution and metadata lookups.
     */
    private final Plugin plugin;

    /**
     * Cached list of placeholder keys defined by the implementation.
     */
    private final List<String> placeholders;

    /**
     * Creates a new expansion and caches the placeholder definitions to avoid repetitive lookups
     * during registration.
     *
     * @param plugin plugin providing metadata and lifecycle ownership for the expansion.
     */
    protected AbstractPlaceholderExpansion(final @NotNull Plugin plugin) {
        this.plugin = plugin;
        this.placeholders = new ArrayList<>(definePlaceholders());
    }

    /**
     * Defines the placeholder identifiers handled by the expansion.
     *
     * @return placeholder identifiers without identifier prefixes.
     */
    protected abstract @NotNull List<String> definePlaceholders();

    /**
     * Resolves a placeholder for the supplied player context.
     *
     * @param player  player receiving the placeholder value, {@code null} when resolving without
     *                an online player context.
     * @param params  placeholder parameters following the identifier prefix.
     * @return resolved placeholder value or {@code null} when the placeholder cannot be resolved.
     */
    protected abstract @Nullable String resolvePlaceholder(
            final @Nullable Player player,
            final @NotNull String params
    );

    /**
     * Retrieves the placeholder identifier used to register with PlaceholderAPI.
     *
     * @return lowercase plugin name ensuring consistent identifier lookups.
     */
    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase();
    }

    /**
     * Retrieves the author name displayed for the expansion.
     *
     * @return comma separated author list derived from the plugin metadata.
     */
    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    /**
     * Retrieves the version string exposed to PlaceholderAPI.
     *
     * @return plugin version from the plugin metadata.
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /**
     * Builds the list of fully qualified placeholders that PlaceholderAPI should register.
     *
     * @return placeholder identifiers formatted with {@code %identifier_placeholder%}.
     */
    @Override
    public @NotNull List<String> getPlaceholders() {
        return placeholders.stream()
                .map(placeholder -> String.format("%%%s_%s%%", getIdentifier(), placeholder))
                .collect(Collectors.toList());
    }

    /**
     * Indicates the expansion should persist across PlaceholderAPI reloads.
     *
     * @return {@code true} to keep the expansion registered after reload operations.
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Delegates PlaceholderAPI offline player requests to the player-based handler when an online
     * player instance is available.
     *
     * @param offlinePlayer offline player requesting the placeholder.
     * @param params        placeholder parameters following the identifier prefix.
     * @return resolved placeholder value, or {@code null} when no online player is available.
     */
    @Override
    public @Nullable String onRequest(
            final @Nullable OfflinePlayer offlinePlayer,
            final @NotNull String params
    ) {
        if (offlinePlayer != null && offlinePlayer.isOnline()) {
            return onPlaceholderRequest(offlinePlayer.getPlayer(), params);
        }
        return null;
    }

    /**
     * Resolves a placeholder request for an online player by delegating to
     * {@link #resolvePlaceholder(Player, String)}.
     *
     * @param player player context for resolution, may be {@code null} depending on PlaceholderAPI
     *               invocation.
     * @param params placeholder parameters following the identifier prefix.
     * @return resolved placeholder value or {@code null} when the placeholder is unsupported.
     */
    @Override
    public @Nullable String onPlaceholderRequest(
            final @Nullable Player player,
            final @NotNull String params
    ) {
        return resolvePlaceholder(player, params);
    }
}
