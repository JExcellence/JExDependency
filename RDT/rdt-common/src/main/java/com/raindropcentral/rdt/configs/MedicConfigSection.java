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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parsed configuration snapshot for Medic chunk level progression and perk tuning.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class MedicConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.SEA_LANTERN;
    private static final boolean DEFAULT_TOWN_MEMBERS_ONLY = true;
    private static final boolean DEFAULT_FOOD_REGEN_ENABLED = true;
    private static final int DEFAULT_FOOD_REGEN_UNLOCK_LEVEL = 1;
    private static final int DEFAULT_FOOD_REGEN_INTERVAL_TICKS = 40;
    private static final int DEFAULT_FOOD_REGEN_FOOD_POINTS = 1;
    private static final double DEFAULT_FOOD_REGEN_SATURATION = 0.5D;
    private static final boolean DEFAULT_HEALTH_REGEN_ENABLED = true;
    private static final int DEFAULT_HEALTH_REGEN_UNLOCK_LEVEL = 2;
    private static final int DEFAULT_HEALTH_REGEN_INTERVAL_TICKS = 40;
    private static final double DEFAULT_HEALTH_REGEN_POINTS = 1.0D;
    private static final boolean DEFAULT_CLEANSE_ENABLED = true;
    private static final int DEFAULT_CLEANSE_UNLOCK_LEVEL = 3;
    private static final int DEFAULT_CLEANSE_INTERVAL_TICKS = 40;
    private static final boolean DEFAULT_FORTIFIED_RECOVERY_ENABLED = true;
    private static final int DEFAULT_FORTIFIED_RECOVERY_UNLOCK_LEVEL = 4;
    private static final int DEFAULT_FORTIFIED_RECOVERY_DURATION_SECONDS = 300;
    private static final boolean DEFAULT_FORTIFIED_RECOVERY_REFRESH_WHILE_INSIDE = true;
    private static final double DEFAULT_FORTIFIED_RECOVERY_TARGET_MAX_HEALTH = 40.0D;
    private static final int DEFAULT_FORTIFIED_RECOVERY_UPKEEP_INTERVAL_TICKS = 20;
    private static final int DEFAULT_FORTIFIED_RECOVERY_TARGET_FOOD_LEVEL = 20;
    private static final double DEFAULT_FORTIFIED_RECOVERY_TARGET_SATURATION = 20.0D;
    private static final boolean DEFAULT_EMERGENCY_REFILL_ENABLED = true;
    private static final int DEFAULT_EMERGENCY_REFILL_UNLOCK_LEVEL = 5;
    private static final boolean DEFAULT_EMERGENCY_REFILL_TRIGGER_ON_ENTRY = true;
    private static final int DEFAULT_EMERGENCY_REFILL_COOLDOWN_SECONDS = 30;
    private static final TargetHealthMode DEFAULT_TARGET_HEALTH_MODE = TargetHealthMode.CURRENT_MAX;
    private static final int DEFAULT_EMERGENCY_REFILL_TARGET_FOOD_LEVEL = 20;
    private static final double DEFAULT_EMERGENCY_REFILL_TARGET_SATURATION = 20.0D;
    private static final Set<String> DEFAULT_HARMFUL_EFFECT_KEYS = Set.of(
        "bad_omen",
        "blindness",
        "darkness",
        "hunger",
        "instant_damage",
        "levitation",
        "mining_fatigue",
        "nausea",
        "poison",
        "raid_omen",
        "slowness",
        "trial_omen",
        "unluck",
        "weakness",
        "wither"
    );

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;
    private final FoodRegenSettings foodRegen;
    private final HealthRegenSettings healthRegen;
    private final CleanseSettings cleanse;
    private final FortifiedRecoverySettings fortifiedRecovery;
    private final EmergencyRefillSettings emergencyRefill;

    private MedicConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial,
        final @NotNull FoodRegenSettings foodRegen,
        final @NotNull HealthRegenSettings healthRegen,
        final @NotNull CleanseSettings cleanse,
        final @NotNull FortifiedRecoverySettings fortifiedRecovery,
        final @NotNull EmergencyRefillSettings emergencyRefill
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        this.foodRegen = Objects.requireNonNull(foodRegen, "foodRegen");
        this.healthRegen = Objects.requireNonNull(healthRegen, "healthRegen");
        this.cleanse = Objects.requireNonNull(cleanse, "cleanse");
        this.fortifiedRecovery = Objects.requireNonNull(fortifiedRecovery, "fortifiedRecovery");
        this.emergencyRefill = Objects.requireNonNull(emergencyRefill, "emergencyRefill");
    }

    /**
     * Returns a defensive copy of configured Medic levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Medic level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for Medic chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the parsed food-regeneration settings.
     *
     * @return food-regeneration settings snapshot
     */
    public @NotNull FoodRegenSettings getFoodRegen() {
        return this.foodRegen;
    }

    /**
     * Returns the parsed health-regeneration settings.
     *
     * @return health-regeneration settings snapshot
     */
    public @NotNull HealthRegenSettings getHealthRegen() {
        return this.healthRegen;
    }

    /**
     * Returns the parsed harmful-effect cleanse settings.
     *
     * @return cleanse settings snapshot
     */
    public @NotNull CleanseSettings getCleanse() {
        return this.cleanse;
    }

    /**
     * Returns the parsed fortified-recovery settings.
     *
     * @return fortified-recovery settings snapshot
     */
    public @NotNull FortifiedRecoverySettings getFortifiedRecovery() {
        return this.fortifiedRecovery;
    }

    /**
     * Returns the parsed emergency-refill settings.
     *
     * @return emergency-refill settings snapshot
     */
    public @NotNull EmergencyRefillSettings getEmergencyRefill() {
        return this.emergencyRefill;
    }

    /**
     * Returns the highest configured Medic level.
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
     * Parses a Medic config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull MedicConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a Medic config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull MedicConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read medic config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Medic config
     */
    public static @NotNull MedicConfigSection createDefault() {
        return new MedicConfigSection(
            LevelConfigSupport.createDefaultMedicLevels(),
            DEFAULT_BLOCK_MATERIAL,
            FoodRegenSettings.createDefault(),
            HealthRegenSettings.createDefault(),
            CleanseSettings.createDefault(),
            FortifiedRecoverySettings.createDefault(),
            EmergencyRefillSettings.createDefault()
        );
    }

    private static @NotNull MedicConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new MedicConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultMedicLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            ),
            FoodRegenSettings.fromConfiguration(configuration.getConfigurationSection("food_regen")),
            HealthRegenSettings.fromConfiguration(configuration.getConfigurationSection("health_regen")),
            CleanseSettings.fromConfiguration(configuration.getConfigurationSection("cleanse")),
            FortifiedRecoverySettings.fromConfiguration(configuration.getConfigurationSection("fortified_recovery")),
            EmergencyRefillSettings.fromConfiguration(configuration.getConfigurationSection("emergency_refill"))
        );
    }

    /**
     * Immutable food-regeneration settings parsed from {@code medic.yml}.
     *
     * @param enabled whether food regeneration may run
     * @param unlockLevel Medic level that unlocks food regeneration
     * @param townMembersOnly whether only town members may receive the perk
     * @param intervalTicks interval between food pulses
     * @param foodPointsPerPulse food points restored each pulse
     * @param saturationPerPulse saturation restored each pulse
     */
    public record FoodRegenSettings(
        boolean enabled,
        int unlockLevel,
        boolean townMembersOnly,
        int intervalTicks,
        int foodPointsPerPulse,
        double saturationPerPulse
    ) {

        /**
         * Creates one immutable food-regeneration settings snapshot.
         *
         * @param enabled whether food regeneration may run
         * @param unlockLevel Medic level that unlocks food regeneration
         * @param townMembersOnly whether only town members may receive the perk
         * @param intervalTicks interval between food pulses
         * @param foodPointsPerPulse food points restored each pulse
         * @param saturationPerPulse saturation restored each pulse
         */
        public FoodRegenSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_FOOD_REGEN_UNLOCK_LEVEL);
            intervalTicks = sanitizePositiveInt(intervalTicks, DEFAULT_FOOD_REGEN_INTERVAL_TICKS);
            foodPointsPerPulse = sanitizePositiveInt(foodPointsPerPulse, DEFAULT_FOOD_REGEN_FOOD_POINTS);
            saturationPerPulse = sanitizePositiveDouble(saturationPerPulse, DEFAULT_FOOD_REGEN_SATURATION);
        }

        /**
         * Returns whether food regeneration is unlocked at the supplied Medic level.
         *
         * @param medicLevel Medic chunk level
         * @return {@code true} when food regeneration is unlocked
         */
        public boolean isUnlocked(final int medicLevel) {
            return medicLevel >= this.unlockLevel;
        }

        private static @NotNull FoodRegenSettings createDefault() {
            return new FoodRegenSettings(
                DEFAULT_FOOD_REGEN_ENABLED,
                DEFAULT_FOOD_REGEN_UNLOCK_LEVEL,
                DEFAULT_TOWN_MEMBERS_ONLY,
                DEFAULT_FOOD_REGEN_INTERVAL_TICKS,
                DEFAULT_FOOD_REGEN_FOOD_POINTS,
                DEFAULT_FOOD_REGEN_SATURATION
            );
        }

        private static @NotNull FoodRegenSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new FoodRegenSettings(
                section.getBoolean("enabled", DEFAULT_FOOD_REGEN_ENABLED),
                section.getInt("unlock_level", DEFAULT_FOOD_REGEN_UNLOCK_LEVEL),
                section.getBoolean("town_members_only", DEFAULT_TOWN_MEMBERS_ONLY),
                section.getInt("interval_ticks", DEFAULT_FOOD_REGEN_INTERVAL_TICKS),
                section.getInt("food_points_per_pulse", DEFAULT_FOOD_REGEN_FOOD_POINTS),
                section.getDouble("saturation_per_pulse", DEFAULT_FOOD_REGEN_SATURATION)
            );
        }
    }

    /**
     * Immutable health-regeneration settings parsed from {@code medic.yml}.
     *
     * @param enabled whether health regeneration may run
     * @param unlockLevel Medic level that unlocks health regeneration
     * @param townMembersOnly whether only town members may receive the perk
     * @param intervalTicks interval between health pulses
     * @param healthPointsPerPulse health restored each pulse
     */
    public record HealthRegenSettings(
        boolean enabled,
        int unlockLevel,
        boolean townMembersOnly,
        int intervalTicks,
        double healthPointsPerPulse
    ) {

        /**
         * Creates one immutable health-regeneration settings snapshot.
         *
         * @param enabled whether health regeneration may run
         * @param unlockLevel Medic level that unlocks health regeneration
         * @param townMembersOnly whether only town members may receive the perk
         * @param intervalTicks interval between health pulses
         * @param healthPointsPerPulse health restored each pulse
         */
        public HealthRegenSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_HEALTH_REGEN_UNLOCK_LEVEL);
            intervalTicks = sanitizePositiveInt(intervalTicks, DEFAULT_HEALTH_REGEN_INTERVAL_TICKS);
            healthPointsPerPulse = sanitizePositiveDouble(healthPointsPerPulse, DEFAULT_HEALTH_REGEN_POINTS);
        }

        /**
         * Returns whether health regeneration is unlocked at the supplied Medic level.
         *
         * @param medicLevel Medic chunk level
         * @return {@code true} when health regeneration is unlocked
         */
        public boolean isUnlocked(final int medicLevel) {
            return medicLevel >= this.unlockLevel;
        }

        private static @NotNull HealthRegenSettings createDefault() {
            return new HealthRegenSettings(
                DEFAULT_HEALTH_REGEN_ENABLED,
                DEFAULT_HEALTH_REGEN_UNLOCK_LEVEL,
                DEFAULT_TOWN_MEMBERS_ONLY,
                DEFAULT_HEALTH_REGEN_INTERVAL_TICKS,
                DEFAULT_HEALTH_REGEN_POINTS
            );
        }

        private static @NotNull HealthRegenSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new HealthRegenSettings(
                section.getBoolean("enabled", DEFAULT_HEALTH_REGEN_ENABLED),
                section.getInt("unlock_level", DEFAULT_HEALTH_REGEN_UNLOCK_LEVEL),
                section.getBoolean("town_members_only", DEFAULT_TOWN_MEMBERS_ONLY),
                section.getInt("interval_ticks", DEFAULT_HEALTH_REGEN_INTERVAL_TICKS),
                section.getDouble("health_points_per_pulse", DEFAULT_HEALTH_REGEN_POINTS)
            );
        }
    }

    /**
     * Immutable harmful-effect cleanse settings parsed from {@code medic.yml}.
     *
     * @param enabled whether harmful effects may be removed
     * @param unlockLevel Medic level that unlocks cleansing
     * @param townMembersOnly whether only town members may receive the perk
     * @param intervalTicks interval between cleanse pulses
     * @param harmfulEffects normalized harmful effect keys removed by the cleanse pulse
     */
    public record CleanseSettings(
        boolean enabled,
        int unlockLevel,
        boolean townMembersOnly,
        int intervalTicks,
        @NotNull Set<String> harmfulEffects
    ) {

        /**
         * Creates one immutable cleanse settings snapshot.
         *
         * @param enabled whether harmful effects may be removed
         * @param unlockLevel Medic level that unlocks cleansing
         * @param townMembersOnly whether only town members may receive the perk
         * @param intervalTicks interval between cleanse pulses
         * @param harmfulEffects normalized harmful effect keys removed by the cleanse pulse
         */
        public CleanseSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_CLEANSE_UNLOCK_LEVEL);
            intervalTicks = sanitizePositiveInt(intervalTicks, DEFAULT_CLEANSE_INTERVAL_TICKS);
            harmfulEffects = copyHarmfulEffects(harmfulEffects);
        }

        /**
         * Returns whether cleansing is unlocked at the supplied Medic level.
         *
         * @param medicLevel Medic chunk level
         * @return {@code true} when cleansing is unlocked
         */
        public boolean isUnlocked(final int medicLevel) {
            return medicLevel >= this.unlockLevel;
        }

        /**
         * Returns the configured harmful effects in insertion order.
         *
         * @return copied harmful-effect keys
         */
        @Override
        public @NotNull Set<String> harmfulEffects() {
            return new LinkedHashSet<>(this.harmfulEffects);
        }

        private static @NotNull CleanseSettings createDefault() {
            return new CleanseSettings(
                DEFAULT_CLEANSE_ENABLED,
                DEFAULT_CLEANSE_UNLOCK_LEVEL,
                DEFAULT_TOWN_MEMBERS_ONLY,
                DEFAULT_CLEANSE_INTERVAL_TICKS,
                DEFAULT_HARMFUL_EFFECT_KEYS
            );
        }

        private static @NotNull CleanseSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new CleanseSettings(
                section.getBoolean("enabled", DEFAULT_CLEANSE_ENABLED),
                section.getInt("unlock_level", DEFAULT_CLEANSE_UNLOCK_LEVEL),
                section.getBoolean("town_members_only", DEFAULT_TOWN_MEMBERS_ONLY),
                section.getInt("interval_ticks", DEFAULT_CLEANSE_INTERVAL_TICKS),
                parseHarmfulEffects(section.getStringList("harmful_effects"))
            );
        }
    }

    /**
     * Immutable fortified-recovery settings parsed from {@code medic.yml}.
     *
     * @param enabled whether the timed fortified-recovery buff may run
     * @param unlockLevel Medic level that unlocks fortified recovery
     * @param townMembersOnly whether only town members may receive the perk
     * @param durationSeconds timed buff duration
     * @param refreshWhileInside whether the timer refreshes while the player remains inside the Medic chunk
     * @param targetMaxHealth target maximum health the buff should guarantee
     * @param upkeepIntervalTicks interval between upkeep pulses while the buff is active
     * @param targetFoodLevel food level maintained while the buff is active
     * @param targetSaturation saturation maintained while the buff is active
     */
    public record FortifiedRecoverySettings(
        boolean enabled,
        int unlockLevel,
        boolean townMembersOnly,
        int durationSeconds,
        boolean refreshWhileInside,
        double targetMaxHealth,
        int upkeepIntervalTicks,
        int targetFoodLevel,
        double targetSaturation
    ) {

        /**
         * Creates one immutable fortified-recovery settings snapshot.
         *
         * @param enabled whether the timed fortified-recovery buff may run
         * @param unlockLevel Medic level that unlocks fortified recovery
         * @param townMembersOnly whether only town members may receive the perk
         * @param durationSeconds timed buff duration
         * @param refreshWhileInside whether the timer refreshes while the player remains inside the Medic chunk
         * @param targetMaxHealth target maximum health the buff should guarantee
         * @param upkeepIntervalTicks interval between upkeep pulses while the buff is active
         * @param targetFoodLevel food level maintained while the buff is active
         * @param targetSaturation saturation maintained while the buff is active
         */
        public FortifiedRecoverySettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_FORTIFIED_RECOVERY_UNLOCK_LEVEL);
            durationSeconds = sanitizePositiveInt(durationSeconds, DEFAULT_FORTIFIED_RECOVERY_DURATION_SECONDS);
            targetMaxHealth = sanitizePositiveDouble(targetMaxHealth, DEFAULT_FORTIFIED_RECOVERY_TARGET_MAX_HEALTH);
            upkeepIntervalTicks = sanitizePositiveInt(
                upkeepIntervalTicks,
                DEFAULT_FORTIFIED_RECOVERY_UPKEEP_INTERVAL_TICKS
            );
            targetFoodLevel = sanitizeFoodLevel(targetFoodLevel, DEFAULT_FORTIFIED_RECOVERY_TARGET_FOOD_LEVEL);
            targetSaturation = sanitizeNonNegativeDouble(targetSaturation, DEFAULT_FORTIFIED_RECOVERY_TARGET_SATURATION);
        }

        /**
         * Returns whether fortified recovery is unlocked at the supplied Medic level.
         *
         * @param medicLevel Medic chunk level
         * @return {@code true} when fortified recovery is unlocked
         */
        public boolean isUnlocked(final int medicLevel) {
            return medicLevel >= this.unlockLevel;
        }

        private static @NotNull FortifiedRecoverySettings createDefault() {
            return new FortifiedRecoverySettings(
                DEFAULT_FORTIFIED_RECOVERY_ENABLED,
                DEFAULT_FORTIFIED_RECOVERY_UNLOCK_LEVEL,
                DEFAULT_TOWN_MEMBERS_ONLY,
                DEFAULT_FORTIFIED_RECOVERY_DURATION_SECONDS,
                DEFAULT_FORTIFIED_RECOVERY_REFRESH_WHILE_INSIDE,
                DEFAULT_FORTIFIED_RECOVERY_TARGET_MAX_HEALTH,
                DEFAULT_FORTIFIED_RECOVERY_UPKEEP_INTERVAL_TICKS,
                DEFAULT_FORTIFIED_RECOVERY_TARGET_FOOD_LEVEL,
                DEFAULT_FORTIFIED_RECOVERY_TARGET_SATURATION
            );
        }

        private static @NotNull FortifiedRecoverySettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new FortifiedRecoverySettings(
                section.getBoolean("enabled", DEFAULT_FORTIFIED_RECOVERY_ENABLED),
                section.getInt("unlock_level", DEFAULT_FORTIFIED_RECOVERY_UNLOCK_LEVEL),
                section.getBoolean("town_members_only", DEFAULT_TOWN_MEMBERS_ONLY),
                section.getInt("duration_seconds", DEFAULT_FORTIFIED_RECOVERY_DURATION_SECONDS),
                section.getBoolean("refresh_while_inside", DEFAULT_FORTIFIED_RECOVERY_REFRESH_WHILE_INSIDE),
                section.getDouble("target_max_health", DEFAULT_FORTIFIED_RECOVERY_TARGET_MAX_HEALTH),
                section.getInt("upkeep_interval_ticks", DEFAULT_FORTIFIED_RECOVERY_UPKEEP_INTERVAL_TICKS),
                section.getInt("target_food_level", DEFAULT_FORTIFIED_RECOVERY_TARGET_FOOD_LEVEL),
                section.getDouble("target_saturation", DEFAULT_FORTIFIED_RECOVERY_TARGET_SATURATION)
            );
        }
    }

    /**
     * Immutable emergency-refill settings parsed from {@code medic.yml}.
     *
     * @param enabled whether emergency refills may run
     * @param unlockLevel Medic level that unlocks emergency refills
     * @param townMembersOnly whether only town members may receive the perk
     * @param triggerOnEntry whether entry into the Medic chunk should immediately trigger a refill
     * @param cooldownSeconds cooldown between instant refills
     * @param targetHealthMode configured health target mode
     * @param targetFoodLevel food level restored by the refill
     * @param targetSaturation saturation restored by the refill
     */
    public record EmergencyRefillSettings(
        boolean enabled,
        int unlockLevel,
        boolean townMembersOnly,
        boolean triggerOnEntry,
        int cooldownSeconds,
        @NotNull TargetHealthMode targetHealthMode,
        int targetFoodLevel,
        double targetSaturation
    ) {

        /**
         * Creates one immutable emergency-refill settings snapshot.
         *
         * @param enabled whether emergency refills may run
         * @param unlockLevel Medic level that unlocks emergency refills
         * @param townMembersOnly whether only town members may receive the perk
         * @param triggerOnEntry whether entry into the Medic chunk should immediately trigger a refill
         * @param cooldownSeconds cooldown between instant refills
         * @param targetHealthMode configured health target mode
         * @param targetFoodLevel food level restored by the refill
         * @param targetSaturation saturation restored by the refill
         */
        public EmergencyRefillSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_EMERGENCY_REFILL_UNLOCK_LEVEL);
            cooldownSeconds = sanitizeNonNegativeInt(cooldownSeconds, DEFAULT_EMERGENCY_REFILL_COOLDOWN_SECONDS);
            targetHealthMode = Objects.requireNonNull(targetHealthMode, "targetHealthMode");
            targetFoodLevel = sanitizeFoodLevel(targetFoodLevel, DEFAULT_EMERGENCY_REFILL_TARGET_FOOD_LEVEL);
            targetSaturation = sanitizeNonNegativeDouble(targetSaturation, DEFAULT_EMERGENCY_REFILL_TARGET_SATURATION);
        }

        /**
         * Returns whether emergency refills are unlocked at the supplied Medic level.
         *
         * @param medicLevel Medic chunk level
         * @return {@code true} when emergency refills are unlocked
         */
        public boolean isUnlocked(final int medicLevel) {
            return medicLevel >= this.unlockLevel;
        }

        private static @NotNull EmergencyRefillSettings createDefault() {
            return new EmergencyRefillSettings(
                DEFAULT_EMERGENCY_REFILL_ENABLED,
                DEFAULT_EMERGENCY_REFILL_UNLOCK_LEVEL,
                DEFAULT_TOWN_MEMBERS_ONLY,
                DEFAULT_EMERGENCY_REFILL_TRIGGER_ON_ENTRY,
                DEFAULT_EMERGENCY_REFILL_COOLDOWN_SECONDS,
                DEFAULT_TARGET_HEALTH_MODE,
                DEFAULT_EMERGENCY_REFILL_TARGET_FOOD_LEVEL,
                DEFAULT_EMERGENCY_REFILL_TARGET_SATURATION
            );
        }

        private static @NotNull EmergencyRefillSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new EmergencyRefillSettings(
                section.getBoolean("enabled", DEFAULT_EMERGENCY_REFILL_ENABLED),
                section.getInt("unlock_level", DEFAULT_EMERGENCY_REFILL_UNLOCK_LEVEL),
                section.getBoolean("town_members_only", DEFAULT_TOWN_MEMBERS_ONLY),
                section.getBoolean("trigger_on_entry", DEFAULT_EMERGENCY_REFILL_TRIGGER_ON_ENTRY),
                section.getInt("cooldown_seconds", DEFAULT_EMERGENCY_REFILL_COOLDOWN_SECONDS),
                TargetHealthMode.fromValue(section.getString("target_health_mode"), DEFAULT_TARGET_HEALTH_MODE),
                section.getInt("target_food_level", DEFAULT_EMERGENCY_REFILL_TARGET_FOOD_LEVEL),
                section.getDouble("target_saturation", DEFAULT_EMERGENCY_REFILL_TARGET_SATURATION)
            );
        }
    }

    /**
     * Configured health target modes for emergency refills.
     */
    public enum TargetHealthMode {
        CURRENT_MAX,
        VANILLA_MAX;

        /**
         * Resolves one configured target-health mode with a fallback.
         *
         * @param rawValue raw configured string
         * @param fallback fallback mode
         * @return resolved target-health mode
         */
        public static @NotNull TargetHealthMode fromValue(
            final @Nullable String rawValue,
            final @NotNull TargetHealthMode fallback
        ) {
            if (rawValue == null || rawValue.isBlank()) {
                return Objects.requireNonNull(fallback, "fallback");
            }
            try {
                return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ignored) {
                return Objects.requireNonNull(fallback, "fallback");
            }
        }
    }

    private static int sanitizePositiveInt(final int value, final int fallback) {
        return value > 0 ? value : fallback;
    }

    private static int sanitizeFoodLevel(final int value, final int fallback) {
        if (value < 0) {
            return fallback;
        }
        return Math.min(20, value);
    }

    private static double sanitizePositiveDouble(final double value, final double fallback) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return fallback;
        }
        return value;
    }

    private static int sanitizeNonNegativeInt(final int value, final int fallback) {
        return value < 0 ? fallback : value;
    }

    private static double sanitizeNonNegativeDouble(final double value, final double fallback) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return fallback;
        }
        return value;
    }

    private static @NotNull Set<String> parseHarmfulEffects(final @Nullable Iterable<String> rawValues) {
        final LinkedHashSet<String> resolvedEffects = new LinkedHashSet<>();
        if (rawValues != null) {
            for (final String rawValue : rawValues) {
                final String normalized = normalizeEffectKey(rawValue);
                if (isSupportedHarmfulEffectKey(normalized)) {
                    resolvedEffects.add(normalized);
                }
            }
        }
        return resolvedEffects.isEmpty() ? copyHarmfulEffects(DEFAULT_HARMFUL_EFFECT_KEYS) : resolvedEffects;
    }

    private static @NotNull Set<String> copyHarmfulEffects(final @Nullable Set<String> harmfulEffects) {
        if (harmfulEffects == null || harmfulEffects.isEmpty()) {
            return new LinkedHashSet<>(DEFAULT_HARMFUL_EFFECT_KEYS);
        }
        final LinkedHashSet<String> copied = new LinkedHashSet<>();
        for (final String harmfulEffect : harmfulEffects) {
            final String normalized = normalizeEffectKey(harmfulEffect);
            if (isSupportedHarmfulEffectKey(normalized)) {
                copied.add(normalized);
            }
        }
        return copied.isEmpty() ? new LinkedHashSet<>(DEFAULT_HARMFUL_EFFECT_KEYS) : copied;
    }

    private static @NotNull String normalizeEffectKey(final @Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        return rawValue.trim()
            .toLowerCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_');
    }

    private static boolean isSupportedHarmfulEffectKey(final @NotNull String normalizedKey) {
        return !normalizedKey.isEmpty() && DEFAULT_HARMFUL_EFFECT_KEYS.contains(normalizedKey);
    }
}
