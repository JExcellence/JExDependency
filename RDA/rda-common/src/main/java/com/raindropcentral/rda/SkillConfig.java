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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable generic skill configuration loaded from a skill ability resource.
 *
 * <p>The configuration captures shared progression settings, prestige behavior, and the ordered
 * rate table used both for gameplay and menu presentation.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class SkillConfig {

    /**
     * Prestige progression trigger modes supported by skills.
     */
    public enum PrestigeTrigger {
        MANUAL
    }

    /**
     * Supported skill level formula types.
     */
    public enum LevelFormulaType {
        POWER
    }

    /**
     * Supported rounding strategies for skill formulas.
     */
    public enum LevelFormulaRounding {
        FLOOR
    }

    /**
     * Supported tool requirements for block-break rates.
     */
    public enum ToolRequirement {
        ANY,
        PICKAXE,
        AXE,
        SHOVEL,
        HOE;

        /**
         * Reports whether the supplied item satisfies the tool requirement.
         *
         * @param tool material to probe, or {@code null} when no tool is held
         * @return {@code true} when the material satisfies the requirement
         */
        public boolean matches(final @Nullable Material tool) {
            if (this == ANY) {
                return true;
            }

            if (tool == null || tool == Material.AIR) {
                return false;
            }

            final String name = tool.name();
            return switch (this) {
                case ANY -> true;
                case PICKAXE -> name.endsWith("_PICKAXE");
                case AXE -> name.endsWith("_AXE") && !name.endsWith("_PICKAXE");
                case SHOVEL -> name.endsWith("_SHOVEL");
                case HOE -> name.endsWith("_HOE");
            };
        }
    }

    /**
     * Supported projectile categories for archery-style rates.
     */
    public enum ProjectileKind {
        ARROW,
        TIPPED_ARROW,
        SPECTRAL_ARROW
    }

    /**
     * Represents the configured skill level formula.
     *
     * @param type progression formula type
     * @param baseXp base XP added to every next-level requirement
     * @param growth growth multiplier applied to the level term
     * @param exponent exponent applied to the level term
     * @param rounding rounding strategy used when resolving the requirement
     * @param formulaDoc documentation string shipped in configuration
     */
    public record LevelFormula(
        @NotNull LevelFormulaType type,
        double baseXp,
        double growth,
        double exponent,
        @NotNull LevelFormulaRounding rounding,
        @NotNull String formulaDoc
    ) {

        /**
         * Creates a level formula descriptor.
         *
         * @param type progression formula type
         * @param baseXp base XP added to every next-level requirement
         * @param growth growth multiplier applied to the level term
         * @param exponent exponent applied to the level term
         * @param rounding rounding strategy used when resolving the requirement
         * @param formulaDoc documentation string shipped in configuration
         */
        public LevelFormula {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(rounding, "rounding");
            Objects.requireNonNull(formulaDoc, "formulaDoc");
        }

        /**
         * Calculates the XP required to advance from the supplied level.
         *
         * @param level current internal skill level
         * @return XP required to reach the next level
         */
        public long xpToNextLevel(final int level) {
            final double resolved = this.baseXp + this.growth * Math.pow(level + 1.0D, this.exponent);
            return switch (this.rounding) {
                case FLOOR -> (long) Math.floor(resolved);
            };
        }
    }

    /**
     * Immutable rate entry used for both event routing and GUI rendering.
     *
     * @param key stable config key
     * @param label display label used by the GUI and gain messages
     * @param description longer description shown in the GUI when no translation key is configured
     * @param descriptionTranslationKey translation key used to localize the rate description
     * @param icon representative icon used by the GUI
     * @param triggerType owning trigger family
     * @param xp configured XP value per matching unit
     * @param materials optional material match set, limited to a single explicit target rate
     * @param entityTypes optional entity match set, limited to a single explicit target rate
     * @param requiredItems optional held-item match set, limited to a single explicit target rate
     * @param projectileKinds optional projectile category match set, limited to a single explicit target rate
     * @param requireFullyGrown whether ageable blocks must be mature
     * @param hostileOnly whether the victim must be hostile
     * @param playerOnly whether the victim must be a player
     * @param excludeSameTown whether same-town player interactions should be ignored
     * @param distance optional movement threshold used by agility travel rates
     * @param requiredTool required block-break tool family
     */
    public record RateDefinition(
        @NotNull String key,
        @NotNull String label,
        @NotNull String description,
        @Nullable String descriptionTranslationKey,
        @NotNull Material icon,
        @NotNull SkillTriggerType triggerType,
        int xp,
        @NotNull Set<Material> materials,
        @NotNull Set<EntityType> entityTypes,
        @NotNull Set<Material> requiredItems,
        @NotNull Set<ProjectileKind> projectileKinds,
        boolean requireFullyGrown,
        boolean hostileOnly,
        boolean playerOnly,
        boolean excludeSameTown,
        double distance,
        @NotNull ToolRequirement requiredTool
    ) {

        /**
         * Creates a rate definition.
         */
        public RateDefinition {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(triggerType, "triggerType");
            Objects.requireNonNull(materials, "materials");
            Objects.requireNonNull(entityTypes, "entityTypes");
            Objects.requireNonNull(requiredItems, "requiredItems");
            Objects.requireNonNull(projectileKinds, "projectileKinds");
            Objects.requireNonNull(requiredTool, "requiredTool");
            if (xp < 0) {
                throw new IllegalArgumentException("xp must be non-negative");
            }
            if (distance < 0.0D) {
                throw new IllegalArgumentException("distance must be non-negative");
            }
        }

        /**
         * Reports whether the rate applies to the supplied block context.
         *
         * @param material block type being broken
         * @param toolType tool used for the break
         * @param fullyGrown whether the block is fully grown
         * @return {@code true} when the rate matches the block context
         */
        public boolean matchesBlock(
            final @NotNull Material material,
            final @Nullable Material toolType,
            final boolean fullyGrown
        ) {
            return this.matchesMaterial(material)
                && this.requiredTool.matches(toolType)
                && (!this.requireFullyGrown || fullyGrown);
        }

        /**
         * Reports whether the rate applies to the supplied material.
         *
         * @param material material to probe
         * @return {@code true} when the material matches, or when the rate is a wildcard
         */
        public boolean matchesMaterial(final @NotNull Material material) {
            return this.materials.isEmpty() || this.materials.contains(Objects.requireNonNull(material, "material"));
        }

        /**
         * Reports whether the rate applies to the supplied combat victim.
         *
         * @param entityType victim type
         * @param hostile whether the victim is hostile
         * @param player whether the victim is a player
         * @return {@code true} when the victim matches the rate constraints
         */
        public boolean matchesEntity(
            final @NotNull EntityType entityType,
            final boolean hostile,
            final boolean player
        ) {
            if (this.hostileOnly && !hostile) {
                return false;
            }

            if (this.playerOnly && !player) {
                return false;
            }

            return this.entityTypes.isEmpty()
                || this.entityTypes.contains(Objects.requireNonNull(entityType, "entityType"));
        }

        /**
         * Reports whether the rate applies to the supplied projectile kind.
         *
         * @param projectileKind projectile category to probe
         * @return {@code true} when the projectile matches, or when the rate is a wildcard
         */
        public boolean matchesProjectileKind(final @Nullable ProjectileKind projectileKind) {
            return this.projectileKinds.isEmpty()
                || (projectileKind != null && this.projectileKinds.contains(projectileKind));
        }

        /**
         * Reports whether the rate applies to the supplied held item.
         *
         * @param requiredItem held material to probe
         * @return {@code true} when the held item matches, or when the rate is a wildcard
         */
        public boolean matchesRequiredItem(final @Nullable Material requiredItem) {
            return this.requiredItems.isEmpty()
                || (requiredItem != null && this.requiredItems.contains(requiredItem));
        }

        /**
         * Resolves the localized description shown for the rate.
         *
         * @param player player whose locale should be used
         * @return resolved rate description
         */
        public @NotNull String resolveDescription(final @NotNull Player player) {
            return ConfiguredTextResolver.resolvePlainText(player, this.descriptionTranslationKey, this.description);
        }
    }

    /**
     * Immutable ability tier entry derived from skill progression and core-stat spending.
     *
     * @param key stable tier key
     * @param requiredSkillLevel minimum skill level required to unlock the tier, or {@code 0} to disable the gate
     * @param requiredStatPoints minimum primary-stat points required to unlock the tier, or {@code 0} to disable the gate
     * @param baseValue base potency contributed by the tier
     * @param primaryCoefficient coefficient applied to the primary stat's allocated points
     * @param secondaryCoefficient coefficient applied to the secondary stat's allocated points when present
     * @param hardCap optional per-tier hard cap for the resolved potency, or {@code null} to disable the cap
     */
    public record AbilityTierDefinition(
        @NotNull String key,
        int requiredSkillLevel,
        int requiredStatPoints,
        double baseValue,
        double primaryCoefficient,
        double secondaryCoefficient,
        @Nullable Double hardCap
    ) {

        /**
         * Creates an ability tier definition.
         */
        public AbilityTierDefinition {
            Objects.requireNonNull(key, "key");
        }

        /**
         * Reports whether the supplied skill and stat values unlock this tier.
         *
         * @param skillLevel current skill level
         * @param primaryStatPoints spent points in the ability's primary stat
         * @return {@code true} when the tier is unlocked
         */
        public boolean isUnlocked(final int skillLevel, final int primaryStatPoints) {
            final boolean skillGatePassed = this.requiredSkillLevel <= 0 || skillLevel >= this.requiredSkillLevel;
            final boolean statGatePassed = this.requiredStatPoints <= 0 || primaryStatPoints >= this.requiredStatPoints;
            return skillGatePassed && statGatePassed;
        }

        /**
         * Resolves the tier potency for the supplied primary and secondary stat values.
         *
         * @param primaryStatPoints spent points in the ability's primary stat
         * @param secondaryStatPoints spent points in the ability's secondary stat
         * @return resolved tier potency after the optional hard cap
         */
        public double resolvePotency(final int primaryStatPoints, final int secondaryStatPoints) {
            final double resolved = this.baseValue
                + Math.max(0, primaryStatPoints) * this.primaryCoefficient
                + Math.max(0, secondaryStatPoints) * this.secondaryCoefficient;
            return this.hardCap == null ? resolved : Math.min(resolved, this.hardCap);
        }
    }

    /**
     * Trigger and resource settings for an active ability.
     *
     * @param cooldownSeconds cooldown applied after a successful cast
     * @param durationSeconds active duration after a successful cast
     * @param manaCost shared mana consumed on cast
     * @param activatorItem held item required for click-based activations
     * @param allowedActivationModes ability-specific trigger allowlist
     */
    public record ActiveAbilityConfig(
        int cooldownSeconds,
        int durationSeconds,
        double manaCost,
        @NotNull Material activatorItem,
        @NotNull Set<ActivationMode> allowedActivationModes
    ) {

        /**
         * Creates an active ability config.
         */
        public ActiveAbilityConfig {
            Objects.requireNonNull(activatorItem, "activatorItem");
            Objects.requireNonNull(allowedActivationModes, "allowedActivationModes");
        }
    }

    /**
     * Immutable ability definition displayed in stat and skill menus.
     *
     * @param key stable ability key
     * @param name display name
     * @param description human-readable function summary used by menus when no translation key is configured
     * @param descriptionTranslationKey translation key used to localize the ability description
     * @param icon representative icon used by menus
     * @param primaryStat primary scaling stat
     * @param secondaryStat optional secondary scaling stat
     * @param active whether the ability is the skill's active ability
     * @param tiers ordered ability tiers
     * @param activeConfig active-ability trigger config, or {@code null} for passives
     */
    public record AbilityDefinition(
        @NotNull String key,
        @NotNull String name,
        @NotNull String description,
        @Nullable String descriptionTranslationKey,
        @NotNull Material icon,
        @NotNull CoreStatType primaryStat,
        @Nullable CoreStatType secondaryStat,
        boolean active,
        @NotNull List<AbilityTierDefinition> tiers,
        @Nullable ActiveAbilityConfig activeConfig
    ) {

        /**
         * Creates an ability definition.
         */
        public AbilityDefinition {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(primaryStat, "primaryStat");
            Objects.requireNonNull(tiers, "tiers");
        }

        /**
         * Returns the highest unlocked tier for the supplied skill and stat values.
         *
         * @param skillLevel current skill level
         * @param primaryStatPoints spent points in the primary stat
         * @return highest unlocked tier, or {@code null} when none are unlocked yet
         */
        public @Nullable AbilityTierDefinition resolveTier(final int skillLevel, final int primaryStatPoints) {
            AbilityTierDefinition resolved = null;
            for (final AbilityTierDefinition tier : this.tiers) {
                if (tier.isUnlocked(skillLevel, primaryStatPoints)) {
                    resolved = tier;
                }
            }
            return resolved;
        }

        /**
         * Resolves the localized description shown for the ability.
         *
         * @param player player whose locale should be used
         * @return resolved ability description
         */
        public @NotNull String resolveDescription(final @NotNull Player player) {
            return ConfiguredTextResolver.resolvePlainText(player, this.descriptionTranslationKey, this.description);
        }
    }

    private final SkillType skillType;
    private final boolean enabled;
    private final Material displayIcon;
    private final int softMaxLevel;
    private final int maxPrestiges;
    private final PrestigeTrigger prestigeTrigger;
    private final int prestigeXpBonusPerPrestigePercent;
    private final LevelFormula levelFormula;
    private final List<RateDefinition> rates;
    private final int abilityPointIntervalOverride;
    private final List<AbilityDefinition> passiveAbilities;
    private final @Nullable AbilityDefinition activeAbility;

    /**
     * Creates a skill configuration instance.
     *
     * @param skillType owning skill type
     * @param enabled whether the skill progression is active
     * @param displayIcon icon shown for skill menus
     * @param softMaxLevel soft level cap before overlevels require final prestige completion
     * @param maxPrestiges maximum prestige count before overlevels begin
     * @param prestigeTrigger prestige trigger mode
     * @param prestigeXpBonusPerPrestigePercent XP bonus applied per completed prestige
     * @param levelFormula configured level formula
     * @param rates ordered configured rates
     * @param abilityPointIntervalOverride per-skill build-point interval override, or {@code 0} to use the shared default
     * @param passiveAbilities configured passive abilities
     * @param activeAbility configured active ability, or {@code null} when absent
     */
    public SkillConfig(
        final @NotNull SkillType skillType,
        final boolean enabled,
        final @NotNull Material displayIcon,
        final int softMaxLevel,
        final int maxPrestiges,
        final @NotNull PrestigeTrigger prestigeTrigger,
        final int prestigeXpBonusPerPrestigePercent,
        final @NotNull LevelFormula levelFormula,
        final @NotNull List<RateDefinition> rates,
        final int abilityPointIntervalOverride,
        final @NotNull List<AbilityDefinition> passiveAbilities,
        final @Nullable AbilityDefinition activeAbility
    ) {
        this.skillType = Objects.requireNonNull(skillType, "skillType");
        this.enabled = enabled;
        this.displayIcon = Objects.requireNonNull(displayIcon, "displayIcon");
        this.softMaxLevel = softMaxLevel;
        this.maxPrestiges = maxPrestiges;
        this.prestigeTrigger = Objects.requireNonNull(prestigeTrigger, "prestigeTrigger");
        this.prestigeXpBonusPerPrestigePercent = prestigeXpBonusPerPrestigePercent;
        this.levelFormula = Objects.requireNonNull(levelFormula, "levelFormula");
        this.rates = List.copyOf(Objects.requireNonNull(rates, "rates"));
        this.abilityPointIntervalOverride = Math.max(0, abilityPointIntervalOverride);
        this.passiveAbilities = List.copyOf(Objects.requireNonNull(passiveAbilities, "passiveAbilities"));
        this.activeAbility = activeAbility;
    }

    /**
     * Returns the owning skill type.
     *
     * @return owning skill type
     */
    public @NotNull SkillType getSkillType() {
        return this.skillType;
    }

    /**
     * Reports whether the skill progression is active.
     *
     * @return {@code true} when the skill is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Returns the configured skill icon.
     *
     * @return skill menu icon
     */
    public @NotNull Material getDisplayIcon() {
        return this.displayIcon;
    }

    /**
     * Returns the configured soft level cap.
     *
     * @return soft max level
     */
    public int getSoftMaxLevel() {
        return this.softMaxLevel;
    }

    /**
     * Returns the configured maximum prestige count.
     *
     * @return maximum prestige count
     */
    public int getMaxPrestiges() {
        return this.maxPrestiges;
    }

    /**
     * Returns the configured prestige trigger.
     *
     * @return prestige trigger mode
     */
    public @NotNull PrestigeTrigger getPrestigeTrigger() {
        return this.prestigeTrigger;
    }

    /**
     * Returns the configured XP bonus percent granted per prestige.
     *
     * @return XP bonus percent per prestige
     */
    public int getPrestigeXpBonusPerPrestigePercent() {
        return this.prestigeXpBonusPerPrestigePercent;
    }

    /**
     * Returns the configured level formula.
     *
     * @return level formula
     */
    public @NotNull LevelFormula getLevelFormula() {
        return this.levelFormula;
    }

    /**
     * Returns the ordered configured rate table used by gameplay and the GUI.
     *
     * @return ordered rate table
     */
    public @NotNull List<RateDefinition> getRates() {
        return this.rates;
    }

    /**
     * Returns the per-skill build-point interval override.
     *
     * @return interval override, or {@code 0} when the shared default should be used
     */
    public int getAbilityPointIntervalOverride() {
        return this.abilityPointIntervalOverride;
    }

    /**
     * Returns the configured passive abilities for the skill.
     *
     * @return configured passive abilities
     */
    public @NotNull List<AbilityDefinition> getPassiveAbilities() {
        return this.passiveAbilities;
    }

    /**
     * Returns the configured active ability for the skill.
     *
     * @return configured active ability, or {@code null} when absent
     */
    public @Nullable AbilityDefinition getActiveAbility() {
        return this.activeAbility;
    }

    /**
     * Returns every configured ability for the skill in UI order.
     *
     * @return passive abilities followed by the active ability when present
     */
    public @NotNull List<AbilityDefinition> getAllAbilities() {
        if (this.activeAbility == null) {
            return this.passiveAbilities;
        }

        final ArrayList<AbilityDefinition> abilities = new ArrayList<>(this.passiveAbilities);
        abilities.add(this.activeAbility);
        return List.copyOf(abilities);
    }

    /**
     * Returns all configured rates for the supplied trigger family in insertion order.
     *
     * @param triggerType trigger family to resolve
     * @return matching rate definitions
     */
    public @NotNull List<RateDefinition> getRatesByTrigger(final @NotNull SkillTriggerType triggerType) {
        Objects.requireNonNull(triggerType, "triggerType");
        return this.rates.stream().filter(rate -> rate.triggerType() == triggerType).toList();
    }

    /**
     * Returns the tracked block materials owned by this skill.
     *
     * @return tracked block materials
     */
    public @NotNull Set<Material> getTrackedMaterials() {
        final LinkedHashSet<Material> trackedMaterials = new LinkedHashSet<>();
        for (final RateDefinition rate : this.getRatesByTrigger(SkillTriggerType.BLOCK_BREAK)) {
            trackedMaterials.addAll(rate.materials());
        }
        return Collections.unmodifiableSet(trackedMaterials);
    }

    /**
     * Formats a material name into a readable display label.
     *
     * @param material material to format
     * @return title-cased material name
     */
    public static @NotNull String formatMaterialName(final @NotNull Material material) {
        return List.of(material.name().split("_")).stream()
            .map(part -> part.substring(0, 1) + part.substring(1).toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(" "));
    }

    /**
     * Formats an arbitrary config key into a readable label.
     *
     * @param key config key to format
     * @return title-cased key text
     */
    public static @NotNull String formatKeyLabel(final @NotNull String key) {
        final List<String> parts = new ArrayList<>();
        for (final String rawPart : Objects.requireNonNull(key, "key").split("[-_]")) {
            if (rawPart.isBlank()) {
                continue;
            }
            parts.add(rawPart.substring(0, 1).toUpperCase(Locale.ROOT) + rawPart.substring(1).toLowerCase(Locale.ROOT));
        }
        return String.join(" ", parts);
    }
}
