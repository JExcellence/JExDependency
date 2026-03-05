package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.RDSPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Represents r r d s player.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({
        "unused",
        "FieldCanBeLocal"
})
public class RRDSPlayer extends CachedRepository<RDSPlayer, Long, UUID> {

    private final EntityManagerFactory emf;

    /**
     * Creates a new r r d s player.
     *
     * @param executorService executor used for repository work
     * @param entityManagerFactory entity manager factory backing the repository
     * @param entityClass entity type managed by the repository
     * @param keyExtractor cache key extractor for loaded entities
     */
    public RRDSPlayer(
            @NotNull ExecutorService executorService,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDSPlayer> entityClass,
            @NotNull Function<RDSPlayer, UUID> keyExtractor
    ) {
        super(executorService, entityManagerFactory, entityClass, keyExtractor);
        this.emf = entityManagerFactory;
    }

    /**
     * Finds by player.
     *
     * @param player_uuid player identifier to look up
     * @return the matched by player, or {@code null} when none exists
     */
    public RDSPlayer findByPlayer(UUID player_uuid) {
        return findByAttributes(Map.of("player_uuid", player_uuid)).orElse(null);
    }

}
