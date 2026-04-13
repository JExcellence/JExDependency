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

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;

/**
 * Loads the module-hub config from {@code plugins/RCore/configs/config.yml}.
 */
public final class RCoreMainMenuConfigLoader {

    private static final String CONFIG_FOLDER = "configs";
    private static final String CONFIG_FILE = "config.yml";

    private RCoreMainMenuConfigLoader() {
    }

    /**
     * Loads the module-hub configuration from disk, creating the bundled default file when needed.
     *
     * @param plugin owning plugin used for file resolution and bundled-resource extraction
     * @return loaded module-hub configuration, or defaults when loading fails
     */
    public static @NotNull RCoreMainMenuConfig load(final @NotNull JavaPlugin plugin) {
        try {
            final File configFolder = new File(plugin.getDataFolder(), CONFIG_FOLDER);
            if (!configFolder.exists() && !configFolder.mkdirs()) {
                plugin.getLogger().warning("Could not create configs folder; using defaults if needed.");
            }

            final File configFile = new File(configFolder, CONFIG_FILE);
            if (!configFile.exists()) {
                plugin.saveResource(CONFIG_FOLDER + "/" + CONFIG_FILE, false);
            }

            final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            return RCoreMainMenuConfig.fromConfiguration(configuration);
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load /rc main config; using defaults.", exception);
            return RCoreMainMenuConfig.defaults();
        }
    }
}
