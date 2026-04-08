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

import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link RPlayer} entities. Operations run on the.
 * supplied executor to avoid blocking Bukkit threads and leverage identifier-based caching for
 * {@code r_player} rows.
 *
 * <p>Service layers calling this repository are expected to emit structured logs through
 * {@link com.raindropcentral.rplatform.logging.CentralLogger CentralLogger} whenever an
 * asynchronous lookup falls through the cache (an empty {@link Optional} result), prior to
 * scheduling create, update, or delete mutations, and upon completion of those mutations. Error
 * paths—both exceptional futures and detected validation failures—must also be logged with
 * correlation identifiers so cluster-wide auditing can trace persistence issues back to the
 * originating request.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@InjectRepository
public class RPlayerRepository extends CachedRepository<RPlayer, Long, UUID> {

    /**
     * Creates the repository binding it to the module executor and entity manager factory.
     *
     * @param executor             asynchronous executor used for query execution
     * @param entityManagerFactory JPA factory providing entity managers
     */
    public RPlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RPlayer> entityClass,
            @NotNull Function<RPlayer, UUID> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Retrieves a player by UUID using asynchronous execution. The lookup first consults the.
     * identifier cache maintained by {@link CachedRepository}, falling back to the entity
     * manager when needed, and executes on the repository's dedicated executor. The result is
     * wrapped in an {@link Optional} so callers can safely react to missing records.
     *
     * @param uniqueId player UUID to search for
     * @return future resolving to an optional containing the cached or freshly loaded entity
     */
    public CompletableFuture<Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        return findByKeyAsync("uniqueId", uniqueId);
    }

    /**
     * Looks up a player by the stored username. The cache is consulted with the username key.
     * before hitting the database, and the supplied executor keeps the work off the Bukkit
     * threads. The optional return communicates whether a cached or persisted record exists
     * without forcing null checks on consumers.
     *
     * @param playerName username to search for
     * @return future containing an optional with the resolved player when found
     */
    public CompletableFuture<Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        return CompletableFuture.supplyAsync(
            () -> findByAttribute("playerName", playerName),
            getExecutorService()
        );
    }

    /**
     * Checks if a player row exists for the provided UUID. This reuses the cached UUID lookup so.
     * repeated probes can short-circuit to cached values while staying on the managed executor.
     *
     * @param uniqueId identifier to probe
     * @return future evaluating to {@code true} when the player exists
     */
    public CompletableFuture<Boolean> existsByUuidAsync(final @NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
            .thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates the supplied player depending on whether the UUID already exists. The.
     * player is validated for nullity prior to scheduling, then the cache-backed existence check
     * determines whether to branch into {@link #updateAsync(Object)} or {@link #createAsync(Object)}.
     * Both paths execute asynchronously on the repository executor so write operations respect the
     * same threading guarantees as lookups, and cache state is refreshed by the parent
     * implementation after persistence.
     *
     * @param player player entity to persist
     * @return future resolving to the managed entity after persistence
     */
    public CompletableFuture<RPlayer> createOrUpdateAsync(final @NotNull RPlayer player) {

        return existsByUuidAsync(player.getUniqueId())
            .thenCompose(exists -> exists
                ? updateAsync(player)
                : createAsync(player)
            );
    }
}
