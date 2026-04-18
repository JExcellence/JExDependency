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
import com.raindropcentral.rdq.machine.MachineManager;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.repository.MachineCache;
import com.raindropcentral.rdq.machine.repository.MachineRepository;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listens to chunk load and unload events for machine lifecycle management.
 *
 * <p>This listener monitors chunk loading and unloading to manage machine state
 * in memory. When a chunk loads, it asynchronously loads all machines in that
 * chunk from the database and registers them in the active registry. When a
 * chunk unloads, it saves any dirty machines and unregisters them from memory.
 *
 * <p>This approach ensures that only machines in loaded chunks consume memory,
 * improving performance for servers with many machines across large worlds.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class MachineChunkListener implements Listener {

    private final RDQ rdq;
    private final MachineManager machineManager;
    private final MachineRepository machineRepository;
    private final MachineCache machineCache;

    /**
     * Creates a machine chunk listener and automatically registers it.
     *
     * @param rdq the RDQ instance providing access to all dependencies
     */
    public MachineChunkListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.machineManager = rdq.getMachineManager();
        this.machineRepository = rdq.getMachineRepository();
        this.machineCache = rdq.getMachineCache();
    }

    /**
     * Handles chunk load events to load machines asynchronously.
     *
     * <p>This method queries the database for all machines in the loaded chunk
     * and registers them in the active registry. The database query is performed
     * asynchronously to avoid blocking the main thread.
     *
     * @param event the chunk load event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(@NotNull final ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        final String worldName = chunk.getWorld().getName();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();

        // Calculate block coordinate ranges for this chunk
        final int minX = chunkX * 16;
        final int maxX = minX + 15;
        final int minZ = chunkZ * 16;
        final int maxZ = minZ + 15;

        // Load machines in this chunk asynchronously
        machineRepository.findByWorldAsync(worldName)
            .thenAccept(machines -> {
                // Filter machines that are in this chunk
                final List<Machine> chunkMachines = machines.stream()
                    .filter(machine -> {
                        final int x = machine.getX();
                        final int z = machine.getZ();
                        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
                    })
                    .collect(Collectors.toList());

                // Register machines on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    for (final Machine machine : chunkMachines) {
                        machineManager.registerMachine(machine);
                    }

                    if (!chunkMachines.isEmpty()) {
                        rdq.getPlugin().getLogger().fine("Loaded " + chunkMachines.size() +
                            " machines in chunk " + chunkX + "," + chunkZ +
                            " in world " + worldName);
                    }
                });
            })
            .exceptionally(ex -> {
                rdq.getPlugin().getLogger().severe("Failed to load machines for chunk " +
                    chunkX + "," + chunkZ + " in world " + worldName + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Handles chunk unload events to save and unload machines.
     *
     * <p>This method saves any dirty machines in the unloading chunk and
     * unregisters them from the active registry. This ensures that machine
     * state is persisted before the chunk is unloaded.
     *
     * @param event the chunk unload event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(@NotNull final ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        final String worldName = chunk.getWorld().getName();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();

        // Calculate block coordinate ranges for this chunk
        final int minX = chunkX * 16;
        final int maxX = minX + 15;
        final int minZ = chunkZ * 16;
        final int maxZ = minZ + 15;

        // Find all machines in this chunk from the registry
        final List<Machine> chunkMachines = machineManager.getRegistry().getAllMachines().stream()
            .filter(machine -> {
                final Location location = machine.getLocation();
                if (!location.getWorld().getName().equals(worldName)) {
                    return false;
                }
                final int x = location.getBlockX();
                final int z = location.getBlockZ();
                return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            })
            .collect(Collectors.toList());

        // Save and unregister machines
        for (final Machine machine : chunkMachines) {
            if (machine.getId() != null) {
                // Save if dirty
                machineCache.saveMachine(machine.getId());

                // Unregister from active registry
                machineManager.unregisterMachine(machine.getId());
            }
        }

        if (!chunkMachines.isEmpty()) {
            rdq.getPlugin().getLogger().fine("Unloaded " + chunkMachines.size() +
                " machines in chunk " + chunkX + "," + chunkZ +
                " in world " + worldName);
        }
    }
}
