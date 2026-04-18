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
import com.raindropcentral.rdq.machine.type.EMachineType;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link Machine} entities.
 *
 * <p>Provides database operations for machines including queries by location,
 * owner, and machine type, with async variants for non-blocking operations.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineRepository extends CachedRepository<Machine, Long, Long> {

    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;

    /**
     * Constructs a new {@code MachineRepository}.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public MachineRepository(
        @NotNull final ExecutorService executor,
        @NotNull final EntityManagerFactory entityManagerFactory,
        @NotNull final Class<Machine> entityClass,
        @NotNull final Function<Machine, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
    }

    /**
     * Finds a machine by its location.
     *
     * @param location the location to search for
     * @return an Optional containing the machine if found, empty otherwise
     */
    public Optional<Machine> findByLocation(@NotNull final Location location) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM Machine m " +
                    "LEFT JOIN FETCH m.trustedPlayers " +
                    "LEFT JOIN FETCH m.upgrades " +
                    "WHERE m.world = :world AND m.x = :x AND m.y = :y AND m.z = :z",
                    Machine.class
                )
                .setParameter("world", location.getWorld().getName())
                .setParameter("x", location.getBlockX())
                .setParameter("y", location.getBlockY())
                .setParameter("z", location.getBlockZ())
                .getResultStream()
                .findFirst();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds a machine by its location asynchronously.
     *
     * @param location the location to search for
     * @return a CompletableFuture containing an Optional with the machine if found
     */
    public CompletableFuture<Optional<Machine>> findByLocationAsync(@NotNull final Location location) {
        return CompletableFuture.supplyAsync(() -> findByLocation(location), executor);
    }

    /**
     * Finds all machines owned by a specific player.
     *
     * @param ownerUuid the UUID of the owner
     * @return a list of machines owned by the player
     */
    public List<Machine> findByOwner(@NotNull final UUID ownerUuid) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT m FROM Machine m " +
                    "LEFT JOIN FETCH m.trustedPlayers " +
                    "LEFT JOIN FETCH m.upgrades " +
                    "WHERE m.ownerUuid = :ownerUuid",
                    Machine.class
                )
                .setParameter("ownerUuid", ownerUuid)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all machines owned by a specific player asynchronously.
     *
     * @param ownerUuid the UUID of the owner
     * @return a CompletableFuture containing a list of machines
     */
    public CompletableFuture<List<Machine>> findByOwnerAsync(@NotNull final UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> findByOwner(ownerUuid), executor);
    }

    /**
     * Finds all machines of a specific type.
     *
     * @param machineType the type of machine to search for
     * @return a list of machines of the specified type
     */
    public List<Machine> findByType(@NotNull final EMachineType machineType) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT m FROM Machine m " +
                    "LEFT JOIN FETCH m.trustedPlayers " +
                    "LEFT JOIN FETCH m.upgrades " +
                    "WHERE m.machineType = :machineType",
                    Machine.class
                )
                .setParameter("machineType", machineType)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all machines of a specific type asynchronously.
     *
     * @param machineType the type of machine to search for
     * @return a CompletableFuture containing a list of machines
     */
    public CompletableFuture<List<Machine>> findByTypeAsync(@NotNull final EMachineType machineType) {
        return CompletableFuture.supplyAsync(() -> findByType(machineType), executor);
    }

    /**
     * Finds all machines in a specific world.
     *
     * @param worldName the name of the world
     * @return a list of machines in the specified world
     */
    public List<Machine> findByWorld(@NotNull final String worldName) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT m FROM Machine m " +
                    "LEFT JOIN FETCH m.trustedPlayers " +
                    "LEFT JOIN FETCH m.upgrades " +
                    "WHERE m.world = :world",
                    Machine.class
                )
                .setParameter("world", worldName)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all machines in a specific world asynchronously.
     *
     * @param worldName the name of the world
     * @return a CompletableFuture containing a list of machines
     */
    public CompletableFuture<List<Machine>> findByWorldAsync(@NotNull final String worldName) {
        return CompletableFuture.supplyAsync(() -> findByWorld(worldName), executor);
    }

    /**
     * Finds all machines with eagerly loaded relationships.
     * This prevents LazyInitializationException when accessing storage, upgrades, or trust lists.
     *
     * @param machineId the ID of the machine
     * @return an Optional containing the machine with loaded relationships if found
     */
    public Optional<Machine> findByIdWithRelationships(@NotNull final Long machineId) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM Machine m " +
                    "LEFT JOIN FETCH m.storage " +
                    "LEFT JOIN FETCH m.upgrades " +
                    "LEFT JOIN FETCH m.trustedPlayers " +
                    "WHERE m.id = :id",
                    Machine.class
                )
                .setParameter("id", machineId)
                .getResultStream()
                .findFirst();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all machines with eagerly loaded relationships asynchronously.
     *
     * @param machineId the ID of the machine
     * @return a CompletableFuture containing an Optional with the machine if found
     */
    public CompletableFuture<Optional<Machine>> findByIdWithRelationshipsAsync(@NotNull final Long machineId) {
        return CompletableFuture.supplyAsync(() -> findByIdWithRelationships(machineId), executor);
    }
}
