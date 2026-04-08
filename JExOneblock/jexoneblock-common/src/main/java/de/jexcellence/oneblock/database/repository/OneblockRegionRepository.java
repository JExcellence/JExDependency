package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockRegion;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing cached CRUD access to {@link OneblockRegion} entities.
 * Handles region data persistence with coordinate-based queries and asynchronous operations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockRegionRepository extends CachedRepository<OneblockRegion, Long, Long> {

    public OneblockRegionRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockRegion> entityClass,
            @NotNull Function<OneblockRegion, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds a region by coordinates
     */
    @Nullable
    public OneblockRegion findByCoordinates(int x1, int z1, int x2, int z2) {
        return findAll().stream()
            .filter(r -> r.getX1() == x1 && r.getZ1() == z1 && r.getX2() == x2 && r.getZ2() == z2)
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds the region containing a point
     */
    @Nullable
    public OneblockRegion findContainingPoint(int x, int z) {
        return findAll().stream()
            .filter(r -> r.getX1() <= x && r.getX2() >= x && r.getZ1() <= z && r.getZ2() >= z)
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds all overlapping regions
     */
    @NotNull
    public List<OneblockRegion> findOverlappingRegions(int x1, int z1, int x2, int z2) {
        return findAll().stream()
            .filter(r -> !(r.getX2() < x1 || r.getX1() > x2 || r.getZ2() < z1 || r.getZ1() > z2))
            .toList();
    }

    /**
     * Finds regions within an area
     */
    @NotNull
    public List<OneblockRegion> findRegionsInArea(int minX, int minZ, int maxX, int maxZ) {
        return findAll().stream()
            .filter(r -> r.getX1() >= minX && r.getZ1() >= minZ && r.getX2() <= maxX && r.getZ2() <= maxZ)
            .toList();
    }

    /**
     * Finds large regions above a minimum area
     */
    @NotNull
    public List<OneblockRegion> findLargeRegions(int minArea) {
        return findAll().stream()
            .filter(r -> (r.getX2() - r.getX1() + 1) * (r.getZ2() - r.getZ1() + 1) >= minArea)
            .toList();
    }

    /**
     * Checks if a region exists at coordinates
     */
    public boolean existsByCoordinates(int x1, int z1, int x2, int z2) {
        return findByCoordinates(x1, z1, x2, z2) != null;
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds a region by coordinates
     */
    @NotNull
    public CompletableFuture<Optional<OneblockRegion>> findByCoordinatesAsync(int x1, int z1, int x2, int z2) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByCoordinates(x1, z1, x2, z2)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds the region containing a point
     */
    @NotNull
    public CompletableFuture<Optional<OneblockRegion>> findContainingPointAsync(int x, int z) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findContainingPoint(x, z)),
            getExecutorService()
        );
    }

    /**
     * Asynchronously finds overlapping regions
     */
    @NotNull
    public CompletableFuture<List<OneblockRegion>> findOverlappingRegionsAsync(int x1, int z1, int x2, int z2) {
        return CompletableFuture.supplyAsync(
            () -> findOverlappingRegions(x1, z1, x2, z2),
            getExecutorService()
        );
    }

    /**
     * Asynchronously counts all regions
     */
    @NotNull
    public CompletableFuture<Long> countAllRegionsAsync() {
        return CompletableFuture.supplyAsync(
            () -> (long) findAll().size(),
            getExecutorService()
        );
    }
    
    /**
     * Saves a region asynchronously
     */
    @NotNull
    public CompletableFuture<OneblockRegion> saveAsync(@NotNull OneblockRegion region) {
        return CompletableFuture.supplyAsync(
            () -> {
                if (region.getId() != null && region.getId() > 0) {
                    return updateAsync(region).join();
                } else {
                    return createAsync(region).join();
                }
            },
            getExecutorService()
        );
    }
}
