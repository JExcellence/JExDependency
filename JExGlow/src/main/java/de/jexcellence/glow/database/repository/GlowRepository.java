package de.jexcellence.glow.database.repository;

import de.jexcellence.glow.database.entity.PlayerGlow;
import de.jexcellence.hibernate.repository.CachedRepository;
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
 * Repository for managing {@link PlayerGlow} entities in the JExGlow system.
 * <p>
 * Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for player glow entities, using the player's UUID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class GlowRepository extends CachedRepository<PlayerGlow, Long, UUID> {

    /**
     * Constructs a new {@code GlowRepository} with the specified executor and entity manager factory.
     *
     * @param asyncExecutorService    the {@link ExecutorService} for asynchronous operations
     * @param jpaEntityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass             the entity class
     * @param keyExtractor            the key extractor function
     */
    public GlowRepository(
        final @NotNull ExecutorService asyncExecutorService,
        final @NotNull EntityManagerFactory jpaEntityManagerFactory,
        @NotNull Class<PlayerGlow> entityClass,
        @NotNull Function<PlayerGlow, UUID> keyExtractor
    ) {
        super(asyncExecutorService, jpaEntityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a player's glow state by their UUID asynchronously.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing an Optional with the PlayerGlow if found
     */
    public @NotNull CompletableFuture<Optional<PlayerGlow>> findByPlayerUuid(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return CompletableFuture.supplyAsync(() -> this.findByAttributes(Map.of(
            "playerUuid", playerUuid
        )), this.getExecutorService());
    }

    /**
     * Creates or updates a player's glow state.
     *
     * @param playerGlow the PlayerGlow entity to save
     * @return a CompletableFuture containing the saved PlayerGlow
     */
    public @NotNull CompletableFuture<PlayerGlow> saveGlowState(final @NotNull PlayerGlow playerGlow) {
        Objects.requireNonNull(playerGlow, "playerGlow cannot be null");
        if (playerGlow.getId() == null) {
            return this.createAsync(playerGlow);
        }
        return this.updateAsync(playerGlow);
    }

    /**
     * Deletes a player's glow state by their UUID.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing true if a glow state was deleted
     */
    public @NotNull CompletableFuture<Boolean> deleteByPlayerUuid(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return this.findByPlayerUuid(playerUuid)
            .thenCompose(optionalGlow -> optionalGlow
                .map(glow -> this.deleteAsync(glow.getId()).thenApply(v -> true))
                .orElseGet(() -> CompletableFuture.completedFuture(false)));
    }

    /**
     * Finds all players with glow enabled.
     *
     * @return a CompletableFuture containing a list of PlayerGlow entities with glow enabled
     */
    public @NotNull CompletableFuture<List<PlayerGlow>> findAllEnabled() {
        return CompletableFuture.supplyAsync(() -> {
            return this.findAll().stream()
                .filter(PlayerGlow::isGlowEnabled)
                .collect(Collectors.toList());
        }, this.getExecutorService());
    }

    /**
     * Checks if a player has glow enabled.
     *
     * @param playerUuid the UUID of the player
     * @return a CompletableFuture containing true if the player has glow enabled
     */
    public @NotNull CompletableFuture<Boolean> isGlowEnabled(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return this.findByPlayerUuid(playerUuid)
            .thenApply(optionalGlow -> optionalGlow
                .map(PlayerGlow::isGlowEnabled)
                .orElse(false));
    }
}
