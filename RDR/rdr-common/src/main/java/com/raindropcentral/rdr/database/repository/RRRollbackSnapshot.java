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

import com.raindropcentral.rdr.database.entity.RRollbackSnapshot;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for persisted rollback snapshots and snapshot-history pruning.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRRollbackSnapshot extends BaseRepository<RRollbackSnapshot, Long> {

    /**
     * Creates a repository for {@link RRollbackSnapshot} entities.
     *
     * @param executorService executor used for async repository operations
     * @param entityManagerFactory entity manager factory used for persistence operations
     */
    public RRRollbackSnapshot(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RRollbackSnapshot.class);
    }

    /**
     * Creates one rollback snapshot and prunes history to the supplied retention limit.
     *
     * @param snapshot snapshot to create
     * @param maxSnapshotsPerPlayer maximum snapshots retained per player
     * @return async created snapshot
     */
    public @NotNull CompletableFuture<RRollbackSnapshot> createAndPruneAsync(
        final @NotNull RRollbackSnapshot snapshot,
        final int maxSnapshotsPerPlayer
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final RRollbackSnapshot createdSnapshot = this.create(Objects.requireNonNull(snapshot, "snapshot cannot be null"));
            this.pruneOldestSnapshots(createdSnapshot.getTargetPlayerUuid(), maxSnapshotsPerPlayer);
            return createdSnapshot;
        }, this.getExecutorService());
    }

    /**
     * Returns every snapshot for one player ordered newest-first with storage children preloaded.
     *
     * @param playerUuid target player UUID
     * @return immutable newest-first snapshot list
     */
    public @NotNull List<RRollbackSnapshot> findByPlayerUuid(final @NotNull UUID playerUuid) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select distinct snapshot from RRollbackSnapshot snapshot "
                    + "join fetch snapshot.player player "
                    + "left join fetch snapshot.storageSnapshots storageSnapshots "
                    + "where snapshot.targetPlayerUuid = :playerUuid "
                    + "order by snapshot.createdAt desc, snapshot.id desc",
                RRollbackSnapshot.class
            )
            .setParameter("playerUuid", validatedPlayerUuid)
            .getResultList()));
    }

    /**
     * Returns one snapshot with child storage snapshots preloaded.
     *
     * @param snapshotId target snapshot identifier
     * @return loaded snapshot, or {@code null} when none exists
     */
    public @Nullable RRollbackSnapshot findByIdWithChildren(final @NotNull Long snapshotId) {
        final Long validatedSnapshotId = Objects.requireNonNull(snapshotId, "snapshotId cannot be null");
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select distinct snapshot from RRollbackSnapshot snapshot "
                    + "join fetch snapshot.player player "
                    + "left join fetch snapshot.storageSnapshots storageSnapshots "
                    + "where snapshot.id = :snapshotId",
                RRollbackSnapshot.class
            )
            .setParameter("snapshotId", validatedSnapshotId)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    /**
     * Returns every snapshot ordered by player and recency with player rows preloaded.
     *
     * @return immutable snapshot list ordered by player UUID then creation timestamp descending
     */
    public @NotNull List<RRollbackSnapshot> findAllWithPlayer() {
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select snapshot from RRollbackSnapshot snapshot "
                    + "join fetch snapshot.player player "
                    + "order by snapshot.targetPlayerUuid asc, snapshot.createdAt desc, snapshot.id desc",
                RRollbackSnapshot.class
            )
            .getResultList()));
    }

    /**
     * Prunes oldest snapshots for one player beyond the requested retention limit.
     *
     * @param playerUuid target player UUID
     * @param maxSnapshotsPerPlayer positive snapshot retention limit
     * @return number of removed snapshots
     */
    public int pruneOldestSnapshots(
        final @NotNull UUID playerUuid,
        final int maxSnapshotsPerPlayer
    ) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        final int retentionLimit = Math.max(1, maxSnapshotsPerPlayer);
        return this.executeInTransaction(entityManager -> {
            final List<RRollbackSnapshot> snapshots = entityManager.createQuery(
                    "select snapshot from RRollbackSnapshot snapshot "
                        + "where snapshot.targetPlayerUuid = :playerUuid "
                        + "order by snapshot.createdAt desc, snapshot.id desc",
                    RRollbackSnapshot.class
                )
                .setParameter("playerUuid", validatedPlayerUuid)
                .getResultList();

            int removedSnapshots = 0;
            for (int index = retentionLimit; index < snapshots.size(); index++) {
                entityManager.remove(snapshots.get(index));
                removedSnapshots++;
            }
            return removedSnapshots;
        });
    }
}
