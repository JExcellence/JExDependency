package de.jexcellence.dependency.delegate;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public interface PluginDelegate<T extends JavaPlugin> {

    @NotNull T getPlugin();

    void onLoad();

    void onEnable();

    void onDisable();
}
