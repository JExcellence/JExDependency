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
import com.raindropcentral.rda.database.entity.RDASkillPreference;
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
 * Cached repository for persisted skill-trigger preferences.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public class RRDASkillPreference extends CachedRepository<RDASkillPreference, Long, String> {

    /**
     * Creates the skill-preference repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDASkillPreference(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDASkillPreference> entityClass,
        final @NotNull Function<RDASkillPreference, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds one player's trigger preference for the supplied skill.
     *
     * @param playerUuid owning player UUID
     * @param skillType owning skill
     * @return matching preference row, or {@code null} when none exists
     */
    public @Nullable RDASkillPreference findByPlayerAndSkill(
        final @NotNull UUID playerUuid,
        final @NotNull SkillType skillType
    ) {
        return this.findByAttributes(Map.of(
            "playerProfile.playerUuid", playerUuid,
            "skillId", skillType.getId()
        )).orElse(null);
    }

    /**
     * Finds every trigger preference row owned by the supplied player.
     *
     * @param playerUuid owning player UUID
     * @return preference rows owned by the player
     */
    public @NotNull List<RDASkillPreference> findAllByPlayer(final @NotNull UUID playerUuid) {
        return this.findAll().stream()
            .filter(preference -> preference.getPlayerProfile().getPlayerUuid().equals(playerUuid))
            .toList();
    }
}
