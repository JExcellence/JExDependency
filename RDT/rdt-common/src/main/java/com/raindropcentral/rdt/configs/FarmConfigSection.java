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

import com.raindropcentral.rdt.utils.FarmReplantPriority;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parsed configuration snapshot for Farm chunk level progression.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class FarmConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.HAY_BLOCK;
    private static final boolean DEFAULT_GROWTH_ENABLED_BY_DEFAULT = true;
    private static final int DEFAULT_GROWTH_TIER_ONE_UNLOCK_LEVEL = 2;
    private static final double DEFAULT_GROWTH_TIER_ONE_SPEED_MULTIPLIER = 2.0D;
    private static final int DEFAULT_GROWTH_TIER_TWO_UNLOCK_LEVEL = 4;
    private static final double DEFAULT_GROWTH_TIER_TWO_SPEED_MULTIPLIER = 3.0D;
    private static final boolean DEFAULT_CROP_FAILURE_ENABLED = true;
    private static final double DEFAULT_CROP_FAILURE_RATE = 0.5D;
    private static final int DEFAULT_SEED_BOX_UNLOCK_LEVEL = 3;
    private static final int DEFAULT_SEED_BOX_PLACEMENT_RADIUS_BLOCKS = 3;
    private static final int DEFAULT_REPLANT_UNLOCK_LEVEL = 3;
    private static final boolean DEFAULT_REPLANT_ENABLED_BY_DEFAULT = true;
    private static final FarmReplantPriority DEFAULT_REPLANT_PRIORITY = FarmReplantPriority.INVENTORY_FIRST;
    private static final int DEFAULT_DOUBLE_HARVEST_UNLOCK_LEVEL = 5;
    private static final int DEFAULT_DOUBLE_HARVEST_MULTIPLIER = 2;
    private static final Set<Material> DEFAULT_ALLOWED_SEED_MATERIALS = Set.of(
        Material.WHEAT_SEEDS,
        Material.BEETROOT_SEEDS,
        Material.CARROT,
        Material.POTATO,
        Material.MELON_SEEDS,
        Material.PUMPKIN_SEEDS
    );

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;
    private final GrowthSettings growth;
    private final CropFailureSettings cropFailure;
    private final SeedBoxSettings seedBox;
    private final ReplantSettings replant;
    private final DoubleHarvestSettings doubleHarvest;

    private FarmConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial,
        final @NotNull GrowthSettings growth,
        final @NotNull CropFailureSettings cropFailure,
        final @NotNull SeedBoxSettings seedBox,
        final @NotNull ReplantSettings replant,
        final @NotNull DoubleHarvestSettings doubleHarvest
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        this.growth = Objects.requireNonNull(growth, "growth");
        this.cropFailure = Objects.requireNonNull(cropFailure, "cropFailure");
        this.seedBox = Objects.requireNonNull(seedBox, "seedBox");
        this.replant = Objects.requireNonNull(replant, "replant");
        this.doubleHarvest = Objects.requireNonNull(doubleHarvest, "doubleHarvest");
    }

    /**
     * Returns a defensive copy of configured Farm levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Farm level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for Farm chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the parsed Farm growth-enhancement settings.
     *
     * @return growth settings snapshot
     */
    public @NotNull GrowthSettings getGrowth() {
        return this.growth;
    }

    /**
     * Returns the parsed Farm crop-failure settings.
     *
     * @return crop-failure settings snapshot
     */
    public @NotNull CropFailureSettings getCropFailure() {
        return this.cropFailure;
    }

    /**
     * Returns the parsed Farm seed-box settings.
     *
     * @return seed-box settings snapshot
     */
    public @NotNull SeedBoxSettings getSeedBox() {
        return this.seedBox;
    }

    /**
     * Returns the parsed Farm auto-replant settings.
     *
     * @return auto-replant settings snapshot
     */
    public @NotNull ReplantSettings getReplant() {
        return this.replant;
    }

    /**
     * Returns the parsed Farm double-harvest settings.
     *
     * @return double-harvest settings snapshot
     */
    public @NotNull DoubleHarvestSettings getDoubleHarvest() {
        return this.doubleHarvest;
    }

    /**
     * Returns whether the supplied material may be stored in Farm seed boxes.
     *
     * @param material material to inspect
     * @return {@code true} when the material is allowed inside seed boxes
     */
    public boolean isAllowedSeedMaterial(final @Nullable Material material) {
        return this.seedBox.isAllowedMaterial(material);
    }

    /**
     * Returns the highest configured Farm level.
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
     * Parses a Farm config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull FarmConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a Farm config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull FarmConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read farm config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Farm config
     */
    public static @NotNull FarmConfigSection createDefault() {
        return new FarmConfigSection(
            LevelConfigSupport.createDefaultFarmLevels(),
            DEFAULT_BLOCK_MATERIAL,
            GrowthSettings.createDefault(),
            CropFailureSettings.createDefault(),
            SeedBoxSettings.createDefault(),
            ReplantSettings.createDefault(),
            DoubleHarvestSettings.createDefault()
        );
    }

    private static @NotNull FarmConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new FarmConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultFarmLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            ),
            GrowthSettings.fromConfiguration(configuration.getConfigurationSection("growth")),
            CropFailureSettings.fromConfiguration(configuration.getConfigurationSection("crop_failure")),
            SeedBoxSettings.fromConfiguration(configuration.getConfigurationSection("seed_box")),
            ReplantSettings.fromConfiguration(configuration.getConfigurationSection("replant")),
            DoubleHarvestSettings.fromConfiguration(configuration.getConfigurationSection("double_harvest"))
        );
    }

    /**
     * Immutable Farm growth-enhancement settings parsed from {@code farm.yml}.
     *
     * @param enabledByDefault whether newly unlocked growth starts enabled
     * @param tierOneUnlockLevel Farm level that unlocks the first growth boost
     * @param tierOneGrowthSpeedMultiplier growth-speed multiplier applied once tier one is unlocked
     * @param tierTwoUnlockLevel Farm level that unlocks the stronger growth boost
     * @param tierTwoGrowthSpeedMultiplier growth-speed multiplier applied once tier two is unlocked
     */
    public record GrowthSettings(
        boolean enabledByDefault,
        int tierOneUnlockLevel,
        double tierOneGrowthSpeedMultiplier,
        int tierTwoUnlockLevel,
        double tierTwoGrowthSpeedMultiplier
    ) {

        /**
         * Creates one immutable Farm growth-settings snapshot.
         *
         * @param enabledByDefault whether newly unlocked growth starts enabled
         * @param tierOneUnlockLevel Farm level that unlocks the first growth boost
         * @param tierOneGrowthSpeedMultiplier growth-speed multiplier applied once tier one is unlocked
         * @param tierTwoUnlockLevel Farm level that unlocks the stronger growth boost
         * @param tierTwoGrowthSpeedMultiplier growth-speed multiplier applied once tier two is unlocked
         */
        public GrowthSettings {
            tierOneUnlockLevel = sanitizePositiveInt(tierOneUnlockLevel, DEFAULT_GROWTH_TIER_ONE_UNLOCK_LEVEL);
            tierOneGrowthSpeedMultiplier = sanitizeGrowthSpeedMultiplier(
                tierOneGrowthSpeedMultiplier,
                DEFAULT_GROWTH_TIER_ONE_SPEED_MULTIPLIER
            );
            tierTwoUnlockLevel = Math.max(
                tierOneUnlockLevel,
                sanitizePositiveInt(tierTwoUnlockLevel, DEFAULT_GROWTH_TIER_TWO_UNLOCK_LEVEL)
            );
            tierTwoGrowthSpeedMultiplier = Math.max(
                tierOneGrowthSpeedMultiplier,
                sanitizeGrowthSpeedMultiplier(
                    tierTwoGrowthSpeedMultiplier,
                    DEFAULT_GROWTH_TIER_TWO_SPEED_MULTIPLIER
                )
            );
        }

        /**
         * Returns whether the first growth tier is unlocked at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return {@code true} when tier one is unlocked
         */
        public boolean isUnlocked(final int farmLevel) {
            return farmLevel >= this.tierOneUnlockLevel;
        }

        /**
         * Returns whether the second growth tier is unlocked at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return {@code true} when tier two is unlocked
         */
        public boolean usesTierTwo(final int farmLevel) {
            return farmLevel >= this.tierTwoUnlockLevel;
        }

        /**
         * Returns the configured growth-speed multiplier granted at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return configured growth-speed multiplier, or {@code 1.0} when locked
         */
        public double resolveGrowthSpeedMultiplier(final int farmLevel) {
            if (this.usesTierTwo(farmLevel)) {
                return this.tierTwoGrowthSpeedMultiplier;
            }
            return this.isUnlocked(farmLevel) ? this.tierOneGrowthSpeedMultiplier : 1.0D;
        }

        private static @NotNull GrowthSettings createDefault() {
            return new GrowthSettings(
                DEFAULT_GROWTH_ENABLED_BY_DEFAULT,
                DEFAULT_GROWTH_TIER_ONE_UNLOCK_LEVEL,
                DEFAULT_GROWTH_TIER_ONE_SPEED_MULTIPLIER,
                DEFAULT_GROWTH_TIER_TWO_UNLOCK_LEVEL,
                DEFAULT_GROWTH_TIER_TWO_SPEED_MULTIPLIER
            );
        }

        private static @NotNull GrowthSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new GrowthSettings(
                section.getBoolean("enabled_by_default", DEFAULT_GROWTH_ENABLED_BY_DEFAULT),
                section.getInt("tier_1_unlock_level", DEFAULT_GROWTH_TIER_ONE_UNLOCK_LEVEL),
                resolveConfiguredGrowthSpeedMultiplier(
                    section,
                    "tier_1_growth_speed_multiplier",
                    "tier_1_extra_growth_steps",
                    DEFAULT_GROWTH_TIER_ONE_SPEED_MULTIPLIER
                ),
                section.getInt("tier_2_unlock_level", DEFAULT_GROWTH_TIER_TWO_UNLOCK_LEVEL),
                resolveConfiguredGrowthSpeedMultiplier(
                    section,
                    "tier_2_growth_speed_multiplier",
                    "tier_2_extra_growth_steps",
                    DEFAULT_GROWTH_TIER_TWO_SPEED_MULTIPLIER
                )
            );
        }

        private static double resolveConfiguredGrowthSpeedMultiplier(
            final @NotNull ConfigurationSection section,
            final @NotNull String multiplierKey,
            final @NotNull String legacyExtraStepsKey,
            final double fallback
        ) {
            if (section.isSet(multiplierKey)) {
                return section.getDouble(multiplierKey, fallback);
            }
            if (section.isSet(legacyExtraStepsKey)) {
                return 1.0D + section.getDouble(legacyExtraStepsKey, fallback - 1.0D);
            }
            return fallback;
        }

        private static double sanitizeGrowthSpeedMultiplier(final double value, final double fallback) {
            return value >= 1.0D ? value : fallback;
        }
    }

    /**
     * Immutable crop-failure settings parsed from {@code farm.yml}.
     *
     * @param enabled whether natural crop growth may fail outside Farm chunks
     * @param failureRate probability in the range {@code [0.0, 1.0]} that one grow tick fails outside Farm chunks
     */
    public record CropFailureSettings(
        boolean enabled,
        double failureRate
    ) {

        /**
         * Creates one immutable crop-failure settings snapshot.
         *
         * @param enabled whether natural crop growth may fail outside Farm chunks
         * @param failureRate probability in the range {@code [0.0, 1.0]} that one grow tick fails outside Farm chunks
         */
        public CropFailureSettings {
            failureRate = sanitizeProbability(failureRate, DEFAULT_CROP_FAILURE_RATE);
        }

        /**
         * Returns whether one random roll causes a natural crop-growth tick to fail outside Farm chunks.
         *
         * @param roll random roll in the range {@code [0, 1)}
         * @return {@code true} when the tick should fail
         */
        public boolean shouldFail(final double roll) {
            return this.enabled && roll < this.failureRate;
        }

        private static @NotNull CropFailureSettings createDefault() {
            return new CropFailureSettings(
                DEFAULT_CROP_FAILURE_ENABLED,
                DEFAULT_CROP_FAILURE_RATE
            );
        }

        private static @NotNull CropFailureSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new CropFailureSettings(
                section.getBoolean("enabled", DEFAULT_CROP_FAILURE_ENABLED),
                section.getDouble("failure_rate", DEFAULT_CROP_FAILURE_RATE)
            );
        }
    }

    /**
     * Immutable Farm seed-box settings parsed from {@code farm.yml}.
     *
     * @param unlockLevel Farm level that unlocks the seed box
     * @param placementRadiusBlocks maximum radius from the Farm chunk marker block
     * @param allowedMaterials seed materials that may be stored inside the seed box
     */
    public record SeedBoxSettings(
        int unlockLevel,
        int placementRadiusBlocks,
        @NotNull Set<Material> allowedMaterials
    ) {

        /**
         * Creates one immutable Farm seed-box settings snapshot.
         *
         * @param unlockLevel Farm level that unlocks the seed box
         * @param placementRadiusBlocks maximum radius from the Farm chunk marker block
         * @param allowedMaterials seed materials that may be stored inside the seed box
         */
        public SeedBoxSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_SEED_BOX_UNLOCK_LEVEL);
            placementRadiusBlocks = sanitizePositiveInt(
                placementRadiusBlocks,
                DEFAULT_SEED_BOX_PLACEMENT_RADIUS_BLOCKS
            );
            allowedMaterials = copyAllowedMaterials(allowedMaterials);
        }

        /**
         * Returns the configured allowed seed materials in insertion order.
         *
         * @return copied allowed seed materials
         */
        @Override
        public @NotNull Set<Material> allowedMaterials() {
            return new LinkedHashSet<>(this.allowedMaterials);
        }

        /**
         * Returns whether the seed box is unlocked at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return {@code true} when the seed box is unlocked
         */
        public boolean isUnlocked(final int farmLevel) {
            return farmLevel >= this.unlockLevel;
        }

        /**
         * Returns whether the supplied material may be stored in Farm seed boxes.
         *
         * @param material material to inspect
         * @return {@code true} when the material is allowed inside seed boxes
         */
        public boolean isAllowedMaterial(final @Nullable Material material) {
            return material != null && this.allowedMaterials.contains(material);
        }

        private static @NotNull SeedBoxSettings createDefault() {
            return new SeedBoxSettings(
                DEFAULT_SEED_BOX_UNLOCK_LEVEL,
                DEFAULT_SEED_BOX_PLACEMENT_RADIUS_BLOCKS,
                DEFAULT_ALLOWED_SEED_MATERIALS
            );
        }

        private static @NotNull SeedBoxSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new SeedBoxSettings(
                section.getInt("unlock_level", DEFAULT_SEED_BOX_UNLOCK_LEVEL),
                section.getInt("placement_radius_blocks", DEFAULT_SEED_BOX_PLACEMENT_RADIUS_BLOCKS),
                parseAllowedSeedMaterials(section.getStringList("allowed_materials"))
            );
        }
    }

    /**
     * Immutable Farm auto-replant settings parsed from {@code farm.yml}.
     *
     * @param unlockLevel Farm level that unlocks auto-replanting
     * @param enabledByDefault whether newly unlocked auto-replant starts enabled
     * @param defaultSourcePriority default seed-consumption priority for auto-replanting
     */
    public record ReplantSettings(
        int unlockLevel,
        boolean enabledByDefault,
        @NotNull FarmReplantPriority defaultSourcePriority
    ) {

        /**
         * Creates one immutable Farm auto-replant settings snapshot.
         *
         * @param unlockLevel Farm level that unlocks auto-replanting
         * @param enabledByDefault whether newly unlocked auto-replant starts enabled
         * @param defaultSourcePriority default seed-consumption priority for auto-replanting
         */
        public ReplantSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_REPLANT_UNLOCK_LEVEL);
            defaultSourcePriority = Objects.requireNonNull(defaultSourcePriority, "defaultSourcePriority");
        }

        /**
         * Returns whether auto-replanting is unlocked at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return {@code true} when auto-replanting is unlocked
         */
        public boolean isUnlocked(final int farmLevel) {
            return farmLevel >= this.unlockLevel;
        }

        private static @NotNull ReplantSettings createDefault() {
            return new ReplantSettings(
                DEFAULT_REPLANT_UNLOCK_LEVEL,
                DEFAULT_REPLANT_ENABLED_BY_DEFAULT,
                DEFAULT_REPLANT_PRIORITY
            );
        }

        private static @NotNull ReplantSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new ReplantSettings(
                section.getInt("unlock_level", DEFAULT_REPLANT_UNLOCK_LEVEL),
                section.getBoolean("enabled_by_default", DEFAULT_REPLANT_ENABLED_BY_DEFAULT),
                FarmReplantPriority.fromValue(
                    section.getString("default_source_priority"),
                    DEFAULT_REPLANT_PRIORITY
                )
            );
        }
    }

    /**
     * Immutable Farm double-harvest settings parsed from {@code farm.yml}.
     *
     * @param unlockLevel Farm level that unlocks double harvest
     * @param multiplier harvest multiplier applied once unlocked
     */
    public record DoubleHarvestSettings(
        int unlockLevel,
        int multiplier
    ) {

        /**
         * Creates one immutable Farm double-harvest settings snapshot.
         *
         * @param unlockLevel Farm level that unlocks double harvest
         * @param multiplier harvest multiplier applied once unlocked
         */
        public DoubleHarvestSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_DOUBLE_HARVEST_UNLOCK_LEVEL);
            multiplier = sanitizePositiveInt(multiplier, DEFAULT_DOUBLE_HARVEST_MULTIPLIER);
        }

        /**
         * Returns whether double harvest is unlocked at the supplied Farm level.
         *
         * @param farmLevel Farm chunk level
         * @return {@code true} when double harvest is unlocked
         */
        public boolean isUnlocked(final int farmLevel) {
            return farmLevel >= this.unlockLevel;
        }

        private static @NotNull DoubleHarvestSettings createDefault() {
            return new DoubleHarvestSettings(
                DEFAULT_DOUBLE_HARVEST_UNLOCK_LEVEL,
                DEFAULT_DOUBLE_HARVEST_MULTIPLIER
            );
        }

        private static @NotNull DoubleHarvestSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new DoubleHarvestSettings(
                section.getInt("unlock_level", DEFAULT_DOUBLE_HARVEST_UNLOCK_LEVEL),
                section.getInt("multiplier", DEFAULT_DOUBLE_HARVEST_MULTIPLIER)
            );
        }
    }

    private static int sanitizePositiveInt(final int value, final int fallback) {
        return value > 0 ? value : fallback;
    }

    private static double sanitizeProbability(final double value, final double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static @NotNull Set<Material> parseAllowedSeedMaterials(final @Nullable Iterable<String> rawMaterials) {
        final LinkedHashSet<Material> allowedMaterials = new LinkedHashSet<>();
        if (rawMaterials != null) {
            for (final String rawMaterial : rawMaterials) {
                if (rawMaterial == null || rawMaterial.isBlank()) {
                    continue;
                }
                final Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase(java.util.Locale.ROOT));
                if (material != null) {
                    allowedMaterials.add(material);
                }
            }
        }
        return allowedMaterials.isEmpty()
            ? new LinkedHashSet<>(DEFAULT_ALLOWED_SEED_MATERIALS)
            : allowedMaterials;
    }

    private static @NotNull Set<Material> copyAllowedMaterials(final @Nullable Set<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return new LinkedHashSet<>(DEFAULT_ALLOWED_SEED_MATERIALS);
        }
        final LinkedHashSet<Material> copied = new LinkedHashSet<>();
        for (final Material material : materials) {
            if (material != null) {
                copied.add(material);
            }
        }
        return copied.isEmpty() ? new LinkedHashSet<>(DEFAULT_ALLOWED_SEED_MATERIALS) : copied;
    }
}
