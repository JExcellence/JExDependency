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

import org.bukkit.Material;
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
 * Parsed configuration snapshot for Outpost chunk level progression.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class OutpostConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.LODESTONE;

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;

    private OutpostConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
    }

    /**
     * Returns a defensive copy of configured Outpost levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Outpost level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for Outpost chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the highest configured Outpost level.
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
     * Parses an Outpost config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull OutpostConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses an Outpost config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull OutpostConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read outpost config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Outpost config
     */
    public static @NotNull OutpostConfigSection createDefault() {
        return new OutpostConfigSection(LevelConfigSupport.createDefaultOutpostLevels(), DEFAULT_BLOCK_MATERIAL);
    }

    private static @NotNull OutpostConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new OutpostConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultOutpostLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            )
        );
    }
}
