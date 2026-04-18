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

package com.raindropcentral.rdq.machine.config;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration section for Fabricator machine settings.
 *
 * <p>This section defines all Fabricator-specific configuration including structure definition,
 * blueprint requirements, crafting settings, fuel system, and upgrade definitions. It provides
 * comprehensive validation to ensure all settings are within acceptable ranges.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
public class FabricatorSection extends AConfigSection {

    /**
     * Whether the Fabricator machine type is enabled.
     */
    private Boolean enabled;

    /**
     * Permission node required to use Fabricator machines.
     */
    private String permission;

    /**
     * Multi-block structure definition.
     */
    private MachineStructureSection structure;

    /**
     * Blueprint requirements for constructing a Fabricator.
     */
    private BlueprintSection blueprint;

    /**
     * Crafting configuration.
     */
    private CraftingSection crafting;

    /**
     * Fuel system configuration.
     */
    private FuelSection fuel;

    /**
     * Upgrade definitions.
     */
    private UpgradesSection upgrades;

    /**
     * Creates a Fabricator configuration section.
     *
     * @param baseEnvironment evaluation environment builder for expression resolution
     */
    public FabricatorSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Creates a Fabricator configuration section with default environment.
     */
    public FabricatorSection() {
        this(new EvaluationEnvironmentBuilder());
    }

    /**
     * Indicates whether Fabricator machines are enabled.
     *
     * @return {@code true} when enabled, defaults to {@code true}
     */
    public boolean isEnabled() {
        return this.enabled == null || this.enabled;
    }

    /**
     * Returns the permission node required for Fabricator machines.
     *
     * @return permission node, defaults to "rdq.machine.fabricator"
     */
    public @NotNull String getPermission() {
        return this.permission == null ? "rdq.machine.fabricator" : this.permission;
    }

    /**
     * Returns the structure definition.
     *
     * @return structure configuration
     * @throws IllegalStateException when structure is not configured
     */
    public @NotNull MachineStructureSection getStructure() {
        if (this.structure == null) {
            throw new IllegalStateException("Fabricator structure must be configured");
        }
        return this.structure;
    }

    /**
     * Returns the blueprint requirements.
     *
     * @return blueprint configuration, never {@code null}
     */
    public @NotNull BlueprintSection getBlueprint() {
        return this.blueprint == null ? new BlueprintSection(new EvaluationEnvironmentBuilder()) : this.blueprint;
    }

    /**
     * Returns the crafting configuration.
     *
     * @return crafting configuration, never {@code null}
     */
    public @NotNull CraftingSection getCrafting() {
        return this.crafting == null ? new CraftingSection(new EvaluationEnvironmentBuilder()) : this.crafting;
    }

    /**
     * Returns the fuel system configuration.
     *
     * @return fuel configuration, never {@code null}
     */
    public @NotNull FuelSection getFuel() {
        return this.fuel == null ? new FuelSection(new EvaluationEnvironmentBuilder()) : this.fuel;
    }

    /**
     * Returns the upgrades configuration.
     *
     * @return upgrades configuration, never {@code null}
     */
    public @NotNull UpgradesSection getUpgrades() {
        return this.upgrades == null ? new UpgradesSection(new EvaluationEnvironmentBuilder()) : this.upgrades;
    }

    /**
     * Validates the Fabricator configuration.
     *
     * @throws IllegalStateException when configuration is invalid
     */
    /**
     * Performs post-parsing validation and initialization.
     *
     * @param fields the parsed fields
     * @throws Exception if validation fails
     */
    public void afterParsing(final @NotNull List<Field> fields) throws Exception {
        super.afterParsing(fields);

        // Structure validation is handled by its own afterParsing during parent parsing

        // Validate crafting settings
        final CraftingSection craftingSection = this.getCrafting();
        if (craftingSection.getBaseCooldownTicks() < 1) {
            throw new IllegalStateException("Base cooldown ticks must be at least 1: " + craftingSection.getBaseCooldownTicks());
        }

        if (craftingSection.getRecipeGridSize() != 3) {
            throw new IllegalStateException("Recipe grid size must be 3 for standard crafting: " + craftingSection.getRecipeGridSize());
        }

        // Validate fuel settings
        final FuelSection fuelSection = this.getFuel();
        if (fuelSection.getBaseConsumption() < 0) {
            throw new IllegalStateException("Base fuel consumption cannot be negative: " + fuelSection.getBaseConsumption());
        }
    }

    /**
     * Blueprint requirements configuration subsection.
     */
    @CSAlways
    public static class BlueprintSection extends AConfigSection {

        /**
         * Requirements map for blueprint construction.
         */
        private Map<String, Object> requirements;

        /**
         * Creates a blueprint configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public BlueprintSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates a blueprint configuration section with default environment.
         */
        public BlueprintSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns the requirements definition map.
         *
         * @return requirements map, never {@code null}
         */
        public @NotNull Map<String, Object> getRequirements() {
            return this.requirements == null ? new HashMap<>() : new HashMap<>(this.requirements);
        }
    }

    /**
     * Crafting configuration subsection.
     */
    @CSAlways
    public static class CraftingSection extends AConfigSection {

        /**
         * Base cooldown in ticks between crafting operations.
         */
        private Integer baseCooldownTicks;

        /**
         * Recipe grid size (3 for 3x3).
         */
        private Integer recipeGridSize;

        /**
         * Maximum output stack size.
         */
        private Integer maxOutputStackSize;

        /**
         * Creates a crafting configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public CraftingSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates a crafting configuration section with default environment.
         */
        public CraftingSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns the base cooldown in ticks.
         *
         * @return base cooldown ticks, defaults to 100 (5 seconds)
         */
        public int getBaseCooldownTicks() {
            return this.baseCooldownTicks == null ? 100 : this.baseCooldownTicks;
        }

        /**
         * Returns the recipe grid size.
         *
         * @return grid size, defaults to 3
         */
        public int getRecipeGridSize() {
            return this.recipeGridSize == null ? 3 : this.recipeGridSize;
        }

        /**
         * Returns the maximum output stack size.
         *
         * @return max stack size, defaults to 64
         */
        public int getMaxOutputStackSize() {
            return this.maxOutputStackSize == null ? 64 : this.maxOutputStackSize;
        }
    }

    /**
     * Fuel system configuration subsection.
     */
    @CSAlways
    public static class FuelSection extends AConfigSection {

        /**
         * Whether fuel system is enabled.
         */
        private Boolean enabled;

        /**
         * Base fuel consumption per craft.
         */
        private Integer baseConsumption;

        /**
         * Map of fuel types to their energy values.
         */
        private Map<String, FuelTypeSection> fuelTypes;

        /**
         * Creates a fuel configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public FuelSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates a fuel configuration section with default environment.
         */
        public FuelSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Indicates whether fuel system is enabled.
         *
         * @return {@code true} when enabled, defaults to {@code true}
         */
        public boolean isEnabled() {
            return this.enabled == null || this.enabled;
        }

        /**
         * Returns the base fuel consumption per craft.
         *
         * @return base consumption, defaults to 10
         */
        public int getBaseConsumption() {
            return this.baseConsumption == null ? 10 : this.baseConsumption;
        }

        /**
         * Returns the fuel types configuration.
         *
         * @return map of fuel type names to configurations, never {@code null}
         */
        public @NotNull Map<String, FuelTypeSection> getFuelTypes() {
            return this.fuelTypes == null ? new HashMap<>() : new HashMap<>(this.fuelTypes);
        }
    }

    /**
     * Fuel type configuration subsection.
     */
    @CSAlways
    public static class FuelTypeSection extends AConfigSection {

        /**
         * Material type for this fuel.
         */
        private String material;

        /**
         * Energy value provided by this fuel type.
         */
        private Integer energyValue;

        /**
         * Creates a fuel type configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public FuelTypeSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates a fuel type configuration section with default environment.
         */
        public FuelTypeSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns the fuel material type.
         *
         * @return material
         * @throws IllegalStateException when material is not configured or invalid
         */
        public @NotNull Material getMaterial() {
            if (this.material == null || this.material.trim().isEmpty()) {
                throw new IllegalStateException("Fuel material must be configured");
            }

            try {
                return Material.valueOf(this.material.trim().toUpperCase());
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException("Invalid fuel material: " + this.material, e);
            }
        }

        /**
         * Returns the energy value.
         *
         * @return energy value, defaults to 100
         */
        public int getEnergyValue() {
            return this.energyValue == null ? 100 : this.energyValue;
        }
    }

    /**
     * Upgrades configuration subsection.
     */
    @CSAlways
    public static class UpgradesSection extends AConfigSection {

        /**
         * Speed upgrade configuration.
         */
        private UpgradeDefinitionSection speed;

        /**
         * Efficiency upgrade configuration.
         */
        private UpgradeDefinitionSection efficiency;

        /**
         * Bonus output upgrade configuration.
         */
        private UpgradeDefinitionSection bonusOutput;

        /**
         * Fuel reduction upgrade configuration.
         */
        private UpgradeDefinitionSection fuelReduction;

        /**
         * Creates an upgrades configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public UpgradesSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates an upgrades configuration section with default environment.
         */
        public UpgradesSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns upgrade definition for the specified type.
         *
         * @param type upgrade type
         * @return upgrade definition, never {@code null}
         */
        public @NotNull UpgradeDefinitionSection getUpgrade(final @NotNull EUpgradeType type) {
            final UpgradeDefinitionSection definition = switch (type) {
                case SPEED -> this.speed;
                case EFFICIENCY -> this.efficiency;
                case BONUS_OUTPUT -> this.bonusOutput;
                case FUEL_REDUCTION -> this.fuelReduction;
            };

            return definition == null ? new UpgradeDefinitionSection(new EvaluationEnvironmentBuilder()) : definition;
        }

        /**
         * Returns all configured upgrade definitions.
         *
         * @return map of upgrade types to definitions
         */
        public @NotNull Map<EUpgradeType, UpgradeDefinitionSection> getAllUpgrades() {
            final Map<EUpgradeType, UpgradeDefinitionSection> upgrades = new HashMap<>();
            for (final EUpgradeType type : EUpgradeType.values()) {
                upgrades.put(type, this.getUpgrade(type));
            }
            return upgrades;
        }
    }

    /**
     * Upgrade definition configuration subsection.
     */
    @CSAlways
    public static class UpgradeDefinitionSection extends AConfigSection {

        /**
         * Maximum level for this upgrade.
         */
        private Integer maxLevel;

        /**
         * Effect value per level (percentage as decimal).
         */
        private Double effectPerLevel;

        /**
         * Requirements map for each level.
         */
        private Map<String, com.raindropcentral.rdq.config.requirement.BaseRequirementSection> requirements;

        /**
         * Creates an upgrade definition configuration section.
         *
         * @param baseEnvironment evaluation environment builder
         */
        public UpgradeDefinitionSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
            super(baseEnvironment);
        }

        /**
         * Creates an upgrade definition section with default environment.
         */
        public UpgradeDefinitionSection() {
            this(new EvaluationEnvironmentBuilder());
        }

        /**
         * Returns the maximum level.
         *
         * @return max level, defaults to 5
         */
        public int getMaxLevel() {
            return this.maxLevel == null ? 5 : this.maxLevel;
        }

        /**
         * Returns the effect per level.
         *
         * @return effect per level as decimal, defaults to 0.10 (10%)
         */
        public double getEffectPerLevel() {
            return this.effectPerLevel == null ? 0.10 : this.effectPerLevel;
        }

        /**
         * Returns the requirements for a specific level.
         *
         * @param level upgrade level (1-indexed)
         * @return requirements section for the level, or null if not configured
         */
        public com.raindropcentral.rdq.config.requirement.BaseRequirementSection getRequirements(final int level) {
            if (this.requirements == null) {
                return null;
            }

            final String levelKey = "level-" + level;
            return this.requirements.get(levelKey);
        }

        /**
         * Returns all level requirements.
         *
         * @return map of level keys to requirement sections
         */
        public @NotNull Map<String, com.raindropcentral.rdq.config.requirement.BaseRequirementSection> getAllRequirements() {
            return this.requirements == null ? new HashMap<>() : new HashMap<>(this.requirements);
        }
    }
}
