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

import com.raindropcentral.rda.SkillType;
import com.raindropcentral.rda.database.entity.RDASkillState;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted child skill states.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public class RRDASkillState extends CachedRepository<RDASkillState, Long, String> {

    /**
     * Creates the child skill-state repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDASkillState(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDASkillState> entityClass,
        final @NotNull Function<RDASkillState, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a child state by player UUID and skill type.
     *
     * @param playerUuid owning player UUID
     * @param skillType owning skill type
     * @return matching child state, or {@code null} when none exists
     */
    public @Nullable RDASkillState findByPlayerAndSkill(
        final @NotNull UUID playerUuid,
        final @NotNull SkillType skillType
    ) {
        return this.findByAttributes(Map.of(
            "playerProfile.playerUuid", playerUuid,
            "skillId", skillType.getId()
        )).orElse(null);
    }
}
