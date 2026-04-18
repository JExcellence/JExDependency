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

package com.raindropcentral.rdq.machine.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rdq.machine.MachineManager;
import com.raindropcentral.rdq.machine.config.MachineSystemSection;
import com.raindropcentral.rdq.machine.item.MachineItemFactory;
import com.raindropcentral.rdq.machine.structure.StructureDetector;
import com.raindropcentral.rdq.machine.type.EMachineType;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Listens to block placement and breaking events for machine structure detection and destruction.
 *
 * <p>This listener monitors block placement events to detect when players complete multi-block
 * machine structures. When a potential structure is detected, it validates the structure,
 * checks player permissions, and creates the machine if all requirements are met.
 *
 * <p>For block breaking events, this listener detects when machine core blocks are destroyed,
 * validates permissions, and handles machine destruction including item drops or virtual
 * storage retention based on configuration.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class MachineBlockListener implements Listener {

    private final RDQ rdq;
    private final IMachineService machineService;
    private final MachineManager machineManager;
    private final StructureDetector structureDetector;
    private final MachineSystemSection config;
    private final MachineItemFactory machineItemFactory;

    /**
     * Creates a machine block listener and automatically registers it.
     *
     * @param rdq the RDQ instance providing access to all dependencies
     */
    public MachineBlockListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.machineService = rdq.getMachineService();
        this.machineManager = rdq.getMachineManager();
        this.structureDetector = rdq.getStructureDetector();
        this.config = rdq.getMachineSystemConfig();
        this.machineItemFactory = rdq.getMachineItemFactory();
    }

    /**
     * Handles block placement events for structure detection.
     *
     * <p>This method checks if the placed block could be a machine core block.
     * If so, it validates the complete structure, checks permissions, and
     * creates the machine if all requirements are met.
     *
     * <p>Additionally, this method detects when a player places a machine item
     * and triggers structure creation if the item is valid.
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(@NotNull final BlockPlaceEvent event) {
        final Block placedBlock = event.getBlockPlaced();
        final Player player = event.getPlayer();
        final Location location = placedBlock.getLocation();
        final ItemStack itemInHand = event.getItemInHand();

        // Check if player is placing a machine item
        if (machineItemFactory.isMachineItem(itemInHand)) {
            handleMachineItemPlacement(event, player, location, itemInHand);
            return;
        }

        // Check if this could be a machine core block (legacy placement)
        final EMachineType potentialType = structureDetector.detectPotentialStructure(placedBlock);
        if (potentialType == null) {
            return;
        }

        // Detect and validate the complete structure
        final StructureDetector.DetectionResult result = structureDetector.detectAndValidate(location);

        if (!result.isDetected()) {
            return;
        }

        if (!result.isValid()) {
            // Structure is invalid - send error message
            event.setCancelled(true);
            new I18n.Builder("machine.creation.invalid_structure", player)
                .withPlaceholder("error", result.getErrorMessage())
                .build()
                .sendMessage();
            return;
        }

        final EMachineType machineType = result.getMachineType();

        // Check if player has permission to create this machine type
        if (!machineService.hasPermission(player, machineType)) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.no_permission", player)
                .withPlaceholder("machine_type", machineType.getDisplayName())
                .build()
                .sendMessage();
            return;
        }

        // Create the machine asynchronously
        machineService.createMachine(player.getUniqueId(), machineType, location)
            .thenAccept(machine -> {
                // Success message on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("machine.creation.success", player)
                        .withPlaceholder("machine_type", machineType.getDisplayName())
                        .withPlaceholder("location", formatLocation(location))
                        .build()
                        .sendMessage();
                });
            })
            .exceptionally(ex -> {
                // Error message on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("machine.creation.failed", player)
                        .withPlaceholder("error", ex.getMessage())
                        .build()
                        .sendMessage();
                    rdq.getPlugin().getLogger().warning("Failed to create machine for " + player.getName() + ": " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Handles machine item placement.
     *
     * <p>This method validates that the player has permission to place the machine
     * type, then triggers structure creation at the placement location.
     *
     * @param event      the block place event
     * @param player     the player placing the machine item
     * @param location   the location where the machine is being placed
     * @param machineItem the machine item being placed
     */
    private void handleMachineItemPlacement(
        @NotNull final BlockPlaceEvent event,
        @NotNull final Player player,
        @NotNull final Location location,
        @NotNull final ItemStack machineItem
    ) {
        final EMachineType machineType = machineItemFactory.getMachineType(machineItem);
        
        if (machineType == null) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.invalid_item", player)
                .build()
                .sendMessage();
            return;
        }

        // Check if player has permission to create this machine type
        if (!machineService.hasPermission(player, machineType)) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.no_permission_place", player)
                .withPlaceholder("machine_type", machineType.getDisplayName())
                .build()
                .sendMessage();
            return;
        }

        // Detect and validate the complete structure
        final StructureDetector.DetectionResult result = structureDetector.detectAndValidate(location);

        if (!result.isDetected()) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.invalid_structure", player)
                .build()
                .sendMessage();
            return;
        }

        if (!result.isValid()) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.invalid_structure", player)
                .withPlaceholder("error", result.getErrorMessage())
                .build()
                .sendMessage();
            return;
        }

        // Validate that the structure matches the machine type
        if (result.getMachineType() != machineType) {
            event.setCancelled(true);
            new I18n.Builder("machine.creation.structure_mismatch", player)
                .withPlaceholder("machine_type", machineType.getDisplayName())
                .build()
                .sendMessage();
            return;
        }

        // Create the machine asynchronously
        machineService.createMachine(player.getUniqueId(), machineType, location)
            .thenAccept(machine -> {
                // Success message on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("machine.creation.success", player)
                        .withPlaceholder("machine_type", machineType.getDisplayName())
                        .withPlaceholder("location", formatLocation(location))
                        .build()
                        .sendMessage();
                });
            })
            .exceptionally(ex -> {
                // Error message on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    new I18n.Builder("machine.creation.failed", player)
                        .withPlaceholder("error", ex.getMessage())
                        .build()
                        .sendMessage();
                    rdq.getPlugin().getLogger().warning("Failed to create machine for " + player.getName() + ": " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Handles block breaking events for machine destruction.
     *
     * <p>This method checks if the broken block is a machine core block.
     * If so, it validates permissions and handles machine destruction
     * including item drops or virtual storage retention.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(@NotNull final BlockBreakEvent event) {
        final Block brokenBlock = event.getBlock();
        final Player player = event.getPlayer();
        final Location location = brokenBlock.getLocation();

        // Check if this is a machine core block
        if (!structureDetector.isCoreBlock(brokenBlock.getType())) {
            return;
        }

        // Check if there's a machine at this location
        final Optional<Machine> machineOpt = machineManager.getActiveMachine(location);
        if (machineOpt.isEmpty()) {
            return;
        }

        final Machine machine = machineOpt.get();

        // Check if player can break this machine
        if (!canBreakMachine(player, machine)) {
            event.setCancelled(true);
            new I18n.Builder("machine.destruction.no_permission", player)
                .build()
                .sendMessage();
            return;
        }

        // Handle item drops based on configuration
        if (config.getBreaking().isDropItems()) {
            dropMachineItems(machine, location);
        }

        // Drop machine item if configured
        if (config.getBreaking().isDropMachineItem()) {
            dropMachineItem(machine, location);
        }

        // Delete the machine asynchronously
        machineService.deleteMachine(machine.getId())
            .thenAccept(success -> {
                if (success) {
                    rdq.getPlatform().getScheduler().runSync(() -> {
                        new I18n.Builder("machine.destruction.success", player)
                            .build()
                            .sendMessage();
                    });
                } else {
                    rdq.getPlugin().getLogger().warning("Failed to delete machine " + machine.getId() + " from database");
                }
            })
            .exceptionally(ex -> {
                rdq.getPlugin().getLogger().severe("Error deleting machine " + machine.getId() + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Checks if a player can break a machine.
     *
     * @param player  the player attempting to break the machine
     * @param machine the machine being broken
     * @return true if the player can break the machine, false otherwise
     */
    private boolean canBreakMachine(@NotNull final Player player, @NotNull final Machine machine) {
        // Check if only owner can break
        if (config.getBreaking().isRequireOwner()) {
            return machine.getOwnerUuid().equals(player.getUniqueId());
        }

        // Allow owner or trusted players to break
        return machineService.canInteract(player, machine);
    }

    /**
     * Drops machine items at the specified location.
     *
     * @param machine  the machine being destroyed
     * @param location the location to drop items
     */
    private void dropMachineItems(@NotNull final Machine machine, @NotNull final Location location) {
        // Get storage contents and drop them
        machineService.getStorageContents(machine.getId())
            .thenAccept(contents -> {
                rdq.getPlatform().getScheduler().runSync(() -> {
                    contents.forEach((material, quantity) -> {
                        final ItemStack item = new ItemStack(material, quantity);
                        location.getWorld().dropItemNaturally(location, item);
                    });
                });
            })
            .exceptionally(ex -> {
                rdq.getPlugin().getLogger().warning("Failed to drop machine items: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Drops the machine item at the specified location.
     *
     * @param machine  the machine being destroyed
     * @param location the location to drop the item
     */
    private void dropMachineItem(@NotNull final Machine machine, @NotNull final Location location) {
        // Create machine item using the factory
        final Player nearestPlayer = location.getWorld().getNearbyPlayers(location, 10.0).stream()
            .findFirst()
            .orElse(null);
        
        if (nearestPlayer == null) {
            rdq.getPlugin().getLogger().warning("No nearby player found to create machine item for locale");
            return;
        }

        final ItemStack machineItem = machineItemFactory.createMachineItem(
            machine.getMachineType(),
            nearestPlayer
        );
        
        location.getWorld().dropItemNaturally(location, machineItem);
    }

    /**
     * Formats a location as a readable string.
     *
     * @param location the location to format
     * @return formatted location string
     */
    private @NotNull String formatLocation(@NotNull final Location location) {
        return String.format("%s (%d, %d, %d)",
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}
