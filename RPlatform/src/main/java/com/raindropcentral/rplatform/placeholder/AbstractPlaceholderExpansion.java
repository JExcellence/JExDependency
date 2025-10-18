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

public abstract class AbstractPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final List<String> placeholders;

    protected AbstractPlaceholderExpansion(final @NotNull Plugin plugin) {
        this.plugin = plugin;
        this.placeholders = new ArrayList<>(definePlaceholders());
    }

    protected abstract @NotNull List<String> definePlaceholders();

    protected abstract @Nullable String resolvePlaceholder(
            final @Nullable Player player,
            final @NotNull String params
    );

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return placeholders.stream()
                .map(placeholder -> String.format("%%%s_%s%%", getIdentifier(), placeholder))
                .collect(Collectors.toList());
    }

    @Override
    public boolean persist() {
        return true;
    }

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

    @Override
    public @Nullable String onPlaceholderRequest(
            final @Nullable Player player,
            final @NotNull String params
    ) {
        return resolvePlaceholder(player, params);
    }
}
