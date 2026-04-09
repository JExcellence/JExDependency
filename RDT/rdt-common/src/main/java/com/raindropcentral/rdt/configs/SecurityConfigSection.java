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

import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed configuration snapshot for Security chunk level progression and FE fuel rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class SecurityConfigSection {

    private static final int DEFAULT_CALCULATION_INTERVAL_SECONDS = 60;
    private static final int DEFAULT_TANK_PLACEMENT_RADIUS_BLOCKS = 3;
    private static final boolean DEFAULT_FUEL_ENABLED = true;
    private static final boolean DEFAULT_OFFLINE_PROTECTION = false;
    private static final double DEFAULT_BASE_RATE = 0.80D;
    private static final double DEFAULT_CHUNK_EXPONENT = 1.08D;
    private static final double DEFAULT_TOWN_LEVEL_RATE = 0.04D;
    private static final double DEFAULT_MINIMUM_EFFECTIVE_CHUNK_RATIO = 0.75D;
    private static final Material DEFAULT_BLOCK_MATERIAL = Material.CRYING_OBSIDIAN;

    private final Map<Integer, LevelDefinition> levels;
    private final FuelSettings fuel;
    private final Material blockMaterial;

    private SecurityConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull FuelSettings fuel,
        final @NotNull Material blockMaterial
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.fuel = fuel.copy();
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
    }

    /**
     * Returns a defensive copy of configured Security levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Security level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the highest configured Security level.
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
     * Returns the parsed FE fuel configuration.
     *
     * @return defensive copy of the FE fuel configuration
     */
    public @NotNull FuelSettings getFuel() {
        return this.fuel.copy();
    }

    /**
     * Returns the configured marker-block material for Security chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns whether a material is configured as a valid fuel.
     *
     * @param material material to inspect
     * @return {@code true} when the material is configured as a fuel tier
     */
    public boolean isFuelMaterial(final @Nullable Material material) {
        return this.getFuelDefinition(material) != null;
    }

    /**
     * Returns the configured fuel definition for one material.
     *
     * @param material material to resolve
     * @return matching fuel definition, or {@code null} when the material is not fuel
     */
    public @Nullable FuelDefinition getFuelDefinition(final @Nullable Material material) {
        return this.fuel.findFuelByMaterial(material);
    }

    /**
     * Returns the configured FE chunk-weight settings for one chunk type.
     *
     * @param chunkType chunk type to resolve
     * @return matching chunk-weight settings
     */
    public @NotNull FuelChunkTypeDefinition getFuelChunkTypeDefinition(final @Nullable ChunkType chunkType) {
        return this.fuel.getChunkTypeDefinition(resolveFuelChunkTypeKey(chunkType));
    }

    /**
     * Parses a Security config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull SecurityConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a Security config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull SecurityConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read security config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Security config
     */
    public static @NotNull SecurityConfigSection createDefault() {
        return new SecurityConfigSection(
            LevelConfigSupport.createDefaultSecurityLevels(),
            FuelSettings.createDefault(),
            DEFAULT_BLOCK_MATERIAL
        );
    }

    private static @NotNull SecurityConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new SecurityConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultSecurityLevels()
            ),
            FuelSettings.fromConfiguration(configuration.getConfigurationSection("fuel")),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            )
        );
    }

    private static @NotNull String resolveFuelChunkTypeKey(final @Nullable ChunkType chunkType) {
        if (chunkType == null) {
            return "base";
        }
        return switch (chunkType) {
            case NEXUS -> "nexus";
            case SECURITY -> "security";
            case BANK -> "bank";
            case FARM -> "farm";
            case OUTPOST -> "outpost";
            case DEFAULT, CLAIM_PENDING, MEDIC, ARMORY -> "base";
        };
    }

    /**
     * Immutable FE fuel settings parsed from {@code security.yml}.
     */
    public static final class FuelSettings {

        private final boolean enabled;
        private final boolean offlineProtection;
        private final int calculationIntervalSeconds;
        private final int tankPlacementRadiusBlocks;
        private final double baseRate;
        private final double chunkExponent;
        private final double townLevelRate;
        private final double minimumEffectiveChunkRatio;
        private final LinkedHashMap<String, FuelDefinition> fuels;
        private final LinkedHashMap<String, FuelChunkTypeDefinition> chunkTypes;

        private FuelSettings(
            final boolean enabled,
            final boolean offlineProtection,
            final int calculationIntervalSeconds,
            final int tankPlacementRadiusBlocks,
            final double baseRate,
            final double chunkExponent,
            final double townLevelRate,
            final double minimumEffectiveChunkRatio,
            final @NotNull Map<String, FuelDefinition> fuels,
            final @NotNull Map<String, FuelChunkTypeDefinition> chunkTypes
        ) {
            this.enabled = enabled;
            this.offlineProtection = offlineProtection;
            this.calculationIntervalSeconds = sanitizePositiveInt(
                calculationIntervalSeconds,
                DEFAULT_CALCULATION_INTERVAL_SECONDS
            );
            this.tankPlacementRadiusBlocks = sanitizePositiveInt(
                tankPlacementRadiusBlocks,
                DEFAULT_TANK_PLACEMENT_RADIUS_BLOCKS
            );
            this.baseRate = baseRate;
            this.chunkExponent = chunkExponent;
            this.townLevelRate = townLevelRate;
            this.minimumEffectiveChunkRatio = minimumEffectiveChunkRatio;
            this.fuels = new LinkedHashMap<>(fuels);
            this.chunkTypes = new LinkedHashMap<>(chunkTypes);
        }

        /**
         * Returns whether FE-based protection fuel is enabled.
         *
         * @return {@code true} when FE fuel is enabled
         */
        public boolean isEnabled() {
            return this.enabled;
        }

        /**
         * Returns whether drain pauses while no town members are online.
         *
         * @return {@code true} when offline towns pause FE drain
         */
        public boolean isOfflineProtection() {
            return this.offlineProtection;
        }

        /**
         * Returns the FE drain calculation interval in seconds.
         *
         * @return FE drain calculation interval in seconds
         */
        public int getCalculationIntervalSeconds() {
            return this.calculationIntervalSeconds;
        }

        /**
         * Returns the required placement radius from the Security chunk marker block.
         *
         * @return radius in blocks
         */
        public int getTankPlacementRadiusBlocks() {
            return this.tankPlacementRadiusBlocks;
        }

        /**
         * Returns the configured base FE drain rate per hour.
         *
         * @return base FE drain rate per hour
         */
        public double getBaseRate() {
            return this.baseRate;
        }

        /**
         * Returns the configured effective-chunk exponent.
         *
         * @return effective-chunk exponent
         */
        public double getChunkExponent() {
            return this.chunkExponent;
        }

        /**
         * Returns the configured town-level FE drain multiplier.
         *
         * @return town-level FE drain multiplier
         */
        public double getTownLevelRate() {
            return this.townLevelRate;
        }

        /**
         * Returns the minimum effective chunk ratio clamp.
         *
         * @return minimum effective chunk ratio
         */
        public double getMinimumEffectiveChunkRatio() {
            return this.minimumEffectiveChunkRatio;
        }

        /**
         * Returns the configured ordered FE fuel tiers.
         *
         * @return ordered FE fuel tiers keyed by config name
         */
        public @NotNull Map<String, FuelDefinition> getFuels() {
            return new LinkedHashMap<>(this.fuels);
        }

        /**
         * Returns the configured FE chunk-type weighting rules.
         *
         * @return configured FE chunk-type weighting rules
         */
        public @NotNull Map<String, FuelChunkTypeDefinition> getChunkTypes() {
            return new LinkedHashMap<>(this.chunkTypes);
        }

        /**
         * Returns the configured FE chunk-type rule for a specific logical key.
         *
         * @param key logical config key such as {@code security} or {@code base}
         * @return matching chunk-type FE rule
         */
        public @NotNull FuelChunkTypeDefinition getChunkTypeDefinition(final @Nullable String key) {
            final String normalizedKey = key == null ? "base" : key.trim().toLowerCase(Locale.ROOT);
            final FuelChunkTypeDefinition configuredDefinition = this.chunkTypes.get(normalizedKey);
            if (configuredDefinition != null) {
                return configuredDefinition;
            }
            return Objects.requireNonNull(this.chunkTypes.get("base"), "base chunk type fuel definition");
        }

        /**
         * Returns the configured FE fuel tier for a specific material.
         *
         * @param material material to resolve
         * @return matching FE fuel tier, or {@code null} when absent
         */
        public @Nullable FuelDefinition findFuelByMaterial(final @Nullable Material material) {
            if (material == null) {
                return null;
            }
            for (final FuelDefinition definition : this.fuels.values()) {
                if (definition.material() == material) {
                    return definition;
                }
            }
            return null;
        }

        /**
         * Returns a defensive copy of this FE fuel settings snapshot.
         *
         * @return copied FE fuel settings snapshot
         */
        public @NotNull FuelSettings copy() {
            return new FuelSettings(
                this.enabled,
                this.offlineProtection,
                this.calculationIntervalSeconds,
                this.tankPlacementRadiusBlocks,
                this.baseRate,
                this.chunkExponent,
                this.townLevelRate,
                this.minimumEffectiveChunkRatio,
                this.fuels,
                this.chunkTypes
            );
        }

        /**
         * Creates the default FE fuel settings snapshot.
         *
         * @return default FE fuel settings snapshot
         */
        public static @NotNull FuelSettings createDefault() {
            return new FuelSettings(
                DEFAULT_FUEL_ENABLED,
                DEFAULT_OFFLINE_PROTECTION,
                DEFAULT_CALCULATION_INTERVAL_SECONDS,
                DEFAULT_TANK_PLACEMENT_RADIUS_BLOCKS,
                DEFAULT_BASE_RATE,
                DEFAULT_CHUNK_EXPONENT,
                DEFAULT_TOWN_LEVEL_RATE,
                DEFAULT_MINIMUM_EFFECTIVE_CHUNK_RATIO,
                createDefaultFuels(),
                createDefaultChunkTypes()
            );
        }

        private static @NotNull FuelSettings fromConfiguration(final @Nullable ConfigurationSection fuelSection) {
            if (fuelSection == null) {
                return createDefault();
            }

            final LinkedHashMap<String, FuelDefinition> fuels = parseFuels(fuelSection.getConfigurationSection("fuels"));
            final LinkedHashMap<String, FuelChunkTypeDefinition> chunkTypes = parseChunkTypes(
                fuelSection.getConfigurationSection("chunk_types")
            );
            return new FuelSettings(
                fuelSection.getBoolean("enabled", DEFAULT_FUEL_ENABLED),
                fuelSection.getBoolean("offline_protection", DEFAULT_OFFLINE_PROTECTION),
                fuelSection.getInt("calculation_interval_seconds", DEFAULT_CALCULATION_INTERVAL_SECONDS),
                fuelSection.getInt("tank_placement_radius_blocks", DEFAULT_TANK_PLACEMENT_RADIUS_BLOCKS),
                fuelSection.getDouble("base_rate", DEFAULT_BASE_RATE),
                fuelSection.getDouble("chunk_exponent", DEFAULT_CHUNK_EXPONENT),
                fuelSection.getDouble("town_level_rate", DEFAULT_TOWN_LEVEL_RATE),
                fuelSection.getDouble("minimum_effective_chunk_ratio", DEFAULT_MINIMUM_EFFECTIVE_CHUNK_RATIO),
                fuels.isEmpty() ? createDefaultFuels() : fuels,
                chunkTypes.isEmpty() ? createDefaultChunkTypes() : chunkTypes
            );
        }

        private static @NotNull LinkedHashMap<String, FuelDefinition> parseFuels(
            final @Nullable ConfigurationSection fuelsSection
        ) {
            final LinkedHashMap<String, FuelDefinition> fuels = new LinkedHashMap<>();
            if (fuelsSection == null) {
                return fuels;
            }
            for (final String key : fuelsSection.getKeys(false)) {
                final ConfigurationSection fuelDefinitionSection = fuelsSection.getConfigurationSection(key);
                if (fuelDefinitionSection == null) {
                    continue;
                }
                final Material material = parseFuelMaterial(fuelDefinitionSection.getString("material"));
                final double units = fuelDefinitionSection.getDouble("units", 0.0D);
                if (material == null || units <= 0.0D) {
                    continue;
                }
                fuels.put(
                    LevelConfigSupport.normalizeDefinitionKey(key),
                    new FuelDefinition(LevelConfigSupport.normalizeDefinitionKey(key), material, units)
                );
            }
            return fuels;
        }

        private static @NotNull LinkedHashMap<String, FuelChunkTypeDefinition> parseChunkTypes(
            final @Nullable ConfigurationSection chunkTypesSection
        ) {
            final LinkedHashMap<String, FuelChunkTypeDefinition> chunkTypes = createDefaultChunkTypes();
            if (chunkTypesSection == null) {
                return chunkTypes;
            }
            for (final String key : chunkTypesSection.getKeys(false)) {
                final String normalizedKey = LevelConfigSupport.normalizeDefinitionKey(key);
                if (normalizedKey.isEmpty()) {
                    continue;
                }
                final ConfigurationSection definitionSection = chunkTypesSection.getConfigurationSection(key);
                if (definitionSection == null) {
                    continue;
                }
                final FuelChunkTypeDefinition fallback = chunkTypes.getOrDefault(
                    normalizedKey,
                    new FuelChunkTypeDefinition(normalizedKey, 1.0D, 0.0D, null)
                );
                chunkTypes.put(
                    normalizedKey,
                    new FuelChunkTypeDefinition(
                        normalizedKey,
                        definitionSection.getDouble("weight", fallback.weight()),
                        definitionSection.getDouble("level_scale", fallback.levelScale()),
                        definitionSection.isSet("min_weight")
                            ? Double.valueOf(definitionSection.getDouble("min_weight"))
                            : fallback.minWeight()
                    )
                );
            }
            return chunkTypes;
        }

        private static @Nullable Material parseFuelMaterial(final @Nullable String rawMaterial) {
            if (rawMaterial == null || rawMaterial.isBlank()) {
                return null;
            }
            final Material material = Material.matchMaterial(rawMaterial.trim().toUpperCase(Locale.ROOT));
            if (material == null || material == Material.AIR || material.name().endsWith("_AIR")) {
                return null;
            }
            return material;
        }

        private static @NotNull LinkedHashMap<String, FuelDefinition> createDefaultFuels() {
            final LinkedHashMap<String, FuelDefinition> defaults = new LinkedHashMap<>();
            defaults.put("redstone", new FuelDefinition("redstone", Material.REDSTONE, 25.0D));
            return defaults;
        }

        private static @NotNull LinkedHashMap<String, FuelChunkTypeDefinition> createDefaultChunkTypes() {
            final LinkedHashMap<String, FuelChunkTypeDefinition> defaults = new LinkedHashMap<>();
            defaults.put("base", new FuelChunkTypeDefinition("base", 1.00D, 0.00D, null));
            defaults.put("nexus", new FuelChunkTypeDefinition("nexus", 1.35D, 0.06D, null));
            defaults.put("security", new FuelChunkTypeDefinition("security", 0.70D, -0.05D, 0.45D));
            defaults.put("bank", new FuelChunkTypeDefinition("bank", 1.20D, 0.05D, null));
            defaults.put("farm", new FuelChunkTypeDefinition("farm", 0.80D, -0.04D, 0.55D));
            defaults.put("outpost", new FuelChunkTypeDefinition("outpost", 1.15D, 0.05D, null));
            return defaults;
        }

        private static int sanitizePositiveInt(final int value, final int fallback) {
            return value > 0 ? value : fallback;
        }
    }

    /**
     * One configured FE fuel item tier.
     *
     * @param key stable config key
     * @param material backing Bukkit material
     * @param units FE units contributed by one item
     */
    public record FuelDefinition(
        @NotNull String key,
        @NotNull Material material,
        double units
    ) {
    }

    /**
     * One configured chunk-type FE weighting rule.
     *
     * @param key stable config key
     * @param weight base chunk weight
     * @param levelScale per-level weight modifier
     * @param minWeight optional lower clamp applied after scaling
     */
    public record FuelChunkTypeDefinition(
        @NotNull String key,
        double weight,
        double levelScale,
        @Nullable Double minWeight
    ) {

        /**
         * Returns the effective FE weight for one chunk at the supplied level.
         *
         * @param level chunk or nexus progression level
         * @return effective FE chunk weight after scaling and optional clamp
         */
        public double resolveWeight(final int level) {
            final double scaledWeight = this.weight * (1.0D + (this.levelScale * Math.max(0, level - 1)));
            if (this.minWeight == null) {
                return scaledWeight;
            }
            return Math.max(this.minWeight, scaledWeight);
        }
    }
}
