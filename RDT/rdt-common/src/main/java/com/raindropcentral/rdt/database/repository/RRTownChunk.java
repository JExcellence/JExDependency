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

package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RTownChunk;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository for persisted {@link RTownChunk} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTownChunk extends BaseRepository<RTownChunk, Long> {

    /**
     * Creates the town chunk repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     */
    public RRTownChunk(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTownChunk.class);
    }

    /**
     * Finds all chunks owned by a town.
     *
     * @param townUuid town UUID
     * @return owned town chunks
     */
    public @NotNull List<RTownChunk> findByTownUuid(final @NotNull UUID townUuid) {
        final UUID validatedTownUuid = Objects.requireNonNull(townUuid, "townUuid");
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select chunk from RTownChunk chunk join fetch chunk.town town "
                    + "where town.townUuid = :townUuid order by chunk.worldName asc, chunk.xLoc asc, chunk.zLoc asc",
                RTownChunk.class
            )
            .setParameter("townUuid", validatedTownUuid)
            .getResultList()));
    }

    /**
     * Finds a claimed chunk by world and chunk coordinates while eagerly loading its owning town so
     * detached runtime checks can safely read town protection data.
     *
     * @param worldName world name
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return matching town chunk, or {@code null} when none exists
     */
    public @Nullable RTownChunk findByChunk(final @NotNull String worldName, final int chunkX, final int chunkZ) {
        final String normalizedWorldName = Objects.requireNonNull(worldName, "worldName").trim();
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select chunk from RTownChunk chunk join fetch chunk.town "
                    + "where chunk.worldName = :worldName and chunk.xLoc = :chunkX and chunk.zLoc = :chunkZ",
                RTownChunk.class
            )
            .setParameter("worldName", normalizedWorldName)
            .setParameter("chunkX", chunkX)
            .setParameter("chunkZ", chunkZ)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }
}
