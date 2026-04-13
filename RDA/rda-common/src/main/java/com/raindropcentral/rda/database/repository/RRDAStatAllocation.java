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

import com.raindropcentral.rda.CoreStatType;
import com.raindropcentral.rda.database.entity.RDAStatAllocation;
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
 * Cached repository for persisted stat-allocation rows.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public class RRDAStatAllocation extends CachedRepository<RDAStatAllocation, Long, String> {

    /**
     * Creates the stat-allocation repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRDAStatAllocation(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAStatAllocation> entityClass,
        final @NotNull Function<RDAStatAllocation, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds one player's allocation row for the supplied stat.
     *
     * @param playerUuid owning player UUID
     * @param coreStatType allocated stat
     * @return matching allocation row, or {@code null} when none exists
     */
    public @Nullable RDAStatAllocation findByPlayerAndStat(
        final @NotNull UUID playerUuid,
        final @NotNull CoreStatType coreStatType
    ) {
        return this.findByAttributes(Map.of(
            "playerProfile.playerUuid", playerUuid,
            "statId", coreStatType.getId()
        )).orElse(null);
    }

    /**
     * Finds every allocation row owned by the supplied player.
     *
     * @param playerUuid owning player UUID
     * @return allocation rows owned by the player
     */
    public @NotNull List<RDAStatAllocation> findAllByPlayer(final @NotNull UUID playerUuid) {
        return this.findAll().stream()
            .filter(allocation -> allocation.getPlayerProfile().getPlayerUuid().equals(playerUuid))
            .toList();
    }
}
