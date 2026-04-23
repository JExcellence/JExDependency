package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for Rank entities.
 */
public class RankRepository extends AbstractCrudRepository<Rank, Long> {

    /**
     * Constructs a RankRepository.
     */
    public RankRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<Rank> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /** Returns ranks for the tree. Callers sort by {@link Rank#getOrderIndex()}. */
    public @NotNull CompletableFuture<List<Rank>> findByTreeAsync(@NotNull RankTree tree) {
        return query().and("tree", tree).listAsync();
    }

    /**
     * Finds a rank by tree and identifier.
     */
    public @NotNull CompletableFuture<Optional<Rank>> findByTreeAndIdentifierAsync(
            @NotNull RankTree tree, @NotNull String identifier) {
        return query().and("tree", tree).and("identifier", identifier).firstAsync();
    }

    /** Synchronous variant used by the content loader's upsert path. */
    public @NotNull Optional<Rank> findByTreeAndIdentifier(@NotNull RankTree tree, @NotNull String identifier) {
        return query().and("tree", tree).and("identifier", identifier).first();
    }
}
