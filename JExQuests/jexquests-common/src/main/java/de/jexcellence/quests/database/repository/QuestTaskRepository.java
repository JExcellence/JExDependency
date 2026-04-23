package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestTask;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for QuestTask entities.
 */
public class QuestTaskRepository extends AbstractCrudRepository<QuestTask, Long> {

    /**
     * Constructs a QuestTaskRepository.
     */
    public QuestTaskRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<QuestTask> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Returns tasks for the given quest. Callers must sort by
     * {@link QuestTask#getOrderIndex()} — the query is unordered so
     * the service layer decides.
     */
    public @NotNull CompletableFuture<List<QuestTask>> findByQuestAsync(@NotNull Quest quest) {
        return query().and("quest", quest).listAsync();
    }

    /** Synchronous variant used by the content loader's upsert path. */
    public @NotNull List<QuestTask> findByQuest(@NotNull Quest quest) {
        return query().and("quest", quest).list();
    }

    /** Find a specific task within a quest by its task identifier. */
    public @NotNull java.util.Optional<QuestTask> findByQuestAndTaskIdentifier(
            @NotNull Quest quest, @NotNull String taskIdentifier) {
        return query().and("quest", quest).and("taskIdentifier", taskIdentifier).first();
    }
}
