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

import com.raindropcentral.rdq.machine.config.FabricatorSection;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/**
 * Component responsible for managing machine fuel.
 *
 * <p>This component handles fuel level tracking, fuel type validation,
 * consumption calculation with upgrade modifiers, and fuel depletion handling.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class FuelComponent {

    private final Machine machine;
    private final FabricatorSection config;
    private final Random random;

    /**
     * Constructs a new Fuel component.
     *
     * @param machine the machine entity this component manages
     * @param config  the Fabricator configuration section
     */
    public FuelComponent(
        final @NotNull Machine machine,
        final @NotNull FabricatorSection config
    ) {
        this.machine = machine;
        this.config = config;
        this.random = new Random();
    }

    /**
     * Gets the current fuel level.
     *
     * @return the current fuel level
     */
    public int getFuelLevel() {
        return machine.getFuelLevel();
    }

    /**
     * Sets the fuel level.
     *
     * @param level the new fuel level
     */
    public void setFuelLevel(final int level) {
        machine.setFuelLevel(Math.max(0, level));
    }

    /**
     * Adds fuel to the machine.
     *
     * @param amount the amount of fuel to add
     */
    public void addFuel(final int amount) {
        machine.setFuelLevel(machine.getFuelLevel() + amount);
    }

    /**
     * Validates if an item is a valid fuel type.
     *
     * @param item the item to validate
     * @return true if the item is a valid fuel type, false otherwise
     */
    public boolean isValidFuel(final @NotNull ItemStack item) {
        if (!config.getFuel().isEnabled()) {
            return false;
        }

        final Material material = item.getType();
        final Map<String, FabricatorSection.FuelTypeSection> fuelTypes = config.getFuel().getFuelTypes();

        for (final FabricatorSection.FuelTypeSection fuelType : fuelTypes.values()) {
            if (fuelType.getMaterial() == material) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the energy value for a fuel item.
     *
     * @param item the fuel item
     * @return the energy value, or 0 if not a valid fuel type
     */
    public int getFuelEnergyValue(final @NotNull ItemStack item) {
        if (!config.getFuel().isEnabled()) {
            return 0;
        }

        final Material material = item.getType();
        final Map<String, FabricatorSection.FuelTypeSection> fuelTypes = config.getFuel().getFuelTypes();

        for (final FabricatorSection.FuelTypeSection fuelType : fuelTypes.values()) {
            if (fuelType.getMaterial() == material) {
                return fuelType.getEnergyValue() * item.getAmount();
            }
        }

        return 0;
    }

    /**
     * Adds fuel from an item.
     *
     * <p>This method validates the fuel type and adds the appropriate energy
     * value to the machine's fuel level.
     *
     * @param item the fuel item to add
     * @return true if fuel was added successfully, false if invalid fuel type
     */
    public boolean addFuelFromItem(final @NotNull ItemStack item) {
        if (!isValidFuel(item)) {
            return false;
        }

        final int energyValue = getFuelEnergyValue(item);
        addFuel(energyValue);
        return true;
    }

    /**
     * Calculates the fuel cost for a crafting operation with modifiers.
     *
     * <p>This method applies the fuel reduction upgrade modifier to the base
     * fuel consumption rate.
     *
     * @return the fuel cost after applying modifiers
     */
    public int calculateFuelCost() {
        if (!config.getFuel().isEnabled()) {
            return 0;
        }

        final int baseCost = config.getFuel().getBaseConsumption();
        final int fuelReductionLevel = machine.getUpgradeLevel(EUpgradeType.FUEL_REDUCTION);

        if (fuelReductionLevel == 0) {
            return baseCost;
        }

        final double reductionModifier = config.getUpgrades()
            .getUpgrade(EUpgradeType.FUEL_REDUCTION)
            .getEffectPerLevel() * fuelReductionLevel;

        return (int) (baseCost * (1.0 - reductionModifier));
    }

    /**
     * Checks if the machine has sufficient fuel for a crafting operation.
     *
     * @return true if sufficient fuel exists, false otherwise
     */
    public boolean hasSufficientFuel() {
        if (!config.getFuel().isEnabled()) {
            return true;
        }

        final int fuelCost = calculateFuelCost();
        return machine.getFuelLevel() >= fuelCost;
    }

    /**
     * Consumes fuel for a crafting operation.
     *
     * <p>This method applies the efficiency upgrade chance to potentially
     * skip fuel consumption. If fuel is consumed and the level reaches zero,
     * the machine should be disabled by the caller.
     *
     * @return true if fuel was consumed (or skipped due to efficiency), false if insufficient fuel
     */
    public boolean consumeFuel() {
        if (!config.getFuel().isEnabled()) {
            return true;
        }

        if (!hasSufficientFuel()) {
            return false;
        }

        // Check efficiency upgrade chance
        final int efficiencyLevel = machine.getUpgradeLevel(EUpgradeType.EFFICIENCY);
        if (efficiencyLevel > 0) {
            final double efficiencyChance = config.getUpgrades()
                .getUpgrade(EUpgradeType.EFFICIENCY)
                .getEffectPerLevel() * efficiencyLevel;

            if (random.nextDouble() < efficiencyChance) {
                // Efficiency triggered - no fuel consumed
                return true;
            }
        }

        // Consume fuel
        final int fuelCost = calculateFuelCost();
        machine.setFuelLevel(machine.getFuelLevel() - fuelCost);

        return true;
    }

    /**
     * Checks if the fuel level is depleted.
     *
     * @return true if fuel level is 0 or less, false otherwise
     */
    public boolean isFuelDepleted() {
        if (!config.getFuel().isEnabled()) {
            return false;
        }

        return machine.getFuelLevel() <= 0;
    }

    /**
     * Gets the maximum fuel capacity.
     *
     * <p>Since fuel storage is unlimited, this returns Integer.MAX_VALUE.
     * This method exists for potential future capacity limits.
     *
     * @return the maximum fuel capacity
     */
    public int getMaxFuelCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets the fuel level as a percentage of a reference capacity.
     *
     * <p>This is useful for GUI display purposes. The reference capacity
     * can be configured or calculated based on typical usage patterns.
     *
     * @param referenceCapacity the reference capacity for percentage calculation
     * @return the fuel level percentage (0.0 to 1.0)
     */
    public double getFuelPercentage(final int referenceCapacity) {
        if (referenceCapacity <= 0) {
            return 0.0;
        }

        return Math.min(1.0, (double) machine.getFuelLevel() / referenceCapacity);
    }

    /**
     * Checks if fuel system is enabled.
     *
     * @return true if fuel system is enabled, false otherwise
     */
    public boolean isFuelEnabled() {
        return config.getFuel().isEnabled();
    }

    /**
     * Gets all configured fuel types.
     *
     * @return map of fuel type names to their configurations
     */
    @NotNull
    public Map<String, FabricatorSection.FuelTypeSection> getFuelTypes() {
        return config.getFuel().getFuelTypes();
    }

    /**
     * Gets the base fuel consumption rate.
     *
     * @return the base fuel consumption per craft
     */
    public int getBaseConsumption() {
        return config.getFuel().getBaseConsumption();
    }

    /**
     * Estimates the number of crafting operations possible with current fuel.
     *
     * @return the estimated number of operations
     */
    public int estimateOperationsRemaining() {
        if (!config.getFuel().isEnabled()) {
            return Integer.MAX_VALUE;
        }

        final int fuelCost = calculateFuelCost();
        if (fuelCost <= 0) {
            return Integer.MAX_VALUE;
        }

        return machine.getFuelLevel() / fuelCost;
    }
}
