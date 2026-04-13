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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable global stats and build-system configuration for RDA.
 *
 * <p>The config owns point-entitlement intervals, stat passive formulas, mana defaults, manual
 * respec settings, and the server allowlist for active-ability trigger modes.</p>
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class StatsConfig {

    /**
     * Static linear-soft-cap passive formula shared by every core stat.
     *
     * @param baseValue base passive value before any points are spent
     * @param fullRate gain applied per point before the first breakpoint
     * @param halfRate gain applied per point between the first and second breakpoints
     * @param quarterRate gain applied per point after the second breakpoint
     * @param firstSoftCap first breakpoint where returns taper
     * @param secondSoftCap second breakpoint where returns taper again
     * @param maxValue optional hard cap; {@code null} disables the cap
     */
    public record LinearSoftCapFormula(
        double baseValue,
        double fullRate,
        double halfRate,
        double quarterRate,
        int firstSoftCap,
        int secondSoftCap,
        @Nullable Double maxValue
    ) {

        /**
         * Creates a passive formula.
         */
        public LinearSoftCapFormula {
            if (firstSoftCap < 0 || secondSoftCap < firstSoftCap) {
                throw new IllegalArgumentException("Soft caps must be non-negative and ordered.");
            }
        }

        /**
         * Resolves the effective passive value for the supplied allocated points.
         *
         * @param allocatedPoints spent points in the owning stat
         * @return resolved passive value after soft caps
         */
        public double resolveValue(final int allocatedPoints) {
            final int clampedPoints = Math.max(0, allocatedPoints);
            final int fullBandPoints = Math.min(clampedPoints, this.firstSoftCap);
            final int halfBandPoints = Math.max(0, Math.min(clampedPoints, this.secondSoftCap) - this.firstSoftCap);
            final int quarterBandPoints = Math.max(0, clampedPoints - this.secondSoftCap);
            final double resolved = this.baseValue
                + fullBandPoints * this.fullRate
                + halfBandPoints * this.halfRate
                + quarterBandPoints * this.quarterRate;
            return this.maxValue == null ? resolved : Math.min(resolved, this.maxValue);
        }
    }

    /**
     * Per-stat menu and passive metadata.
     *
     * @param statType owning stat
     * @param icon item icon shown in menus
     * @param loreDescription short identity summary shown in menus when no translation key is configured
     * @param loreDescriptionTranslationKey translation key used to localize the stat description
     * @param passiveLabel display label for the passive bonus
     * @param passiveUnit suffix/unit text shown in menus
     * @param passiveFormula passive value formula
     */
    public record StatDefinition(
        @NotNull CoreStatType statType,
        @NotNull Material icon,
        @NotNull String loreDescription,
        @Nullable String loreDescriptionTranslationKey,
        @NotNull String passiveLabel,
        @NotNull String passiveUnit,
        @NotNull LinearSoftCapFormula passiveFormula
    ) {

        /**
         * Creates a stat definition.
         */
        public StatDefinition {
            Objects.requireNonNull(statType, "statType");
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(loreDescription, "loreDescription");
            Objects.requireNonNull(passiveLabel, "passiveLabel");
            Objects.requireNonNull(passiveUnit, "passiveUnit");
            Objects.requireNonNull(passiveFormula, "passiveFormula");
        }

        /**
         * Resolves the passive bonus value for the supplied spent points.
         *
         * @param allocatedPoints spent points in the stat
         * @return resolved passive bonus
         */
        public double resolvePassiveValue(final int allocatedPoints) {
            return this.passiveFormula.resolveValue(allocatedPoints);
        }

        /**
         * Resolves the localized stat description for the supplied player.
         *
         * @param player player whose locale should be used
         * @return resolved stat description
         */
        public @NotNull String resolveLoreDescription(final @NotNull org.bukkit.entity.Player player) {
            return ConfiguredTextResolver.resolvePlainText(player, this.loreDescriptionTranslationKey, this.loreDescription);
        }
    }

    /**
     * Respec cooldown and tax configuration.
     *
     * @param cooldownSeconds cooldown applied after a manual respec
     * @param pointTaxPercent percentage of total skill progression removed before points are recalculated
     */
    public record RespecSettings(long cooldownSeconds, int pointTaxPercent) {

        /**
         * Creates a respec settings descriptor.
         */
        public RespecSettings {
            if (cooldownSeconds < 0L || pointTaxPercent < 0) {
                throw new IllegalArgumentException("Respec settings must be non-negative.");
            }
        }
    }

    /**
     * Shared mana defaults and SPI scaling configuration.
     *
     * @param baseMana base maximum mana before SPI points are applied
     * @param manaPerSpiPoint mana granted per spent SPI point
     * @param baseRegenPerSecond base mana regenerated per second
     * @param regenPerSpiPoint mana regen granted per spent SPI point
     * @param defaultDisplayMode default player HUD mode for mana
     * @param lowThresholdPercent percentage considered "low mana" for warnings
     */
    public record ManaSettings(
        double baseMana,
        double manaPerSpiPoint,
        double baseRegenPerSecond,
        double regenPerSpiPoint,
        @NotNull ManaDisplayMode defaultDisplayMode,
        double lowThresholdPercent
    ) {

        /**
         * Creates mana settings.
         */
        public ManaSettings {
            Objects.requireNonNull(defaultDisplayMode, "defaultDisplayMode");
        }

        /**
         * Resolves the maximum mana for the supplied SPI points.
         *
         * @param spiPoints spent SPI points
         * @return resolved maximum mana
         */
        public double resolveMaxMana(final int spiPoints) {
            return this.baseMana + Math.max(0, spiPoints) * this.manaPerSpiPoint;
        }

        /**
         * Resolves the mana regenerated each second for the supplied SPI points.
         *
         * @param spiPoints spent SPI points
         * @return resolved mana regen per second
         */
        public double resolveManaRegenPerSecond(final int spiPoints) {
            return this.baseRegenPerSecond + Math.max(0, spiPoints) * this.regenPerSpiPoint;
        }
    }

    private final int defaultAbilityPointInterval;
    private final EnumMap<SkillType, Integer> perSkillPointIntervals;
    private final EnumMap<CoreStatType, StatDefinition> statDefinitions;
    private final EnumSet<ActivationMode> allowedActivationModes;
    private final RespecSettings respecSettings;
    private final ManaSettings manaSettings;

    /**
     * Creates the immutable stats configuration.
     *
     * @param defaultAbilityPointInterval default skill-level interval that grants one build point
     * @param perSkillPointIntervals per-skill interval overrides
     * @param statDefinitions configured stat definitions
     * @param allowedActivationModes globally allowed active trigger modes
     * @param respecSettings manual respec settings
     * @param manaSettings shared mana settings
     */
    public StatsConfig(
        final int defaultAbilityPointInterval,
        final @NotNull Map<SkillType, Integer> perSkillPointIntervals,
        final @NotNull Map<CoreStatType, StatDefinition> statDefinitions,
        final @NotNull Set<ActivationMode> allowedActivationModes,
        final @NotNull RespecSettings respecSettings,
        final @NotNull ManaSettings manaSettings
    ) {
        this.defaultAbilityPointInterval = Math.max(1, defaultAbilityPointInterval);
        this.perSkillPointIntervals = new EnumMap<>(Objects.requireNonNull(perSkillPointIntervals, "perSkillPointIntervals"));
        this.statDefinitions = new EnumMap<>(Objects.requireNonNull(statDefinitions, "statDefinitions"));
        this.allowedActivationModes = EnumSet.copyOf(Objects.requireNonNull(allowedActivationModes, "allowedActivationModes"));
        this.respecSettings = Objects.requireNonNull(respecSettings, "respecSettings");
        this.manaSettings = Objects.requireNonNull(manaSettings, "manaSettings");
    }

    /**
     * Returns the default interval that grants one ability point.
     *
     * @return default point interval
     */
    public int getDefaultAbilityPointInterval() {
        return this.defaultAbilityPointInterval;
    }

    /**
     * Returns the resolved point interval for the supplied skill.
     *
     * @param skillType skill whose interval should be resolved
     * @return point interval for the skill
     */
    public int getAbilityPointInterval(final @NotNull SkillType skillType) {
        return Math.max(1, this.perSkillPointIntervals.getOrDefault(
            Objects.requireNonNull(skillType, "skillType"),
            this.defaultAbilityPointInterval
        ));
    }

    /**
     * Returns the configured stat definition.
     *
     * @param coreStatType stat to resolve
     * @return configured stat definition
     */
    public @NotNull StatDefinition getStatDefinition(final @NotNull CoreStatType coreStatType) {
        final StatDefinition statDefinition = this.statDefinitions.get(Objects.requireNonNull(coreStatType, "coreStatType"));
        if (statDefinition == null) {
            throw new IllegalStateException("Missing stat definition for " + coreStatType.getId());
        }
        return statDefinition;
    }

    /**
     * Returns the globally allowed trigger modes.
     *
     * @return allowed activation modes
     */
    public @NotNull Set<ActivationMode> getAllowedActivationModes() {
        return EnumSet.copyOf(this.allowedActivationModes);
    }

    /**
     * Returns the manual respec settings.
     *
     * @return respec settings
     */
    public @NotNull RespecSettings getRespecSettings() {
        return this.respecSettings;
    }

    /**
     * Returns the shared mana settings.
     *
     * @return mana settings
     */
    public @NotNull ManaSettings getManaSettings() {
        return this.manaSettings;
    }
}
