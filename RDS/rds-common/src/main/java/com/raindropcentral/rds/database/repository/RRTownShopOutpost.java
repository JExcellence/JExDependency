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

import com.raindropcentral.rds.database.entity.TownShopOutpost;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for bound town-shop outpost entitlement rows.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTownShopOutpost extends CachedRepository<TownShopOutpost, Long, UUID> {

    /**
     * Creates the town-shop outpost repository.
     *
     * @param executorService async executor
     * @param entityManagerFactory entity manager factory
     * @param entityClass managed entity class
     * @param keyExtractor cache key extractor
     */
    public RRTownShopOutpost(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory,
        final @NotNull Class<TownShopOutpost> entityClass,
        final @NotNull Function<TownShopOutpost, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds one outpost row by chunk UUID.
     *
     * @param chunkUuid outpost chunk UUID
     * @return matching outpost row, or {@code null} when none exists
     */
    public @Nullable TownShopOutpost findByChunkUuid(final @NotNull UUID chunkUuid) {
        return findByAttributes(Map.of("chunkUuid", chunkUuid)).orElse(null);
    }

    /**
     * Finds one outpost row by chunk location.
     *
     * @param worldName world name
     * @param chunkX chunk x
     * @param chunkZ chunk z
     * @return matching outpost row, or {@code null} when none exists
     */
    public @Nullable TownShopOutpost findByChunkLocation(
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        final String normalizedWorldName = worldName.trim();
        return this.findAll().stream()
            .filter(outpost ->
                outpost.getWorldName().equalsIgnoreCase(normalizedWorldName)
                    && outpost.getChunkX() == chunkX
                    && outpost.getChunkZ() == chunkZ
            )
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all outpost rows for one town identifier.
     *
     * @param townIdentifier stable town identifier
     * @return immutable list of matching outpost rows
     */
    public @NotNull List<TownShopOutpost> findByTownIdentifier(final @NotNull String townIdentifier) {
        final String normalizedTownIdentifier = townIdentifier.trim().toLowerCase(Locale.ROOT);
        return this.findAll().stream()
            .filter(outpost -> outpost.getTownIdentifier().equalsIgnoreCase(normalizedTownIdentifier))
            .toList();
    }
}
