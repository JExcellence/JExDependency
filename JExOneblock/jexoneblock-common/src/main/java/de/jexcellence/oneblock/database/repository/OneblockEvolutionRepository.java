package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link OneblockEvolution} entities.
 * Handles evolution data persistence with name-based caching and asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockEvolutionRepository extends CachedRepository<OneblockEvolution, Long, String> {

    public OneblockEvolutionRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockEvolution> entityClass,
            @NotNull Function<OneblockEvolution, String> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds an evolution by name
     */
    @Nullable
    public OneblockEvolution findByName(@NotNull String evolutionName) {
        return findByKey("evolutionName", evolutionName).orElse(null);
    }

    /**
     * Finds an evolution by level
     */
    @Nullable
    public OneblockEvolution findByLevel(int level) {
        return findAll().stream()
            .filter(e -> e.getLevel() == level)
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all active evolutions
     */
    @NotNull
    public List<OneblockEvolution> findAllActive() {
        return findAll().stream()
            .filter(e -> !e.isDisabled())
            .toList();
    }

    /**
     * Finds evolutions within a level range
     */
    @NotNull
    public List<OneblockEvolution> findByLevelRange(int minLevel, int maxLevel) {
        return findAll().stream()
            .filter(e -> e.getLevel() >= minLevel && e.getLevel() <= maxLevel)
            .sorted(Comparator.comparingInt(OneblockEvolution::getLevel))
            .toList();
    }

    /**
     * Finds the next evolution after a given level
     */
    @Nullable
    public OneblockEvolution findNextEvolution(int currentLevel) {
        return findAll().stream()
            .filter(e -> e.getLevel() > currentLevel)
            .filter(e -> !e.isDisabled())
            .min(Comparator.comparingInt(OneblockEvolution::getLevel))
            .orElse(null);
    }

    /**
     * Finds the previous evolution before a given level
     */
    @Nullable
    public OneblockEvolution findPreviousEvolution(int currentLevel) {
        return findAll().stream()
            .filter(e -> e.getLevel() < currentLevel)
            .filter(e -> !e.isDisabled())
            .max(Comparator.comparingInt(OneblockEvolution::getLevel))
            .orElse(null);
    }

    /**
     * Finds all ready evolutions (have content and are not disabled)
     */
    @NotNull
    public List<OneblockEvolution> findReadyEvolutions() {
        return findAll().stream()
            .filter(e -> !e.isDisabled())
            .filter(e -> !e.getBlocks().isEmpty() || !e.getEntities().isEmpty() || !e.getItems().isEmpty())
            .sorted(Comparator.comparingInt(OneblockEvolution::getLevel))
            .toList();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds an evolution by name
     */
    @NotNull
    public CompletableFuture<Optional<OneblockEvolution>> findByNameAsync(@NotNull String evolutionName) {
        return findByKeyAsync("evolutionName", evolutionName);
    }

    /**
     * Asynchronously finds evolutions by level range
     */
    @NotNull
    public CompletableFuture<List<OneblockEvolution>> findByLevelRangeAsync(int minLevel, int maxLevel) {
        return CompletableFuture.supplyAsync(
            () -> findByLevelRange(minLevel, maxLevel),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds all active evolutions
     */
    @NotNull
    public CompletableFuture<List<OneblockEvolution>> findAllActiveAsync() {
        return CompletableFuture.supplyAsync(
            this::findAllActive,
            getExecutorService()
        );
    }

    /**
     * Checks if an evolution exists by name
     */
    @NotNull
    public CompletableFuture<Boolean> existsByNameAsync(@NotNull String evolutionName) {
        return findByNameAsync(evolutionName).thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates an evolution
     */
    @NotNull
    public CompletableFuture<OneblockEvolution> createOrUpdateAsync(@NotNull OneblockEvolution evolution) {
        return existsByNameAsync(evolution.getEvolutionName())
            .thenCompose(exists -> exists
                ? updateAsync(evolution)
                : createAsync(evolution)
            );
    }
    
    /**
     * Finds evolutions by level and disabled status.
     * @param level the evolution level
     * @param disabled the disabled status
     * @return list of matching evolutions
     */
    @NotNull
    public List<OneblockEvolution> findByLevelAndDisabled(int level, boolean disabled) {
        return findAll().stream()
            .filter(evolution -> evolution.getLevel() == level && evolution.isDisabled() == disabled)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Finds evolutions by disabled status.
     * @param disabled the disabled status
     * @return list of matching evolutions
     */
    @NotNull
    public List<OneblockEvolution> findByDisabled(boolean disabled) {
        return findAll().stream()
            .filter(evolution -> evolution.isDisabled() == disabled)
            .collect(java.util.stream.Collectors.toList());
    }
}
