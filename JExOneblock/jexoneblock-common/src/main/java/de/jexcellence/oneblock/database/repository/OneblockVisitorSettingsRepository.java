package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockVisitorSettings;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link OneblockVisitorSettings} entities.
 * Handles visitor settings data persistence with asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockVisitorSettingsRepository extends CachedRepository<OneblockVisitorSettings, Long, Long> {

    public OneblockVisitorSettingsRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockVisitorSettings> entityClass,
            @NotNull Function<OneblockVisitorSettings, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds visitor settings for an island
     */
    @Nullable
    public OneblockVisitorSettings findByIsland(@NotNull Long islandId) {
        return findAll().stream()
            .filter(s -> s.getIsland() != null && islandId.equals(s.getIsland().getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if visitor settings exist for an island
     */
    public boolean existsByIsland(@NotNull Long islandId) {
        return findByIsland(islandId) != null;
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds visitor settings for an island
     */
    @NotNull
    public CompletableFuture<Optional<OneblockVisitorSettings>> findByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByIsland(islandId)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously checks if visitor settings exist
     */
    @NotNull
    public CompletableFuture<Boolean> existsByIslandAsync(@NotNull Long islandId) {
        return findByIslandAsync(islandId).thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates visitor settings
     */
    @NotNull
    public CompletableFuture<OneblockVisitorSettings> createOrUpdateAsync(@NotNull OneblockVisitorSettings settings) {
        return existsByIslandAsync(settings.getIsland().getId())
            .thenCompose(exists -> exists
                ? updateAsync(settings)
                : createAsync(settings)
            );
    }

    /**
     * Asynchronously deletes visitor settings for an island
     */
    @NotNull
    public CompletableFuture<Void> deleteByIslandAsync(@NotNull Long islandId) {
        return CompletableFuture.runAsync(() -> {
            OneblockVisitorSettings settings = findByIsland(islandId);
            if (settings != null) {
                deleteAsync(settings.getId());
            }
        }, getExecutorService());
    }
}
