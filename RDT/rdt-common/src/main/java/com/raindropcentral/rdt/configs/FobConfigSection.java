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
 * Parsed configuration snapshot for FOB chunk level progression.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class FobConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.TARGET;

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;

    private FobConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
    }

    /**
     * Returns a defensive copy of configured FOB levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured FOB level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for FOB chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the highest configured FOB level.
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
     * Parses a FOB config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull FobConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a FOB config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull FobConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read FOB config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default FOB config
     */
    public static @NotNull FobConfigSection createDefault() {
        return new FobConfigSection(LevelConfigSupport.createDefaultFobLevels(), DEFAULT_BLOCK_MATERIAL);
    }

    private static @NotNull FobConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new FobConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultFobLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            )
        );
    }
}
