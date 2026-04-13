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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads generic skill configurations from the plugin data folder.
 *
 * <p>The loader supports both the new ordered {@code rates} schema and the legacy
 * {@code block-xp} schema used by mining and woodcutting.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class SkillConfigLoader {

    private static final String DEFAULT_FORMULA_DOC =
        "xp_to_next(level) = floor(base_xp + growth * pow(level + 1, exponent))";

    private final JavaPlugin plugin;
    private final SkillType skillType;

    /**
     * Creates a new skill configuration loader.
     *
     * @param plugin owning plugin
     * @param skillType skill whose configuration should be loaded
     */
    public SkillConfigLoader(
        final @NotNull JavaPlugin plugin,
        final @NotNull SkillType skillType
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.skillType = Objects.requireNonNull(skillType, "skillType");
    }

    /**
     * Loads and validates the skill configuration.
     *
     * @return resolved skill configuration
     */
    public @NotNull SkillConfig load() {
        final File configFile = this.resolveConfigFile();
        return parseConfiguration(this.skillType, YamlConfiguration.loadConfiguration(configFile), configFile.getPath());
    }

    private @NotNull File resolveConfigFile() {
        final File configFile = new File(this.plugin.getDataFolder(), this.skillType.getResourcePath());
        if (configFile.isFile()) {
            return configFile;
        }

        final File legacyConfigFile = new File(this.plugin.getDataFolder(), this.skillType.getLegacyResourcePath());
        return legacyConfigFile.isFile() ? legacyConfigFile : configFile;
    }

    /**
     * Parses a skill configuration from YAML.
     *
     * @param skillType owning skill type
     * @param configuration parsed YAML configuration
     * @param sourceName source description used in validation errors
     * @return resolved skill configuration
     */
    static @NotNull SkillConfig parseConfiguration(
        final @NotNull SkillType skillType,
        final @NotNull YamlConfiguration configuration,
        final @NotNull String sourceName
    ) {
        Objects.requireNonNull(skillType, "skillType");
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(sourceName, "sourceName");

        final boolean enabled = configuration.getBoolean("enabled", true);
        final Material displayIcon = resolveMaterial(
            configuration.getString("display.icon", skillType.getFallbackIcon().name()),
            "display.icon",
            sourceName
        );
        final int softMaxLevel = requireNonNegative(
            configuration.getInt("progression.soft-max-level", 1000),
            "progression.soft-max-level",
            sourceName
        );
        final int maxPrestiges = requireNonNegative(
            configuration.getInt("progression.max-prestiges", 10),
            "progression.max-prestiges",
            sourceName
        );
        final SkillConfig.PrestigeTrigger prestigeTrigger = resolveEnum(
            SkillConfig.PrestigeTrigger.class,
            configuration.getString("progression.prestige.trigger", "MANUAL"),
            "progression.prestige.trigger",
            sourceName
        );
        final int prestigeXpBonus = requireNonNegative(
            configuration.getInt("progression.prestige.xp-bonus-per-prestige-percent", 10),
            "progression.prestige.xp-bonus-per-prestige-percent",
            sourceName
        );
        final SkillConfig.LevelFormulaType formulaType = resolveEnum(
            SkillConfig.LevelFormulaType.class,
            configuration.getString("progression.level-formula.type", "POWER"),
            "progression.level-formula.type",
            sourceName
        );
        final double baseXp = configuration.getDouble("progression.level-formula.base-xp", 50.0D);
        final double growth = configuration.getDouble("progression.level-formula.growth", 4.0D);
        final double exponent = configuration.getDouble("progression.level-formula.exponent", 1.20D);
        final SkillConfig.LevelFormulaRounding rounding = resolveEnum(
            SkillConfig.LevelFormulaRounding.class,
            configuration.getString("progression.level-formula.rounding", "FLOOR"),
            "progression.level-formula.rounding",
            sourceName
        );
        final String formulaDoc = configuration.getString(
            "progression.level-formula.formula-doc",
            DEFAULT_FORMULA_DOC
        );
        final int abilityPointIntervalOverride = requireNonNegative(
            configuration.getInt("ability-point-interval-override", 0),
            "ability-point-interval-override",
            sourceName
        );

        final List<SkillConfig.RateDefinition> rates = parseRates(skillType, configuration, sourceName);
        if (rates.isEmpty()) {
            throw new IllegalStateException("Missing rates or block-xp entries in " + sourceName);
        }
        final AbilityTreeParseResult abilityTreeParseResult = parseAbilityTree(configuration, skillType, sourceName);

        return new SkillConfig(
            skillType,
            enabled,
            displayIcon,
            softMaxLevel,
            maxPrestiges,
            prestigeTrigger,
            prestigeXpBonus,
            new SkillConfig.LevelFormula(formulaType, baseXp, growth, exponent, rounding, formulaDoc),
            rates,
            abilityPointIntervalOverride,
            abilityTreeParseResult.passiveAbilities(),
            abilityTreeParseResult.activeAbility()
        );
    }

    /**
     * Validates that enabled skills do not own overlapping block-break materials.
     *
     * @param configurations configurations keyed by skill type
     */
    static void validateTrackedMaterialUniqueness(final @NotNull Map<SkillType, SkillConfig> configurations) {
        Objects.requireNonNull(configurations, "configurations");
        final EnumMap<Material, SkillType> materialOwners = new EnumMap<>(Material.class);
        for (final Map.Entry<SkillType, SkillConfig> entry : configurations.entrySet()) {
            final SkillType skillType = entry.getKey();
            final SkillConfig configuration = entry.getValue();
            if (skillType == null || configuration == null || !configuration.isEnabled()) {
                continue;
            }

            for (final Material material : configuration.getTrackedMaterials()) {
                final SkillType existingOwner = materialOwners.putIfAbsent(material, skillType);
                if (existingOwner != null) {
                    throw new IllegalStateException(
                        "Tracked material " + material.name()
                            + " is configured for both "
                            + existingOwner.getId()
                            + " and "
                            + skillType.getId()
                    );
                }
            }
        }
    }

    private static @NotNull List<SkillConfig.RateDefinition> parseRates(
        final @NotNull SkillType skillType,
        final @NotNull YamlConfiguration configuration,
        final @NotNull String sourceName
    ) {
        final ConfigurationSection ratesSection = configuration.getConfigurationSection("rates");
        if (ratesSection != null) {
            final List<SkillConfig.RateDefinition> rates = new ArrayList<>();
            for (final String key : ratesSection.getKeys(false)) {
                final ConfigurationSection rateSection = ratesSection.getConfigurationSection(key);
                if (rateSection == null) {
                    throw new IllegalStateException("Missing rate section for " + key + " in " + sourceName);
                }

                final Set<Material> materials = resolveSingleMaterialFilter(
                    rateSection,
                    "material",
                    "materials",
                    "rates." + key,
                    sourceName
                );
                rates.add(new SkillConfig.RateDefinition(
                    key,
                    rateSection.getString("label", SkillConfig.formatKeyLabel(key)),
                    rateSection.getString("description", ""),
                    rateSection.getString("description-key"),
                    resolveMaterial(
                        rateSection.getString("icon", resolveDefaultRateIcon(skillType, materials)),
                        "rates." + key + ".icon",
                        sourceName
                    ),
                    resolveEnum(
                        SkillTriggerType.class,
                        rateSection.getString("trigger"),
                        "rates." + key + ".trigger",
                        sourceName
                    ),
                    requireNonNegative(rateSection.getInt("xp"), "rates." + key + ".xp", sourceName),
                    materials,
                    resolveSingleEntityTypeFilter(rateSection, "entity-type", "entity-types", "rates." + key, sourceName),
                    resolveSingleMaterialFilter(rateSection, "required-item", "required-items", "rates." + key, sourceName),
                    resolveSingleProjectileKindFilter(
                        rateSection,
                        "projectile-kind",
                        "projectile-kinds",
                        "rates." + key,
                        sourceName
                    ),
                    rateSection.getBoolean("require-fully-grown", false),
                    rateSection.getBoolean("hostile-only", false),
                    rateSection.getBoolean("player-only", false),
                    rateSection.getBoolean("exclude-same-town", false),
                    Math.max(0.0D, rateSection.getDouble("distance", 0.0D)),
                    resolveEnum(
                        SkillConfig.ToolRequirement.class,
                        rateSection.getString("required-tool", "ANY"),
                        "rates." + key + ".required-tool",
                        sourceName
                    )
                ));
            }
            return List.copyOf(rates);
        }

        final ConfigurationSection blockXpSection = configuration.getConfigurationSection("block-xp");
        if (blockXpSection == null) {
            return List.of();
        }

        final List<SkillConfig.RateDefinition> legacyRates = new ArrayList<>();
        for (final String materialKey : blockXpSection.getKeys(false)) {
            final Material material = resolveMaterial(materialKey, "block-xp." + materialKey, sourceName);
            legacyRates.add(new SkillConfig.RateDefinition(
                material.name().toLowerCase(Locale.ROOT),
                SkillConfig.formatMaterialName(material),
                "Break " + SkillConfig.formatMaterialName(material) + " to gain XP.",
                null,
                material,
                SkillTriggerType.BLOCK_BREAK,
                requireNonNegative(blockXpSection.getInt(materialKey), "block-xp." + materialKey, sourceName),
                EnumSet.of(material),
                EnumSet.noneOf(EntityType.class),
                EnumSet.noneOf(Material.class),
                EnumSet.noneOf(SkillConfig.ProjectileKind.class),
                false,
                false,
                false,
                false,
                0.0D,
                SkillConfig.ToolRequirement.ANY
            ));
        }
        return List.copyOf(legacyRates);
    }

    private static @NotNull AbilityTreeParseResult parseAbilityTree(
        final @NotNull YamlConfiguration configuration,
        final @NotNull SkillType skillType,
        final @NotNull String sourceName
    ) {
        final List<SkillConfig.AbilityDefinition> passiveAbilities = new ArrayList<>();
        final ConfigurationSection passiveSection = configuration.getConfigurationSection("tree.passives");
        if (passiveSection != null) {
            for (final String key : passiveSection.getKeys(false)) {
                final ConfigurationSection abilitySection = passiveSection.getConfigurationSection(key);
                if (abilitySection == null) {
                    throw new IllegalStateException("Missing passive ability section for " + key + " in " + sourceName);
                }
                passiveAbilities.add(parseAbilityDefinition(abilitySection, key, skillType, sourceName, false));
            }
        }

        final ConfigurationSection activeSection = configuration.getConfigurationSection("tree.active");
        final SkillConfig.AbilityDefinition activeAbility = activeSection == null
            ? null
            : parseAbilityDefinition(activeSection, "active", skillType, sourceName, true);
        return new AbilityTreeParseResult(List.copyOf(passiveAbilities), activeAbility);
    }

    private static @NotNull SkillConfig.AbilityDefinition parseAbilityDefinition(
        final @NotNull ConfigurationSection abilitySection,
        final @NotNull String fallbackKey,
        final @NotNull SkillType skillType,
        final @NotNull String sourceName,
        final boolean active
    ) {
        final String pathRoot = "tree." + (active ? "active" : "passives." + fallbackKey);
        final String key = abilitySection.getString("key", fallbackKey);
        final CoreStatType primaryStat = resolveEnum(
            CoreStatType.class,
            abilitySection.getString("primary-stat"),
            pathRoot + ".primary-stat",
            sourceName
        );
        final String rawSecondaryStat = abilitySection.getString("secondary-stat");
        final CoreStatType secondaryStat = rawSecondaryStat == null || rawSecondaryStat.isBlank()
            ? null
            : resolveEnum(CoreStatType.class, rawSecondaryStat, pathRoot + ".secondary-stat", sourceName);
        final ConfigurationSection tiersSection = abilitySection.getConfigurationSection("tiers");
        if (tiersSection == null || tiersSection.getKeys(false).isEmpty()) {
            throw new IllegalStateException("Missing tiers for " + pathRoot + " in " + sourceName);
        }

        final List<SkillConfig.AbilityTierDefinition> tiers = new ArrayList<>();
        for (final String tierKey : tiersSection.getKeys(false)) {
            final ConfigurationSection tierSection = tiersSection.getConfigurationSection(tierKey);
            if (tierSection == null) {
                throw new IllegalStateException("Missing tier section for " + pathRoot + "." + tierKey + " in " + sourceName);
            }

            tiers.add(new SkillConfig.AbilityTierDefinition(
                tierKey,
                requireNonNegative(
                    tierSection.getInt("required-skill-level", 0),
                    pathRoot + ".tiers." + tierKey + ".required-skill-level",
                    sourceName
                ),
                requireNonNegative(
                    tierSection.getInt("required-stat-points", 0),
                    pathRoot + ".tiers." + tierKey + ".required-stat-points",
                    sourceName
                ),
                tierSection.getDouble("base-value", 0.0D),
                tierSection.getDouble("primary-coefficient", 0.0D),
                tierSection.getDouble("secondary-coefficient", 0.0D),
                tierSection.contains("hard-cap") ? tierSection.getDouble("hard-cap") : null
            ));
        }

        final SkillConfig.ActiveAbilityConfig activeConfig;
        if (active) {
            final ConfigurationSection activeConfigSection = abilitySection.getConfigurationSection("active");
            if (activeConfigSection == null) {
                throw new IllegalStateException("Missing active config for " + pathRoot + " in " + sourceName);
            }

            final EnumSet<ActivationMode> allowedActivationModes = EnumSet.noneOf(ActivationMode.class);
            final List<String> rawModes = activeConfigSection.getStringList("allowed-triggers");
            if (rawModes.isEmpty()) {
                allowedActivationModes.addAll(EnumSet.allOf(ActivationMode.class));
            } else {
                for (final String rawMode : rawModes) {
                    allowedActivationModes.add(resolveEnum(
                        ActivationMode.class,
                        rawMode,
                        pathRoot + ".active.allowed-triggers",
                        sourceName
                    ));
                }
            }

            activeConfig = new SkillConfig.ActiveAbilityConfig(
                requireNonNegative(
                    activeConfigSection.getInt("cooldown-seconds", 0),
                    pathRoot + ".active.cooldown-seconds",
                    sourceName
                ),
                requireNonNegative(
                    activeConfigSection.getInt("duration-seconds", 0),
                    pathRoot + ".active.duration-seconds",
                    sourceName
                ),
                Math.max(0.0D, activeConfigSection.getDouble("mana-cost", 0.0D)),
                resolveMaterial(
                    activeConfigSection.getString("activator-item", skillType.getFallbackIcon().name()),
                    pathRoot + ".active.activator-item",
                    sourceName
                ),
                allowedActivationModes
            );
        } else {
            activeConfig = null;
        }

        return new SkillConfig.AbilityDefinition(
            key,
            abilitySection.getString("name", SkillConfig.formatKeyLabel(key)),
            abilitySection.getString("description", ""),
            abilitySection.getString("description-key"),
            resolveMaterial(
                abilitySection.getString("icon", skillType.getFallbackIcon().name()),
                pathRoot + ".icon",
                sourceName
            ),
            primaryStat,
            secondaryStat,
            active,
            List.copyOf(tiers),
            activeConfig
        );
    }

    private static @NotNull String resolveDefaultRateIcon(
        final @NotNull SkillType skillType,
        final @NotNull Set<Material> materials
    ) {
        return materials.isEmpty() ? skillType.getFallbackIcon().name() : materials.iterator().next().name();
    }

    private static @NotNull Set<Material> resolveSingleMaterialFilter(
        final @NotNull ConfigurationSection section,
        final @NotNull String singularKey,
        final @NotNull String pluralKey,
        final @NotNull String pathRoot,
        final @NotNull String sourceName
    ) {
        final String singularValue = section.getString(singularKey);
        final List<String> pluralValues = thisOrEmpty(section.getStringList(pluralKey));
        if (singularValue != null && !singularValue.isBlank() && !pluralValues.isEmpty()) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        if (singularValue != null && !singularValue.isBlank()) {
            return EnumSet.of(resolveMaterial(singularValue, pathRoot + "." + singularKey, sourceName));
        }

        if (pluralValues.isEmpty()) {
            return EnumSet.noneOf(Material.class);
        }

        if (pluralValues.size() > 1) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        return EnumSet.of(resolveMaterial(pluralValues.getFirst(), pathRoot + "." + pluralKey, sourceName));
    }

    private static @NotNull Set<EntityType> resolveSingleEntityTypeFilter(
        final @NotNull ConfigurationSection section,
        final @NotNull String singularKey,
        final @NotNull String pluralKey,
        final @NotNull String pathRoot,
        final @NotNull String sourceName
    ) {
        final String singularValue = section.getString(singularKey);
        final List<String> pluralValues = thisOrEmpty(section.getStringList(pluralKey));
        if (singularValue != null && !singularValue.isBlank() && !pluralValues.isEmpty()) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        if (singularValue != null && !singularValue.isBlank()) {
            return EnumSet.of(resolveEnum(EntityType.class, singularValue, pathRoot + "." + singularKey, sourceName));
        }

        if (pluralValues.isEmpty()) {
            return EnumSet.noneOf(EntityType.class);
        }

        if (pluralValues.size() > 1) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        return EnumSet.of(resolveEnum(EntityType.class, pluralValues.getFirst(), pathRoot + "." + pluralKey, sourceName));
    }

    private static @NotNull Set<SkillConfig.ProjectileKind> resolveSingleProjectileKindFilter(
        final @NotNull ConfigurationSection section,
        final @NotNull String singularKey,
        final @NotNull String pluralKey,
        final @NotNull String pathRoot,
        final @NotNull String sourceName
    ) {
        final String singularValue = section.getString(singularKey);
        final List<String> pluralValues = thisOrEmpty(section.getStringList(pluralKey));
        if (singularValue != null && !singularValue.isBlank() && !pluralValues.isEmpty()) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        if (singularValue != null && !singularValue.isBlank()) {
            return EnumSet.of(
                resolveEnum(
                    SkillConfig.ProjectileKind.class,
                    singularValue,
                    pathRoot + "." + singularKey,
                    sourceName
                )
            );
        }

        if (pluralValues.isEmpty()) {
            return EnumSet.noneOf(SkillConfig.ProjectileKind.class);
        }

        if (pluralValues.size() > 1) {
            throw groupedRateException(pathRoot, singularKey, pluralKey, sourceName);
        }

        return EnumSet.of(
            resolveEnum(
                SkillConfig.ProjectileKind.class,
                pluralValues.getFirst(),
                pathRoot + "." + pluralKey,
                sourceName
            )
        );
    }

    private static @NotNull IllegalStateException groupedRateException(
        final @NotNull String pathRoot,
        final @NotNull String singularKey,
        final @NotNull String pluralKey,
        final @NotNull String sourceName
    ) {
        return new IllegalStateException(
            "Grouped rate filters are not allowed at "
                + pathRoot
                + " in "
                + sourceName
                + ". Use "
                + singularKey
                + " for a single target or split "
                + pluralKey
                + " into separate rate entries."
        );
    }

    private static @NotNull List<String> thisOrEmpty(final @Nullable List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private static int requireNonNegative(
        final int value,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (value < 0) {
            throw new IllegalStateException("Expected non-negative value at " + path + " in " + sourceName);
        }
        return value;
    }

    private static @NotNull Material resolveMaterial(
        final @Nullable String rawValue,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalStateException("Missing material at " + path + " in " + sourceName);
        }

        try {
            return Material.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid material '" + rawValue + "' at " + path + " in " + sourceName,
                exception
            );
        }
    }

    private static <E extends Enum<E>> @NotNull E resolveEnum(
        final @NotNull Class<E> enumClass,
        final @Nullable String rawValue,
        final @NotNull String path,
        final @NotNull String sourceName
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalStateException("Missing value at " + path + " in " + sourceName);
        }

        try {
            return Enum.valueOf(enumClass, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Invalid value '" + rawValue + "' at " + path + " in " + sourceName,
                exception
            );
        }
    }

    private record AbilityTreeParseResult(
        @NotNull List<SkillConfig.AbilityDefinition> passiveAbilities,
        @Nullable SkillConfig.AbilityDefinition activeAbility
    ) {
        private AbilityTreeParseResult {
            Objects.requireNonNull(passiveAbilities, "passiveAbilities");
        }
    }
}
