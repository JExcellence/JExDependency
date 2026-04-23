package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.RankTree;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for RankTree entities.
 */
public class RankTreeRepository extends AbstractCrudRepository<RankTree, Long> {

    /**
     * Constructs a RankTreeRepository.
     */
    public RankTreeRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<RankTree> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a rank tree by identifier.
     */
    public @NotNull Optional<RankTree> findByIdentifier(@NotNull String identifier) {
        return query().and("identifier", identifier).first();
    }

    /**
     * Finds a rank tree by identifier asynchronously.
     */
    public @NotNull CompletableFuture<Optional<RankTree>> findByIdentifierAsync(@NotNull String identifier) {
        return query().and("identifier", identifier).firstAsync();
    }

    /** Returns enabled trees. Callers sort by {@link RankTree#getDisplayOrder()} if needed. */
    public @NotNull CompletableFuture<List<RankTree>> findEnabledAsync() {
        return query().and("enabled", true).listAsync();
    }
}
