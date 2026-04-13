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

package com.raindropcentral.rda.database.repository;

import com.raindropcentral.rda.database.entity.RDAAgilityVisitedChunk;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted agility chunk visits.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public class RRDAAgilityVisitedChunk extends CachedRepository<RDAAgilityVisitedChunk, Long, String> {

    /**
     * Creates the agility chunk visit repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAAgilityVisitedChunk(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAAgilityVisitedChunk> entityClass,
        final @NotNull Function<RDAAgilityVisitedChunk, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a visited chunk by its stable chunk key.
     *
     * @param chunkKey stable chunk key
     * @return matching chunk row, or {@code null} when none exists
     */
    public @Nullable RDAAgilityVisitedChunk findByChunkKey(final @NotNull String chunkKey) {
        return this.findByAttributes(Map.of("chunkKey", chunkKey)).orElse(null);
    }
}
