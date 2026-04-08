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

import com.raindropcentral.rdt.database.entity.RDTPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted {@link RDTPlayer} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRDTPlayer extends CachedRepository<RDTPlayer, Long, UUID> {

    /**
     * Creates the player repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDTPlayer(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDTPlayer> entityClass,
        final @NotNull Function<RDTPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a player record by player UUID.
     *
     * @param playerUuid player UUID
     * @return matching player record, or {@code null} when none exists
     */
    public @Nullable RDTPlayer findByPlayer(final @NotNull UUID playerUuid) {
        return findByAttributes(Map.of("player_uuid", playerUuid)).orElse(null);
    }

    /**
     * Finds a player record by identifier.
     *
     * @param identifier player UUID
     * @return matching player record, or {@code null} when none exists
     */
    public @Nullable RDTPlayer findByIdentifier(final @NotNull UUID identifier) {
        return this.findByPlayer(identifier);
    }
}
