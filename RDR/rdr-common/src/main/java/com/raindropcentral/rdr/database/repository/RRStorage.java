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

package com.raindropcentral.rdr.database.repository;

import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.entity.StorageTrustStatus;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for direct storage persistence and lease management.
 *
 * <p>This repository performs atomic lease acquisition, renewal, and save-release operations so a
 * storage can only be edited by one active session across all servers at a time.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRStorage extends BaseRepository<RStorage, Long> {

    /**
     * Result of attempting to acquire a storage lease.
     */
    public enum LeaseAcquireResult {
        /**
         * The storage exists and the lease was acquired successfully.
         */
        ACQUIRED,
        /**
         * The requested storage row does not exist.
         */
        MISSING,
        /**
         * Another active session already holds the storage lease.
         */
        LOCKED
    }

    /**
     * Creates a repository for {@link RStorage} entities.
     *
     * @param executorService executor used for asynchronous repository operations
     * @param entityManagerFactory entity manager factory backing persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRStorage(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RStorage.class);
    }

    /**
     * Attempts to acquire the storage lease for a specific editing session.
     *
     * @param storageId storage row identifier
     * @param serverUuid UUID of the current server instance
     * @param playerUuid UUID of the player opening the storage
     * @param leaseToken session token that will own the lease
     * @param leaseExpiresAt lease expiry timestamp to persist
     * @return future containing the lease acquisition result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<LeaseAcquireResult> tryAcquireLeaseAsync(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull LocalDateTime leaseExpiresAt
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.tryAcquireLease(storageId, serverUuid, playerUuid, leaseToken, leaseExpiresAt),
            getExecutorService()
        );
    }

    /**
     * Renews an active storage lease when it is still held by the supplied session identity.
     *
     * @param storageId storage row identifier
     * @param serverUuid UUID of the current server instance
     * @param playerUuid UUID of the player currently editing the storage
     * @param leaseToken active storage session token
     * @param leaseExpiresAt replacement lease expiry timestamp
     * @return future containing {@code true} when the lease was renewed
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<Boolean> renewLeaseAsync(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull LocalDateTime leaseExpiresAt
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.renewLease(storageId, serverUuid, playerUuid, leaseToken, leaseExpiresAt),
            getExecutorService()
        );
    }

    /**
     * Saves the provided inventory snapshot and releases the storage lease in a single transaction.
     *
     * @param storageId storage row identifier
     * @param serverUuid UUID of the current server instance
     * @param playerUuid UUID of the player that owns the active session
     * @param leaseToken active storage session token
     * @param inventory sparse slot-indexed inventory snapshot to persist
     * @return future containing {@code true} when the save and release completed
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<Boolean> saveInventoryAndReleaseLeaseAsync(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull Map<Integer, ItemStack> inventory
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.saveInventoryAndReleaseLease(storageId, serverUuid, playerUuid, leaseToken, inventory),
            getExecutorService()
        );
    }

    /**
     * Releases the storage lease without saving inventory data.
     *
     * @param storageId storage row identifier
     * @param serverUuid UUID of the current server instance
     * @param playerUuid UUID of the player that owns the active session
     * @param leaseToken active storage session token
     * @return future containing {@code true} when the lease was released
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<Boolean> releaseLeaseAsync(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.releaseLease(storageId, serverUuid, playerUuid, leaseToken),
            getExecutorService()
        );
    }

    /**
     * Finds every storage owned by the supplied player UUID.
     *
     * @param playerUuid player UUID whose storages should be loaded
     * @return immutable snapshot of storages ordered by storage key
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @NotNull List<RStorage> findByPlayerUuid(final @NotNull UUID playerUuid) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage from RStorage storage join fetch storage.player player where player.playerUuid = :playerUuid order by storage.storageKey asc",
                RStorage.class
            )
            .setParameter("playerUuid", validatedPlayerUuid)
            .getResultList()));
    }

    /**
     * Finds every storage and eagerly loads each owning player.
     *
     * @return immutable snapshot of every storage ordered by owner UUID and storage key
     */
    public @NotNull List<RStorage> findAllWithPlayer() {
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage from RStorage storage join fetch storage.player player order by player.playerUuid asc, storage.storageKey asc",
                RStorage.class
            )
            .getResultList()));
    }

    /**
     * Finds every storage the supplied player may access, including shared storages.
     *
     * @param playerUuid player UUID whose accessible storages should be loaded
     * @return immutable snapshot of storages the player may access
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @NotNull List<RStorage> findAccessibleByPlayerUuid(final @NotNull UUID playerUuid) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage from RStorage storage join fetch storage.player player order by player.playerUuid asc, storage.storageKey asc",
                RStorage.class
            )
            .getResultStream()
            .filter(storage -> storage.canAccess(validatedPlayerUuid))
            .toList()));
    }

    /**
     * Finds a storage row by identifier and eagerly loads its owning player.
     *
     * @param storageId storage row identifier
     * @return matching storage, or {@code null} when no row exists
     * @throws NullPointerException if {@code storageId} is {@code null}
     */
    public @Nullable RStorage findWithPlayerById(final @NotNull Long storageId) {
        final Long validatedStorageId = Objects.requireNonNull(storageId, "storageId cannot be null");
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage from RStorage storage join fetch storage.player player where storage.id = :storageId",
                RStorage.class
            )
            .setParameter("storageId", validatedStorageId)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    /**
     * Finds the storage bound to the supplied hotkey for the given player.
     *
     * @param playerUuid player UUID that owns the storage
     * @param hotkey numeric storage hotkey to resolve
     * @return matching storage, or {@code null} when no binding exists
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     * @throws IllegalArgumentException if {@code hotkey} is less than {@code 1}
     */
    public @Nullable RStorage findByPlayerAndHotkey(
        final @NotNull UUID playerUuid,
        final int hotkey
    ) {
        if (hotkey < 1) {
            throw new IllegalArgumentException("hotkey must be greater than zero");
        }

        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage from RStorage storage join fetch storage.player player where player.playerUuid = :playerUuid and storage.hotkey = :hotkey",
                RStorage.class
            )
            .setParameter("playerUuid", validatedPlayerUuid)
            .setParameter("hotkey", hotkey)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    /**
     * Returns the assigned storage hotkeys for the supplied player UUID.
     *
     * @param playerUuid player UUID whose hotkeys should be listed
     * @return immutable ascending list of assigned hotkeys
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @NotNull List<Integer> findAssignedHotkeys(final @NotNull UUID playerUuid) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select storage.hotkey from RStorage storage join storage.player player where player.playerUuid = :playerUuid and storage.hotkey is not null order by storage.hotkey asc",
                Integer.class
            )
            .setParameter("playerUuid", validatedPlayerUuid)
            .getResultList()));
    }

    /**
     * Assigns a hotkey to the target storage, clearing any duplicate binding for the same player in.
     * the same transaction.
     *
     * @param storageId target storage row identifier
     * @param playerUuid player UUID that must own the storage
     * @param hotkey positive hotkey number to assign
     * @return {@code true} when the target storage exists for the player and the hotkey was saved
     * @throws NullPointerException if {@code storageId} or {@code playerUuid} is {@code null}
     * @throws IllegalArgumentException if {@code hotkey} is less than {@code 1}
     */
    public boolean assignHotkey(
        final @NotNull Long storageId,
        final @NotNull UUID playerUuid,
        final int hotkey
    ) {
        if (hotkey < 1) {
            throw new IllegalArgumentException("hotkey must be greater than zero");
        }

        final Long validatedStorageId = Objects.requireNonNull(storageId, "storageId cannot be null");
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");

        return this.executeInTransaction(entityManager -> {
            final List<RStorage> playerStorages = entityManager.createQuery(
                    "select storage from RStorage storage join fetch storage.player player where player.playerUuid = :playerUuid order by storage.id asc",
                    RStorage.class
                )
                .setParameter("playerUuid", validatedPlayerUuid)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

            RStorage targetStorage = null;
            for (final RStorage storage : playerStorages) {
                if (Objects.equals(storage.getId(), validatedStorageId)) {
                    targetStorage = storage;
                }
            }

            if (targetStorage == null) {
                return false;
            }

            for (final RStorage storage : playerStorages) {
                if (!Objects.equals(storage.getId(), validatedStorageId) && Objects.equals(storage.getHotkey(), hotkey)) {
                    storage.clearHotkey();
                }
            }

            targetStorage.setHotkey(hotkey);
            return true;
        });
    }

    /**
     * Updates the trust status for a player on the supplied storage.
     *
     * @param storageId target storage row identifier
     * @param ownerUuid player UUID that must own the storage
     * @param targetPlayerUuid player UUID whose trust status should change
     * @param status replacement trust status
     * @return {@code true} when the storage exists for the owner and the trust update was saved
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean updateTrustStatus(
        final @NotNull Long storageId,
        final @NotNull UUID ownerUuid,
        final @NotNull UUID targetPlayerUuid,
        final @NotNull StorageTrustStatus status
    ) {
        final Long validatedStorageId = Objects.requireNonNull(storageId, "storageId cannot be null");
        final UUID validatedOwnerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid cannot be null");
        final UUID validatedTargetPlayerUuid = Objects.requireNonNull(targetPlayerUuid, "targetPlayerUuid cannot be null");
        final StorageTrustStatus validatedStatus = Objects.requireNonNull(status, "status cannot be null");

        return this.executeInTransaction(entityManager -> {
            final RStorage storage = entityManager.createQuery(
                    "select storage from RStorage storage join fetch storage.player player where storage.id = :storageId and player.playerUuid = :ownerUuid",
                    RStorage.class
                )
                .setParameter("storageId", validatedStorageId)
                .setParameter("ownerUuid", validatedOwnerUuid)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (storage == null) {
                return false;
            }

            storage.setTrustStatus(validatedTargetPlayerUuid, validatedStatus);
            return true;
        });
    }

    private @NotNull LeaseAcquireResult tryAcquireLease(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull LocalDateTime leaseExpiresAt
    ) {
        return this.executeInTransaction(entityManager -> {
            final RStorage storage = entityManager.find(RStorage.class, storageId, LockModeType.PESSIMISTIC_WRITE);
            if (storage == null) {
                return LeaseAcquireResult.MISSING;
            }

            final LocalDateTime now = LocalDateTime.now();
            if (storage.hasActiveLease(now) && !storage.isLeaseHeldBy(serverUuid, playerUuid, leaseToken)) {
                return LeaseAcquireResult.LOCKED;
            }

            storage.acquireLease(serverUuid, playerUuid, leaseToken, leaseExpiresAt);
            return LeaseAcquireResult.ACQUIRED;
        });
    }

    private boolean renewLease(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull LocalDateTime leaseExpiresAt
    ) {
        return this.executeInTransaction(entityManager -> {
            final RStorage storage = entityManager.find(RStorage.class, storageId, LockModeType.PESSIMISTIC_WRITE);
            if (storage == null || !storage.isLeaseHeldBy(serverUuid, playerUuid, leaseToken)) {
                return false;
            }

            storage.renewLease(leaseExpiresAt);
            return true;
        });
    }

    private boolean saveInventoryAndReleaseLease(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull Map<Integer, ItemStack> inventory
    ) {
        return this.executeInTransaction(entityManager -> {
            final RStorage storage = entityManager.find(RStorage.class, storageId, LockModeType.PESSIMISTIC_WRITE);
            if (storage == null || !storage.isLeaseHeldBy(serverUuid, playerUuid, leaseToken)) {
                return false;
            }

            storage.setInventory(inventory);
            storage.clearLease();
            return true;
        });
    }

    private boolean releaseLease(
        final @NotNull Long storageId,
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken
    ) {
        return this.executeInTransaction(entityManager -> {
            final RStorage storage = entityManager.find(RStorage.class, storageId, LockModeType.PESSIMISTIC_WRITE);
            if (storage == null || !storage.isLeaseHeldBy(serverUuid, playerUuid, leaseToken)) {
                return false;
            }

            storage.clearLease();
            return true;
        });
    }
}
