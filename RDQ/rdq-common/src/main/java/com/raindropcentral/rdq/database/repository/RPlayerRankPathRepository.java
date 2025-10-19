package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository for managing persistent {@link RPlayerRankPath} entities.
 * Handles player rank path (tree) selections and queries by delegating to the
 * cached repository infrastructure provided by {@link GenericCachedRepository}.
 *
 * <p>The cached repository automatically batches entity lookups and executes
 * operations asynchronously using the supplied {@link ExecutorService}.</p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class RPlayerRankPathRepository extends GenericCachedRepository<RPlayerRankPath, Long, Long> {

    /**
     * Constructs a new {@code RPlayerRankPathRepository} for managing {@link RPlayerRankPath} entities.
     *
     * @param executor             The {@link ExecutorService} used for asynchronous repository operations.
     * @param entityManagerFactory The {@link EntityManagerFactory} used to create and manage entity managers.
     */
    public RPlayerRankPathRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(
                executor,
                entityManagerFactory,
                RPlayerRankPath.class,
                RPlayerRankPath::getId
        );
    }
}