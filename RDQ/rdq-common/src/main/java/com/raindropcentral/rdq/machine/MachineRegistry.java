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
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking active machines in memory.
 *
 * <p>This registry maintains an in-memory cache of active machines for fast lookup
 * and access. It provides thread-safe operations for registering, unregistering,
 * and querying machines by ID or location.
 *
 * <p>The registry uses concurrent data structures to support safe access from
 * multiple threads, including async database operations and main thread game logic.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineRegistry {

    /**
     * Map of machine IDs to machine entities.
     */
    private final Map<Long, Machine> machinesById;

    /**
     * Map of location keys to machine entities for fast location-based lookup.
     */
    private final Map<String, Machine> machinesByLocation;

    /**
     * Constructs a new {@code MachineRegistry}.
     */
    public MachineRegistry() {
        this.machinesById = new ConcurrentHashMap<>();
        this.machinesByLocation = new ConcurrentHashMap<>();
    }

    /**
     * Registers a machine in the registry.
     *
     * <p>The machine is indexed by both its ID and location for efficient lookup.
     * If a machine with the same ID or location already exists, it will be replaced.
     *
     * @param machine the machine to register
     * @throws IllegalArgumentException if the machine ID is null
     */
    public void register(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            throw new IllegalArgumentException("Cannot register machine without ID");
        }

        final Location location = machine.getLocation();
        if (location == null) {
            throw new IllegalArgumentException("Cannot register machine without valid location");
        }

        machinesById.put(machine.getId(), machine);
        machinesByLocation.put(getLocationKey(location), machine);
    }

    /**
     * Unregisters a machine from the registry by ID.
     *
     * <p>Removes the machine from both ID and location indexes.
     *
     * @param machineId the ID of the machine to unregister
     * @return true if the machine was found and removed, false otherwise
     */
    public boolean unregister(@NotNull final Long machineId) {
        final Machine machine = machinesById.remove(machineId);
        if (machine != null) {
            final Location location = machine.getLocation();
            if (location != null) {
                machinesByLocation.remove(getLocationKey(location));
            }
            return true;
        }
        return false;
    }

    /**
     * Unregisters a machine from the registry.
     *
     * <p>Removes the machine from both ID and location indexes.
     *
     * @param machine the machine to unregister
     * @return true if the machine was found and removed, false otherwise
     */
    public boolean unregister(@NotNull final Machine machine) {
        if (machine.getId() == null) {
            return false;
        }
        return unregister(machine.getId());
    }

    /**
     * Gets a machine by its ID.
     *
     * @param machineId the ID of the machine
     * @return an Optional containing the machine if found, empty otherwise
     */
    public Optional<Machine> getById(@NotNull final Long machineId) {
        return Optional.ofNullable(machinesById.get(machineId));
    }

    /**
     * Gets a machine by its location.
     *
     * @param location the location to search for
     * @return an Optional containing the machine if found, empty otherwise
     */
    public Optional<Machine> getByLocation(@NotNull final Location location) {
        return Optional.ofNullable(machinesByLocation.get(getLocationKey(location)));
    }

    /**
     * Checks if a machine exists at the specified location.
     *
     * @param location the location to check
     * @return true if a machine exists at the location, false otherwise
     */
    public boolean existsAt(@NotNull final Location location) {
        return machinesByLocation.containsKey(getLocationKey(location));
    }

    /**
     * Checks if a machine with the specified ID is registered.
     *
     * @param machineId the machine ID to check
     * @return true if the machine is registered, false otherwise
     */
    public boolean isRegistered(@NotNull final Long machineId) {
        return machinesById.containsKey(machineId);
    }

    /**
     * Gets all registered machines.
     *
     * @return an unmodifiable collection of all registered machines
     */
    public Collection<Machine> getAllMachines() {
        return List.copyOf(machinesById.values());
    }

    /**
     * Gets all machines in a specific world.
     *
     * @param worldName the name of the world
     * @return a list of machines in the specified world
     */
    public List<Machine> getMachinesInWorld(@NotNull final String worldName) {
        return machinesById.values().stream()
            .filter(machine -> worldName.equals(machine.getWorld()))
            .toList();
    }

    /**
     * Gets the number of registered machines.
     *
     * @return the count of registered machines
     */
    public int size() {
        return machinesById.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no machines are registered, false otherwise
     */
    public boolean isEmpty() {
        return machinesById.isEmpty();
    }

    /**
     * Clears all machines from the registry.
     *
     * <p>This method should be used with caution, typically only during
     * plugin shutdown or reload operations.
     */
    public void clear() {
        machinesById.clear();
        machinesByLocation.clear();
    }

    /**
     * Creates a location key for indexing machines by location.
     *
     * <p>The key format is: {@code world:x:y:z}
     *
     * @param location the location to create a key for
     * @return the location key string
     */
    private String getLocationKey(@NotNull final Location location) {
        return String.format(
            "%s:%d:%d:%d",
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    /**
     * Gets registry statistics for monitoring and debugging.
     *
     * @return a map containing registry statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "total_machines", machinesById.size(),
            "indexed_by_id", machinesById.size(),
            "indexed_by_location", machinesByLocation.size()
        );
    }
}
