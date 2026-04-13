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

package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.player.RBossBarPreference;
import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted RCore boss-bar preference rows.
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
@InjectRepository
public class RBossBarPreferenceRepository extends CachedRepository<RBossBarPreference, Long, String> {

    /**
     * Creates the repository.
     *
     * @param executorService async executor for repository work
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RBossBarPreferenceRepository(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RBossBarPreference> entityClass,
        final @NotNull Function<RBossBarPreference, String> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds one preference row by player UUID and provider key.
     *
     * @param playerUuid owning player UUID
     * @param providerKey provider key
     * @return stored preference row, or {@code null} when absent
     */
    public @Nullable RBossBarPreference findByPlayerAndProvider(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        return this.findByAttributes(Map.of(
            "playerUuid", playerUuid,
            "providerKey", providerKey.trim().toLowerCase(java.util.Locale.ROOT)
        )).orElse(null);
    }

    /**
     * Finds every stored preference row for one player.
     *
     * @param playerUuid owning player UUID
     * @return stored preference rows, possibly empty
     */
    public @NotNull List<RBossBarPreference> findAllByPlayer(final @NotNull UUID playerUuid) {
        return this.findAllByAttributes(Map.of("playerUuid", playerUuid));
    }
}
