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
import com.raindropcentral.rdq.machine.repository.MachineRepository;
import com.raindropcentral.rdq.machine.type.EMachineType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating and initializing machine instances.
 *
 * <p>This factory handles the complete machine creation process including:
 * <ul>
 *   <li>Blueprint requirement validation</li>
 *   <li>Machine entity instantiation</li>
 *   <li>Component initialization</li>
 *   <li>Database registration</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineFactory {

    private final MachineRepository repository;
    private final FabricatorSection fabricatorConfig;

    /**
     * Constructs a new {@code MachineFactory}.
     *
     * @param repository        the machine repository for database operations
     * @param fabricatorConfig  the fabricator configuration
     */
    public MachineFactory(
        @NotNull final MachineRepository repository,
        @NotNull final FabricatorSection fabricatorConfig
    ) {
        this.repository = repository;
        this.fabricatorConfig = fabricatorConfig;
    }

    /**
     * Creates a new machine instance asynchronously.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Validates blueprint requirements (if applicable)</li>
     *   <li>Creates the machine entity</li>
     *   <li>Initializes machine components</li>
     *   <li>Persists the machine to the database</li>
     * </ol>
     *
     * @param ownerUuid   the UUID of the player who will own the machine
     * @param machineType the type of machine to create
     * @param location    the location where the machine will be placed
     * @return a CompletableFuture containing the created machine
     * @throws IllegalArgumentException if the machine type is not supported
     */
    public CompletableFuture<Machine> createMachine(
        @NotNull final UUID ownerUuid,
        @NotNull final EMachineType machineType,
        @NotNull final Location location
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Create machine entity
            final Machine machine = new Machine(ownerUuid, machineType, location);

            // Initialize components based on machine type
            initializeComponents(machine, machineType);

            // Persist to database
            repository.save(machine);
            
            return machine;
        });
    }

    /**
     * Creates a new machine instance with player context for requirement validation.
     *
     * <p>This overload allows for blueprint requirement validation against the player's
     * inventory and permissions before creating the machine.
     *
     * @param player      the player creating the machine
     * @param machineType the type of machine to create
     * @param location    the location where the machine will be placed
     * @return a CompletableFuture containing the created machine
     * @throws IllegalArgumentException if the machine type is not supported
     * @throws IllegalStateException if blueprint requirements are not met
     */
    public CompletableFuture<Machine> createMachine(
        @NotNull final Player player,
        @NotNull final EMachineType machineType,
        @NotNull final Location location
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate blueprint requirements
            validateBlueprintRequirements(player, machineType);

            // Create machine entity
            final Machine machine = new Machine(player.getUniqueId(), machineType, location);

            // Initialize components based on machine type
            initializeComponents(machine, machineType);

            // Persist to database
            repository.save(machine);
            
            return machine;
        });
    }

    /**
     * Validates blueprint requirements for machine construction.
     *
     * <p>This method checks if the player meets all requirements defined in the
     * machine's blueprint configuration, including currency costs and item requirements.
     *
     * @param player      the player to validate against
     * @param machineType the type of machine being created
     * @throws IllegalStateException if requirements are not met
     */
    private void validateBlueprintRequirements(
        @NotNull final Player player,
        @NotNull final EMachineType machineType
    ) {
        // Blueprint validation will be implemented when requirement system integration is added
        // For now, this is a placeholder that can be extended
        
        // TODO: Integrate with requirement system to validate:
        // - Currency requirements
        // - Item requirements
        // - Permission requirements
        // - Any other configured requirements
    }

    /**
     * Initializes machine components based on machine type.
     *
     * <p>Components are created and configured according to the machine type's
     * specific requirements and capabilities.
     *
     * @param machine     the machine entity to initialize
     * @param machineType the type of machine
     * @throws IllegalArgumentException if the machine type is not supported
     */
    private void initializeComponents(
        @NotNull final Machine machine,
        @NotNull final EMachineType machineType
    ) {
        switch (machineType) {
            case FABRICATOR -> initializeFabricatorComponents(machine);
            default -> throw new IllegalArgumentException("Unsupported machine type: " + machineType);
        }
    }

    /**
     * Initializes components specific to Fabricator machines.
     *
     * <p>Creates and configures all components required for Fabricator operation:
     * <ul>
     *   <li>FabricatorComponent - core crafting logic</li>
     *   <li>StorageComponent - item storage management</li>
     *   <li>UpgradeComponent - upgrade system</li>
     *   <li>FuelComponent - fuel management</li>
     *   <li>RecipeComponent - recipe handling</li>
     *   <li>TrustComponent - access control</li>
     * </ul>
     *
     * @param machine the machine entity to initialize
     */
    private void initializeFabricatorComponents(@NotNull final Machine machine) {
        // Components will be initialized when the machine is loaded into the registry
        // The actual component instances are created by MachineManager when needed
        // This method serves as a placeholder for any entity-level initialization
        
        // Set initial state
        machine.setState(com.raindropcentral.rdq.machine.type.EMachineState.INACTIVE);
        machine.setFuelLevel(0);
    }

    /**
     * Gets the fabricator configuration.
     *
     * @return the fabricator configuration section
     */
    public @NotNull FabricatorSection getFabricatorConfig() {
        return fabricatorConfig;
    }
}
