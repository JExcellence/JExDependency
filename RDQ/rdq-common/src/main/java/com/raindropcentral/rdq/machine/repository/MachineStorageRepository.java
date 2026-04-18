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
import com.raindropcentral.rdq.database.entity.machine.MachineStorage;
import com.raindropcentral.rdq.machine.type.EStorageType;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link MachineStorage} entities.
 *
 * <p>Provides database operations for machine storage entries including queries
 * by machine and storage type, with async variants for non-blocking operations.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineStorageRepository extends CachedRepository<MachineStorage, Long, Long> {

    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;

    /**
     * Constructs a new {@code MachineStorageRepository}.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public MachineStorageRepository(
        @NotNull final ExecutorService executor,
        @NotNull final EntityManagerFactory entityManagerFactory,
        @NotNull final Class<MachineStorage> entityClass,
        @NotNull final Function<MachineStorage, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
    }

    /**
     * Finds all storage entries for a specific machine.
     *
     * @param machine the machine to search for
     * @return a list of storage entries for the machine
     */
    public List<MachineStorage> findByMachine(@NotNull final Machine machine) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT ms FROM MachineStorage ms WHERE ms.machine = :machine",
                    MachineStorage.class
                )
                .setParameter("machine", machine)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all storage entries for a specific machine asynchronously.
     *
     * @param machine the machine to search for
     * @return a CompletableFuture containing a list of storage entries
     */
    public CompletableFuture<List<MachineStorage>> findByMachineAsync(@NotNull final Machine machine) {
        return CompletableFuture.supplyAsync(() -> findByMachine(machine), executor);
    }

    /**
     * Finds all storage entries for a specific machine by machine ID.
     *
     * @param machineId the ID of the machine
     * @return a list of storage entries for the machine
     */
    public List<MachineStorage> findByMachineId(@NotNull final Long machineId) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT ms FROM MachineStorage ms WHERE ms.machine.id = :machineId",
                    MachineStorage.class
                )
                .setParameter("machineId", machineId)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all storage entries for a specific machine by machine ID asynchronously.
     *
     * @param machineId the ID of the machine
     * @return a CompletableFuture containing a list of storage entries
     */
    public CompletableFuture<List<MachineStorage>> findByMachineIdAsync(@NotNull final Long machineId) {
        return CompletableFuture.supplyAsync(() -> findByMachineId(machineId), executor);
    }

    /**
     * Finds all storage entries of a specific type for a machine.
     *
     * @param machine     the machine to search for
     * @param storageType the type of storage to filter by
     * @return a list of storage entries matching the criteria
     */
    public List<MachineStorage> findByStorageType(
        @NotNull final Machine machine,
        @NotNull final EStorageType storageType
    ) {
        var em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT ms FROM MachineStorage ms WHERE ms.machine = :machine AND ms.storageType = :storageType",
                    MachineStorage.class
                )
                .setParameter("machine", machine)
                .setParameter("storageType", storageType)
                .getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Finds all storage entries of a specific type for a machine asynchronously.
     *
     * @param machine     the machine to search for
     * @param storageType the type of storage to filter by
     * @return a CompletableFuture containing a list of storage entries
     */
    public CompletableFuture<List<MachineStorage>> findByStorageTypeAsync(
        @NotNull final Machine machine,
        @NotNull final EStorageType storageType
    ) {
        return CompletableFuture.supplyAsync(() -> findByStorageType(machine, storageType), executor);
    }

    /**
     * Deletes all storage entries for a specific machine.
     *
     * @param machineId the ID of the machine
     * @return the number of entries deleted
     */
    public int deleteByMachineId(@NotNull final Long machineId) {
        var em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            int deleted = em.createQuery(
                    "DELETE FROM MachineStorage ms WHERE ms.machine.id = :machineId"
                )
                .setParameter("machineId", machineId)
                .executeUpdate();
            em.getTransaction().commit();
            return deleted;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Deletes all storage entries for a specific machine asynchronously.
     *
     * @param machineId the ID of the machine
     * @return a CompletableFuture containing the number of entries deleted
     */
    public CompletableFuture<Integer> deleteByMachineIdAsync(@NotNull final Long machineId) {
        return CompletableFuture.supplyAsync(() -> deleteByMachineId(machineId), executor);
    }
}
