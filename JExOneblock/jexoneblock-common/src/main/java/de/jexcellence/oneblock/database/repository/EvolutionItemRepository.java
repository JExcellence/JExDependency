package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionItem;
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
 * Repository providing cached CRUD access to {@link EvolutionItem} entities.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class EvolutionItemRepository extends CachedRepository<EvolutionItem, Long, Long> {

    public EvolutionItemRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<EvolutionItem> entityClass,
            @NotNull Function<EvolutionItem, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    @Nullable
    public EvolutionItem findByEvolutionAndRarity(@NotNull Long evolutionId, @NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .filter(ei -> rarity.equals(ei.getRarity()))
            .findFirst()
            .orElse(null);
    }

    @NotNull
    public List<EvolutionItem> findByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .toList();
    }

    @NotNull
    public List<EvolutionItem> findEnabledByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .filter(EvolutionItem::isEnabled)
            .toList();
    }

    @NotNull
    public List<EvolutionItem> findByRarity(@NotNull EEvolutionRarityType rarity) {
        return findAll().stream()
            .filter(ei -> rarity.equals(ei.getRarity()))
            .toList();
    }

    @NotNull
    public List<EvolutionItem> findValidByEvolution(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .filter(EvolutionItem::isEnabled)
            .filter(ei -> !ei.getItemStacks().isEmpty())
            .toList();
    }

    @NotNull
    public List<EvolutionItem> findByMinDropChance(@NotNull Long evolutionId, double minDropChance) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .filter(EvolutionItem::isEnabled)
            .filter(ei -> ei.getDropChance() >= minDropChance)
            .toList();
    }

    @NotNull
    public List<EvolutionItem> findSilkTouchItems(@NotNull Long evolutionId) {
        return findAll().stream()
            .filter(ei -> ei.getEvolution() != null && evolutionId.equals(ei.getEvolution().getId()))
            .filter(EvolutionItem::isEnabled)
            .filter(EvolutionItem::isRequiresSilkTouch)
            .toList();
    }

    public long countByEvolution(@NotNull Long evolutionId) {
        return findByEvolution(evolutionId).size();
    }

    @NotNull
    public CompletableFuture<List<EvolutionItem>> findByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(() -> findByEvolution(evolutionId), getExecutorService());
    }

    @NotNull
    public CompletableFuture<Optional<EvolutionItem>> findByEvolutionAndRarityAsync(
            @NotNull Long evolutionId, @NotNull EEvolutionRarityType rarity) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByEvolutionAndRarity(evolutionId, rarity)), getExecutorService());
    }

    @NotNull
    public CompletableFuture<List<EvolutionItem>> findEnabledByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(() -> findEnabledByEvolution(evolutionId), getExecutorService());
    }

    @NotNull
    public CompletableFuture<Long> countByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.supplyAsync(() -> countByEvolution(evolutionId), getExecutorService());
    }

    @NotNull
    public CompletableFuture<Void> deleteByEvolutionAsync(@NotNull Long evolutionId) {
        return CompletableFuture.runAsync(() -> {
            findByEvolution(evolutionId).forEach(item -> deleteAsync(item.getId()));
        }, getExecutorService());
    }
}
