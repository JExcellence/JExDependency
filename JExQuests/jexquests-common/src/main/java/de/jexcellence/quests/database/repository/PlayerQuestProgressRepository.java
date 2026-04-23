package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.QuestStatus;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for PlayerQuestProgress entities.
 */
public class PlayerQuestProgressRepository extends AbstractCrudRepository<PlayerQuestProgress, Long> {

    /**
     * Constructs a PlayerQuestProgressRepository.
     */
    public PlayerQuestProgressRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerQuestProgress> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds player quest progress by player and quest identifier.
     */
    public @NotNull CompletableFuture<Optional<PlayerQuestProgress>> findAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier) {
        return query()
                .and("playerUuid", playerUuid)
                .and("questIdentifier", questIdentifier)
                .firstAsync();
    }

    /**
     * Finds all player quest progress by player UUID.
     */
    public @NotNull CompletableFuture<List<PlayerQuestProgress>> findByPlayerAsync(@NotNull UUID playerUuid) {
        return query().and("playerUuid", playerUuid).listAsync();
    }

    /**
     * Finds active player quest progress by player UUID.
     */
    public @NotNull CompletableFuture<List<PlayerQuestProgress>> findActiveForPlayerAsync(@NotNull UUID playerUuid) {
        return query()
                .and("playerUuid", playerUuid)
                .and("status", QuestStatus.ACTIVE)
                .listAsync();
    }

    /**
     * Finds player quest progress by status.
     */
    public @NotNull CompletableFuture<List<PlayerQuestProgress>> findByStatusAsync(@NotNull QuestStatus status) {
        return query().and("status", status).listAsync();
    }
}
