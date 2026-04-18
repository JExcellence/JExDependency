package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link Currency} entities.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyRepository extends AbstractCrudRepository<Currency, Long> {

    /**
     * Creates a currency repository.
     *
     * @param executor         the executor for async operations
     * @param emf              the entity manager factory
     * @param entityClass      the entity class
     */
    public CurrencyRepository(@NotNull ExecutorService executor,
                              @NotNull EntityManagerFactory emf,
                              @NotNull Class<Currency> entityClass) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a currency by its unique identifier.
     *
     * @param identifier the currency identifier
     * @return the currency, or empty
     */
    public @NotNull Optional<Currency> findByIdentifier(@NotNull String identifier) {
        return query().and("identifier", identifier).first();
    }

    /**
     * Finds a currency by its unique identifier asynchronously.
     *
     * @param identifier the currency identifier
     * @return future resolving to the currency, or empty
     */
    public @NotNull CompletableFuture<Optional<Currency>> findByIdentifierAsync(
            @NotNull String identifier) {
        return query().and("identifier", identifier).firstAsync();
    }
}
