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

package com.raindropcentral.rdq.machine.repository;

import com.raindropcentral.rdq.database.entity.machine.Machine;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Cache for managing active {@link Machine} entities in memory.
 *
 * <p>Extends {@link CachedRepository} to provide in-memory caching for machines
 * that are currently loaded (e.g., in loaded chunks). Includes auto-save functionality
 * for crash protection and dirty tracking for efficient persistence.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineCache extends CachedRepository<Machine, Long, Long> {

    private static final Logger LOGGER = Logger.getLogger(MachineCache.class.getName());

    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;
    private final Set<Long> dirtyMachines;
    private final boolean logPerformance;

    /**
     * Constructs a new {@code MachineCache}.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param logPerformance       whether to log performance metrics
     */
    public MachineCache(
        @NotNull final ExecutorService executor,
        @NotNull final EntityManagerFactory entityManagerFactory,
        final boolean logPerformance
    ) {
        super(
            executor,
            entityManagerFactory,
            Machine.class,
            Machine::getId
        );
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
        this.dirtyMachines = ConcurrentHashMap.newKeySet();
        this.logPerformance = logPerformance;
    }

    /**
     * Loads a machine into the cache asynchronously by its ID.
     *
     * @param machineId the ID of the machine to load
     * @return a CompletableFuture that completes when the machine is loaded
     */
    public CompletableFuture<Void> loadMachineAsync(@NotNull final Long machineId) {
        return findByIdAsync(machineId).thenAccept(optional -> {
            if (optional.isPresent()) {
                Machine machine = optional.get();
                // Machine is automatically cached by CachedRepository
                if (logPerformance) {
                    LOGGER.fine("Loaded machine " + machineId + " into cache");
                }
            } else {
                if (logPerformance) {
                    LOGGER.warning("Machine " + machineId + " not found in database");
                }
            }
        });
    }

    /**
     * Loads a machine into the cache asynchronously by its location.
     *
     * @param location the location of the machine
     * @return a CompletableFuture containing an Optional with the machine if found
     */
    public CompletableFuture<Optional<Machine>> loadMachineByLocationAsync(@NotNull final Location location) {
        return CompletableFuture.supplyAsync(() -> {
            var em = entityManagerFactory.createEntityManager();
            try {
                Optional<Machine> result = em.createQuery(
                        "SELECT m FROM Machine m " +
                        "LEFT JOIN FETCH m.storage " +
                        "LEFT JOIN FETCH m.upgrades " +
                        "LEFT JOIN FETCH m.trustedPlayers " +
                        "WHERE m.world = :world AND m.x = :x AND m.y = :y AND m.z = :z",
                        Machine.class
                    )
                    .setParameter("world", location.getWorld().getName())
                    .setParameter("x", location.getBlockX())
                    .setParameter("y", location.getBlockY())
                    .setParameter("z", location.getBlockZ())
                    .getResultStream()
                    .findFirst();

                result.ifPresent(machine -> {
                    // Machine will be cached automatically by CachedRepository
                    if (logPerformance) {
                        LOGGER.fine("Loaded machine at " + location + " into cache");
                    }
                });

                return result;
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }, executor);
    }

    /**
     * Gets a machine from the cache by its location.
     *
     * @param location the location to search for
     * @return the machine if found in cache, null otherwise
     */
    @Nullable
    public Machine getMachineByLocation(@NotNull final Location location) {
        // Search through cached machines for matching location
        return getCachedByKey().values().stream()
            .filter(machine ->
                machine.getWorld().equals(location.getWorld().getName()) &&
                machine.getX() == location.getBlockX() &&
                machine.getY() == location.getBlockY() &&
                machine.getZ() == location.getBlockZ()
            )
            .findFirst()
            .orElse(null);
    }

    /**
     * Saves a machine to the database and removes it from the cache.
     *
     * @param machineId the ID of the machine to save
     * @return a CompletableFuture that completes when the save is done
     */
    public CompletableFuture<Void> saveMachine(@NotNull final Long machineId) {
        Machine machine = getCachedByKey().get(machineId);
        if (machine == null) {
            return CompletableFuture.completedFuture(null);
        }

        return updateAsync(machine).thenRun(() -> {
            dirtyMachines.remove(machineId);
            evict(machine);
            if (logPerformance) {
                LOGGER.fine("Saved and evicted machine " + machineId);
            }
        });
    }

    /**
     * Marks a machine as dirty (having unsaved changes).
     *
     * @param machineId the ID of the machine to mark as dirty
     */
    public void markDirty(@NotNull final Long machineId) {
        dirtyMachines.add(machineId);
    }

    /**
     * Checks if a machine has unsaved changes.
     *
     * @param machineId the ID of the machine to check
     * @return true if the machine is dirty, false otherwise
     */
    public boolean isDirty(@NotNull final Long machineId) {
        return dirtyMachines.contains(machineId);
    }

    /**
     * Auto-saves all dirty machines to the database.
     * This is used for periodic saves to prevent data loss.
     *
     * @return a CompletableFuture containing the number of machines saved
     */
    public CompletableFuture<Integer> autoSaveAll() {
        Set<Long> toSave = Set.copyOf(dirtyMachines);
        if (toSave.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        // Create save futures for all dirty machines in parallel
        List<CompletableFuture<Boolean>> saveFutures = new ArrayList<>();
        for (Long machineId : toSave) {
            Machine machine = getCachedByKey().get(machineId);
            if (machine != null) {
                CompletableFuture<Boolean> saveFuture = updateAsync(machine)
                    .thenApply(updated -> {
                        dirtyMachines.remove(machineId);
                        return true;
                    })
                    .exceptionally(ex -> {
                        LOGGER.warning("Auto-save failed for machine " + machineId + ": " + ex.getMessage());
                        return false;
                    });
                saveFutures.add(saveFuture);
            }
        }

        // Wait for all saves to complete and count successes
        return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                int savedCount = (int) saveFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(success -> success)
                    .count();

                if (logPerformance && savedCount > 0) {
                    LOGGER.info("Auto-saved " + savedCount + " machines");
                }

                return savedCount;
            });
    }

    /**
     * Gets the number of machines currently in the cache.
     *
     * @return the cache size
     */
    public long getCacheSize() {
        return getCachedByKey().size();
    }

    /**
     * Gets the number of machines with unsaved changes.
     *
     * @return the number of dirty machines
     */
    public int getDirtyCount() {
        return dirtyMachines.size();
    }

    /**
     * Clears all dirty flags.
     * This should only be used after a successful save-all operation.
     */
    public void clearDirtyFlags() {
        dirtyMachines.clear();
    }

    /**
     * Saves all dirty machines and clears the cache.
     * This should be called during plugin shutdown.
     *
     * @return a CompletableFuture that completes when all saves are done
     */
    public CompletableFuture<Void> saveAllAndClear() {
        return autoSaveAll().thenRun(() -> {
            // Cache will be cleared automatically by parent class on shutdown
            clearDirtyFlags();
            if (logPerformance) {
                LOGGER.info("Saved all machines");
            }
        });
    }
}
