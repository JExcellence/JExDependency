package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.User;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link User} entities with caching support.
 * <p>
 * This repository provides asynchronous and cached access to user entities,
 * allowing efficient retrieval and management of users by their unique UUID.
 * It extends {@link GenericCachedRepository} to leverage generic CRUD operations and caching.
 * </p>
 *
 * <p>
 * The repository supports the following operations:
 * </p>
 * <ul>
 *   <li>Creating new user entities</li>
 *   <li>Updating existing user entities</li>
 *   <li>Deleting user entities</li>
 *   <li>Fetching users by ID or UUID</li>
 *   <li>Cached lookups for improved performance</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * UserRepository repository = new UserRepository(executorService, entityManagerFactory);
 * UUID playerUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
 * Optional<User> user = repository.findByKey(playerUuid).join();
 * }</pre>
 *
 * @author JExcellence
 * @see User
 * @see GenericCachedRepository
 */
public class UserRepository extends GenericCachedRepository<User, Long, UUID> {
	
	/**
	 * Constructs a new {@code UserRepository} with the specified executor service and entity manager factory.
	 * <p>
	 * The repository will use the provided executor service for asynchronous operations and the entity manager factory
	 * for JPA database operations. The cache key is configured to use the user's unique identifier (UUID).
	 * </p>
	 *
	 * @param asyncExecutorService the executor service for handling asynchronous database operations, must not be null
	 * @param jpaEntityManagerFactory the entity manager factory for JPA database operations, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public UserRepository(
		final @NotNull ExecutorService asyncExecutorService,
		final @NotNull EntityManagerFactory jpaEntityManagerFactory
	) {
		super(
			asyncExecutorService,
			jpaEntityManagerFactory,
			User.class,
			User::getUniqueId
		);
	}
}