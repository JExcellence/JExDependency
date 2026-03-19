package de.jexcellence.oneblock.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository providing cached CRUD access to {@link IslandRegion} entities.
 * Handles island region data persistence with UUID-based caching and asynchronous operations.
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class IslandRegionRepository extends CachedRepository<IslandRegion, Long, UUID> {

    private static final Logger LOGGER = Logger.getLogger(IslandRegionRepository.class.getName());

    private final ExecutorService executor;
    private final EntityManagerFactory entityManagerFactory;

    public IslandRegionRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<IslandRegion> entityClass,
            @NotNull Function<IslandRegion, UUID> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.executor = executor;
        this.entityManagerFactory = entityManagerFactory;
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds a region by island ID (uses cache)
     */
    @Nullable
    public IslandRegion findByIslandId(@NotNull UUID islandId) {
        return findByKey("islandId", islandId)
            .filter(IslandRegion::isActive)
            .orElse(null);
    }

    /**
     * Finds a region by owner UUID
     */
    @NotNull
    public Optional<IslandRegion> findByOwner(@NotNull UUID ownerId) {
        // Check cache first
        for (IslandRegion region : getCachedByKey().values()) {
            if (region.getOwnerId().equals(ownerId) && region.isActive()) {
                return Optional.of(region);
            }
        }
        
        // Query all regions and filter by owner
        return findAll().stream()
            .filter(region -> region.getOwnerId().equals(ownerId))
            .filter(IslandRegion::isActive)
            .findFirst();
    }

    /**
     * Finds all regions in a specific world
     */
    @NotNull
    public List<IslandRegion> findByWorld(@NotNull String worldName) {
        return findAll().stream()
            .filter(region -> region.getWorldName().equals(worldName))
            .filter(IslandRegion::isActive)
            .sorted((r1, r2) -> Integer.compare(r1.getSpiralPosition(), r2.getSpiralPosition()))
            .toList();
    }

    /**
     * Finds all active regions
     */
    @NotNull
    public List<IslandRegion> findAllActive() {
        return findAll().stream()
            .filter(IslandRegion::isActive)
            .toList();
    }

    /**
     * Soft deletes a region by marking it as inactive
     */
    public void softDelete(@NotNull UUID islandId) {
        IslandRegion region = findByIslandId(islandId);
        if (region != null) {
            region.setActive(false);
            region.updateTimestamp();
            update(region);
            LOGGER.info("Soft deleted island region: " + islandId);
        }
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds a region by island ID
     */
    @NotNull
    public CompletableFuture<Optional<IslandRegion>> findByIslandIdAsync(@NotNull UUID islandId) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByIslandId(islandId)),
            this.executor
        );
    }

    /**
     * Asynchronously finds a region by owner UUID
     */
    @NotNull
    public CompletableFuture<Optional<IslandRegion>> findByOwnerAsync(@NotNull UUID ownerId) {
        return CompletableFuture.supplyAsync(
            () -> findByOwner(ownerId),
            this.executor
        );
    }

    /**
     * Asynchronously finds regions by world
     */
    @NotNull
    public CompletableFuture<List<IslandRegion>> findByWorldAsync(@NotNull String worldName) {
        return CompletableFuture.supplyAsync(
            () -> findByWorld(worldName),
            this.executor
        );
    }

    /**
     * Asynchronously soft deletes a region
     */
    @NotNull
    public CompletableFuture<Void> deleteAsync(@NotNull UUID islandId) {
        return CompletableFuture.runAsync(
            () -> softDelete(islandId),
            this.executor
        );
    }

    /**
     * Asynchronously finds all active regions
     */
    @NotNull
    public CompletableFuture<List<IslandRegion>> findAllAsync() {
        return CompletableFuture.supplyAsync(
            this::findAllActive,
            this.executor
        );
    }

    /**
     * Saves a region asynchronously
     */
    @NotNull
    public CompletableFuture<IslandRegion> saveAsync(@NotNull IslandRegion region) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (region.getId() == null) {
                    LOGGER.info("Creating new island region with island ID: " + region.getIslandId());
                    return create(region);
                } else {
                    region.updateTimestamp();
                    LOGGER.info("Updating island region with island ID: " + region.getIslandId());
                    return update(region);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to save island region: " + region.getIslandId(), e);
                throw new RuntimeException("Failed to save island region", e);
            }
        }, this.executor);
    }

    // ========== Additional Query Methods ==========

    /**
     * Finds regions within a specific radius of a location.
     */
    @NotNull
    public CompletableFuture<List<IslandRegion>> findNearLocationAsync(@NotNull String worldName, 
                                                                       double x, double z, double radius) {
        return CompletableFuture.supplyAsync(() -> {
            return findByWorld(worldName).stream()
                .filter(region -> {
                    if (region.getCenterLocation() == null) return false;
                    double deltaX = region.getCenterLocation().getX() - x;
                    double deltaZ = region.getCenterLocation().getZ() - z;
                    double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                    return distance <= radius;
                })
                .toList();
        }, this.executor);
    }

    /**
     * Finds regions by spiral position range.
     */
    @NotNull
    public CompletableFuture<List<IslandRegion>> findBySpiralPositionRangeAsync(@NotNull String worldName, 
                                                                                int minPosition, int maxPosition) {
        return CompletableFuture.supplyAsync(() -> {
            return findByWorld(worldName).stream()
                .filter(region -> region.getSpiralPosition() >= minPosition && 
                                 region.getSpiralPosition() <= maxPosition)
                .sorted((r1, r2) -> Integer.compare(r1.getSpiralPosition(), r2.getSpiralPosition()))
                .toList();
        }, this.executor);
    }

    /**
     * Gets the highest spiral position used in a world.
     */
    @NotNull
    public CompletableFuture<Integer> getMaxSpiralPositionAsync(@NotNull String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            return findByWorld(worldName).stream()
                .mapToInt(IslandRegion::getSpiralPosition)
                .max()
                .orElse(0);
        }, this.executor);
    }

    /**
     * Counts active regions in a world.
     */
    @NotNull
    public CompletableFuture<Long> countActiveRegionsAsync(@NotNull String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            return (long) findByWorld(worldName).size();
        }, this.executor);
    }

    /**
     * Performs cleanup of expired temporary permissions.
     */
    @NotNull
    public CompletableFuture<Integer> cleanupExpiredPermissionsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var em = this.entityManagerFactory.createEntityManager();
                try {
                    em.getTransaction().begin();
                    int deletedCount = em.createQuery(
                        "DELETE FROM RegionPermission p WHERE p.temporary = true AND p.expiresAt < CURRENT_TIMESTAMP"
                    ).executeUpdate();
                    em.getTransaction().commit();
                    
                    LOGGER.info("Cleaned up " + deletedCount + " expired permissions");
                    return deletedCount;
                } finally {
                    em.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to cleanup expired permissions", e);
                return 0;
            }
        }, this.executor);
    }
}