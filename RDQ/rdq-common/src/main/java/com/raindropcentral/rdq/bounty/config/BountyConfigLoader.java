package com.raindropcentral.rdq.bounty.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads bounty configuration from the bounty/bounty.yml file.
 * 
 * <p>This loader handles:
 * <ul>
 *   <li>Loading configuration from the plugin's data folder</li>
 *   <li>Saving default configuration if not present</li>
 *   <li>Hot-reloading configuration</li>
 * </ul>
 */
public final class BountyConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(BountyConfigLoader.class.getName());
    private static final String BOUNTY_FOLDER = "bounty";
    private static final String BOUNTY_FILE = "bounty.yml";

    private final Plugin plugin;
    private final File configFile;
    private BountyConfig config;

    /**
     * Creates a new BountyConfigLoader.
     *
     * @param plugin the plugin instance
     */
    public BountyConfigLoader(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), BOUNTY_FOLDER + File.separator + BOUNTY_FILE);
    }

    /**
     * Loads the bounty configuration from file.
     * If the file doesn't exist, saves the default configuration first.
     *
     * @return the loaded BountyConfig
     */
    @NotNull
    public BountyConfig loadConfig() {
        saveDefaultConfig();

        if (!configFile.exists()) {
            LOGGER.info("Bounty config not found, using defaults: " + configFile.getAbsolutePath());
            config = BountyConfig.defaults();
            return config;
        }

        try {
            var yaml = YamlConfiguration.loadConfiguration(configFile);
            config = BountyConfig.fromConfig(yaml);
            LOGGER.info("Loaded bounty configuration from " + configFile.getAbsolutePath());
            return config;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load bounty config, using defaults", e);
            config = BountyConfig.defaults();
            return config;
        }
    }

    /**
     * Reloads the bounty configuration from file.
     *
     * @return the reloaded BountyConfig
     */
    @NotNull
    public BountyConfig reload() {
        LOGGER.info("Reloading bounty configuration...");
        return loadConfig();
    }

    /**
     * Gets the current bounty configuration.
     * If not loaded yet, loads it first.
     *
     * @return the current BountyConfig
     */
    @NotNull
    public BountyConfig getConfig() {
        if (config == null) {
            return loadConfig();
        }
        return config;
    }

    /**
     * Saves the default configuration file if it doesn't exist.
     */
    private void saveDefaultConfig() {
        if (configFile.exists()) {
            return;
        }

        // Create parent directories
        var parentDir = configFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            LOGGER.warning("Failed to create bounty config directory: " + parentDir.getAbsolutePath());
            return;
        }

        // Copy default config from resources
        var resourcePath = BOUNTY_FOLDER + "/" + BOUNTY_FILE;
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                LOGGER.warning("Default bounty config not found in resources: " + resourcePath);
                return;
            }

            // Load from resource and save to file
            var defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            defaultConfig.save(configFile);
            LOGGER.info("Saved default bounty configuration to " + configFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save default bounty config", e);
        }
    }
}
