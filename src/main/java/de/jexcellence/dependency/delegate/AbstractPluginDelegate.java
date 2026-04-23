package de.jexcellence.dependency.delegate;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.logging.Logger;

/**
 * Convenience base implementation of {@link PluginDelegate} that forwards common {@link JavaPlugin} helpers to.
 * subclasses. Delegates gain access to configuration, logging and command registration utilities without repeatedly
 * dereferencing the underlying plugin.
 *
 * @param <T> concrete plugin type being delegated
 */
public abstract class AbstractPluginDelegate<T extends JavaPlugin> implements PluginDelegate<T> {

    private final T plugin;

    /**
     * Creates a new delegate bound to the provided plugin.
     *
     * @param plugin plugin instance the delegate will act upon
     */
    protected AbstractPluginDelegate(@NotNull final T plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the plugin backing this delegate.
     *
     * @return underlying {@link JavaPlugin}
     */
    @Override
    public final @NotNull T getPlugin() {
        return plugin;
    }

    /**
     * Provides convenient access to the plugin's logger for subclasses.
     *
     * @return plugin logger
     */
    protected @NotNull Logger getLogger() {
        return plugin.getLogger();
    }

    /**
     * Exposes the Bukkit {@link Server} associated with the plugin.
     *
     * @return Bukkit server instance
     */
    protected @NotNull Server getServer() {
        return plugin.getServer();
    }

    /**
     * Returns the {@link PluginManager} for event registration and plugin discovery.
     *
     * @return server plugin manager
     */
    protected @NotNull PluginManager getPluginManager() {
        return getServer().getPluginManager();
    }

    /**
     * Resolves the plugin's data folder for file persistence.
     *
     * @return plugin data directory
     */
    protected @NotNull File getDataFolder() {
        return plugin.getDataFolder();
    }

    /**
     * Provides access to the plugin's configuration wrapper.
     *
     * @return configuration handle
     */
    protected @NotNull FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Persists the current configuration state to disk.
     */
    protected void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Saves the default configuration if it has not been generated yet.
     */
    protected void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    /**
     * Reloads the plugin configuration from disk.
     */
    protected void reloadConfig() {
        plugin.reloadConfig();
    }

    /**
     * Returns the plugin's description metadata.
     *
     * @return plugin description
     */
    protected @NotNull PluginDescriptionFile getDescription() {
        return plugin.getDescription();
    }

    /**
     * Convenience wrapper returning the plugin's name from the description.
     *
     * @return plugin name
     */
    protected @NotNull String getName() {
        return getDescription().getName();
    }

    /**
     * Convenience wrapper returning the plugin's version from the description.
     *
     * @return plugin version string
     */
    protected @NotNull String getVersion() {
        return getDescription().getVersion();
    }

    /**
     * Looks up a registered {@link PluginCommand} by name.
     *
     * @param name command name
     *
     * @return command instance or {@code null} when not registered
     */
    protected @Nullable PluginCommand getCommand(@NotNull final String name) {
        return plugin.getCommand(name);
    }
}
