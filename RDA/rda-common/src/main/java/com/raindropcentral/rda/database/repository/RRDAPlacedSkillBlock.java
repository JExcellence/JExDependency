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

import com.raindropcentral.rda.PlacedTrackedBlockRepository;
import com.raindropcentral.rda.database.entity.RDAPlacedSkillBlock;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted placed skill block markers.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public class RRDAPlacedSkillBlock extends CachedRepository<RDAPlacedSkillBlock, Long, String>
    implements PlacedTrackedBlockRepository<RDAPlacedSkillBlock> {

    /**
     * Creates the placed skill block repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAPlacedSkillBlock(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAPlacedSkillBlock> entityClass,
        final @NotNull Function<RDAPlacedSkillBlock, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a placed-block marker by its stable location key.
     *
     * @param locationKey stable location key
     * @return matching marker, or {@code null} when none exists
     */
    public @Nullable RDAPlacedSkillBlock findByLocationKey(final @NotNull String locationKey) {
        return this.findByAttributes(Map.of("locationKey", locationKey)).orElse(null);
    }
}
