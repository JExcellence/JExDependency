package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link Currency} entities with caching support.
 * <p>
 * This repository provides asynchronous and cached access to currency entities,
 * allowing efficient retrieval and management of currencies by their unique identifier.
 * It extends {@link GenericCachedRepository} to leverage generic CRUD operations and caching.
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
 * @see GenericCachedRepository
 */
public class CurrencyRepository extends GenericCachedRepository<Currency, Long, String> {
	
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
}