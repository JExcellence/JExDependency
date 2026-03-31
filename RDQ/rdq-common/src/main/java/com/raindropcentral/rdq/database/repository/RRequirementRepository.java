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

import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 *
 *
 * <p>Extends {@link de.jexcellence.hibernate.repository.CachedRepository} to provide caching and asynchronous database operations.
 * for player entities, using the player's unique UUID as the cache key.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRequirementRepository extends CachedRepository<BaseRequirement, Long, Long> {

    /**
     * Constructs a new {@code RDQPlayerRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link java.util.concurrent.ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link jakarta.persistence.EntityManagerFactory} for JPA entity management
     */
    public RRequirementRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory,
            @NotNull Class<BaseRequirement> entityClass,
            @NotNull Function<BaseRequirement, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
