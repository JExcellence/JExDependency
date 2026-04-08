/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.config;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
     * Creates and loads the RaindropCentral configuration manager.
     *
     * @param plugin plugin whose data folder contains {@code rcentral/rcentral.yml}
     */
    public RCentralConfig(final @NotNull Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        try {
            final File configFolder = new File(this.plugin.getDataFolder(), CONFIG_FOLDER);
            if (!configFolder.exists() && !configFolder.mkdirs()) {
                LOGGER.warning("Could not create RCentral config folder; continuing with defaults if needed.");
            }

            final File configFile = new File(configFolder, CONFIG_FILE);
            if (!configFile.exists()) {
                this.plugin.saveResource(CONFIG_FOLDER + "/" + CONFIG_FILE, false);
            }

            final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            this.rcentralSection = RCentralSection.fromConfiguration(configuration);

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
     * Returns the configured backend URL, or {@code null} when not explicitly set.
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
