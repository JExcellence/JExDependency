package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager {

    private final JavaPlugin plugin;
    private final String identifier;
    private boolean registered;

    public PlaceholderManager(
            final @NotNull JavaPlugin plugin,
            final @NotNull String identifier
    ) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.registered = false;
    }

    public void register() {
        if (registered) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI not found, skipping placeholder registration");
            return;
        }

        try {
            final Class<?> expansionClass = Class.forName("com.raindropcentral.rplatform.placeholder.PAPIHook");
            final Object expansion = expansionClass.getConstructor(JavaPlugin.class, String.class)
                    .newInstance(plugin, identifier);
            
            expansionClass.getMethod("register").invoke(expansion);
            registered = true;
            plugin.getLogger().info("PlaceholderAPI expansion registered: " + identifier);
        } catch (final Exception e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    public void unregister() {
        if (!registered) {
            return;
        }

        try {
            final Class<?> expansionClass = Class.forName("com.raindropcentral.rplatform.placeholder.PAPIHook");
            final Object expansion = expansionClass.getConstructor(JavaPlugin.class, String.class)
                    .newInstance(plugin, identifier);
            
            expansionClass.getMethod("unregister").invoke(expansion);
            registered = false;
        } catch (final Exception e) {
            plugin.getLogger().warning("Failed to unregister PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    public boolean isRegistered() {
        return registered;
    }
}
