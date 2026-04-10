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

import com.raindropcentral.rdr.database.entity.RDRPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for persisted {@link RDRPlayer} records.
 *
 * <p>This repository exposes player-centric lookup helpers while retaining the cached repository behavior
 * supplied by JEHibernate.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRDRPlayer extends CachedRepository<RDRPlayer, Long, UUID> {

    /**
     * Creates a repository for {@link RDRPlayer} entities.
     *
     * @param executorService executor used for asynchronous repository operations
     * @param entityManagerFactory entity manager factory backing persistence operations
     * @param entityClass entity type managed by this repository
     * @param keyExtractor function extracting the cache key from a player entity
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRDRPlayer(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDRPlayer> entityClass,
        final @NotNull Function<RDRPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a player record by its unique player UUID.
     *
     * @param playerUuid player UUID to resolve
     * @return matching player entity, or {@code null} when no row exists
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @Nullable RDRPlayer findByPlayer(final @NotNull UUID playerUuid) {
        return findByAttributes(Map.of("playerUuid", playerUuid)).orElse(null);
    }

    /**
     * Returns every persisted RDR player ordered by player UUID.
     *
     * @return immutable snapshot of all persisted players
     */
    public @NotNull List<RDRPlayer> findAllPlayers() {
        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select player from RDRPlayer player order by player.playerUuid asc",
                RDRPlayer.class
            )
            .getResultList()));
    }
}
