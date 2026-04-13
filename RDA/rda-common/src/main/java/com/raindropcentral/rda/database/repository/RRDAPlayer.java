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

import com.raindropcentral.rda.database.entity.RDAPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for persisted {@link RDAPlayer} records.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRDAPlayer extends CachedRepository<RDAPlayer, Long, UUID> {

    private final Map<UUID, Object> creationLocks = new ConcurrentHashMap<>();

    /**
     * Creates the player repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRDAPlayer(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<RDAPlayer> entityClass,
        final @NotNull Function<RDAPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a player record by player UUID.
     *
     * @param playerUuid player UUID
     * @return matching player record, or {@code null} when none exists
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @Nullable RDAPlayer findByPlayer(final @NotNull UUID playerUuid) {
        return findByAttributes(Map.of("playerUuid", playerUuid)).orElse(null);
    }

    /**
     * Finds an existing player record or creates one atomically for the supplied player UUID.
     *
     * <p>The repository serializes concurrent first-write attempts for the same player within this
     * runtime and falls back to a post-failure lookup so unique-key races resolve to the winning
     * row instead of surfacing as an exception.</p>
     *
     * @param playerUuid player UUID
     * @return existing or newly created player record
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @NotNull RDAPlayer findOrCreateByPlayer(final @NotNull UUID playerUuid) {
        final UUID resolvedPlayerUuid = java.util.Objects.requireNonNull(playerUuid, "playerUuid");
        final RDAPlayer existingProfile = this.findByPlayer(resolvedPlayerUuid);
        if (existingProfile != null) {
            return existingProfile;
        }

        final Object creationLock = this.creationLocks.computeIfAbsent(resolvedPlayerUuid, ignored -> new Object());
        synchronized (creationLock) {
            try {
                final RDAPlayer concurrentProfile = this.findByPlayer(resolvedPlayerUuid);
                if (concurrentProfile != null) {
                    return concurrentProfile;
                }

                try {
                    return this.create(new RDAPlayer(resolvedPlayerUuid));
                } catch (final RuntimeException exception) {
                    final RDAPlayer persistedProfile = this.findByPlayer(resolvedPlayerUuid);
                    if (persistedProfile != null) {
                        return persistedProfile;
                    }
                    throw exception;
                }
            } finally {
                this.creationLocks.remove(resolvedPlayerUuid, creationLock);
            }
        }
    }
}
