package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository providing low-level access to {@link RAbstractStatistic} records. Enables shared
 * manipulation of statistics outside aggregate contexts while leveraging executor-managed
 * asynchronous operations.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RStatisticRepository extends GenericCachedRepository<RAbstractStatistic, Long, Long> {

    /**
     * Sets up the repository with module-wide executor and entity manager factory bindings.
     *
     * @param executor             executor coordinating async JPA interactions
     * @param entityManagerFactory factory creating entity managers for statistic operations
     */
    public RStatisticRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RAbstractStatistic.class, AbstractEntity::getId);
    }
}
