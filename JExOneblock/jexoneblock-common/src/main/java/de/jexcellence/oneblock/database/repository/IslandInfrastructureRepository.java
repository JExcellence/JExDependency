package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.entity.storage.StorageTier;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for IslandInfrastructure entities with caching and async operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class IslandInfrastructureRepository extends CachedRepository<IslandInfrastructure, Long, Long> {

    public IslandInfrastructureRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<IslandInfrastructure> entityClass,
            @NotNull Function<IslandInfrastructure, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds infrastructure by island ID
     */
    @Nullable
    public IslandInfrastructure findByIslandId(@NotNull Long islandId) {
        return findByKey("islandId", islandId).orElse(null);
    }

    /**
     * Finds infrastructure by owner UUID
     */
    @Nullable
    public IslandInfrastructure findByOwner(@NotNull UUID ownerId) {
        return findAll().stream()
            .filter(i -> ownerId.equals(i.getOwnerId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all infrastructure with a specific storage tier
     */
    @NotNull
    public List<IslandInfrastructure> findByStorageTier(@NotNull StorageTier tier) {
        return findAll().stream()
            .filter(i -> tier.equals(i.getStorageTier()))
            .toList();
    }

    /**
     * Finds all prestiged infrastructure
     */
    @NotNull
    public List<IslandInfrastructure> findPrestiged() {
        return findAll().stream()
            .filter(i -> i.getPrestigeLevel() > 0)
            .toList();
    }

    /**
     * Finds all infrastructure with active crafting
     */
    @NotNull
    public List<IslandInfrastructure> findWithActiveCrafting() {
        return findAll().stream()
            .filter(i -> !i.getCraftingQueue().isEmpty())
            .toList();
    }

    /**
     * Finds all infrastructure with energy deficit
     */
    @NotNull
    public List<IslandInfrastructure> findWithEnergyDeficit() {
        return findAll().stream()
            .filter(i -> i.calculateEnergyConsumption() > i.calculateEnergyGeneration())
            .toList();
    }

    /**
     * Gets top infrastructure by blocks mined
     */
    @NotNull
    public List<IslandInfrastructure> findTopByBlocksMined(int limit) {
        return findAll().stream()
            .sorted(Comparator.comparingLong(IslandInfrastructure::getTotalBlocksMined).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * Gets top infrastructure by prestige level
     */
    @NotNull
    public List<IslandInfrastructure> findTopByPrestige(int limit) {
        return findAll().stream()
            .sorted(Comparator.comparingInt(IslandInfrastructure::getPrestigeLevel).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * Counts total blocks mined across all infrastructure
     */
    public long countTotalBlocksMined() {
        return findAll().stream()
            .mapToLong(IslandInfrastructure::getTotalBlocksMined)
            .sum();
    }

    /**
     * Counts total energy generated across all infrastructure
     */
    public long countTotalEnergyGenerated() {
        return findAll().stream()
            .mapToLong(IslandInfrastructure::getTotalEnergyGenerated)
            .sum();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds infrastructure by island ID
     */
    @NotNull
    public CompletableFuture<Optional<IslandInfrastructure>> findByIslandIdAsync(@NotNull Long islandId) {
        return findByKeyAsync("islandId", islandId);
    }

    /**
     * Asynchronously finds infrastructure by owner
     */
    @NotNull
    public CompletableFuture<Optional<IslandInfrastructure>> findByOwnerAsync(@NotNull UUID ownerId) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByOwner(ownerId)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds infrastructure by storage tier
     */
    @NotNull
    public CompletableFuture<List<IslandInfrastructure>> findByStorageTierAsync(@NotNull StorageTier tier) {
        return CompletableFuture.supplyAsync(
            () -> findByStorageTier(tier),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds prestiged infrastructure
     */
    @NotNull
    public CompletableFuture<List<IslandInfrastructure>> findPrestigedAsync() {
        return CompletableFuture.supplyAsync(
            this::findPrestiged,
            getExecutorService()
        );
    }

    /**
     * Gets or creates infrastructure for an island
     */
    @NotNull
    public CompletableFuture<IslandInfrastructure> getOrCreateAsync(@NotNull Long islandId, @NotNull UUID ownerId) {
        return findByIslandIdAsync(islandId)
            .thenCompose(opt -> opt
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> createAsync(new IslandInfrastructure(islandId, ownerId)))
            );
    }

    /**
     * Saves infrastructure (creates or updates)
     */
    @NotNull
    public CompletableFuture<IslandInfrastructure> saveAsync(@NotNull IslandInfrastructure infrastructure) {
        return findByIslandIdAsync(infrastructure.getIslandId())
            .thenCompose(opt -> opt.isPresent()
                ? updateAsync(infrastructure)
                : createAsync(infrastructure)
            );
    }
}
