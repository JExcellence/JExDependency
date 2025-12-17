package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link RPlayerRankPath} entities.
 * Handles player rank path (tree) selections and queries.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RPlayerRankPathRepository extends CachedRepository<RPlayerRankPath, Long, Long> {

    /**
     * Constructs a new {@code RDQPlayerRankPathRepository} for managing {@link RPlayerRankPath} entities.
     *
     * @param executor             the {@link ExecutorService} used for asynchronous repository operations
     * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers
     */
    public RPlayerRankPathRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RPlayerRankPath> entityClass,
            @NotNull Function<RPlayerRankPath, Long> keyExtractor
    ) {
        super(
                executor,
                entityManagerFactory,
                entityClass,
                keyExtractor
        );
    }
}