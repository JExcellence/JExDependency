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
 * <p>
 * The statistic table relies on a surrogate identifier instead of the natural UUID from the
 * player record to avoid repetitive joins when statistics are queried or updated in bulk. The
 * repository therefore exposes the surrogate key as the cache identifier so repeated lookups by
 * primary key remain hot and avoid unnecessary round-trips to the database layer.
 * </p>
 * <p>
 * Aggregated statistics are materialized lazily using the cached lookup before being rehydrated
 * into the calling {@code RPlayer} aggregate. By keeping the statistics cached with their
 * surrogate identifiers, load operations perform in constant time even as the number of tracked
 * statistic rows grows across sessions.
 * </p>
 * <p>
 * All asynchronous work is scheduled on the shared {@link java.util.concurrent.ExecutorService}
 * provided by the {@link GenericCachedRepository} base class. This guarantees that database and
 * hydration tasks operate on the same bounded executor configured for the core services, avoiding
 * accidental execution on the common fork-join pool and ensuring predictable threading semantics
 * during batch statistic updates.
 * </p>
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
