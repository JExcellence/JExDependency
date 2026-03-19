package de.jexcellence.home.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.home.database.entity.Home;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository for managing {@link Home} entities in the JExHome system.
 * <p>
 * Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for home entities, using the home's ID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class HomeRepository extends CachedRepository<Home, Long, Long> {

    /**
     * Constructs a new {@code HomeRepository} with the specified executor and entity manager factory.
     *
     * @param asyncExecutorService    the {@link ExecutorService} for asynchronous operations
     * @param jpaEntityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass             the entity class
     * @param keyExtractor            the key extractor function
     */
    public HomeRepository(
        final @NotNull ExecutorService asyncExecutorService,
        final @NotNull EntityManagerFactory jpaEntityManagerFactory,
        @NotNull Class<Home> entityClass,
        @NotNull Function<Home, Long> keyExtractor
    ) {
        super(asyncExecutorService, jpaEntityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds all homes belonging to a player asynchronously.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing the list of homes
     */
    public @NotNull CompletableFuture<List<Home>> findByPlayerUuid(final @NotNull UUID playerUuid) {
        return this.findAllByAttributesAsync(Map.of("playerUuid", playerUuid));
    }

    /**
     * Finds a home by player UUID and home name asynchronously.
     *
     * @param playerUuid the UUID of the player
     * @param homeName   the name of the home
     * @return a CompletableFuture containing an Optional with the home if found
     */
    public @NotNull CompletableFuture<Optional<Home>> findByPlayerAndName(
        final @NotNull UUID playerUuid,
        final @NotNull String homeName
    ) {
        return CompletableFuture.supplyAsync(() -> this.findByAttributes(Map.of(
            "playerUuid", playerUuid,
            "homeName", homeName
        )), this.getExecutorService());
    }

    /**
     * Finds all homes in a specific category for a player.
     *
     * @param playerUuid the UUID of the player
     * @param category   the category to filter by
     * @return a CompletableFuture containing homes in the category
     */
    public @NotNull CompletableFuture<List<Home>> findByCategory(
        final @NotNull UUID playerUuid,
        final @NotNull String category
    ) {
        return findByPlayerUuid(playerUuid)
            .thenApply(homes -> homes.stream()
                .filter(h -> category.equalsIgnoreCase(h.getCategory()))
                .collect(Collectors.toList()));
    }

    /**
     * Finds all favorite homes for a player.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing favorite homes
     */
    public @NotNull CompletableFuture<List<Home>> findFavorites(final @NotNull UUID playerUuid) {
        return findByPlayerUuid(playerUuid)
            .thenApply(homes -> homes.stream()
                .filter(Home::isFavorite)
                .collect(Collectors.toList()));
    }

    /**
     * Counts the number of homes belonging to a player asynchronously.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing the count
     */
    public @NotNull CompletableFuture<Long> countByPlayerUuid(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return this.findByPlayerUuid(playerUuid)
            .thenApply(homes -> (long) homes.size());
    }

    /**
     * Checks if a home exists for the given player and name.
     *
     * @param playerUuid the UUID of the player
     * @param homeName   the name of the home
     * @return a CompletableFuture containing true if the home exists
     */
    public @NotNull CompletableFuture<Boolean> existsByPlayerAndName(
        final @NotNull UUID playerUuid,
        final @NotNull String homeName
    ) {
        return this.findByPlayerAndName(playerUuid, homeName)
            .thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates a home entity.
     *
     * @param home the home to save
     * @return a CompletableFuture containing the saved home
     */
    public @NotNull CompletableFuture<Home> saveHome(final @NotNull Home home) {
        Objects.requireNonNull(home, "home cannot be null");
        if (home.getId() == null) {
            return this.createAsync(home);
        }
        return this.updateAsync(home);
    }

    /**
     * Deletes a home by player UUID and home name.
     *
     * @param playerUuid the UUID of the player
     * @param homeName   the name of the home
     * @return a CompletableFuture containing true if a home was deleted
     */
    public @NotNull CompletableFuture<Boolean> deleteByPlayerAndName(
        final @NotNull UUID playerUuid,
        final @NotNull String homeName
    ) {
        return this.findByPlayerAndName(playerUuid, homeName)
            .thenCompose(optionalHome -> optionalHome
                .map(home -> this.deleteAsync(home.getId()).thenApply(v -> true))
                .orElseGet(() -> CompletableFuture.completedFuture(false)));
    }
}
