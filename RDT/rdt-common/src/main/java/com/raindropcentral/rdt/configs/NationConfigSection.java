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

package com.raindropcentral.rdt.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed configuration snapshot for nation-formation and active nation-progression definitions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationConfigSection {

    private final Map<Integer, LevelDefinition> formationLevels;
    private final Map<Integer, LevelDefinition> progressionLevels;

    private NationConfigSection(
        final @NotNull Map<Integer, LevelDefinition> formationLevels,
        final @NotNull Map<Integer, LevelDefinition> progressionLevels
    ) {
        this.formationLevels = LevelConfigSupport.copyLevels(formationLevels);
        this.progressionLevels = LevelConfigSupport.copyLevels(progressionLevels);
    }

    /**
     * Returns a defensive copy of configured nation-formation definitions.
     *
     * @return configured nation-formation definitions keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getFormationLevels() {
        return LevelConfigSupport.copyLevels(this.formationLevels);
    }

    /**
     * Returns the configured nation-formation definition for one level.
     *
     * @param level target level to resolve
     * @return matching nation-formation definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getFormationLevelDefinition(final int level) {
        return this.formationLevels.get(level);
    }

    /**
     * Returns the highest configured nation-formation level.
     *
     * @return highest configured nation-formation level
     */
    public int getHighestConfiguredFormationLevel() {
        return resolveHighestConfiguredLevel(this.formationLevels);
    }

    /**
     * Returns the next configured nation-formation level after the supplied level.
     *
     * @param currentLevel current level
     * @return next configured level, or {@code null} when already at max
     */
    public @Nullable Integer getNextFormationLevel(final int currentLevel) {
        return resolveNextConfiguredLevel(this.formationLevels, currentLevel);
    }

    /**
     * Returns a defensive copy of configured active nation-progression definitions.
     *
     * @return configured nation-progression definitions keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getProgressionLevels() {
        return LevelConfigSupport.copyLevels(this.progressionLevels);
    }

    /**
     * Returns the configured active nation-progression definition for one level.
     *
     * @param level target level to resolve
     * @return matching nation-progression definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getProgressionLevelDefinition(final int level) {
        return this.progressionLevels.get(level);
    }

    /**
     * Returns the highest configured active nation-progression level.
     *
     * @return highest configured nation-progression level
     */
    public int getHighestConfiguredProgressionLevel() {
        return resolveHighestConfiguredLevel(this.progressionLevels);
    }

    /**
     * Returns the next configured active nation-progression level after the supplied level.
     *
     * @param currentLevel current active nation level
     * @return next configured nation-progression level, or {@code null} when already at max
     */
    public @Nullable Integer getNextProgressionLevel(final int currentLevel) {
        return resolveNextConfiguredLevel(this.progressionLevels, currentLevel);
    }

    /**
     * Parses a nation config section from a YAML file.
     *
     * @param file source config file
     * @return parsed nation config snapshot
     */
    public static @NotNull NationConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a nation config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed nation config snapshot
     */
    public static @NotNull NationConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read nation config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default nation config
     */
    public static @NotNull NationConfigSection createDefault() {
        return fromConfiguration(new YamlConfiguration());
    }

    private static @NotNull NationConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        final Map<Integer, LevelDefinition> defaultFormationLevels = LevelConfigSupport.createDefaultNationFormationLevels();
        final Map<Integer, LevelDefinition> defaultProgressionLevels = LevelConfigSupport.createDefaultNationProgressionLevels();
        final ConfigurationSection formationSection = configuration.getConfigurationSection("formation");
        final ConfigurationSection progressionSection = configuration.getConfigurationSection("progression");
        final ConfigurationSection legacyFormationLevels = formationSection == null
            ? configuration.getConfigurationSection("levels")
            : null;
        return new NationConfigSection(
            LevelConfigSupport.parseLevels(
                formationSection == null ? legacyFormationLevels : formationSection.getConfigurationSection("levels"),
                defaultFormationLevels
            ),
            LevelConfigSupport.parseLevels(
                progressionSection == null ? null : progressionSection.getConfigurationSection("levels"),
                defaultProgressionLevels
            )
        );
    }

    private static int resolveHighestConfiguredLevel(final @NotNull Map<Integer, LevelDefinition> levels) {
        return levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    private static @Nullable Integer resolveNextConfiguredLevel(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final int currentLevel
    ) {
        return levels.keySet().stream()
            .filter(level -> level > currentLevel)
            .sorted()
            .findFirst()
            .orElse(null);
    }
}
