package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionEntity;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link EvolutionEntity} entities.
 * Handles evolution entity data persistence with composite key caching and asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class EvolutionEntityRepository extends CachedRepository<EvolutionEntity, Long, Long> {

    public EvolutionEntityRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<EvolutionEntity> entityClass,
            @NotNull Function<EvolutionEntity, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds an entity by evolution and rarity
     */
    @Nullable
    public EvolutionEntity findByEvolutionAndRarity(@NotNull Long evolutionId, @NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(ee -> ee.getEvolution() != null && evolutionId.equals(ee.getEvolution().getId()))
            .filter(ee -> rarity.equals(ee.getRarity()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all entities for an evolution
     */
    @NotNull
    public List<EvolutionEntity> findByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ee -> ee.getEvolution() != null && evolutionId.equals(ee.getEvolution().getId()))
            .toList();
    }

    /**
     * Finds all enabled entities for an evolution
     */
    @NotNull
    public List<EvolutionEntity> findEnabledByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ee -> ee.getEvolution() != null && evolutionId.equals(ee.getEvolution().getId()))
            .filter(EvolutionEntity::isEnabled)
            .toList();
    }

    /**
     * Finds all entities by rarity
     */
    @NotNull
    public List<EvolutionEntity> findByRarity(@NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(ee -> rarity.equals(ee.getRarity()))
            .toList();
    }

    /**
     * Finds all valid entities (enabled with spawn eggs)
     */
    @NotNull
    public List<EvolutionEntity> findValidByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ee -> ee.getEvolution() != null && evolutionId.equals(ee.getEvolution().getId()))
            .filter(EvolutionEntity::isEnabled)
            .filter(ee -> !ee.getSpawnEggs().isEmpty())
            .toList();
    }

    /**
     * Finds entities with minimum spawn chance
     */
    @NotNull
    public List<EvolutionEntity> findByMinSpawnChance(@NotNull Long evolutionId, double minSpawnChance) {
        return findAll().stream()
            .filter(ee -> ee.getEvolution() != null && evolutionId.equals(ee.getEvolution().getId()))
            .filter(EvolutionEntity::isEnabled)
            .filter(ee -> ee.getSpawnChance() >= minSpawnChance)
            .toList();
    }

    /**
     * Counts entities for an evolution
     */
    public long countByEvolution(@NotNull Long evolutionId) {
        return findByEvolution(evolutionId).size();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds entities for an evolution
     */
    @NotNull
    public CompletableFuture<List<EvolutionEntity>> findByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> findByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds an entity by evolution and rarity
     */
    @NotNull
    public CompletableFuture<Optional<EvolutionEntity>> findByEvolutionAndRarityAsync(
            @NotNull Long evolutionId, 
            @NotNull EEvolutionRarityType rarity) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByEvolutionAndRarity(evolutionId, rarity)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds enabled entities for an evolution
     */
    @NotNull
    public CompletableFuture<List<EvolutionEntity>> findEnabledByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> findEnabledByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously counts entities for an evolution
     */
    @NotNull
    public CompletableFuture<Long> countByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> countByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously deletes all entities for an evolution
     */
    @NotNull
    public CompletableFuture<Void> deleteByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.runAsync(() -> {
            findByEvolution(evolutionId).forEach(entity -> deleteAsync(entity.getId()));
        }, getExecutorService());
    }
}
