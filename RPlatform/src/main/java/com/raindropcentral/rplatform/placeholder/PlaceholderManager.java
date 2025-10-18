package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Manages the reflection-based registration lifecycle for the PlaceholderAPI expansion bridge.
 * The manager validates PlaceholderAPI availability, reflects the {@code PAPIHook} expansion to
 * register placeholders on {@link #register()}, and mirrors the deregistration on
 * {@link #unregister()}. It keeps an internal flag to avoid duplicate registrations while logging
 * both successful registrations and recovery details when dependencies are missing or reflection
 * calls fail.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PlaceholderManager {

    /**
     * Owning plugin used to construct the expansion and emit lifecycle logs.
     */
    private final JavaPlugin plugin;

    /**
     * Placeholder identifier passed to the expansion constructor.
     */
    private final String identifier;

    /**
     * Tracks whether the reflection registration has succeeded.
     */
    private boolean registered;

    /**
     * Creates the manager with the plugin context and expansion identifier.
     *
     * @param plugin     plugin used to access loggers and class loaders.
     * @param identifier placeholder identifier shared with PlaceholderAPI.
     */
    public PlaceholderManager(
            final @NotNull JavaPlugin plugin,
            final @NotNull String identifier
    ) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.registered = false;
    }

    /**
     * Registers the PlaceholderAPI expansion when available and not yet registered. The method
     * aborts when PlaceholderAPI is missing, preventing reflective calls, and logs both successful
     * and failed registration attempts. On success the {@link #registered} flag is set to
     * {@code true}.
     */
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

    /**
     * Unregisters the PlaceholderAPI expansion only when previously registered. Failures during
     * reflection are logged while leaving the {@link #registered} flag unchanged when deregistration
     * cannot be completed.
     */
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

    /**
     * Indicates whether the manager believes the PlaceholderAPI expansion is currently registered.
     *
     * @return {@code true} when registration succeeded and was not undone.
     */
    public boolean isRegistered() {
        return registered;
    }
}
