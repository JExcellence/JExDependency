package com.raindropcentral.core.database.repository;


import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository providing cached CRUD access to {@link RPlayer} entities. Operations run on the
 * supplied executor to avoid blocking Bukkit threads and leverage identifier-based caching for
 * {@code r_player} rows.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RPlayerRepository extends GenericCachedRepository<RPlayer, Long, UUID> {

    /**
     * Creates the repository binding it to the module executor and entity manager factory.
     *
     * @param executor             asynchronous executor used for query execution
     * @param entityManagerFactory JPA factory providing entity managers
     */
    public RPlayerRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayer.class, RPlayer::getUniqueId);
    }

    /**
     * Retrieves a player by UUID using asynchronous execution.
     *
     * @param uniqueId player UUID to search for
     * @return future resolving to the matching player when present
     */
    public CompletableFuture<Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");

        return findByAttributesAsync(Map.of("uniqueId", uniqueId))
            .thenApply(Optional::ofNullable);
    }

    /**
     * Looks up a player by the stored username. Uses the repository executor for background
     * execution to maintain thread-safety with Bukkit.
     *
     * @param playerName username to search for
     * @return future containing the resolved player when found
     */
    public CompletableFuture<Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return findByAttributesAsync(Map.of("playerName", playerName))
            .thenApply(Optional::ofNullable);
    }

    /**
     * Checks if a player row exists for the provided UUID.
     *
     * @param uniqueId identifier to probe
     * @return future evaluating to {@code true} when the player exists
     */
    public CompletableFuture<Boolean> existsByUuidAsync(final @NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
            .thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates the supplied player depending on whether the UUID already exists.
     *
     * @param player player entity to persist
     * @return future resolving to the managed entity after persistence
     */
    public CompletableFuture<RPlayer> createOrUpdateAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        return existsByUuidAsync(player.getUniqueId())
            .thenCompose(exists -> exists
                ? updateAsync(player)
                : createAsync(player)
            );
    }
}
