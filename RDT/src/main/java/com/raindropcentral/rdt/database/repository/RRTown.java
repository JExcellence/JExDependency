package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RTown;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for {@link com.raindropcentral.rdt.database.entity.RTown} entities.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Provide standard CRUD operations backed by {@link CachedRepository}</li>
 *   <li>Expose convenience finders for common lookups (by mayor, name, or town UUID)</li>
 *   <li>Support async usage through the provided {@link ExecutorService}</li>
 * </ul>
 * Notes:
 * <ul>
 *   <li>Blocking operations should be executed off the main server thread.</li>
 * </ul>
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RRTown extends CachedRepository<RTown, Long, UUID> {

    // Keep a reference to the EntityManagerFactory for custom ad-hoc queries
    private final EntityManagerFactory emf;

    /**
     * Construct a repository for {@link RTown}.
     *
     * @param executorService       executor for async operations
     * @param entityManagerFactory  JPA entity manager factory
     * @param entityClass           entity class (typically {@link RTown}.class)
     * @param keyExtractor          cache key extractor (unique external identifier)
     */
    public RRTown(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RTown> entityClass,
            @NotNull Function<RTown, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Find the town where the given UUID is the mayor.
     * IMPORTANT: Run on a background thread; do not call from the main server thread.
     *
     * @param mayor UUID of the player to check
     * @return the first town found where this player is mayor, or {@code null} if none
     */
    public RTown findByMayor(UUID mayor) {
        return findByAttributes(Map.of("mayor", mayor)).orElseThrow();
    }


    /**
     * Find a town by its human-friendly name.
     *
     * @param townName case-sensitive town name
     * @return matching town or {@code null} if none
     */
    public RTown findByTName(String townName) {
        return findByAttributes(Map.of("townName", townName)).orElseThrow();
    }

    /**
     * Find a town by its public UUID identifier.
     *
     * @param uuid town UUID
     * @return matching town or {@code null} if none
     */
    public RTown findByTownUUID(UUID uuid) {
        return findByAttributes(Map.of("uuid", uuid)).orElseThrow();
    }

}