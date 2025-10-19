package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository facade handling asynchronous CRUD access for {@link RDQPlayer} entities.
 *
 * <p>The repository delegates caching and entity manager operations to the shared
 * {@link GenericCachedRepository} infrastructure while providing player specific lookup
 * helpers.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RDQPlayerRepository extends GenericCachedRepository<RDQPlayer, Long, UUID> {

    /**
     * Creates a new repository using the provided asynchronous executor and entity manager
     * factory for persistence operations.
     *
     * @param executor the executor responsible for asynchronous database work
     * @param entityManagerFactory the entity manager factory powering persistence access
     */
    public RDQPlayerRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RDQPlayer.class, RDQPlayer::getUniqueId);
    }

    /**
     * Asynchronously retrieves a player by their unique identifier.
     *
     * @param uniqueId the player's unique identifier
     * @return a future completing with the matching player when present
     */
    public @NotNull CompletableFuture<Optional<RDQPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        return findByCacheKeyAsync("uniqueId", uniqueId)
                .thenApply(Optional::ofNullable);
    }

    /**
     * Asynchronously retrieves a player using their last known name.
     *
     * @param playerName the player name to search for
     * @return a future completing with the matching player when present
     */
    public @NotNull CompletableFuture<Optional<RDQPlayer>> findByNameAsync(final @NotNull String playerName) {
        return findByAttributesAsync(Map.of("playerName", playerName))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Determines asynchronously whether a player exists in storage for the provided unique
     * identifier.
     *
     * @param uniqueId the unique identifier to check
     * @return a future completing with {@code true} when the player exists, otherwise {@code false}
     */
    public @NotNull CompletableFuture<Boolean> existsByUuidAsync(final @NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
                .thenApply(Optional::isPresent);
    }

    /**
     * Creates a new player or updates an existing record based on their unique identifier.
     *
     * @param player the player to create or update
     * @return a future completing with the persisted player entity
     */
    public @NotNull CompletableFuture<RDQPlayer> createOrUpdateAsync(final @NotNull RDQPlayer player) {
        return existsByUuidAsync(player.getUniqueId())
                .thenCompose(exists -> exists ? updateAsync(player) : createAsync(player));
    }
}