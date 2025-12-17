package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link Currency} entities with caching support.
 * <p>
 * This repository provides asynchronous and cached access to currency entities,
 * allowing efficient retrieval and management of currencies by their unique identifier.
 * It extends {@link CachedRepository} to leverage generic CRUD operations and caching.
 * </p>
 *
 * <p>
 * The repository supports the following operations:
 * </p>
 * <ul>
 *   <li>Creating new currency entities</li>
 *   <li>Updating existing currency entities</li>
 *   <li>Deleting currency entities</li>
 *   <li>Fetching currencies by ID or identifier</li>
 *   <li>Cached lookups for improved performance</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * CurrencyRepository repository = new CurrencyRepository(executorService, entityManagerFactory);
 * Optional<Currency> currency = repository.findByKey("gold").join();
 * }</pre>
 *
 * @author JExcellence
 * @see Currency
 * @see CachedRepository
 * @since 1.0.0
 * @version 1.0.1
 */
public class CurrencyRepository extends CachedRepository<Currency, Long, String> {
	
	/**
	 * Constructs a new {@code CurrencyRepository} with the specified executor service and entity manager factory.
	 * <p>
	 * The repository will use the provided executor service for asynchronous operations and the entity manager factory
	 * for JPA database operations. The cache key is configured to use the currency's unique identifier.
	 * </p>
	 *
	 * @param asyncExecutorService the executor service for handling asynchronous database operations, must not be null
	 * @param jpaEntityManagerFactory the entity manager factory for JPA database operations, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
        public CurrencyRepository(
                final @NotNull ExecutorService asyncExecutorService,
                final @NotNull EntityManagerFactory jpaEntityManagerFactory
        ) {
                super(
                        asyncExecutorService,
                        jpaEntityManagerFactory,
                        Currency.class,
                        Currency::getIdentifier
                );
        }

        /**
         * Resolves the currency matching the supplied identifier from the cache or persistence layer.
         *
         * @param identifier the identifier associated with the desired currency, must not be null
         * @return an optional containing the located currency when present, otherwise an empty optional
         * @since 1.0.1
         */
        public @NotNull Optional<Currency> findByIdentifier(final @NotNull String identifier) {
                return findByKey("identifier", identifier);
        }

        /**
         * Asynchronously resolves the currency matching the supplied identifier.
         *
         * @param identifier the identifier associated with the desired currency, must not be null
         * @return a future yielding an optional currency result once the lookup completes
         * @since 1.0.1
         */
        public @NotNull CompletableFuture<Optional<Currency>> findByIdentifierAsync(final @NotNull String identifier) {
                return findByKeyAsync("identifier", identifier);
        }
}