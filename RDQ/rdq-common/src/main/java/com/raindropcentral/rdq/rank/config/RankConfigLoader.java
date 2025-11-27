package com.raindropcentral.rdq.rank.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(RankConfigLoader.class.getName());

    private final File configFile;
    private RankSystemConfig config;

    public RankConfigLoader(@NotNull File configFile) {
        this.configFile = configFile;
        this.config = RankSystemConfig.defaults();
    }

    @NotNull
    public RankSystemConfig load() {
        if (!configFile.exists()) {
            LOGGER.info("Rank system config not found, using defaults: " + configFile.getAbsolutePath());
            return config;
        }

        try {
            var yaml = YamlConfiguration.loadConfiguration(configFile);

            var notificationSection = yaml.getConfigurationSection("notifications");
            var notifications = notificationSection != null
                ? new RankSystemConfig.NotificationConfig(
                    notificationSection.getBoolean("titleEnabled", true),
                    notificationSection.getBoolean("subtitleEnabled", true),
                    notificationSection.getBoolean("actionbarEnabled", false),
                    notificationSection.getBoolean("soundEnabled", true),
                    notificationSection.getString("unlockSound", "ENTITY_PLAYER_LEVELUP"),
                    notificationSection.getBoolean("broadcastEnabled", true)
                )
                : RankSystemConfig.NotificationConfig.defaults();

            config = new RankSystemConfig(
                yaml.getBoolean("enabled", true),
                yaml.getBoolean("linearProgression", true),
                yaml.getBoolean("allowSkipping", false),
                yaml.getInt("maxActiveTrees", 1),
                yaml.getBoolean("crossTreeSwitching", false),
                yaml.getLong("switchingCooldownSeconds", 1728000),
                notifications
            );

            LOGGER.info("Loaded rank system configuration");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load rank system config, using defaults", e);
            config = RankSystemConfig.defaults();
        }

        return config;
    }

    @NotNull
    public RankSystemConfig reload() {
        return load();
    }

    @NotNull
    public RankSystemConfig getConfig() {
        return config;
    }
}
