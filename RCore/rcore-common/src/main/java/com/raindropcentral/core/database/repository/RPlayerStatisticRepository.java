package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository handling {@link RPlayerStatistic} aggregates. Provides cached access keyed by the
 * surrogate identifier to support efficient statistic loading on dedicated executors.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RPlayerStatisticRepository extends GenericCachedRepository<RPlayerStatistic, Long, Long> {

    /**
     * Configures the repository with the shared executor and entity manager factory.
     *
     * @param executor             asynchronous executor shared across database operations
     * @param entityManagerFactory JPA factory responsible for creating entity managers
     */
    public RPlayerStatisticRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerStatistic.class, AbstractEntity::getId);
    }
}
