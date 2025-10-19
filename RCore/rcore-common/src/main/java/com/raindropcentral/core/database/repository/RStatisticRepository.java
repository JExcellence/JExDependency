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
 * asynchronous operations. Prefer direct repository usage for administrative tooling or
 * cross-profile maintenance tasks that must bypass aggregate loaders yet still respect the
 * service executor model. Cached lookups remain synchronized with aggregate refresh logic, so
 * callers should invalidate or reload aggregate views after committing mutations through this
 * repository to keep in-memory projections consistent.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RStatisticRepository extends GenericCachedRepository<RAbstractStatistic, Long, Long> {

    /**
     * Sets up the repository with module-wide executor and entity manager factory bindings. Use
     * this constructor when statistic adjustments occur outside aggregate orchestration layers;
     * it keeps cached entity snapshots aligned with later aggregate refresh operations.
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
