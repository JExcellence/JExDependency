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

package com.raindropcentral.rda;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

/**
 * Loads the shared {@code party.yml} configuration for RDA.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public final class PartyConfigLoader {

    private final JavaPlugin plugin;

    /**
     * Creates a new party configuration loader.
     *
     * @param plugin owning plugin
     */
    public PartyConfigLoader(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads and validates the party configuration from {@code party.yml}.
     *
     * @return resolved party configuration
     */
    public @NotNull PartyConfig load() {
        final File configFile = new File(this.plugin.getDataFolder(), "party.yml");
        return parseConfiguration(YamlConfiguration.loadConfiguration(configFile), configFile.getPath());
    }

    /**
     * Parses a party configuration from YAML.
     *
     * @param configuration parsed YAML configuration
     * @param sourceName source description used in validation errors
     * @return resolved party configuration
     */
    static @NotNull PartyConfig parseConfiguration(
        final @NotNull YamlConfiguration configuration,
        final @NotNull String sourceName
    ) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(sourceName, "sourceName");

        return new PartyConfig(
            configuration.getInt("max-members", 4),
            requireNonNegative(configuration.getLong("invite-timeout-seconds", 300L), "invite-timeout-seconds", sourceName),
            new PartyConfig.XpShareSettings(
                requireNonNegative(configuration.getDouble("xp-share.self", 0.75D), "xp-share.self", sourceName),
                requireNonNegative(
                    configuration.getDouble("xp-share.others-total", 0.25D),
                    "xp-share.others-total",
                    sourceName
                ),
                requireNonNegative(configuration.getDouble("xp-share.range-blocks", 32.0D), "xp-share.range-blocks", sourceName)
            )
        );
    }

    private static long requireNonNegative(
        final long value,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (value < 0L) {
            throw new IllegalStateException("Expected non-negative value at " + path + " in " + sourceName);
        }
        return value;
    }

    private static double requireNonNegative(
        final double value,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (value < 0.0D) {
            throw new IllegalStateException("Expected non-negative value at " + path + " in " + sourceName);
        }
        return value;
    }
}
