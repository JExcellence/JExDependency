package de.jexcellence.oneblock.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for PlayerGeneratorStructure entities with cached access.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@InjectRepository
public class PlayerGeneratorStructureRepository extends CachedRepository<PlayerGeneratorStructure, Long, Long> {
    
    private final ExecutorService executor;
    
    public PlayerGeneratorStructureRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<PlayerGeneratorStructure> entityClass,
            @NotNull Function<PlayerGeneratorStructure, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.executor = executor;
    }
    
    // ========== Synchronous Methods ==========
    
    /**
     * Finds structures by island ID
     */
    @NotNull
    public List<PlayerGeneratorStructure> findByIslandId(@NotNull Long islandId) {
        return findAll().stream().filter(structure -> islandId.equals(structure.getIslandId())).toList();
    }
    
    /**
     * Finds structures by owner ID
     */
    @NotNull
    public List<PlayerGeneratorStructure> findByOwnerId(@NotNull UUID ownerId) {
        return findAll().stream()
                .filter(structure -> ownerId.equals(structure.getOwnerId()))
                .toList();
    }
    
    /**
     * Finds active structures by island ID
     */
    @NotNull
    public List<PlayerGeneratorStructure> findActiveByIslandId(@NotNull Long islandId) {
        return findAll().stream()
                .filter(structure -> islandId.equals(structure.getIslandId()))
                .filter(structure -> Boolean.TRUE.equals(structure.getIsActive()))
                .toList();
    }
    
    /**
     * Finds structure by island and design type
     */
    @NotNull
    public Optional<PlayerGeneratorStructure> findByIslandAndDesignType(
            @NotNull Long islandId, @NotNull EGeneratorDesignType designType) {
        return findAll().stream()
                .filter(structure -> islandId.equals(structure.getIslandId()))
                .filter(structure -> structure.getDesign() != null)
                .filter(structure -> designType.equals(structure.getDesign().getDesignType()))
                .findFirst();
    }
    
    /**
     * Checks if design is unlocked by island ID and design type
     */
    public boolean hasUnlockedDesign(@NotNull Long islandId, @NotNull EGeneratorDesignType designType) {
        return findAll().stream()
                .filter(structure -> islandId.equals(structure.getIslandId()))
                .filter(structure -> structure.getDesign() != null)
                .filter(structure -> designType.equals(structure.getDesign().getDesignType()))
                .anyMatch(structure -> Boolean.TRUE.equals(structure.getIsValid()));
    }
    
    /**
     * Checks if design is unlocked by player ID and design ID
     */
    public boolean hasUnlockedDesign(@NotNull UUID playerId, @NotNull Long designId) {
        return findAll().stream()
                .filter(structure -> playerId.equals(structure.getOwnerId()))
                .filter(structure -> structure.getDesign() != null)
                .filter(structure -> designId.equals(structure.getDesign().getId()))
                .anyMatch(structure -> Boolean.TRUE.equals(structure.getIsValid()));
    }
    
    /**
     * Gets highest unlocked tier for an island
     */
    public int getHighestUnlockedTier(@NotNull Long islandId) {
        return findAll().stream()
                .filter(structure -> islandId.equals(structure.getIslandId()))
                .filter(structure -> Boolean.TRUE.equals(structure.getIsValid()))
                .filter(structure -> structure.getDesign() != null)
                .mapToInt(structure -> structure.getDesign().getTier())
                .max()
                .orElse(0);
    }
    
    /**
     * Counts structures by island ID
     */
    public long countByIslandId(@NotNull Long islandId) {
        return findAll().stream()
                .filter(structure -> islandId.equals(structure.getIslandId()))
                .count();
    }
    
    // ========== Asynchronous Methods ==========
    
    /**
     * Asynchronously finds structures by island ID
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> findByIslandIdAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(() -> findByIslandId(islandId), executor);
    }
    
    /**
     * Asynchronously finds structures by owner ID
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> findByOwnerIdAsync(@NotNull UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> findByOwnerId(ownerId), executor);
    }
    
    /**
     * Asynchronously finds active structures by island ID
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> findActiveByIslandIdAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(() -> findActiveByIslandId(islandId), executor);
    }
    
    /**
     * Asynchronously finds structure by island and design type
     */
    @NotNull
    public CompletableFuture<Optional<PlayerGeneratorStructure>> findByIslandAndDesignTypeAsync(
            @NotNull Long islandId, @NotNull EGeneratorDesignType designType) {
        return CompletableFuture.supplyAsync(() -> findByIslandAndDesignType(islandId, designType), executor);
    }
    
    /**
     * Asynchronously checks if design is unlocked by island ID and design type
     */
    @NotNull
    public CompletableFuture<Boolean> hasUnlockedDesignAsync(@NotNull Long islandId, @NotNull EGeneratorDesignType designType) {
        return CompletableFuture.supplyAsync(() -> hasUnlockedDesign(islandId, designType), executor);
    }
    
    /**
     * Asynchronously checks if design is unlocked by player ID and design ID
     */
    @NotNull
    public CompletableFuture<Boolean> hasUnlockedDesignAsync(@NotNull UUID playerId, @NotNull Long designId) {
        return CompletableFuture.supplyAsync(() -> hasUnlockedDesign(playerId, designId), executor);
    }
    
    /**
     * Asynchronously gets highest unlocked tier for an island
     */
    @NotNull
    public CompletableFuture<Integer> getHighestUnlockedTierAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(() -> getHighestUnlockedTier(islandId), executor);
    }
    
    /**
     * Asynchronously counts structures by island ID
     */
    @NotNull
    public CompletableFuture<Long> countByIslandIdAsync(@NotNull Long islandId) {
        return CompletableFuture.supplyAsync(() -> countByIslandId(islandId), executor);
    }
}
