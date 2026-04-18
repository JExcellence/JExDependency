package de.jexcellence.dependency.delegate;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * High-level abstraction that mirrors the {@link JavaPlugin} lifecycle while allowing delegation of behaviour to.
 * alternate classes. Implementations can offload startup, shutdown and configuration logic without extending
 * {@link JavaPlugin} directly.
 *
 * @param <T> concrete plugin type being delegated
 */
public interface PluginDelegate<T extends JavaPlugin> {

    /**
     * Returns the plugin instance backing this delegate.
     *
     * @return owning {@link JavaPlugin}
     */
    @NotNull T getPlugin();

    /**
     * Called during {@link JavaPlugin#onLoad()} to allow delegates to perform pre-enable setup.
     */
    void onLoad();

    /**
     * Called during {@link JavaPlugin#onEnable()} once dependencies have been initialised.
     */
    void onEnable();

    /**
     * Called during {@link JavaPlugin#onDisable()} for cleanup logic.
     */
    void onDisable();
}
