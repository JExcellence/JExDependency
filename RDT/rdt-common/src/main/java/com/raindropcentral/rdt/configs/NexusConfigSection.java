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
 * Parsed configuration snapshot for Nexus level progression.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NexusConfigSection {

    private final Map<Integer, LevelDefinition> levels;
    private final Map<Integer, NexusCombatStats> combatStats;

    private NexusConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Map<Integer, NexusCombatStats> combatStats
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.combatStats = LevelConfigSupport.copyNexusCombatStats(combatStats);
    }

    /**
     * Returns a defensive copy of configured Nexus levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns a defensive copy of configured Nexus combat stats.
     *
     * @return configured combat stats keyed by Nexus level
     */
    public @NotNull Map<Integer, NexusCombatStats> getCombatStats() {
        return LevelConfigSupport.copyNexusCombatStats(this.combatStats);
    }

    /**
     * Returns the configured Nexus level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured Nexus combat stats for one level.
     *
     * @param level level to resolve
     * @return matching combat stats, or bundled defaults for that level when absent
     */
    public @NotNull NexusCombatStats getCombatStats(final int level) {
        final int normalizedLevel = Math.max(1, level);
        return this.combatStats.getOrDefault(normalizedLevel, NexusCombatStats.createDefault(normalizedLevel));
    }

    /**
     * Returns the highest configured Nexus level.
     *
     * @return highest configured level
     */
    public int getHighestConfiguredLevel() {
        return this.levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /**
     * Returns the next configured level after the supplied current level.
     *
     * @param currentLevel current level
     * @return next configured level, or {@code null} when already at max
     */
    public @Nullable Integer getNextLevel(final int currentLevel) {
        return this.levels.keySet().stream()
            .filter(level -> level > currentLevel)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    /**
     * Parses a Nexus config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull NexusConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a Nexus config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull NexusConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read nexus config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Nexus config
     */
    public static @NotNull NexusConfigSection createDefault() {
        return new NexusConfigSection(
            LevelConfigSupport.createDefaultNexusLevels(),
            LevelConfigSupport.createDefaultNexusCombatStats()
        );
    }

    private static @NotNull NexusConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        final Map<Integer, LevelDefinition> levels = LevelConfigSupport.parseLevels(
            configuration.getConfigurationSection("levels"),
            LevelConfigSupport.createDefaultNexusLevels()
        );
        return new NexusConfigSection(
            levels,
            LevelConfigSupport.parseNexusCombatStats(configuration.getConfigurationSection("levels"), levels.keySet())
        );
    }
}
