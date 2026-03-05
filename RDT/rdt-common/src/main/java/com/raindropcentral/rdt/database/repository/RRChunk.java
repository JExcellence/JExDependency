package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RChunk;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for {@link RChunk} entities.
 * <p>
 * Provides standard CRUD operations and a convenience finder for locating
 * a chunk by its X/Z coordinates. Use off the main server thread for blocking calls.
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RRChunk extends CachedRepository<RChunk, Long, UUID> {
    // Keep a reference to the EntityManagerFactory for custom ad-hoc queries
    private final EntityManagerFactory emf;

    /**
     * Construct a repository for {@link RChunk}.
     *
     * @param executorService       executor for async operations
     * @param entityManagerFactory  JPA entity manager factory
     * @param entityClass           entity class (typically {@link RChunk}.class)
     * @param keyExtractor          cache key extractor
     */
    public RRChunk(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RChunk> entityClass,
            @NotNull Function<RChunk, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Find a chunk by its coordinate pair.
     * IMPORTANT: Run on a background thread; do not call from the main server thread.
     *
     * @param x_loc chunk X coordinate
     * @param z_loc chunk Z coordinate
     * @return matching chunk or {@code null} if none
     */
    public RChunk findByCoords(int x_loc, int z_loc) {
        return findByAttributes(Map.of("x_loc", x_loc, "z_loc", z_loc)).orElse(null);
    }
}