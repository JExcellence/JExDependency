package de.jexcellence.multiverse.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.multiverse.database.entity.MVWorld;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link MVWorld} entities in the JExMultiverse system.
 * <p>
 * Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for world entities, using the world's identifier as the cache key.
 * </p>
 * <p>
 * The repository provides both synchronous and asynchronous methods for:
 * <ul>
 *   <li>Finding worlds by identifier</li>
 *   <li>Finding the global spawn world</li>
 *   <li>Retrieving all managed worlds</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MVWorldRepository extends CachedRepository<MVWorld, Long, String> {

    /**
     * Constructs a new {@code MVWorldRepository} with the specified executor and entity manager factory.
     *
     * @param asyncExecutorService    the {@link ExecutorService} for asynchronous operations
     * @param jpaEntityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass             the entity class
     * @param keyExtractor            the key extractor function (extracts identifier from MVWorld)
     */
    public MVWorldRepository(
            final @NotNull ExecutorService asyncExecutorService,
            final @NotNull EntityManagerFactory jpaEntityManagerFactory,
            final @NotNull Class<MVWorld> entityClass,
            final @NotNull Function<MVWorld, String> keyExtractor
    ) {
        super(asyncExecutorService, jpaEntityManagerFactory, entityClass, keyExtractor);
    }

    // ==================== Synchronous Methods ====================

    /**
     * Finds a world by its identifier (world name).
     *
     * @param identifier the world identifier
     * @return an Optional containing the world if found
     */
    public @NotNull Optional<MVWorld> findByIdentifier(final @NotNull String identifier) {
        return this.findByAttributes(Map.of("identifier", identifier));
    }

    /**
     * Finds the world designated as the global spawn.
     *
     * @return an Optional containing the global spawn world if one exists
     */
    public @NotNull Optional<MVWorld> findByGlobalSpawn() {
        return this.findByAttributes(Map.of("globalizedSpawn", true));
    }

    // ==================== Asynchronous Methods ====================

    /**
     * Finds a world by its identifier asynchronously.
     *
     * @param identifier the world identifier
     * @return a CompletableFuture containing an Optional with the world if found
     */
    public @NotNull CompletableFuture<Optional<MVWorld>> findByIdentifierAsync(final @NotNull String identifier) {
        return CompletableFuture.supplyAsync(
                () -> this.findByIdentifier(identifier),
                this.getExecutorService()
        );
    }

    /**
     * Finds the global spawn world asynchronously.
     *
     * @return a CompletableFuture containing an Optional with the global spawn world if one exists
     */
    public @NotNull CompletableFuture<Optional<MVWorld>> findByGlobalSpawnAsync() {
        return CompletableFuture.supplyAsync(
                () -> this.findByGlobalSpawn(),
                this.getExecutorService()
        );
    }

    /**
     * Retrieves all managed worlds asynchronously.
     *
     * @return a CompletableFuture containing a list of all worlds
     */
    public @NotNull CompletableFuture<List<MVWorld>> findAllAsync() {
        return this.findAllByAttributesAsync(Map.of());
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if a world with the given identifier exists.
     *
     * @param identifier the world identifier
     * @return true if the world exists
     */
    public boolean existsByIdentifier(final @NotNull String identifier) {
        return this.findByIdentifier(identifier).isPresent();
    }

    /**
     * Checks if a world with the given identifier exists asynchronously.
     *
     * @param identifier the world identifier
     * @return a CompletableFuture containing true if the world exists
     */
    public @NotNull CompletableFuture<Boolean> existsByIdentifierAsync(final @NotNull String identifier) {
        return this.findByIdentifierAsync(identifier)
                .thenApply(Optional::isPresent);
    }

    /**
     * Saves or updates a world entity.
     *
     * @param world the world to save
     * @return a CompletableFuture containing the saved world
     */
    public @NotNull CompletableFuture<MVWorld> saveWorld(final @NotNull MVWorld world) {
        if (world.getId() == null) {
            return this.createAsync(world);
        }
        return this.updateAsync(world);
    }

    /**
     * Deletes a world by its identifier.
     *
     * @param identifier the world identifier
     * @return a CompletableFuture containing true if a world was deleted
     */
    public @NotNull CompletableFuture<Boolean> deleteByIdentifier(final @NotNull String identifier) {
        return this.findByIdentifierAsync(identifier)
                .thenCompose(optionalWorld -> optionalWorld
                        .map(world -> this.deleteAsync(world.getId()).thenApply(v -> true))
                        .orElseGet(() -> CompletableFuture.completedFuture(false)));
    }

    /**
     * Clears the global spawn flag from all worlds except the specified one.
     * This ensures only one world can be the global spawn at a time.
     *
     * @param excludeIdentifier the identifier of the world to exclude from clearing
     * @return a CompletableFuture that completes when the operation is done
     */
    public @NotNull CompletableFuture<Void> clearGlobalSpawnExcept(final @NotNull String excludeIdentifier) {
        return this.findAllAsync()
                .thenCompose(worlds -> {
                    List<CompletableFuture<MVWorld>> updates = worlds.stream()
                            .filter(world -> world.isGlobalizedSpawn() && !world.getIdentifier().equals(excludeIdentifier))
                            .map(world -> {
                                world.setGlobalizedSpawn(false);
                                return this.updateAsync(world);
                            })
                            .toList();

                    return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]));
                });
    }
}
