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

import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.repository.MachineCache;
import com.raindropcentral.rdq.machine.repository.MachineRepository;
import com.raindropcentral.rdq.machine.type.EMachineState;
import com.raindropcentral.rdq.machine.type.EMachineType;
import com.raindropcentral.rdq.machine.type.EStorageType;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link IMachineService} for managing machine operations.
 *
 * <p>This service provides the primary API for interacting with the Machine Fabrication System.
 * It coordinates between the manager, factory, cache, and repository layers to provide
 * a cohesive interface for machine operations.
 *
 * <h2>Architecture:</h2>
 * <pre>
 * MachineService (this class)
 *     ├── MachineManager - coordinates components and lifecycle
 *     ├── MachineFactory - creates new machines
 *     ├── MachineCache - in-memory cache for active machines
 *     └── MachineRepository - database persistence
 * </pre>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class MachineService implements IMachineService {

    private final MachineManager manager;
    private final MachineFactory factory;
    private final MachineCache cache;
    private final MachineRepository repository;

    /**
     * Constructs a new {@code MachineService}.
     *
     * @param manager    the machine manager for coordinating operations
     * @param factory    the machine factory for creating new machines
     * @param cache      the machine cache for in-memory storage
     * @param repository the machine repository for database operations
     */
    public MachineService(
        @NotNull final MachineManager manager,
        @NotNull final MachineFactory factory,
        @NotNull final MachineCache cache,
        @NotNull final MachineRepository repository
    ) {
        this.manager = manager;
        this.factory = factory;
        this.cache = cache;
        this.repository = repository;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Ensures a machine is loaded into the active registry.
     * Tries cache first, then database if needed.
     *
     * @param machineId the machine ID
     * @return Optional containing the machine if found and loaded
     */
    private Optional<Machine> ensureMachineLoaded(@NotNull final Long machineId) {
        // Check if already in active registry
        Optional<Machine> machineOpt = manager.getActiveMachine(machineId);
        if (machineOpt.isPresent()) {
            return machineOpt;
        }

        // Try cache
        Machine machine = cache.getCachedByKey().get(machineId);
        if (machine != null) {
            manager.registerMachine(machine);
            return Optional.of(machine);
        }

        // Try database (synchronous for simplicity in this helper)
        machineOpt = repository.findByIdWithRelationships(machineId);
        if (machineOpt.isPresent()) {
            manager.registerMachine(machineOpt.get());
            cache.getCachedByKey().put(machineId, machineOpt.get());
        }

        return machineOpt;
    }

    // ========================================================================
    // Machine Lifecycle Operations
    // ========================================================================

    @Override
    public @NotNull CompletableFuture<Object> createMachine(
        final @NotNull UUID ownerUuid,
        final @NotNull EMachineType type,
        final @NotNull Location location
    ) {
        return factory.createMachine(ownerUuid, type, location)
            .thenApply(machine -> {
                // Register in active registry
                manager.registerMachine(machine);
                return (Object) machine;
            });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteMachine(final @NotNull Long machineId) {
        return CompletableFuture.supplyAsync(() -> {
            // Get machine from cache or database
            Optional<Machine> machineOpt = manager.getActiveMachine(machineId);
            
            if (machineOpt.isEmpty()) {
                // Try loading from database
                Machine machine = cache.getCachedByKey().get(machineId);
                machineOpt = Optional.ofNullable(machine);
            }

            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();

            // Stop any active operations
            if (machine.isActive()) {
                manager.stopCraftingCycle(machine);
            }

            // Unregister from active registry
            manager.unregisterMachine(machineId);

            // Delete from database
            repository.delete(machine.getId());

            return true;
        });
    }

    @Override
    public @NotNull CompletableFuture<Object> getMachine(final @NotNull Long machineId) {
        // Check active registry first (synchronous, fast)
        Optional<Machine> machineOpt = manager.getActiveMachine(machineId);
        if (machineOpt.isPresent()) {
            return CompletableFuture.completedFuture(machineOpt.get());
        }

        // Check cache (synchronous, fast)
        Machine machine = cache.getCachedByKey().get(machineId);
        if (machine != null) {
            return CompletableFuture.completedFuture(machine);
        }

        // Load from database asynchronously (no blocking!)
        return repository.findByIdAsync(machineId)
            .thenApply(opt -> (Object) opt.orElse(null));
    }

    @Override
    public @NotNull CompletableFuture<List<Object>> getPlayerMachines(final @NotNull UUID playerUuid) {
        return repository.findByOwnerAsync(playerUuid)
            .thenApply(machines -> {
                List<Object> result = new ArrayList<>();
                result.addAll(machines);
                return result;
            });
    }

    // ========================================================================
    // Machine Operation Methods (Task 7.2)
    // ========================================================================

    @Override
    public @NotNull CompletableFuture<Boolean> toggleMachine(
        final @NotNull Long machineId,
        final boolean enabled
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();

            if (enabled) {
                // Start the machine
                return manager.startCraftingCycle(machine);
            } else {
                // Stop the machine
                return manager.stopCraftingCycle(machine);
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> setRecipe(
        final @NotNull Long machineId,
        final @NotNull ItemStack[] recipe
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (recipe.length != 9) {
                throw new IllegalArgumentException("Recipe must be 3x3 (9 items)");
            }

            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();

            // Machine must be inactive to change recipe
            if (machine.isActive()) {
                throw new IllegalStateException("Cannot change recipe while machine is active");
            }

            final var components = manager.getComponents(machine);
            if (components == null) {
                return false;
            }

            // Set recipe using recipe component
            final boolean success = components.recipe().lockRecipe(recipe);
            
            if (success) {
                cache.markDirty(machineId);
            }

            return success;
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> addFuel(
        final @NotNull Long machineId,
        final int amount
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return false;
            }

            // Add fuel using fuel component
            components.fuel().addFuel(amount);
            cache.markDirty(machineId);

            return true;
        });
    }

    // ========================================================================
    // Storage Operation Methods (Task 7.3)
    // ========================================================================

    @Override
    public @NotNull CompletableFuture<Boolean> depositItems(
        final @NotNull Long machineId,
        final @NotNull ItemStack item
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return false;
            }

            // Deposit to INPUT storage
            final boolean success = components.storage().deposit(item, EStorageType.INPUT);
            
            if (success) {
                cache.markDirty(machineId);
            }

            return success;
        });
    }

    @Override
    public @NotNull CompletableFuture<ItemStack> withdrawItems(
        final @NotNull Long machineId,
        final @NotNull Material material,
        final int amount
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return null;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return null;
            }

            // Withdraw from OUTPUT storage
            final ItemStack withdrawn = components.storage().withdraw(material, amount, EStorageType.OUTPUT);
            
            if (withdrawn != null) {
                cache.markDirty(machineId);
            }

            return withdrawn;
        });
    }

    @Override
    public @NotNull CompletableFuture<Map<Material, Integer>> getStorageContents(
        final @NotNull Long machineId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return new HashMap<>();
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return new HashMap<>();
            }

            // Get all storage contents
            return components.storage().getAllContents();
        });
    }

    // ========================================================================
    // Trust System Methods (Task 7.4)
    // ========================================================================

    @Override
    public @NotNull CompletableFuture<Boolean> addTrustedPlayer(
        final @NotNull Long machineId,
        final @NotNull UUID playerUuid
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return false;
            }

            // Add trusted player
            final boolean success = components.trust().addTrustedPlayer(playerUuid);
            
            if (success) {
                cache.markDirty(machineId);
            }

            return success;
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> removeTrustedPlayer(
        final @NotNull Long machineId,
        final @NotNull UUID playerUuid
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return false;
            }

            // Remove trusted player
            final boolean success = components.trust().removeTrustedPlayer(playerUuid);
            
            if (success) {
                cache.markDirty(machineId);
            }

            return success;
        });
    }

    @Override
    public @NotNull CompletableFuture<List<UUID>> getTrustedPlayers(final @NotNull Long machineId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return new ArrayList<>();
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return new ArrayList<>();
            }

            // Get all trusted players
            return components.trust().getTrustedPlayers();
        });
    }

    // ========================================================================
    // Upgrade System Methods (Task 7.5)
    // ========================================================================

    @Override
    public @NotNull CompletableFuture<Boolean> applyUpgrade(
        final @NotNull Long machineId,
        final @NotNull EUpgradeType type
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return false;
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return false;
            }

            // Validate upgrade can be applied
            if (!components.upgrade().canApplyUpgrade(type)) {
                return false;
            }

            // TODO: Validate requirements (currency, items) when requirement system is integrated

            // Apply upgrade
            final boolean success = components.upgrade().applyUpgrade(type);
            
            if (success) {
                cache.markDirty(machineId);
            }

            return success;
        });
    }

    @Override
    public @NotNull CompletableFuture<Map<EUpgradeType, Integer>> getUpgrades(
        final @NotNull Long machineId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Machine> machineOpt = ensureMachineLoaded(machineId);
            
            if (machineOpt.isEmpty()) {
                return new HashMap<>();
            }

            final Machine machine = machineOpt.get();
            final var components = manager.getComponents(machine);
            
            if (components == null) {
                return new HashMap<>();
            }

            // Get all upgrades
            return components.upgrade().getAllUpgrades();
        });
    }

    // ========================================================================
    // Validation Methods (Task 7.6)
    // ========================================================================

    @Override
    public boolean canInteract(final @NotNull Player player, final @NotNull Object machine) {
        if (!(machine instanceof Machine machineEntity)) {
            return false;
        }

        // Check if player is owner or trusted
        return machineEntity.isTrusted(player.getUniqueId());
    }

    @Override
    public boolean hasPermission(final @NotNull Player player, final @NotNull EMachineType type) {
        // Check permission node: rdq.machine.{machine_type}
        final String permission = "rdq.machine." + type.name().toLowerCase();
        return player.hasPermission(permission);
    }
}
