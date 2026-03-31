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

package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.RDSPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Represents r r d s player.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
/**
 * Represents the RRDSPlayer API type.
 */
public class RRDSPlayer extends CachedRepository<RDSPlayer, Long, UUID> {

    private final EntityManagerFactory emf;

    /**
     * Creates a new r r d s player.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     * @param entityClass entity type managed by the repository
     * @param keyExtractor cache key extractor for loaded entities
     */
    public RRDSPlayer(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDSPlayer> entityClass,
            @NotNull Function<RDSPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Finds by player.
     *
     * @param player_uuid player identifier to look up
     * @return the matched by player, or {@code null} when none exists
     */
    public RDSPlayer findByPlayer(UUID player_uuid) {
        return findByAttributes(Map.of("player_uuid", player_uuid)).orElse(null);
    }

}
