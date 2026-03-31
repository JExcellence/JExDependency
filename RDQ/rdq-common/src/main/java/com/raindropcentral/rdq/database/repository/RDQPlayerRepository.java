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

package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link RDQPlayer} entities in the RaindropQuests system.
 *
 * <p>Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for player entities, using the player's unique UUID as the cache key.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RDQPlayerRepository extends CachedRepository<RDQPlayer, Long, UUID> {

    /**
     * Constructs a new {@code RDQPlayerRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     */
    public RDQPlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDQPlayer> entityClass,
            @NotNull Function<RDQPlayer, UUID> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
