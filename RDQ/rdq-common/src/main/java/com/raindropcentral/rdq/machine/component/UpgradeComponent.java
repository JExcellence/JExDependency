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

package com.raindropcentral.rdq.machine.component;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.machine.config.FabricatorSection;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.database.entity.machine.MachineUpgrade;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Component responsible for managing machine upgrades.
 *
 * <p>This component handles upgrade validation, application, and effect calculation.
 * It ensures upgrades meet requirements and calculates performance modifiers
 * for speed, efficiency, bonus output, and fuel reduction.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class UpgradeComponent {

    private final Machine machine;
    private final FabricatorSection config;

    /**
     * Constructs a new Upgrade component.
     *
     * @param machine the machine entity this component manages
     * @param config  the Fabricator configuration section
     */
    public UpgradeComponent(
        final @NotNull Machine machine,
        final @NotNull FabricatorSection config
    ) {
        this.machine = machine;
        this.config = config;
    }

    /**
     * Validates if an upgrade can be applied.
     *
     * <p>This method checks if:
     * <ul>
     *     <li>The upgrade type is valid</li>
     *     <li>The current level is below the maximum</li>
     *     <li>The upgrade is being applied to the next sequential level</li>
     * </ul>
     *
     * @param upgradeType the type of upgrade to validate
     * @return true if the upgrade can be applied, false otherwise
     */
    public boolean canApplyUpgrade(final @NotNull EUpgradeType upgradeType) {
        final int currentLevel = machine.getUpgradeLevel(upgradeType);
        final int maxLevel = config.getUpgrades().getUpgrade(upgradeType).getMaxLevel();

        return currentLevel < maxLevel;
    }

    /**
     * Applies an upgrade to the machine.
     *
     * <p>This method creates or updates the upgrade entry for the specified type.
     * If the upgrade already exists, its level is incremented. Otherwise, a new
     * upgrade entry is created at level 1.
     *
     * <p>Note: This method does not validate requirements. Requirement validation
     * should be performed by the caller before applying the upgrade.
     *
     * @param upgradeType the type of upgrade to apply
     * @return true if the upgrade was applied successfully, false if at max level
     */
    public boolean applyUpgrade(final @NotNull EUpgradeType upgradeType) {
        if (!canApplyUpgrade(upgradeType)) {
            return false;
        }

        final Optional<MachineUpgrade> existingUpgrade = findUpgrade(upgradeType);

        if (existingUpgrade.isPresent()) {
            // Increment existing upgrade level
            final MachineUpgrade upgrade = existingUpgrade.get();
            upgrade.setLevel(upgrade.getLevel() + 1);
        } else {
            // Create new upgrade at level 1
            final MachineUpgrade newUpgrade = new MachineUpgrade(machine, upgradeType, 1);
            machine.addUpgrade(newUpgrade);
        }

        return true;
    }

    /**
     * Gets the current level of an upgrade.
     *
     * @param upgradeType the upgrade type to check
     * @return the current level, or 0 if not applied
     */
    public int getUpgradeLevel(final @NotNull EUpgradeType upgradeType) {
        return machine.getUpgradeLevel(upgradeType);
    }

    /**
     * Gets all applied upgrades with their levels.
     *
     * @return map of upgrade types to their current levels
     */
    @NotNull
    public Map<EUpgradeType, Integer> getAllUpgrades() {
        final Map<EUpgradeType, Integer> upgrades = new HashMap<>();

        for (final MachineUpgrade upgrade : machine.getUpgrades()) {
            upgrades.put(upgrade.getUpgradeType(), upgrade.getLevel());
        }

        return upgrades;
    }

    /**
     * Calculates the speed modifier for crafting cooldown.
     *
     * <p>The speed modifier reduces the crafting cooldown by a percentage
     * based on the speed upgrade level.
     *
     * @return the speed reduction multiplier (0.0 to 1.0)
     */
    public double calculateSpeedModifier() {
        final int speedLevel = getUpgradeLevel(EUpgradeType.SPEED);
        if (speedLevel == 0) {
            return 0.0;
        }

        final double effectPerLevel = config.getUpgrades()
            .getUpgrade(EUpgradeType.SPEED)
            .getEffectPerLevel();

        return effectPerLevel * speedLevel;
    }

    /**
     * Calculates the efficiency chance for fuel consumption.
     *
     * <p>The efficiency chance represents the probability that fuel will not
     * be consumed during a crafting operation.
     *
     * @return the efficiency chance (0.0 to 1.0)
     */
    public double calculateEfficiencyChance() {
        final int efficiencyLevel = getUpgradeLevel(EUpgradeType.EFFICIENCY);
        if (efficiencyLevel == 0) {
            return 0.0;
        }

        final double effectPerLevel = config.getUpgrades()
            .getUpgrade(EUpgradeType.EFFICIENCY)
            .getEffectPerLevel();

        return Math.min(effectPerLevel * efficiencyLevel, 1.0);
    }

    /**
     * Calculates the bonus output chance.
     *
     * <p>The bonus output chance represents the probability that the crafting
     * operation will produce double output.
     *
     * @return the bonus output chance (0.0 to 1.0)
     */
    public double calculateBonusOutputChance() {
        final int bonusLevel = getUpgradeLevel(EUpgradeType.BONUS_OUTPUT);
        if (bonusLevel == 0) {
            return 0.0;
        }

        final double effectPerLevel = config.getUpgrades()
            .getUpgrade(EUpgradeType.BONUS_OUTPUT)
            .getEffectPerLevel();

        return Math.min(effectPerLevel * bonusLevel, 1.0);
    }

    /**
     * Calculates the fuel reduction modifier.
     *
     * <p>The fuel reduction modifier decreases the fuel cost per operation
     * by a percentage based on the fuel reduction upgrade level.
     *
     * @return the fuel reduction multiplier (0.0 to 1.0)
     */
    public double calculateFuelReductionModifier() {
        final int fuelLevel = getUpgradeLevel(EUpgradeType.FUEL_REDUCTION);
        if (fuelLevel == 0) {
            return 0.0;
        }

        final double effectPerLevel = config.getUpgrades()
            .getUpgrade(EUpgradeType.FUEL_REDUCTION)
            .getEffectPerLevel();

        return Math.min(effectPerLevel * fuelLevel, 1.0);
    }

    /**
     * Gets the requirements for the next level of an upgrade.
     *
     * <p>This returns the requirements section from the configuration for the
     * next level of the specified upgrade type.
     *
     * @param upgradeType the upgrade type to check
     * @return requirement section for the next level, or null if at max level
     */
    @Nullable
    public BaseRequirementSection getNextLevelRequirements(final @NotNull EUpgradeType upgradeType) {
        final int currentLevel = getUpgradeLevel(upgradeType);
        final int nextLevel = currentLevel + 1;

        final FabricatorSection.UpgradeDefinitionSection definition = config.getUpgrades()
            .getUpgrade(upgradeType);

        if (nextLevel > definition.getMaxLevel()) {
            return null;
        }

        return definition.getRequirements(nextLevel);
    }

    /**
     * Checks if an upgrade is at maximum level.
     *
     * @param upgradeType the upgrade type to check
     * @return true if at maximum level, false otherwise
     */
    public boolean isAtMaxLevel(final @NotNull EUpgradeType upgradeType) {
        final int currentLevel = getUpgradeLevel(upgradeType);
        final int maxLevel = config.getUpgrades().getUpgrade(upgradeType).getMaxLevel();

        return currentLevel >= maxLevel;
    }

    /**
     * Gets the maximum level for an upgrade type.
     *
     * @param upgradeType the upgrade type to check
     * @return the maximum level
     */
    public int getMaxLevel(final @NotNull EUpgradeType upgradeType) {
        return config.getUpgrades().getUpgrade(upgradeType).getMaxLevel();
    }

    /**
     * Calculates the total effect for an upgrade at its current level.
     *
     * @param upgradeType the upgrade type to calculate
     * @return the total effect value
     */
    public double calculateTotalEffect(final @NotNull EUpgradeType upgradeType) {
        return switch (upgradeType) {
            case SPEED -> calculateSpeedModifier();
            case EFFICIENCY -> calculateEfficiencyChance();
            case BONUS_OUTPUT -> calculateBonusOutputChance();
            case FUEL_REDUCTION -> calculateFuelReductionModifier();
        };
    }

    /**
     * Gets a summary of all upgrade effects.
     *
     * @return map of upgrade types to their calculated effects
     */
    @NotNull
    public Map<EUpgradeType, Double> getUpgradeEffects() {
        final Map<EUpgradeType, Double> effects = new HashMap<>();

        for (final EUpgradeType type : EUpgradeType.values()) {
            final double effect = calculateTotalEffect(type);
            if (effect > 0.0) {
                effects.put(type, effect);
            }
        }

        return effects;
    }

    /**
     * Finds an upgrade entry for the specified type.
     *
     * @param upgradeType the upgrade type to find
     * @return optional containing the upgrade if found
     */
    @NotNull
    private Optional<MachineUpgrade> findUpgrade(final @NotNull EUpgradeType upgradeType) {
        return machine.getUpgrades().stream()
            .filter(upgrade -> upgrade.getUpgradeType() == upgradeType)
            .findFirst();
    }
}
