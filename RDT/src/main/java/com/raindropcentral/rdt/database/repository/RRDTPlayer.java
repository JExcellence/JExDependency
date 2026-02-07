package com.raindropcentral.rdt.database.repository;

import com.raindropcentral.rdt.database.entity.RDTPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Cached repository for {@link RDTPlayer} entities.
 * <p>
 * Provides CRUD operations and a convenience finder for locating a player by UUID.
 * Use off the main server thread for blocking calls.
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RRDTPlayer extends CachedRepository<RDTPlayer, Long, UUID> {
    // Keep a reference to the EntityManagerFactory for custom ad-hoc queries
    private final EntityManagerFactory emf;

    /**
     * Construct a repository for {@link RDTPlayer}.
     *
     * @param executorService       executor for async operations
     * @param entityManagerFactory  JPA entity manager factory
     * @param entityClass           entity class (typically {@link RDTPlayer}.class)
     * @param keyExtractor          cache key extractor (player UUID)
     */
    public RRDTPlayer(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDTPlayer> entityClass,
            @NotNull Function<RDTPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Find a player record by player UUID.
     * IMPORTANT: Run on a background thread; do not call from the main server thread.
     *
     * @param player_uuid UUID of the player
     * @return matching player or {@code null} if none
     */
    public RDTPlayer findByPlayer(UUID player_uuid) {
        return findByAttributes(Map.of("player_uuid", player_uuid)).orElseThrow();
    }

}