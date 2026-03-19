package de.jexcellence.oneblock.database.repository;

import de.jexcellence.hibernate.repository.CachedRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository providing cached CRUD access to {@link OneblockIsland} entities.
 * Handles island data persistence with identifier-based caching and asynchronous operations.
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class OneblockIslandRepository extends CachedRepository<OneblockIsland, Long, String> {

    private static final String IDENTIFIER_ATTRIBUTE = "identifier";
    
    private final ExecutorService executor;
    private final EntityManagerFactory entityManagerFactory;

    public OneblockIslandRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<OneblockIsland> entityClass,
            @NotNull Function<OneblockIsland, String> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.executor = executor;
        this.entityManagerFactory = entityManagerFactory;
    }

    // ========== Synchronous Methods ==========

    /**
     * Finds an island by identifier
     */
    @Nullable
    public OneblockIsland findByIdentifier(@NotNull String identifier) {
        return findByKey(IDENTIFIER_ATTRIBUTE, identifier).orElse(null);
    }

    /**
     * Finds an island by owner UUID.
     * First checks cache, then queries all islands to find by owner.
     */
    @Nullable
    public OneblockIsland findByOwner(@NotNull UUID ownerUuid) {
        // Check cache first
        for (OneblockIsland island : getCachedByKey().values()) {
            if (island.getOwner() != null && ownerUuid.equals(island.getOwner().getUniqueId())) {
                return island;
            }
        }
        // Query all islands and filter by owner UUID
        for (OneblockIsland island : findAll()) {
            if (island.getOwner() != null && ownerUuid.equals(island.getOwner().getUniqueId())) {
                return island;
            }
        }
        return null;
    }

    /**
     * Finds an island by owner UUID using identifier lookup.
     * This method searches through all known identifiers.
     */
    @Nullable
    public OneblockIsland findByOwnerViaIdentifier(@NotNull UUID ownerUuid, @NotNull String identifierPrefix) {
        for (int i = 1; i <= 1000; i++) {
            String identifier = identifierPrefix + i;
            var island = findByIdentifier(identifier);
            if (island != null && island.getOwner() != null && ownerUuid.equals(island.getOwner().getUniqueId())) {
                return island;
            }
        }
        return null;
    }

    /**
     * Finds an island by location (returns Optional for compatibility)
     */
    @NotNull
    public Optional<OneblockIsland> findByLocation(@NotNull Location location) {
        return findAll().stream()
            .filter(island -> island.getCenterLocation() != null)
            .filter(island -> island.getCenterLocation().getWorld() != null && 
                             island.getCenterLocation().getWorld().equals(location.getWorld()))
            .filter(island -> island.getRegion() != null && island.getRegion().contains(location))
            .findFirst();
    }

    /**
     * Finds all islands where a player is a member
     */
    @NotNull
    public List<OneblockIsland> findByMember(@NotNull UUID playerUuid) {
        return findAll().stream()
            .filter(island -> island.getMembers().stream()
                .anyMatch(member -> playerUuid.equals(member.getUniqueId())))
            .toList();
    }

    /**
     * Finds all private islands
     */
    @NotNull
    public List<OneblockIsland> findAllPrivate() {
        return findAll().stream()
            .filter(OneblockIsland::isPrivacy)
            .toList();
    }

    /**
     * Finds all public islands
     */
    @NotNull
    public List<OneblockIsland> findAllPublic() {
        return findAll().stream()
            .filter(island -> !island.isPrivacy())
            .toList();
    }

    /**
     * Gets all unique world names with islands
     */
    @NotNull
    public Set<String> findAllWorldNames() {
        return findAll().stream()
            .filter(island -> island.getCenterLocation() != null)
            .filter(island -> island.getCenterLocation().getWorld() != null)
            .map(island -> island.getCenterLocation().getWorld().getName())
            .collect(Collectors.toSet());
    }

    /**
     * Counts islands in a specific world
     */
    public int countByWorld(@NotNull String worldName) {
        return (int) findAll().stream()
            .filter(island -> island.getCenterLocation() != null)
            .filter(island -> island.getCenterLocation().getWorld() != null)
            .filter(island -> worldName.equals(island.getCenterLocation().getWorld().getName()))
            .count();
    }

    // ========== Asynchronous Methods ==========

    /**
     * Asynchronously finds an island by identifier
     */
    @NotNull
    public CompletableFuture<Optional<OneblockIsland>> findByIdentifierAsync(@NotNull String identifier) {
        return findByKeyAsync(IDENTIFIER_ATTRIBUTE, identifier);
    }

    /**
     * Asynchronously finds an island by owner UUID
     */
    @NotNull
    public CompletableFuture<Optional<OneblockIsland>> findByOwnerAsync(@NotNull UUID ownerUuid) {
        return CompletableFuture.supplyAsync(
            () -> Optional.ofNullable(findByOwner(ownerUuid)),
            this.executor
        );
    }

    /**
     * Asynchronously finds islands by member UUID
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findByMemberAsync(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> findByMember(playerUuid),
            this.executor
        );
    }

    /**
     * Checks if an island exists by identifier
     */
    @NotNull
    public CompletableFuture<Boolean> existsByIdentifierAsync(@NotNull String identifier) {
        return findByIdentifierAsync(identifier).thenApply(Optional::isPresent);
    }

    /**
     * Creates or updates an island asynchronously
     */
    @NotNull
    public CompletableFuture<OneblockIsland> createOrUpdateAsync(@NotNull OneblockIsland island) {
        return existsByIdentifierAsync(island.getIdentifier())
            .thenCompose(exists -> exists
                ? CompletableFuture.supplyAsync(() -> update(island), this.executor)
                : CompletableFuture.supplyAsync(() -> create(island), this.executor)
            );
    }

    // ========== Additional Methods for Compatibility ==========

    /**
     * Finds islands near a location within a radius
     */
    @NotNull
    public List<OneblockIsland> findByLocationNear(@NotNull Location center, double radius) {
        return findAll().stream()
            .filter(island -> island.getCenterLocation() != null)
            .filter(island -> island.getCenterLocation().getWorld() != null)
            .filter(island -> island.getCenterLocation().getWorld().equals(center.getWorld()))
            .filter(island -> island.getCenterLocation().distance(center) <= radius)
            .toList();
    }

    /**
     * Counts islands in a specific world by name
     */
    public int countByWorldName(@NotNull String worldName) {
        return countByWorld(worldName);
    }

    /**
     * Gets total island count
     */
    @NotNull
    public CompletableFuture<Long> getTotalIslandCount() {
        return CompletableFuture.supplyAsync(
            () -> (long) findAll().size(),
            this.executor
        );
    }

    /**
     * Finds all active (non-deleted) islands asynchronously
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findActiveIslandsAsync() {
        return CompletableFuture.supplyAsync(this::findAll, this.executor);
    }

    /**
     * Finds islands by member UUID asynchronously (alias)
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findIslandsByMemberAsync(@NotNull UUID playerUuid) {
        return findByMemberAsync(playerUuid);
    }

    /**
     * Finds all islands asynchronously (no pagination)
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findAllAsync() {
        return CompletableFuture.supplyAsync(this::findAll, this.executor);
    }

    /**
     * Finds all islands with owner eagerly loaded to avoid LazyInitializationException.
     * Uses entity graph to fetch owner in the same query.
     */
    @NotNull
    public CompletableFuture<List<OneblockIsland>> findAllWithOwnerAsync() {
        return CompletableFuture.supplyAsync(() -> {
            var em = this.entityManagerFactory.createEntityManager();
            try {
                var graph = em.getEntityGraph("Island.withOwner");
                var query = em.createQuery("SELECT i FROM OneblockIsland i", OneblockIsland.class);
                query.setHint("jakarta.persistence.fetchgraph", graph);
                return query.getResultList();
            } finally {
                em.close();
            }
        }, this.executor);
    }

    /**
     * Updates an island asynchronously (alias for createOrUpdateAsync)
     */
    @NotNull
    public CompletableFuture<OneblockIsland> persistAsync(@NotNull OneblockIsland island) {
        return createOrUpdateAsync(island);
    }
}
