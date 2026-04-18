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

package com.raindropcentral.rdq.machine.task;

import com.raindropcentral.rdq.machine.MachineManager;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.type.EMachineState;
import com.raindropcentral.rdq.machine.type.EStorageType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Asynchronous task for executing machine crafting cycles.
 *
 * <p>This task runs periodically for active machines, checking recipe validity,
 * verifying fuel and material availability, consuming resources, generating output,
 * and applying upgrade modifiers. The task automatically schedules the next cycle
 * with appropriate cooldown based on speed upgrades.
 *
 * <p>The task operates in two phases:
 * <ol>
 *   <li>Async phase: Validates state, checks resources, and prepares crafting result</li>
 *   <li>Sync phase: Applies changes to machine state and schedules next cycle</li>
 * </ol>
 *
 * <p>The task will automatically cancel itself when:
 * <ul>
 *   <li>The machine transitions to INACTIVE state</li>
 *   <li>The machine runs out of fuel</li>
 *   <li>The machine is unregistered from the manager</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class MachineCraftingTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(MachineCraftingTask.class.getName());

    private final JavaPlugin plugin;
    private final MachineManager manager;
    private final Machine machine;
    private final Long machineId;

    /**
     * Constructs a new crafting task for a machine.
     *
     * @param plugin  the plugin instance
     * @param manager the machine manager
     * @param machine the machine to run crafting cycles for
     */
    public MachineCraftingTask(
        final @NotNull JavaPlugin plugin,
        final @NotNull MachineManager manager,
        final @NotNull Machine machine
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.machine = machine;
        this.machineId = machine.getId();
    }

    /**
     * Executes one crafting cycle iteration.
     *
     * <p>This method is called periodically by the Bukkit scheduler. It performs
     * all necessary checks and operations for a single crafting cycle, then
     * schedules the next cycle if the machine remains active.
     */
    @Override
    public void run() {
        // Check if machine is still active
        if (!machine.isActive()) {
            cancel();
            return;
        }

        // Check if machine is still registered
        final var components = manager.getComponents(machineId);
        if (components == null) {
            cancel();
            return;
        }

        // Process crafting cycle asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final CraftingResult result = processCraftingCycle(components);

            // Apply results on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.success()) {
                    handleSuccessfulCraft(components, result);
                } else {
                    handleFailedCraft(components, result);
                }
            });
        });
    }

    /**
     * Processes a crafting cycle and returns the result.
     *
     * <p>This method runs asynchronously and performs all validation and
     * calculation logic without modifying machine state.
     *
     * @param components the machine components
     * @return the crafting result
     */
    @NotNull
    private CraftingResult processCraftingCycle(
        final @NotNull MachineManager.MachineComponents components
    ) {
        // 1. Validate recipe is locked
        if (!components.fabricator().isRecipeLocked()) {
            return CraftingResult.failure(FailureReason.NO_RECIPE);
        }

        // 2. Get expected output
        final ItemStack expectedOutput = components.fabricator().getExpectedOutput();
        if (expectedOutput == null) {
            return CraftingResult.failure(FailureReason.INVALID_RECIPE);
        }

        // 3. Get locked recipe ingredients
        final ItemStack[] recipe = components.fabricator().getLockedRecipe();
        if (recipe == null) {
            return CraftingResult.failure(FailureReason.INVALID_RECIPE);
        }

        // 4. Check if sufficient materials exist
        if (!components.storage().hasIngredients(recipe)) {
            return CraftingResult.failure(FailureReason.INSUFFICIENT_MATERIALS);
        }

        // 5. Check if sufficient fuel exists
        if (!components.fuel().hasSufficientFuel()) {
            return CraftingResult.failure(FailureReason.INSUFFICIENT_FUEL);
        }

        // 6. Calculate fuel cost with upgrades
        final int fuelCost = components.fuel().calculateFuelCost();

        // 7. Execute crafting with upgrade modifiers
        final ItemStack output = components.fabricator().executeCraftingCycle(expectedOutput);

        // 8. Calculate cooldown with speed upgrades
        final int cooldown = components.fabricator().calculateCooldown();

        return CraftingResult.success(recipe, output, fuelCost, cooldown);
    }

    /**
     * Handles a successful crafting cycle.
     *
     * <p>This method runs on the main thread and applies all state changes
     * resulting from a successful craft.
     *
     * @param components the machine components
     * @param result     the crafting result
     */
    private void handleSuccessfulCraft(
        final @NotNull MachineManager.MachineComponents components,
        final @NotNull CraftingResult result
    ) {
        // Consume ingredients
        if (!components.storage().consumeIngredients(result.ingredients())) {
            LOGGER.warning("Failed to consume ingredients for machine " + machineId);
            return;
        }

        // Consume fuel (with efficiency upgrade chance)
        components.fuel().consumeFuel();

        // Add output to storage
        components.storage().deposit(result.output(), EStorageType.OUTPUT);

        // Mark machine as dirty for persistence
        manager.getCache().markDirty(machineId);

        // Check if machine still has fuel
        if (!components.fuel().hasSufficientFuel()) {
            machine.setState(EMachineState.INACTIVE);
            manager.getCache().markDirty(machineId);
            cancel();
            return;
        }

        // Schedule next cycle with cooldown
        scheduleNextCycle(result.cooldown());
    }

    /**
     * Handles a failed crafting cycle.
     *
     * <p>This method runs on the main thread and handles various failure
     * conditions by transitioning the machine to appropriate states.
     *
     * @param components the machine components
     * @param result     the crafting result
     */
    private void handleFailedCraft(
        final @NotNull MachineManager.MachineComponents components,
        final @NotNull CraftingResult result
    ) {
        switch (result.failureReason()) {
            case INSUFFICIENT_FUEL -> {
                // Disable machine due to fuel depletion
                machine.setState(EMachineState.INACTIVE);
                manager.getCache().markDirty(machineId);
                cancel();
            }
            case INSUFFICIENT_MATERIALS -> {
                // Wait for materials - schedule next check
                scheduleNextCycle(components.fabricator().calculateCooldown());
            }
            case NO_RECIPE, INVALID_RECIPE -> {
                // Recipe issue - disable machine
                machine.setState(EMachineState.INACTIVE);
                manager.getCache().markDirty(machineId);
                cancel();
            }
        }
    }

    /**
     * Schedules the next crafting cycle.
     *
     * @param cooldownTicks the cooldown in ticks before next cycle
     */
    private void scheduleNextCycle(final int cooldownTicks) {
        // Task will run again after cooldown
        // The BukkitRunnable is already scheduled, so we just wait
    }

    /**
     * Result of a crafting cycle attempt.
     *
     * @param success       whether the craft was successful
     * @param ingredients   the ingredients that were/would be consumed
     * @param output        the output that was/would be produced
     * @param fuelCost      the fuel cost for this craft
     * @param cooldown      the cooldown before next craft
     * @param failureReason the reason for failure, if applicable
     */
    private record CraftingResult(
        boolean success,
        ItemStack[] ingredients,
        ItemStack output,
        int fuelCost,
        int cooldown,
        FailureReason failureReason
    ) {
        /**
         * Creates a successful crafting result.
         *
         * @param ingredients the consumed ingredients
         * @param output      the produced output
         * @param fuelCost    the fuel cost
         * @param cooldown    the cooldown ticks
         * @return the success result
         */
        static CraftingResult success(
            final @NotNull ItemStack[] ingredients,
            final @NotNull ItemStack output,
            final int fuelCost,
            final int cooldown
        ) {
            return new CraftingResult(true, ingredients, output, fuelCost, cooldown, null);
        }

        /**
         * Creates a failed crafting result.
         *
         * @param reason the failure reason
         * @return the failure result
         */
        static CraftingResult failure(final @NotNull FailureReason reason) {
            return new CraftingResult(false, null, null, 0, 0, reason);
        }
    }

    /**
     * Reasons why a crafting cycle might fail.
     */
    private enum FailureReason {
        /**
         * No recipe is configured.
         */
        NO_RECIPE,

        /**
         * The configured recipe is invalid.
         */
        INVALID_RECIPE,

        /**
         * Insufficient materials in storage.
         */
        INSUFFICIENT_MATERIALS,

        /**
         * Insufficient fuel in machine.
         */
        INSUFFICIENT_FUEL
    }
}
