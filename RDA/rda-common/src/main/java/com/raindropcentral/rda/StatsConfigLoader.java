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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Loads the shared {@code stats.yml} configuration for RDA.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class StatsConfigLoader {

    private final JavaPlugin plugin;

    /**
     * Creates a new stats configuration loader.
     *
     * @param plugin owning plugin
     */
    public StatsConfigLoader(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads and validates the stats configuration from {@code stats.yml}.
     *
     * @return resolved stats configuration
     */
    public @NotNull StatsConfig load() {
        final File configFile = new File(this.plugin.getDataFolder(), "stats.yml");
        return parseConfiguration(YamlConfiguration.loadConfiguration(configFile), configFile.getPath());
    }

    /**
     * Parses a stats configuration from YAML.
     *
     * @param configuration parsed YAML configuration
     * @param sourceName source description used in validation errors
     * @return resolved stats configuration
     */
    static @NotNull StatsConfig parseConfiguration(
        final @NotNull YamlConfiguration configuration,
        final @NotNull String sourceName
    ) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(sourceName, "sourceName");

        final int defaultInterval = requirePositive(
            configuration.getInt("ability-points.default-interval", 5),
            "ability-points.default-interval",
            sourceName
        );

        final EnumMap<SkillType, Integer> perSkillIntervals = new EnumMap<>(SkillType.class);
        final ConfigurationSection intervalSection = configuration.getConfigurationSection("ability-points.skill-overrides");
        if (intervalSection != null) {
            for (final String key : intervalSection.getKeys(false)) {
                final SkillType skillType = resolveEnum(SkillType.class, key, "ability-points.skill-overrides." + key, sourceName);
                perSkillIntervals.put(
                    skillType,
                    requirePositive(
                        intervalSection.getInt(key),
                        "ability-points.skill-overrides." + key,
                        sourceName
                    )
                );
            }
        }

        final EnumMap<CoreStatType, StatsConfig.StatDefinition> statDefinitions = new EnumMap<>(CoreStatType.class);
        for (final CoreStatType coreStatType : CoreStatType.values()) {
            final String statPath = "stats." + coreStatType.getId();
            final ConfigurationSection statSection = configuration.getConfigurationSection(statPath);
            if (statSection == null) {
                throw new IllegalStateException("Missing stat section at " + statPath + " in " + sourceName);
            }

            statDefinitions.put(coreStatType, new StatsConfig.StatDefinition(
                coreStatType,
                resolveMaterial(
                    statSection.getString("icon", coreStatType.getFallbackIcon().name()),
                    statPath + ".icon",
                    sourceName
                ),
                statSection.getString("description", ""),
                statSection.getString("description-key"),
                statSection.getString("passive.label", coreStatType.getId().toUpperCase(Locale.ROOT)),
                statSection.getString("passive.unit", ""),
                new StatsConfig.LinearSoftCapFormula(
                    statSection.getDouble("passive.base", 0.0D),
                    statSection.getDouble("passive.full-rate", 1.0D),
                    statSection.getDouble("passive.half-rate", 0.5D),
                    statSection.getDouble("passive.quarter-rate", 0.25D),
                    requireNonNegative(statSection.getInt("passive.first-soft-cap", 20), statPath + ".passive.first-soft-cap", sourceName),
                    requireNonNegative(statSection.getInt("passive.second-soft-cap", 40), statPath + ".passive.second-soft-cap", sourceName),
                    statSection.contains("passive.max") ? statSection.getDouble("passive.max") : null
                )
            ));
        }

        final Set<ActivationMode> allowedModes = EnumSet.noneOf(ActivationMode.class);
        final List<String> rawActivationModes = configuration.getStringList("activation.allowed-modes");
        if (rawActivationModes.isEmpty()) {
            allowedModes.addAll(EnumSet.allOf(ActivationMode.class));
        } else {
            for (final String rawActivationMode : rawActivationModes) {
                allowedModes.add(resolveEnum(
                    ActivationMode.class,
                    rawActivationMode,
                    "activation.allowed-modes",
                    sourceName
                ));
            }
        }

        final StatsConfig.RespecSettings respecSettings = new StatsConfig.RespecSettings(
            requireNonNegative(configuration.getLong("respec.cooldown-seconds", 0L), "respec.cooldown-seconds", sourceName),
            requireNonNegative(configuration.getInt("respec.point-tax-percent", 0), "respec.point-tax-percent", sourceName)
        );

        final StatsConfig.ManaSettings manaSettings = new StatsConfig.ManaSettings(
            configuration.getDouble("mana.base", 100.0D),
            configuration.getDouble("mana.per-spi-point", 5.0D),
            configuration.getDouble("mana.base-regen-per-second", 1.0D),
            configuration.getDouble("mana.regen-per-spi-point", 0.3D),
            resolveEnum(
                ManaDisplayMode.class,
                configuration.getString("mana.default-display-mode", ManaDisplayMode.ACTION_BAR.name()),
                "mana.default-display-mode",
                sourceName
            ),
            configuration.getDouble("mana.low-threshold-percent", 20.0D)
        );

        return new StatsConfig(
            defaultInterval,
            perSkillIntervals,
            statDefinitions,
            allowedModes,
            respecSettings,
            manaSettings
        );
    }

    private static int requirePositive(
        final int value,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (value <= 0) {
            throw new IllegalStateException("Expected positive value at " + path + " in " + sourceName);
        }
        return value;
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

    private static int requireNonNegative(
        final int value,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (value < 0) {
            throw new IllegalStateException("Expected non-negative value at " + path + " in " + sourceName);
        }
        return value;
    }

    private static @NotNull Material resolveMaterial(
        final @Nullable String rawValue,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalStateException("Missing material at " + path + " in " + sourceName);
        }

        try {
            return Material.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid material '" + rawValue + "' at " + path + " in " + sourceName,
                exception
            );
        }
    }

    private static <E extends Enum<E>> @NotNull E resolveEnum(
        final @NotNull Class<E> enumClass,
        final @Nullable String rawValue,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalStateException("Missing value at " + path + " in " + sourceName);
        }

        try {
            return Enum.valueOf(enumClass, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid value '" + rawValue + "' at " + path + " in " + sourceName,
                exception
            );
        }
    }
}
