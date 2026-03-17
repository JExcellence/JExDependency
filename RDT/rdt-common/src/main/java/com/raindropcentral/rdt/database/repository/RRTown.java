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

package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RTown;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for {@link com.raindropcentral.rdt.database.entity.RTown} entities.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Provide standard CRUD operations backed by {@link CachedRepository}</li>
 *   <li>Expose convenience finders for common lookups (by mayor, name, or town UUID)</li>
 *   <li>Support async usage through the provided {@link ExecutorService}</li>
 * </ul>
 * Notes:
 * <ul>
 *   <li>Blocking operations should be executed off the main server thread.</li>
 * </ul>
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
/**
 * Represents the RRTown API type.
 */
public class RRTown extends CachedRepository<RTown, Long, UUID> {

    // Keep a reference to the EntityManagerFactory for custom ad-hoc queries
    private final EntityManagerFactory emf;
    private final String fallbackServerId;

    /**
     * Construct a repository for {@link RTown}.
     *
     * @param executorService       executor for async operations
     * @param entityManagerFactory  JPA entity manager factory
     * @param entityClass           entity class (typically {@link RTown}.class)
     * @param keyExtractor          cache key extractor (unique external identifier)
     */
    public RRTown(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RTown> entityClass,
            @NotNull Function<RTown, UUID> keyExtractor
    ) {
        this(executorService, entityManagerFactory, entityClass, keyExtractor, "server");
    }

    /**
     * Construct a repository for {@link RTown} with one fallback server identifier.
     *
     * @param executorService executor for async operations
     * @param entityManagerFactory JPA entity manager factory
     * @param entityClass entity class (typically {@link RTown}.class)
     * @param keyExtractor cache key extractor (unique external identifier)
     * @param fallbackServerId fallback server ID used for legacy ownership backfill
     */
    public RRTown(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RTown> entityClass,
            @NotNull Function<RTown, UUID> keyExtractor,
            @NotNull String fallbackServerId
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
        this.fallbackServerId = fallbackServerId;
    }

    /**
     * Find the town where the given UUID is the mayor.
     * IMPORTANT: Run on a background thread; do not call from the main server thread.
     *
     * @param mayor UUID of the player to check
     * @return the first town found where this player is mayor, or {@code null} if none
     */
    public RTown findByMayor(UUID mayor) {
        return this.backfillAndReturn(findByAttributes(Map.of("mayor", mayor)).orElse(null));
    }


    /**
     * Find a town by its human-friendly name.
     *
     * @param townName case-sensitive town name
     * @return matching town or {@code null} if none
     */
    public RTown findByTName(String townName) {
        return this.backfillAndReturn(findByAttributes(Map.of("townName", townName)).orElse(null));
    }

    /**
     * Find a town by its public UUID identifier.
     *
     * @param uuid town UUID
     * @return matching town or {@code null} if none
     */
    public RTown findByTownUUID(UUID uuid) {
        return this.backfillAndReturn(findByAttributes(Map.of("uuid", uuid)).orElse(null));
    }

    /**
     * Finds a town by its placed nexus block location.
     *
     * @param location exact persisted nexus location
     * @return matching town or {@code null} when no town owns that nexus location
     */
    public @Nullable RTown findByNexusLocation(final @NotNull Location location) {
        return this.backfillAndReturn(findByAttributes(Map.of("nexus_location", location)).orElse(null));
    }

    private @Nullable RTown backfillAndReturn(final @Nullable RTown town) {
        if (town == null) {
            return null;
        }
        if (town.ensureAuthoritativeServerOwnership(this.fallbackServerId)) {
            this.update(town);
        }
        return town;
    }

}
