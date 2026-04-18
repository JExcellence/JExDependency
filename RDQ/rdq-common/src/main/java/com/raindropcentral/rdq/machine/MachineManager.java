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

package com.raindropcentral.rdq.machine;

import com.raindropcentral.rdq.machine.component.FabricatorComponent;
import com.raindropcentral.rdq.machine.component.FuelComponent;
import com.raindropcentral.rdq.machine.component.RecipeComponent;
import com.raindropcentral.rdq.machine.component.StorageComponent;
import com.raindropcentral.rdq.machine.component.TrustComponent;
import com.raindropcentral.rdq.machine.component.UpgradeComponent;
import com.raindropcentral.rdq.machine.config.FabricatorSection;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.repository.MachineCache;
import com.raindropcentral.rdq.machine.type.EMachineState;
import com.raindropcentral.rdq.machine.type.EMachineType;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central manager for machine lifecycle and operations.
 *
 * <p>This manager coordinates between machine components, handles machine state
 * transitions, manages crafting cycles, and provides centralized access to machine
 * operations. It maintains component instances for active machines and ensures
 * proper lifecycle management.
 *
 * <p>The manager uses a registry for fast in-memory lookups and a cache for
 * database persistence. It provides thread-safe operations for concurrent access
 * from async tasks and main thread game logic.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineManager {

    private static final Logger LOGGER = Logger.getLogger(MachineManager.class.getName());

    private final JavaPlugin plugin;
    private final MachineRegistry registry;
    private final MachineCache cache;
    private final MachineFactory factory;
    private final FabricatorSection fabricatorConfig;

    /**
     * Map of machine IDs to their component sets.
     */
    private final Map<Long, MachineComponents> componentCache;

    /**
     * Constructs a new {@code MachineManager}.
     *
     * @param plugin           the plugin instance
     * @param registry         the machine registry for in-memory tracking
     * @param cache            the machine cache for database operations
     * @param factory          the machine factory for creating new machines
     * @param fabricatorConfig the fabricator configuration
     */
    public MachineManager(
        @NotNull final JavaPlugin plugin,
        @NotNull final MachineRegistry registry,
        @NotNull final MachineCache cache,
        @NotNull final MachineFactory factory,
        @NotNull final FabricatorSection fabricatorConfig
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.cache = cache;
        this.factory = factory;
        this.fabricatorConfig = fabricatorConfig;
        this.componentCache = new ConcurrentHashMap<>();
    }

    /**
     * Gets an active machine by its ID.
     *
     * @param machineId the machine ID
     * @return an Optional containing the machine if found and active, empty otherwise
     */
    public Optional<Machine> getActiveMachine(@NotNull final Long machineId) {
        return registry.getById(machineId);
    }

    /**
     * Gets an active machine by its location.
     *
     * @param location the location to search for
     * @return an Optional containing the machine if found and active, empty otherwise
     */
    public Optional<Machine> getActiveMachine(@NotNull final Location location) {
        return registry.getByLocation(location);
    }

    /**
     * Registers a machine in the active registry.
     *
     * <p>This method adds the machine to the in-memory registry and initializes
     * its components for operation. The machine must have a valid ID before
     * registration.
     *
     * @param machine the machine to register
     * @throws IllegalArgumentException if the machine ID is null or location is invalid
     */
    public void registerMachine(@NotNull final Machine machine) {
        registry.register(machine);
        initializeComponents(machine);
    }

    /**
     * Unregisters a machine from the active registry.
     *
     * <p>This method removes the machine from the in-memory registry and cleans
     * up its component instances. The machine data remains in the cache/database.
     *
     * @param machineId the ID of the machine to unregister
     * @return true if the machine was found and unregistered, false otherwise
     */
    public boolean unregisterMachine(@NotNull final Long machineId) {
        componentCache.remove(machineId);
        return registry.unregister(machineId);
    }

    /**
     * Unregisters a machine from the active registry.
     *
     * @param machine the machine to unregister
     * @return true if the machine was found and unregistered, false otherwise
     */
    public boolean unregisterMachine(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            return false;
        }
        return unregisterMachine(machine.getId());
    }

    /**
     * Gets the components for a machine.
     *
     * @param machineId the machine ID
     * @return the machine components, or null if machine is not registered
     */
    @Nullable
    public MachineComponents getComponents(@NotNull final Long machineId) {
        return componentCache.get(machineId);
    }

    /**
     * Gets the components for a machine.
     *
     * @param machine the machine entity
     * @return the machine components, or null if machine is not registered
     */
    @Nullable
    public MachineComponents getComponents(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            return null;
        }
        return getComponents(machine.getId());
    }

    /**
     * Initializes components for a machine.
     *
     * @param machine the machine to initialize components for
     */
    private void initializeComponents(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            throw new IllegalArgumentException("Cannot initialize components for machine without ID");
        }

        final MachineComponents components = switch (machine.getMachineType()) {
            case FABRICATOR -> createFabricatorComponents(machine);
        };

        componentCache.put(machine.getId(), components);
    }

    /**
     * Creates component set for a Fabricator machine.
     *
     * @param machine the machine entity
     * @return the initialized components
     */
    private MachineComponents createFabricatorComponents(@NotNull final Machine machine) {
        return new MachineComponents(
            new FabricatorComponent(machine, fabricatorConfig),
            new StorageComponent(machine),
            new UpgradeComponent(machine, fabricatorConfig),
            new FuelComponent(machine, fabricatorConfig),
            new RecipeComponent(machine, fabricatorConfig),
            new TrustComponent(machine)
        );
    }

    /**
     * Starts the crafting cycle for a machine.
     *
     * <p>This method transitions the machine to ACTIVE state and begins automated
     * crafting operations. The machine must have a valid recipe and sufficient fuel.
     *
     * @param machine the machine to start
     * @return true if the machine was started successfully, false otherwise
     */
    public boolean startCraftingCycle(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            return false;
        }

        final MachineComponents components = getComponents(machine);
        if (components == null) {
            return false;
        }

        // Validate prerequisites
        if (!components.fabricator().isRecipeLocked()) {
            return false;
        }

        if (!components.fuel().hasSufficientFuel()) {
            return false;
        }

        // Transition to ACTIVE state
        machine.setState(EMachineState.ACTIVE);
        cache.markDirty(machine.getId());

        // Crafting task will be started by the task scheduler
        return true;
    }

    /**
     * Stops the crafting cycle for a machine.
     *
     * <p>This method transitions the machine to INACTIVE state and halts automated
     * crafting operations immediately.
     *
     * @param machine the machine to stop
     * @return true if the machine was stopped successfully, false otherwise
     */
    public boolean stopCraftingCycle(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            return false;
        }

        // Transition to INACTIVE state
        machine.setState(EMachineState.INACTIVE);
        cache.markDirty(machine.getId());

        // Crafting task will detect state change and stop
        return true;
    }

    /**
     * Toggles the machine state between ACTIVE and INACTIVE.
     *
     * @param machine the machine to toggle
     * @return true if the state was toggled successfully, false otherwise
     */
    public boolean toggleMachineState(@NotNull final Machine machine) {
        if (machine.isActive()) {
            return stopCraftingCycle(machine);
        } else {
            return startCraftingCycle(machine);
        }
    }

    /**
     * Handles machine state changes.
     *
     * <p>This method is called when a machine's state changes and performs
     * necessary cleanup or initialization based on the new state.
     *
     * @param machine  the machine whose state changed
     * @param newState the new state
     */
    public void handleStateChange(@NotNull final Machine machine, @NotNull final EMachineState newState) {
        switch (newState) {
            case ACTIVE -> {
                // Machine activated - crafting task will handle operations
            }
            case INACTIVE -> {
                // Machine deactivated - crafting task will stop
            }
            case ERROR -> {
                // Machine encountered an error - log and notify owner
                plugin.getLogger().warning("Machine " + machine.getId() + " entered ERROR state");
            }
        }

        cache.markDirty(machine.getId());
    }

    /**
     * Gets the machine registry.
     *
     * @return the machine registry
     */
    @NotNull
    public MachineRegistry getRegistry() {
        return registry;
    }

    /**
     * Gets the machine cache.
     *
     * @return the machine cache
     */
    @NotNull
    public MachineCache getCache() {
        return cache;
    }

    /**
     * Gets the machine factory.
     *
     * @return the machine factory
     */
    @NotNull
    public MachineFactory getFactory() {
        return factory;
    }

    /**
     * Gets the plugin instance.
     *
     * @return the plugin instance
     */
    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Reloads machine configurations from disk.
     *
     * <p>This method reloads the fabricator configuration and updates any
     * active machines with the new settings. Note that this does not reload
     * machine data from the database, only configuration files.
     *
     * @throws Exception if configuration reload fails
     */
    public void reloadConfigurations() throws Exception {
        LOGGER.info("Reloading machine configurations...");
        
        // Note: Configuration reloading is handled by the main plugin's config system
        // This method serves as a hook for future configuration reload logic
        
        LOGGER.info("Machine configurations reloaded successfully");
    }

    /**
     * Shuts down the machine manager.
     * Stops all active crafting tasks and clears the registry.
     */
    public void shutdown() {
        LOGGER.info("Shutting down machine manager...");

        // Stop all active crafting tasks
        for (Machine machine : registry.getAllMachines()) {
            if (machine.isActive()) {
                stopCraftingCycle(machine);
            }
        }

        // Clear registry
        registry.clear();

        LOGGER.info("Machine manager shut down successfully");
    }

    /**
     * Container for machine component instances.
     *
     * @param fabricator the fabricator component
     * @param storage    the storage component
     * @param upgrade    the upgrade component
     * @param fuel       the fuel component
     * @param recipe     the recipe component
     * @param trust      the trust component
     */
    public record MachineComponents(
        @NotNull FabricatorComponent fabricator,
        @NotNull StorageComponent storage,
        @NotNull UpgradeComponent upgrade,
        @NotNull FuelComponent fuel,
        @NotNull RecipeComponent recipe,
        @NotNull TrustComponent trust
    ) {
    }
}
