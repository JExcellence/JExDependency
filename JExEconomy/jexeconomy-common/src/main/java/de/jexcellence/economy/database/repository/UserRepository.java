package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.User;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
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
     * @param asyncExecutorService     the executor service for handling asynchronous database operations, must not be null
     * @param jpaEntityManagerFactory  the entity manager factory for JPA database operations, must not be null
     */
    public UserRepository(
        final @NotNull ExecutorService asyncExecutorService,
        final @NotNull EntityManagerFactory jpaEntityManagerFactory
    ) {
        super(asyncExecutorService, jpaEntityManagerFactory, User.class, User::getUniqueId);
    }

    /**
     * Retrieves a user by UUID using asynchronous execution. The lookup first attempts to resolve
     * the entity from the cache maintained by {@link GenericCachedRepository} before falling back
     * to the persistence layer if necessary.
     *
     * @param uniqueId the UUID used to locate the user
     * @return future resolving to an optional containing the cached or freshly loaded entity
     */
    public @NotNull CompletableFuture<Optional<User>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");

        return findByAttributesAsync(Map.of("uniqueId", uniqueId))
            .thenApply(Optional::ofNullable);
    }

    /**
     * Synchronously locates a user by UUID leveraging the repository cache to avoid redundant
     * database lookups where possible.
     *
     * @param uniqueId the UUID used to locate the user
     * @return optional containing the located user or empty when absent
     */
    public @NotNull Optional<User> findByUuid(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");

        return Optional.ofNullable(findByAttributes(Map.of("uniqueId", uniqueId)));
    }

    /**
     * Retrieves a user by stored player name using asynchronous execution. The cache is consulted
     * before dispatching a database query, and the work is executed on the configured repository
     * executor.
     *
     * @param playerName player name to search for
     * @return future resolving to an optional containing the cached or persisted entity
     */
    public @NotNull CompletableFuture<Optional<User>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return findByAttributesAsync(Map.of("playerName", playerName))
            .thenApply(Optional::ofNullable);
    }

    /**
     * Synchronously locates a user by player name, returning an {@link Optional} so callers can
     * gracefully react to missing records without handling {@code null} values.
     *
     * @param playerName player name used to search for the user
     * @return optional containing the located user or empty when no record matches
     */
    public @NotNull Optional<User> findByName(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return Optional.ofNullable(findByAttributes(Map.of("playerName", playerName)));
    }

    /**
     * Checks if a user exists for the provided UUID by reusing the cached lookup.
     *
     * @param uniqueId identifier to probe
     * @return future evaluating to {@code true} when a user exists for the UUID
     */
    public @NotNull CompletableFuture<Boolean> existsByUuidAsync(final @NotNull UUID uniqueId) {
        return findByUuidAsync(uniqueId)
            .thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates the supplied user depending on whether the UUID already exists.
     * Cache-backed existence checks allow the repository to branch to either create or update
     * semantics without redundant database probes.
     *
     * @param user user entity to persist
     * @return future resolving to the managed entity after persistence completes
     */
    public @NotNull CompletableFuture<User> createOrUpdateAsync(final @NotNull User user) {
        Objects.requireNonNull(user, "user cannot be null");

        return existsByUuidAsync(user.getUniqueId())
            .thenCompose(exists -> exists
                ? updateAsync(user)
                : createAsync(user)
            );
    }
}