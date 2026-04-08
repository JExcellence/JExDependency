package de.jexcellence.oneblock.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for GeneratorDesign entities with cached access.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@InjectRepository
public class GeneratorDesignRepository extends CachedRepository<GeneratorDesign, Long, String> {
    
    private final ExecutorService executor;
    private final EntityManagerFactory entityManagerFactory;
    
    public GeneratorDesignRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<GeneratorDesign> entityClass,
            @NotNull Function<GeneratorDesign, String> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.executor = executor;
        this.entityManagerFactory = entityManagerFactory;
    }
    
    // ========== Synchronous Methods ==========
    
    /**
     * Finds a design by key
     */
    @NotNull
    public Optional<GeneratorDesign> findByKey(@NotNull String designKey) {
        return findByKey("designKey", designKey);
    }
    
    /**
     * Finds a design by type
     */
    @NotNull
    public Optional<GeneratorDesign> findByType(@NotNull EGeneratorDesignType type) {
        return findAll().stream()
                .filter(design -> type.equals(design.getDesignType()))
                .findFirst();
    }
    
    /**
     * Finds all enabled designs
     */
    @NotNull
    public List<GeneratorDesign> findAllEnabled() {
        return findAll().stream()
                .filter(design -> Boolean.TRUE.equals(design.getEnabled()))
                .sorted((a, b) -> Integer.compare(a.getTier(), b.getTier()))
                .toList();
    }
    
    /**
     * Finds designs by tier range
     */
    @NotNull
    public List<GeneratorDesign> findByTierRange(int minTier, int maxTier) {
        return findAll().stream()
                .filter(design -> Boolean.TRUE.equals(design.getEnabled()))
                .filter(design -> design.getTier() >= minTier && design.getTier() <= maxTier)
                .sorted((a, b) -> Integer.compare(a.getTier(), b.getTier()))
                .toList();
    }
    
    /**
     * Counts enabled designs
     */
    public long count() {
        return findAll().stream()
                .filter(design -> Boolean.TRUE.equals(design.getEnabled()))
                .count();
    }
    
    // ========== Asynchronous Methods ==========
    
    /**
     * Asynchronously finds a design by key
     */
    @NotNull
    public CompletableFuture<Optional<GeneratorDesign>> findByKeyAsync(@NotNull String designKey) {
        return findByKeyAsync("designKey", designKey);
    }
    
    /**
     * Asynchronously finds a design by type
     */
    @NotNull
    public CompletableFuture<Optional<GeneratorDesign>> findByTypeAsync(@NotNull EGeneratorDesignType type) {
        return CompletableFuture.supplyAsync(() -> findByType(type), executor);
    }
    
    /**
     * Asynchronously finds all enabled designs
     */
    @NotNull
    public CompletableFuture<List<GeneratorDesign>> findAllEnabledAsync() {
        return CompletableFuture.supplyAsync(this::findAllEnabled, executor);
    }
    
    /**
     * Asynchronously finds designs by tier range
     */
    @NotNull
    public CompletableFuture<List<GeneratorDesign>> findByTierRangeAsync(int minTier, int maxTier) {
        return CompletableFuture.supplyAsync(() -> findByTierRange(minTier, maxTier), executor);
    }
    
    /**
     * Asynchronously counts enabled designs
     */
    @NotNull
    public CompletableFuture<Long> countAsync() {
        return CompletableFuture.supplyAsync(this::count, executor);
    }
}
