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

public abstract class AbstractPluginDelegate<T extends JavaPlugin> implements PluginDelegate<T> {

    private final T plugin;

    protected AbstractPluginDelegate(@NotNull final T plugin) {
        this.plugin = plugin;
    }

    @Override
    public final @NotNull T getPlugin() {
        return plugin;
    }

    protected @NotNull Logger getLogger() {
        return plugin.getLogger();
    }

    protected @NotNull Server getServer() {
        return plugin.getServer();
    }

    protected @NotNull PluginManager getPluginManager() {
        return getServer().getPluginManager();
    }

    protected @NotNull File getDataFolder() {
        return plugin.getDataFolder();
    }

    protected @NotNull FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    protected void saveConfig() {
        plugin.saveConfig();
    }

    protected void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    protected void reloadConfig() {
        plugin.reloadConfig();
    }

    protected @NotNull PluginDescriptionFile getDescription() {
        return plugin.getDescription();
    }

    protected @NotNull String getName() {
        return getDescription().getName();
    }

    protected @NotNull String getVersion() {
        return getDescription().getVersion();
    }

    protected @Nullable PluginCommand getCommand(@NotNull final String name) {
        return plugin.getCommand(name);
    }
}
