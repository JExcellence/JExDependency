package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.PlayerRank;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for PlayerRank entities.
 */
public class PlayerRankRepository extends AbstractCrudRepository<PlayerRank, Long> {

    /**
     * Constructs a PlayerRankRepository.
     */
    public PlayerRankRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerRank> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a player rank by player and tree identifier.
     */
    public @NotNull CompletableFuture<Optional<PlayerRank>> findAsync(
            @NotNull UUID playerUuid, @NotNull String treeIdentifier) {
        return query()
                .and("playerUuid", playerUuid)
                .and("treeIdentifier", treeIdentifier)
                .firstAsync();
    }

    /**
     * Finds all player ranks by player UUID.
     */
    public @NotNull CompletableFuture<List<PlayerRank>> findByPlayerAsync(@NotNull UUID playerUuid) {
        return query().and("playerUuid", playerUuid).listAsync();
    }

    /**
     * Returns every player-rank row for the given tree — used by the
     * leaderboard path in the service layer to join against rank
     * ordinals.
     */
    public @NotNull CompletableFuture<List<PlayerRank>> findAllByTreeAsync(@NotNull String treeIdentifier) {
        return query().and("treeIdentifier", treeIdentifier).listAsync();
    }
}
