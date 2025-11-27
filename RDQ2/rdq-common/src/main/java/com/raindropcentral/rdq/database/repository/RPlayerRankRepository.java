package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository providing asynchronous access to {@link RPlayerRank} associations for a specific player.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RPlayerRankRepository extends GenericCachedRepository<RPlayerRank, Long, Long> {

    /**
     * Creates a new repository that performs asynchronous operations using the provided executor and entity manager.
     *
     * @param executor              the executor service used for asynchronous execution
     * @param entityManagerFactory  the entity manager factory used for database interactions
     */
    public RPlayerRankRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerRank.class, AbstractEntity::getId);
    }

    /**
     * Retrieves the player rank for the supplied player and rank tree, if present.
     *
     * @param player   the player whose rank is requested
     * @param rankTree the rank tree containing the player rank
     * @return a future producing an optional containing the matching player rank when it exists
     */
    public @NotNull CompletableFuture<Optional<RPlayerRank>> findByPlayerAndRankTreeAsync(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree rankTree
    ) {
        return findByAttributesAsync(Map.of("player", player, "rankTree", rankTree))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves all player ranks associated with the given player.
     *
     * @param player the player whose ranks should be returned
     * @return a future producing a list of all ranks for the player
     */
    public @NotNull CompletableFuture<List<RPlayerRank>> findAllByPlayerAsync(final @NotNull RDQPlayer player) {
        return findListByAttributesAsync(Map.of("player", player));
    }

    /**
     * Retrieves the active player rank for the provided player, if one exists.
     *
     * @param player the player whose active rank is requested
     * @return a future producing an optional containing the active player rank when it exists
     */
    public @NotNull CompletableFuture<Optional<RPlayerRank>> findActiveByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByAttributesAsync(Map.of("player", player, "isActive", true))
                .thenApply(Optional::ofNullable);
    }
}