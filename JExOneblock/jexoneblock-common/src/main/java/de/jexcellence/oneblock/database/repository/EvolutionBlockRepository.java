package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionBlock;
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
 * Repository providing cached CRUD access to {@link EvolutionBlock} entities.
 * Handles evolution block data persistence with composite key caching and asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class EvolutionBlockRepository extends CachedRepository<EvolutionBlock, Long, Long> {

    public EvolutionBlockRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<EvolutionBlock> entityClass,
            @NotNull Function<EvolutionBlock, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds a block by evolution and rarity
     */
    @Nullable
    public EvolutionBlock findByEvolutionAndRarity(@NotNull Long evolutionId, @NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(eb -> eb.getEvolution() != null && evolutionId.equals(eb.getEvolution().getId()))
            .filter(eb -> rarity.equals(eb.getRarity()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all blocks for an evolution
     */
    @NotNull
    public List<EvolutionBlock> findByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(eb -> eb.getEvolution() != null && evolutionId.equals(eb.getEvolution().getId()))
            .toList();
    }

    /**
     * Finds all enabled blocks for an evolution
     */
    @NotNull
    public List<EvolutionBlock> findEnabledByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(eb -> eb.getEvolution() != null && evolutionId.equals(eb.getEvolution().getId()))
            .filter(EvolutionBlock::isEnabled)
            .toList();
    }

    /**
     * Finds all blocks by rarity
     */
    @NotNull
    public List<EvolutionBlock> findByRarity(@NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(eb -> rarity.equals(eb.getRarity()))
            .toList();
    }

    /**
     * Finds all valid blocks (enabled with materials)
     */
    @NotNull
    public List<EvolutionBlock> findValidByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(eb -> eb.getEvolution() != null && evolutionId.equals(eb.getEvolution().getId()))
            .filter(EvolutionBlock::isEnabled)
            .filter(eb -> !eb.getMaterials().isEmpty())
            .toList();
    }

    /**
     * Counts blocks for an evolution
     */
    public long countByEvolution(@NotNull Long evolutionId) {
        return findByEvolution(evolutionId).size();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds blocks for an evolution
     */
    @NotNull
    public CompletableFuture<List<EvolutionBlock>> findByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> findByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds a block by evolution and rarity
     */
    @NotNull
    public CompletableFuture<Optional<EvolutionBlock>> findByEvolutionAndRarityAsync(
            @NotNull Long evolutionId, 
            @NotNull EEvolutionRarityType rarity) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByEvolutionAndRarity(evolutionId, rarity)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds enabled blocks for an evolution
     */
    @NotNull
    public CompletableFuture<List<EvolutionBlock>> findEnabledByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> findEnabledByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously counts blocks for an evolution
     */
    @NotNull
    public CompletableFuture<Long> countByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(
            () -> countByEvolution(evolutionId),
            getExecutorService()
        );
    }

    /**
     * Asynchronously deletes all blocks for an evolution
     */
    @NotNull
    public CompletableFuture<Void> deleteByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.runAsync(() -> {
            findByEvolution(evolutionId).forEach(block -> deleteAsync(block.getId()));
        }, getExecutorService());
    }
}
