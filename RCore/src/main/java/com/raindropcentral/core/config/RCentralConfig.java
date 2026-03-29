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

import com.raindropcentral.core.service.central.cookie.DropletCookieDefinitions;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration manager for RaindropCentral settings.
 */
public class RCentralConfig {

    private static final Logger LOGGER = Logger.getLogger(RCentralConfig.class.getName());
    private static final String CONFIG_FOLDER = "rcentral";
    private static final String CONFIG_FILE = "rcentral.yml";
    private static final List<String> SUPPORTED_DROPLET_STORE_ITEM_CODES = DropletCookieDefinitions.allItemCodes();

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
        final DropletStoreCompatibilitySnapshot compatibilitySnapshot = this.getDropletStoreCompatibilitySnapshot();
        LOGGER.info(String.format("RCentral Config - URL: %s, DevMode: %s, AutoDetect: %s, DropletsStore: %s, EnabledDropletItems: %s",
                getBackendUrl() != null ? getBackendUrl() : "auto",
                isDevelopmentMode(),
                isAutoDetect(),
                compatibilitySnapshot.dropletStoreEnabled(),
                compatibilitySnapshot.enabledItemCodes().isEmpty()
                        ? "none"
                        : String.join(",", compatibilitySnapshot.enabledItemCodes())));
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
     * Checks whether the droplet-store claim command is enabled.
     */
    public boolean isDropletsStoreEnabled() {
        return this.rcentralSection.isDropletsStoreEnabled();
    }

    /**
     * Checks whether one supported droplet-store reward is enabled.
     *
     * @param itemCode backend item code
     * @return {@code true} when claiming is allowed for that reward
     */
    public boolean isDropletStoreRewardEnabled(final @NotNull String itemCode) {
        return this.rcentralSection.isDropletStoreRewardEnabled(itemCode);
    }

    /**
     * Returns the effective droplet-store compatibility snapshot for health reporting.
     *
     * @return master droplet-store state plus enabled supported item codes
     */
    public @NotNull DropletStoreCompatibilitySnapshot getDropletStoreCompatibilitySnapshot() {
        return resolveDropletStoreCompatibilitySnapshot(this.rcentralSection);
    }

    static @NotNull DropletStoreCompatibilitySnapshot resolveDropletStoreCompatibilitySnapshot(
            final @NotNull RCentralSection section
    ) {
        if (!section.isDropletsStoreEnabled()) {
            return new DropletStoreCompatibilitySnapshot(false, List.of());
        }

        final List<String> enabledItemCodes = SUPPORTED_DROPLET_STORE_ITEM_CODES.stream()
                .filter(section::isDropletStoreRewardEnabled)
                .toList();
        return new DropletStoreCompatibilitySnapshot(true, enabledItemCodes);
    }

    public record DropletStoreCompatibilitySnapshot(
            boolean dropletStoreEnabled,
            @NotNull List<String> enabledItemCodes
    ) {
        public DropletStoreCompatibilitySnapshot {
            enabledItemCodes = List.copyOf(enabledItemCodes);
        }
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        loadConfig();
    }
}
