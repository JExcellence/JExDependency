package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link UserCurrency} entities with caching support.
 * <p>
 * This repository provides asynchronous and cached access to user-currency associations,
 * allowing efficient retrieval and management of user balances for specific currencies.
 * It extends {@link GenericCachedRepository} to leverage generic CRUD operations and caching.
 * </p>
 *
 * <p>
 * The repository supports the following operations:
 * </p>
 * <ul>
 *   <li>Creating new user-currency associations</li>
 *   <li>Updating existing user balances</li>
 *   <li>Deleting user-currency associations</li>
 *   <li>Fetching user balances by player UUID</li>
 *   <li>Finding top users by currency with balance limits</li>
 *   <li>Cached lookups for improved performance</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * UserCurrencyRepository repository = new UserCurrencyRepository(executorService, entityManagerFactory);
 * CompletableFuture<List<UserCurrency>> topUsers = repository.findTopByCurrency(currency, 10);
 * }</pre>
 *
 * @author JExcellence
 * @see UserCurrency
 * @see Currency
 * @see GenericCachedRepository
 * @since 1.0.0
 * @version 1.0.1
 */
public class UserCurrencyRepository extends GenericCachedRepository<UserCurrency, Long, UUID> {
	
	/**
	 * Constructs a new {@code UserCurrencyRepository} with the specified executor service and entity manager factory.
	 * <p>
	 * The repository will use the provided executor service for asynchronous operations and the entity manager factory
	 * for JPA database operations. The cache key is configured to use the player's unique identifier (UUID).
	 * </p>
	 *
	 * @param asyncExecutorService the executor service for handling asynchronous database operations, must not be null
	 * @param jpaEntityManagerFactory the entity manager factory for JPA database operations, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public UserCurrencyRepository(
		final @NotNull ExecutorService asyncExecutorService,
		final @NotNull EntityManagerFactory jpaEntityManagerFactory
	) {
		super(
			asyncExecutorService,
			jpaEntityManagerFactory,
			UserCurrency.class,
			userCurrencyEntity -> userCurrencyEntity.getPlayer().getUniqueId()
		);
        }

        /**
         * Retrieves all {@link UserCurrency} associations belonging to the supplied player identifier.
         * <p>
         * The lookup leverages the repository cache keyed by the player's {@link UUID} before falling
         * back to the persistence layer, mirroring the behaviour used by higher-level services when
         * materialising balances for a player session.
         * </p>
         *
         * @param playerUniqueId the unique identifier of the player, must not be {@code null}
         * @return a future resolving to the cached or freshly loaded associations for the player
         */
        public @NotNull CompletableFuture<List<UserCurrency>> findByUserAsync(
                final @NotNull UUID playerUniqueId
        ) {

                Objects.requireNonNull(playerUniqueId, "playerUniqueId cannot be null");

                return this.findListByAttributesAsync(
                        Map.of(
                                "player.uniqueId",
                                playerUniqueId
                        )
                );
        }

        /**
         * Resolves all {@link UserCurrency} records linked to the provided {@link Currency}.
         * <p>
         * The request first probes the cache with the currency identifier to reuse prior query
         * results and then delegates to the persistence layer if required. Consumers typically use
         * this method when presenting currency leaderboards or performing reconciliation jobs.
         * </p>
         *
         * @param targetCurrency the currency to resolve associations for, must not be {@code null}
         * @return a future yielding the cached or loaded associations tied to the currency
         */
        public @NotNull CompletableFuture<List<UserCurrency>> findByCurrencyAsync(
                final @NotNull Currency targetCurrency
        ) {

                Objects.requireNonNull(targetCurrency, "currency cannot be null");

                return this.findListByAttributesAsync(
                        Map.of(
                                "currency.id",
                                targetCurrency.getId()
                        )
                );
        }

        /**
         * Finds the top user-currency associations for a given currency, limited by the specified count.
         * <p>
	 * This method retrieves all {@link UserCurrency} entities associated with the given currency,
	 * ordered by balance (highest first), and returns a sublist containing up to the specified limit.
	 * If no associations are found, an empty list is returned.
	 * </p>
	 *
	 * <h3>Performance Considerations:</h3>
	 * <ul>
	 *   <li>Results are fetched asynchronously to avoid blocking the calling thread</li>
	 *   <li>The limit is applied after fetching to ensure accurate top results</li>
	 *   <li>Caching may be utilized for frequently accessed currencies</li>
	 * </ul>
	 *
	 * @param targetCurrency the currency to filter associations by, must not be null
	 * @param resultLimit the maximum number of results to return, must be positive
	 * @return a {@link CompletableFuture} containing a list of {@link UserCurrency} entities
	 *         for the specified currency, limited by the result limit, never null
	 * @throws IllegalArgumentException if the currency is null or limit is not positive
	 */
	public @NotNull CompletableFuture<List<UserCurrency>> findTopByCurrency(
		final @NotNull Currency targetCurrency,
		final int resultLimit
	) {
		
		if (
			resultLimit <= 0
		) {
			throw new IllegalArgumentException("Result limit must be positive");
		}
		
		return this.findListByAttributesAsync(
			Map.of(
				"currency.id",
				targetCurrency.getId()
			)
		).thenApply(
			retrievedUserCurrencies -> {
				if (
					retrievedUserCurrencies == null
				) {
					return new ArrayList<>();
				}
				
				return retrievedUserCurrencies.subList(
					0,
					Math.min(
						resultLimit,
						retrievedUserCurrencies.size()
					)
				);
			}
		);
	}
}