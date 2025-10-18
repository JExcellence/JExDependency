package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PlaceholderRegistry {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderRegistry.class.getName());

    private final Plugin plugin;
    private final AbstractPlaceholderExpansion expansion;
    private final boolean papiAvailable;

    public PlaceholderRegistry(
            final @NotNull Plugin plugin,
            final @NotNull AbstractPlaceholderExpansion expansion
    ) {
        this.plugin = plugin;
        this.expansion = expansion;
        this.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void register() {
        if (!papiAvailable) {
            LOGGER.warning("PlaceholderAPI not found - placeholders will not be available");
            return;
        }

        expansion.register();
        LOGGER.info("Registered PlaceholderAPI expansion for " + plugin.getName());
    }

    public void unregister() {
        if (papiAvailable && expansion.isRegistered()) {
            expansion.unregister();
            LOGGER.info("Unregistered PlaceholderAPI expansion for " + plugin.getName());
        }
    }

    public boolean isAvailable() {
        return papiAvailable;
    }

    public boolean isRegistered() {
        return papiAvailable && expansion.isRegistered();
    }
}
