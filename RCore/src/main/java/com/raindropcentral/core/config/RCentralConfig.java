package com.raindropcentral.core.config;

import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration manager for RaindropCentral settings.
 */
public class RCentralConfig {

    private static final Logger LOGGER = Logger.getLogger(RCentralConfig.class.getName());
    private static final String CONFIG_FOLDER = "rcentral";
    private static final String CONFIG_FILE = "rcentral.yml";

    private final Plugin plugin;
    private RCentralSection rcentralSection;

    /**
     * Executes RCentralConfig.
     */
    public RCentralConfig(@NotNull Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        try {
            var cfgManager = new ConfigManager(plugin, CONFIG_FOLDER);
            var cfgKeeper = new ConfigKeeper<>(cfgManager, CONFIG_FILE, RCentralSection.class);
            this.rcentralSection = cfgKeeper.rootSection;

            LOGGER.info("RCentral config loaded successfully");
            logConfig();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading RCentral config, using defaults", e);
            this.rcentralSection = new RCentralSection(new EvaluationEnvironmentBuilder());
        }
    }

    private void logConfig() {
        LOGGER.info(String.format("RCentral Config - URL: %s, DevMode: %s, AutoDetect: %s",
                getBackendUrl() != null ? getBackendUrl() : "auto",
                isDevelopmentMode(),
                isAutoDetect()));
    }

    /**
     * Gets the configured backend URL, or null if not set.
     */
    @Nullable
    public String getBackendUrl() {
        return rcentralSection.getBackendUrl();
    }

    /**
     * Checks if development mode is explicitly enabled.
     */
    public boolean isDevelopmentMode() {
        return rcentralSection.isDevelopmentMode();
    }

    /**
     * Checks if auto-detection should be used.
     */
    public boolean isAutoDetect() {
        return rcentralSection.isAutoDetect();
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        loadConfig();
    }
}
