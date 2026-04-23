package de.jexcellence.quests.database.repository;

import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import de.jexcellence.quests.database.entity.Perk;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for Perk entities.
 */
public class PerkRepository extends AbstractCrudRepository<Perk, Long> {

    /**
     * Constructs a PerkRepository.
     */
    public PerkRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<Perk> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a perk by identifier.
     */
    public @NotNull Optional<Perk> findByIdentifier(@NotNull String identifier) {
        return query().and("identifier", identifier).first();
    }

    /**
     * Finds a perk by identifier asynchronously.
     */
    public @NotNull CompletableFuture<Optional<Perk>> findByIdentifierAsync(@NotNull String identifier) {
        return query().and("identifier", identifier).firstAsync();
    }

    /**
     * Finds perks by category asynchronously.
     */
    public @NotNull CompletableFuture<List<Perk>> findByCategoryAsync(@NotNull String category) {
        return query().and("category", category).and("enabled", true).listAsync();
    }

    /**
     * Finds all enabled perks asynchronously.
     */
    public @NotNull CompletableFuture<List<Perk>> findEnabledAsync() {
        return query().and("enabled", true).listAsync();
    }
}
