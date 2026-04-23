package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Quest;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for Quest entities.
 */
public class QuestRepository extends AbstractCrudRepository<Quest, Long> {

    /**
     * Constructs a QuestRepository.
     */
    public QuestRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<Quest> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a quest by identifier.
     */
    public @NotNull Optional<Quest> findByIdentifier(@NotNull String identifier) {
        return query().and("identifier", identifier).first();
    }

    /**
     * Finds a quest by identifier asynchronously.
     */
    public @NotNull CompletableFuture<Optional<Quest>> findByIdentifierAsync(@NotNull String identifier) {
        return query().and("identifier", identifier).firstAsync();
    }

    /**
     * Finds quests by category asynchronously.
     */
    public @NotNull CompletableFuture<List<Quest>> findByCategoryAsync(@NotNull String category) {
        return query().and("category", category).and("enabled", true).listAsync();
    }

    /**
     * Finds all enabled quests asynchronously.
     */
    public @NotNull CompletableFuture<List<Quest>> findEnabledAsync() {
        return query().and("enabled", true).listAsync();
    }
}
