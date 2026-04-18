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

import com.raindropcentral.rdq.machine.type.EMachineType;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing machine operations in the Machine Fabrication System.
 *
 * <p>This service provides a comprehensive API for creating, configuring, and operating
 * automated machines. All database operations are performed asynchronously using
 * {@link CompletableFuture} to prevent blocking the main server thread.
 *
 * <h2>Core Functionality:</h2>
 * <ul>
 *     <li>Machine lifecycle management (create, delete, retrieve)</li>
 *     <li>Machine operations (toggle, configure recipe, add fuel)</li>
 *     <li>Storage management (deposit, withdraw, query contents)</li>
 *     <li>Trust system (add/remove trusted players)</li>
 *     <li>Upgrade system (apply and query upgrades)</li>
 *     <li>Permission validation</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * IMachineService machineService = rdq.getMachineService();
 * 
 * // Create a new Fabricator
 * machineService.createMachine(player.getUniqueId(), EMachineType.FABRICATOR, location)
 *     .thenAccept(machine -> {
 *         player.sendMessage("Machine created with ID: " + machine.getId());
 *         
 *         // Configure the machine
 *         machineService.setRecipe(machine.getId(), recipeItems)
 *             .thenCompose(success -> machineService.addFuel(machine.getId(), 1000))
 *             .thenCompose(success -> machineService.toggleMachine(machine.getId(), true))
 *             .thenAccept(success -> player.sendMessage("Machine is now active!"));
 *     })
 *     .exceptionally(ex -> {
 *         player.sendMessage("Failed to create machine: " + ex.getMessage());
 *         return null;
 *     });
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>All methods in this interface are thread-safe. Async operations are executed
 * on the async thread pool, and results are returned via CompletableFuture.
 * Bukkit API calls within callbacks must be scheduled back to the main thread.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IMachineService {
    
    // ========================================================================
    // Machine Lifecycle Operations
    // ========================================================================
    
    /**
     * Creates a new machine at the specified location.
     *
     * <p>This method:
     * <ul>
     *     <li>Validates the multi-block structure</li>
     *     <li>Checks player permissions</li>
     *     <li>Validates blueprint requirements</li>
     *     <li>Registers the machine in the database</li>
     *     <li>Initializes machine components</li>
     * </ul>
     *
     * @param ownerUuid the UUID of the player creating the machine
     * @param type      the type of machine to create
     * @param location  the location of the machine's core block
     * @return a CompletableFuture containing the created machine entity
     * @throws IllegalArgumentException if location is null or invalid
     * @throws IllegalStateException    if structure validation fails
     */
    @NotNull CompletableFuture<Object> createMachine(
        final @NotNull UUID ownerUuid,
        final @NotNull EMachineType type,
        final @NotNull Location location
    );
    
    /**
     * Deletes a machine and performs cleanup operations.
     *
     * <p>This method:
     * <ul>
     *     <li>Stops any active crafting operations</li>
     *     <li>Handles stored items based on configuration</li>
     *     <li>Removes machine from database</li>
     *     <li>Unregisters from active machine registry</li>
     * </ul>
     *
     * @param machineId the unique identifier of the machine to delete
     * @return a CompletableFuture containing true if deletion was successful
     */
    @NotNull CompletableFuture<Boolean> deleteMachine(final @NotNull Long machineId);
    
    /**
     * Retrieves a machine by its unique identifier.
     *
     * @param machineId the unique identifier of the machine
     * @return a CompletableFuture containing the machine entity, or null if not found
     */
    @NotNull CompletableFuture<Object> getMachine(final @NotNull Long machineId);
    
    /**
     * Retrieves all machines owned by a specific player.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing a list of machines owned by the player
     */
    @NotNull CompletableFuture<List<Object>> getPlayerMachines(final @NotNull UUID playerUuid);
    
    // ========================================================================
    // Machine Operation Methods
    // ========================================================================
    
    /**
     * Toggles a machine's operational state between ACTIVE and INACTIVE.
     *
     * <p>When toggling to ACTIVE:
     * <ul>
     *     <li>Validates sufficient fuel exists</li>
     *     <li>Validates recipe is configured</li>
     *     <li>Starts automated crafting cycles</li>
     * </ul>
     *
     * <p>When toggling to INACTIVE:
     * <ul>
     *     <li>Stops all crafting operations immediately</li>
     *     <li>Saves current machine state</li>
     * </ul>
     *
     * @param machineId the unique identifier of the machine
     * @param enabled   true to activate, false to deactivate
     * @return a CompletableFuture containing true if toggle was successful
     */
    @NotNull CompletableFuture<Boolean> toggleMachine(
        final @NotNull Long machineId,
        final boolean enabled
    );
    
    /**
     * Sets the crafting recipe for a machine.
     *
     * <p>This method:
     * <ul>
     *     <li>Validates the recipe against Minecraft's crafting system</li>
     *     <li>Locks the recipe configuration</li>
     *     <li>Stores recipe data in the database</li>
     * </ul>
     *
     * <p>Recipe can only be changed when machine is INACTIVE.
     *
     * @param machineId the unique identifier of the machine
     * @param recipe    array of ItemStacks representing the crafting grid (3x3 = 9 items)
     * @return a CompletableFuture containing true if recipe was set successfully
     * @throws IllegalArgumentException if recipe array is invalid size
     * @throws IllegalStateException    if machine is not INACTIVE
     */
    @NotNull CompletableFuture<Boolean> setRecipe(
        final @NotNull Long machineId,
        final @NotNull ItemStack[] recipe
    );
    
    /**
     * Adds fuel to a machine's fuel storage.
     *
     * <p>Fuel is converted to energy units based on configured fuel types.
     * Invalid fuel items are rejected.
     *
     * @param machineId the unique identifier of the machine
     * @param amount    the amount of fuel energy to add
     * @return a CompletableFuture containing true if fuel was added successfully
     */
    @NotNull CompletableFuture<Boolean> addFuel(
        final @NotNull Long machineId,
        final int amount
    );
    
    // ========================================================================
    // Storage Operation Methods
    // ========================================================================
    
    /**
     * Deposits items into a machine's input storage.
     *
     * <p>Items are added to the virtual storage system with unlimited capacity.
     *
     * @param machineId the unique identifier of the machine
     * @param item      the ItemStack to deposit
     * @return a CompletableFuture containing true if deposit was successful
     */
    @NotNull CompletableFuture<Boolean> depositItems(
        final @NotNull Long machineId,
        final @NotNull ItemStack item
    );
    
    /**
     * Withdraws items from a machine's output storage.
     *
     * @param machineId the unique identifier of the machine
     * @param material  the material type to withdraw
     * @param amount    the quantity to withdraw
     * @return a CompletableFuture containing the withdrawn ItemStack, or null if insufficient
     */
    @NotNull CompletableFuture<ItemStack> withdrawItems(
        final @NotNull Long machineId,
        final @NotNull Material material,
        final int amount
    );
    
    /**
     * Retrieves the contents of a machine's storage.
     *
     * @param machineId the unique identifier of the machine
     * @return a CompletableFuture containing a map of materials to quantities
     */
    @NotNull CompletableFuture<Map<Material, Integer>> getStorageContents(
        final @NotNull Long machineId
    );
    
    // ========================================================================
    // Trust System Methods
    // ========================================================================
    
    /**
     * Adds a player to a machine's trust list.
     *
     * <p>Trusted players can:
     * <ul>
     *     <li>Open the machine GUI</li>
     *     <li>Toggle machine state</li>
     *     <li>Deposit and withdraw items</li>
     *     <li>View machine status</li>
     * </ul>
     *
     * <p>Trusted players cannot:
     * <ul>
     *     <li>Delete the machine</li>
     *     <li>Modify the trust list</li>
     *     <li>Change the recipe (owner only)</li>
     * </ul>
     *
     * @param machineId  the unique identifier of the machine
     * @param playerUuid the UUID of the player to trust
     * @return a CompletableFuture containing true if player was added successfully
     */
    @NotNull CompletableFuture<Boolean> addTrustedPlayer(
        final @NotNull Long machineId,
        final @NotNull UUID playerUuid
    );
    
    /**
     * Removes a player from a machine's trust list.
     *
     * @param machineId  the unique identifier of the machine
     * @param playerUuid the UUID of the player to remove
     * @return a CompletableFuture containing true if player was removed successfully
     */
    @NotNull CompletableFuture<Boolean> removeTrustedPlayer(
        final @NotNull Long machineId,
        final @NotNull UUID playerUuid
    );
    
    /**
     * Retrieves the list of trusted players for a machine.
     *
     * @param machineId the unique identifier of the machine
     * @return a CompletableFuture containing a list of trusted player UUIDs
     */
    @NotNull CompletableFuture<List<UUID>> getTrustedPlayers(final @NotNull Long machineId);
    
    // ========================================================================
    // Upgrade System Methods
    // ========================================================================
    
    /**
     * Applies an upgrade to a machine.
     *
     * <p>This method:
     * <ul>
     *     <li>Validates upgrade requirements (items, currency)</li>
     *     <li>Checks current upgrade level against maximum</li>
     *     <li>Consumes required resources</li>
     *     <li>Increments upgrade level</li>
     *     <li>Persists upgrade state</li>
     * </ul>
     *
     * @param machineId the unique identifier of the machine
     * @param type      the type of upgrade to apply
     * @return a CompletableFuture containing true if upgrade was applied successfully
     */
    @NotNull CompletableFuture<Boolean> applyUpgrade(
        final @NotNull Long machineId,
        final @NotNull EUpgradeType type
    );
    
    /**
     * Retrieves all upgrades applied to a machine.
     *
     * @param machineId the unique identifier of the machine
     * @return a CompletableFuture containing a map of upgrade types to their current levels
     */
    @NotNull CompletableFuture<Map<EUpgradeType, Integer>> getUpgrades(
        final @NotNull Long machineId
    );
    
    // ========================================================================
    // Validation Methods
    // ========================================================================
    
    /**
     * Checks if a player can interact with a machine.
     *
     * <p>A player can interact if they are:
     * <ul>
     *     <li>The machine owner, OR</li>
     *     <li>On the machine's trust list</li>
     * </ul>
     *
     * @param player  the player to check
     * @param machine the machine entity to check against
     * @return true if the player can interact, false otherwise
     */
    boolean canInteract(final @NotNull Player player, final @NotNull Object machine);
    
    /**
     * Checks if a player has permission to create a specific machine type.
     *
     * @param player the player to check
     * @param type   the machine type to check
     * @return true if the player has the required permission, false otherwise
     */
    boolean hasPermission(final @NotNull Player player, final @NotNull EMachineType type);
}
