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
 * Parsed configuration snapshot for Armory chunk level progression and runtime perks.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ArmoryConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.IRON_BLOCK;
    private static final boolean DEFAULT_FREE_REPAIR_ENABLED = true;
    private static final int DEFAULT_FREE_REPAIR_UNLOCK_LEVEL = 2;
    private static final int DEFAULT_FREE_REPAIR_COOLDOWN_SECONDS = 86_400;
    private static final boolean DEFAULT_SALVAGE_BLOCK_ENABLED = true;
    private static final int DEFAULT_SALVAGE_BLOCK_UNLOCK_LEVEL = 3;
    private static final Material DEFAULT_SALVAGE_BLOCK_MATERIAL = Material.GOLD_BLOCK;
    private static final int DEFAULT_SALVAGE_BLOCK_PLACEMENT_RADIUS_BLOCKS = 3;
    private static final boolean DEFAULT_REPAIR_BLOCK_ENABLED = true;
    private static final int DEFAULT_REPAIR_BLOCK_UNLOCK_LEVEL = 4;
    private static final Material DEFAULT_REPAIR_BLOCK_MATERIAL = Material.IRON_BLOCK;
    private static final int DEFAULT_REPAIR_BLOCK_PLACEMENT_RADIUS_BLOCKS = 3;
    private static final int DEFAULT_REPAIR_BLOCK_MATERIAL_COST = 1;
    private static final double DEFAULT_REPAIR_BLOCK_REPAIR_PERCENT = 25.0D;
    private static final boolean DEFAULT_DOUBLE_SMELT_ENABLED = true;
    private static final int DEFAULT_DOUBLE_SMELT_UNLOCK_LEVEL = 5;
    private static final boolean DEFAULT_DOUBLE_SMELT_ENABLED_BY_DEFAULT = false;
    private static final double DEFAULT_DOUBLE_SMELT_BURN_FASTER_MULTIPLIER = 1.0D;
    private static final int DEFAULT_DOUBLE_SMELT_EXTRA_FUEL_PER_SMELT_UNITS = 0;

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;
    private final FreeRepairSettings freeRepair;
    private final SalvageBlockSettings salvageBlock;
    private final RepairBlockSettings repairBlock;
    private final DoubleSmeltSettings doubleSmelt;

    private ArmoryConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial,
        final @NotNull FreeRepairSettings freeRepair,
        final @NotNull SalvageBlockSettings salvageBlock,
        final @NotNull RepairBlockSettings repairBlock,
        final @NotNull DoubleSmeltSettings doubleSmelt
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        this.freeRepair = Objects.requireNonNull(freeRepair, "freeRepair");
        this.salvageBlock = Objects.requireNonNull(salvageBlock, "salvageBlock");
        this.repairBlock = Objects.requireNonNull(repairBlock, "repairBlock");
        this.doubleSmelt = Objects.requireNonNull(doubleSmelt, "doubleSmelt");
    }

    /**
     * Returns a defensive copy of configured Armory levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Armory level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for Armory chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the parsed free-repair settings.
     *
     * @return free-repair settings snapshot
     */
    public @NotNull FreeRepairSettings getFreeRepair() {
        return this.freeRepair;
    }

    /**
     * Returns the parsed salvage-block settings.
     *
     * @return salvage-block settings snapshot
     */
    public @NotNull SalvageBlockSettings getSalvageBlock() {
        return this.salvageBlock;
    }

    /**
     * Returns the parsed repair-block settings.
     *
     * @return repair-block settings snapshot
     */
    public @NotNull RepairBlockSettings getRepairBlock() {
        return this.repairBlock;
    }

    /**
     * Returns the parsed double-smelt settings.
     *
     * @return double-smelt settings snapshot
     */
    public @NotNull DoubleSmeltSettings getDoubleSmelt() {
        return this.doubleSmelt;
    }

    /**
     * Returns the highest configured Armory level.
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
     * Parses an Armory config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull ArmoryConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses an Armory config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull ArmoryConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read armory config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Armory config
     */
    public static @NotNull ArmoryConfigSection createDefault() {
        return new ArmoryConfigSection(
            LevelConfigSupport.createDefaultArmoryLevels(),
            DEFAULT_BLOCK_MATERIAL,
            FreeRepairSettings.createDefault(),
            SalvageBlockSettings.createDefault(),
            RepairBlockSettings.createDefault(),
            DoubleSmeltSettings.createDefault()
        );
    }

    private static @NotNull ArmoryConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new ArmoryConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultArmoryLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            ),
            FreeRepairSettings.fromConfiguration(configuration.getConfigurationSection("free_repair")),
            SalvageBlockSettings.fromConfiguration(configuration.getConfigurationSection("salvage_block")),
            RepairBlockSettings.fromConfiguration(configuration.getConfigurationSection("repair_block")),
            DoubleSmeltSettings.fromConfiguration(configuration.getConfigurationSection("double_smelt"))
        );
    }

    /**
     * Immutable free-repair settings parsed from {@code armory.yml}.
     *
     * @param enabled whether free repair may be used
     * @param unlockLevel Armory level that unlocks free repair
     * @param cooldownSeconds cooldown between free repairs for one player in one Armory chunk
     */
    public record FreeRepairSettings(
        boolean enabled,
        int unlockLevel,
        int cooldownSeconds
    ) {

        /**
         * Creates one immutable free-repair settings snapshot.
         *
         * @param enabled whether free repair may be used
         * @param unlockLevel Armory level that unlocks free repair
         * @param cooldownSeconds cooldown between free repairs for one player in one Armory chunk
         */
        public FreeRepairSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_FREE_REPAIR_UNLOCK_LEVEL);
            cooldownSeconds = sanitizeNonNegativeInt(cooldownSeconds, DEFAULT_FREE_REPAIR_COOLDOWN_SECONDS);
        }

        /**
         * Returns whether free repair is unlocked at the supplied Armory level.
         *
         * @param armoryLevel Armory chunk level
         * @return {@code true} when free repair is unlocked
         */
        public boolean isUnlocked(final int armoryLevel) {
            return armoryLevel >= this.unlockLevel;
        }

        private static @NotNull FreeRepairSettings createDefault() {
            return new FreeRepairSettings(
                DEFAULT_FREE_REPAIR_ENABLED,
                DEFAULT_FREE_REPAIR_UNLOCK_LEVEL,
                DEFAULT_FREE_REPAIR_COOLDOWN_SECONDS
            );
        }

        private static @NotNull FreeRepairSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new FreeRepairSettings(
                section.getBoolean("enabled", DEFAULT_FREE_REPAIR_ENABLED),
                section.getInt("unlock_level", DEFAULT_FREE_REPAIR_UNLOCK_LEVEL),
                section.getInt("cooldown_seconds", DEFAULT_FREE_REPAIR_COOLDOWN_SECONDS)
            );
        }
    }

    /**
     * Immutable salvage-block settings parsed from {@code armory.yml}.
     *
     * @param enabled whether the salvage block may be used
     * @param unlockLevel Armory level that unlocks the salvage block
     * @param blockMaterial block material used for the bound salvage block item
     * @param placementRadiusBlocks maximum placement radius from the Armory chunk marker
     */
    public record SalvageBlockSettings(
        boolean enabled,
        int unlockLevel,
        @NotNull Material blockMaterial,
        int placementRadiusBlocks
    ) {

        /**
         * Creates one immutable salvage-block settings snapshot.
         *
         * @param enabled whether the salvage block may be used
         * @param unlockLevel Armory level that unlocks the salvage block
         * @param blockMaterial block material used for the bound salvage block item
         * @param placementRadiusBlocks maximum placement radius from the Armory chunk marker
         */
        public SalvageBlockSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_SALVAGE_BLOCK_UNLOCK_LEVEL);
            blockMaterial = sanitizeConfiguredBlockMaterial(blockMaterial, DEFAULT_SALVAGE_BLOCK_MATERIAL);
            placementRadiusBlocks = sanitizePositiveInt(
                placementRadiusBlocks,
                DEFAULT_SALVAGE_BLOCK_PLACEMENT_RADIUS_BLOCKS
            );
        }

        /**
         * Returns whether the salvage block is unlocked at the supplied Armory level.
         *
         * @param armoryLevel Armory chunk level
         * @return {@code true} when the salvage block is unlocked
         */
        public boolean isUnlocked(final int armoryLevel) {
            return armoryLevel >= this.unlockLevel;
        }

        private static @NotNull SalvageBlockSettings createDefault() {
            return new SalvageBlockSettings(
                DEFAULT_SALVAGE_BLOCK_ENABLED,
                DEFAULT_SALVAGE_BLOCK_UNLOCK_LEVEL,
                DEFAULT_SALVAGE_BLOCK_MATERIAL,
                DEFAULT_SALVAGE_BLOCK_PLACEMENT_RADIUS_BLOCKS
            );
        }

        private static @NotNull SalvageBlockSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new SalvageBlockSettings(
                section.getBoolean("enabled", DEFAULT_SALVAGE_BLOCK_ENABLED),
                section.getInt("unlock_level", DEFAULT_SALVAGE_BLOCK_UNLOCK_LEVEL),
                LevelConfigSupport.resolveConfiguredBlockMaterial(
                    section.getString("block_material"),
                    DEFAULT_SALVAGE_BLOCK_MATERIAL
                ),
                section.getInt("placement_radius_blocks", DEFAULT_SALVAGE_BLOCK_PLACEMENT_RADIUS_BLOCKS)
            );
        }
    }

    /**
     * Immutable repair-block settings parsed from {@code armory.yml}.
     *
     * @param enabled whether the repair block may be used
     * @param unlockLevel Armory level that unlocks the repair block
     * @param blockMaterial block material used for the bound repair block item
     * @param placementRadiusBlocks maximum placement radius from the Armory chunk marker
     * @param iron iron-gear repair settings
     * @param gold gold-gear repair settings
     * @param diamond diamond-gear repair settings
     * @param netherite netherite-gear repair settings
     */
    public record RepairBlockSettings(
        boolean enabled,
        int unlockLevel,
        @NotNull Material blockMaterial,
        int placementRadiusBlocks,
        @NotNull RepairFamilySettings iron,
        @NotNull RepairFamilySettings gold,
        @NotNull RepairFamilySettings diamond,
        @NotNull RepairFamilySettings netherite
    ) {

        /**
         * Creates one immutable repair-block settings snapshot.
         *
         * @param enabled whether the repair block may be used
         * @param unlockLevel Armory level that unlocks the repair block
         * @param blockMaterial block material used for the bound repair block item
         * @param placementRadiusBlocks maximum placement radius from the Armory chunk marker
         * @param iron iron-gear repair settings
         * @param gold gold-gear repair settings
         * @param diamond diamond-gear repair settings
         * @param netherite netherite-gear repair settings
         */
        public RepairBlockSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_REPAIR_BLOCK_UNLOCK_LEVEL);
            blockMaterial = sanitizeConfiguredBlockMaterial(blockMaterial, DEFAULT_REPAIR_BLOCK_MATERIAL);
            placementRadiusBlocks = sanitizePositiveInt(
                placementRadiusBlocks,
                DEFAULT_REPAIR_BLOCK_PLACEMENT_RADIUS_BLOCKS
            );
            iron = Objects.requireNonNull(iron, "iron");
            gold = Objects.requireNonNull(gold, "gold");
            diamond = Objects.requireNonNull(diamond, "diamond");
            netherite = Objects.requireNonNull(netherite, "netherite");
        }

        /**
         * Returns whether the repair block is unlocked at the supplied Armory level.
         *
         * @param armoryLevel Armory chunk level
         * @return {@code true} when the repair block is unlocked
         */
        public boolean isUnlocked(final int armoryLevel) {
            return armoryLevel >= this.unlockLevel;
        }

        private static @NotNull RepairBlockSettings createDefault() {
            final RepairFamilySettings defaultFamily = RepairFamilySettings.createDefault();
            return new RepairBlockSettings(
                DEFAULT_REPAIR_BLOCK_ENABLED,
                DEFAULT_REPAIR_BLOCK_UNLOCK_LEVEL,
                DEFAULT_REPAIR_BLOCK_MATERIAL,
                DEFAULT_REPAIR_BLOCK_PLACEMENT_RADIUS_BLOCKS,
                defaultFamily,
                defaultFamily,
                defaultFamily,
                defaultFamily
            );
        }

        private static @NotNull RepairBlockSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new RepairBlockSettings(
                section.getBoolean("enabled", DEFAULT_REPAIR_BLOCK_ENABLED),
                section.getInt("unlock_level", DEFAULT_REPAIR_BLOCK_UNLOCK_LEVEL),
                LevelConfigSupport.resolveConfiguredBlockMaterial(
                    section.getString("block_material"),
                    DEFAULT_REPAIR_BLOCK_MATERIAL
                ),
                section.getInt("placement_radius_blocks", DEFAULT_REPAIR_BLOCK_PLACEMENT_RADIUS_BLOCKS),
                RepairFamilySettings.fromConfiguration(section.getConfigurationSection("iron")),
                RepairFamilySettings.fromConfiguration(section.getConfigurationSection("gold")),
                RepairFamilySettings.fromConfiguration(section.getConfigurationSection("diamond")),
                RepairFamilySettings.fromConfiguration(section.getConfigurationSection("netherite"))
            );
        }
    }

    /**
     * Immutable repair-family settings parsed from {@code armory.yml}.
     *
     * @param materialCost required repair materials consumed per repair use
     * @param repairPercent percentage of total durability restored per repair use
     */
    public record RepairFamilySettings(
        int materialCost,
        double repairPercent
    ) {

        /**
         * Creates one immutable repair-family settings snapshot.
         *
         * @param materialCost required repair materials consumed per repair use
         * @param repairPercent percentage of total durability restored per repair use
         */
        public RepairFamilySettings {
            materialCost = sanitizePositiveInt(materialCost, DEFAULT_REPAIR_BLOCK_MATERIAL_COST);
            repairPercent = sanitizeRepairPercent(repairPercent, DEFAULT_REPAIR_BLOCK_REPAIR_PERCENT);
        }

        private static @NotNull RepairFamilySettings createDefault() {
            return new RepairFamilySettings(
                DEFAULT_REPAIR_BLOCK_MATERIAL_COST,
                DEFAULT_REPAIR_BLOCK_REPAIR_PERCENT
            );
        }

        private static @NotNull RepairFamilySettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new RepairFamilySettings(
                section.getInt("material_cost", DEFAULT_REPAIR_BLOCK_MATERIAL_COST),
                section.getDouble("repair_percent", DEFAULT_REPAIR_BLOCK_REPAIR_PERCENT)
            );
        }
    }

    /**
     * Immutable double-smelt settings parsed from {@code armory.yml}.
     *
     * @param enabled whether double smelting may run
     * @param unlockLevel Armory level that unlocks double smelting
     * @param enabledByDefault whether newly unlocked Armory chunks start with double smelting enabled
     * @param burnFasterMultiplier configured fuel-burn multiplier while double smelt is enabled
     * @param extraFuelPerSmeltUnits additional burn-time units consumed when the doubling bonus applies
     */
    public record DoubleSmeltSettings(
        boolean enabled,
        int unlockLevel,
        boolean enabledByDefault,
        double burnFasterMultiplier,
        int extraFuelPerSmeltUnits
    ) {

        /**
         * Creates one immutable double-smelt settings snapshot.
         *
         * @param enabled whether double smelting may run
         * @param unlockLevel Armory level that unlocks double smelting
         * @param enabledByDefault whether newly unlocked Armory chunks start with double smelting enabled
         * @param burnFasterMultiplier configured fuel-burn multiplier while double smelt is enabled
         * @param extraFuelPerSmeltUnits additional burn-time units consumed when the doubling bonus applies
         */
        public DoubleSmeltSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_DOUBLE_SMELT_UNLOCK_LEVEL);
            burnFasterMultiplier = sanitizeBurnMultiplier(
                burnFasterMultiplier,
                DEFAULT_DOUBLE_SMELT_BURN_FASTER_MULTIPLIER
            );
            extraFuelPerSmeltUnits = sanitizeNonNegativeInt(
                extraFuelPerSmeltUnits,
                DEFAULT_DOUBLE_SMELT_EXTRA_FUEL_PER_SMELT_UNITS
            );
        }

        /**
         * Returns whether double smelting is unlocked at the supplied Armory level.
         *
         * @param armoryLevel Armory chunk level
         * @return {@code true} when double smelting is unlocked
         */
        public boolean isUnlocked(final int armoryLevel) {
            return armoryLevel >= this.unlockLevel;
        }

        private static @NotNull DoubleSmeltSettings createDefault() {
            return new DoubleSmeltSettings(
                DEFAULT_DOUBLE_SMELT_ENABLED,
                DEFAULT_DOUBLE_SMELT_UNLOCK_LEVEL,
                DEFAULT_DOUBLE_SMELT_ENABLED_BY_DEFAULT,
                DEFAULT_DOUBLE_SMELT_BURN_FASTER_MULTIPLIER,
                DEFAULT_DOUBLE_SMELT_EXTRA_FUEL_PER_SMELT_UNITS
            );
        }

        private static @NotNull DoubleSmeltSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new DoubleSmeltSettings(
                section.getBoolean("enabled", DEFAULT_DOUBLE_SMELT_ENABLED),
                section.getInt("unlock_level", DEFAULT_DOUBLE_SMELT_UNLOCK_LEVEL),
                section.getBoolean("enabled_by_default", DEFAULT_DOUBLE_SMELT_ENABLED_BY_DEFAULT),
                section.getDouble("burn_faster_multiplier", DEFAULT_DOUBLE_SMELT_BURN_FASTER_MULTIPLIER),
                section.getInt("extra_fuel_per_smelt_units", DEFAULT_DOUBLE_SMELT_EXTRA_FUEL_PER_SMELT_UNITS)
            );
        }
    }

    private static int sanitizePositiveInt(final int value, final int fallback) {
        return value > 0 ? value : fallback;
    }

    private static int sanitizeNonNegativeInt(final int value, final int fallback) {
        return value < 0 ? fallback : value;
    }

    private static double sanitizeRepairPercent(final double value, final double fallback) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return fallback;
        }
        return Math.min(100.0D, value);
    }

    private static double sanitizeBurnMultiplier(final double value, final double fallback) {
        if (!Double.isFinite(value) || value < 1.0D) {
            return fallback;
        }
        return value;
    }

    private static @NotNull Material sanitizeConfiguredBlockMaterial(
        final @Nullable Material material,
        final @NotNull Material fallback
    ) {
        if (material == null || !LevelConfigSupport.isConfiguredBlockMaterial(material)) {
            return fallback;
        }
        return material;
    }
}
