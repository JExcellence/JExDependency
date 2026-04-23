package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for PlayerTaskProgress entities.
 */
public class PlayerTaskProgressRepository extends AbstractCrudRepository<PlayerTaskProgress, Long> {

    /**
     * Constructs a PlayerTaskProgressRepository.
     */
    public PlayerTaskProgressRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerTaskProgress> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds player task progress by player, quest and task identifier.
     */
    public @NotNull CompletableFuture<Optional<PlayerTaskProgress>> findAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier, @NotNull String taskIdentifier) {
        return query()
                .and("playerUuid", playerUuid)
                .and("questIdentifier", questIdentifier)
                .and("taskIdentifier", taskIdentifier)
                .firstAsync();
    }

    /**
     * Finds player task progress by player and quest identifier.
     */
    public @NotNull CompletableFuture<List<PlayerTaskProgress>> findByPlayerAndQuestAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier) {
        return query()
                .and("playerUuid", playerUuid)
                .and("questIdentifier", questIdentifier)
                .listAsync();
    }
}
