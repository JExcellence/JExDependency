package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository providing low-level access to {@link RAbstractStatistic} records. Enables shared
 * manipulation of statistics outside aggregate contexts while leveraging executor-managed
 * asynchronous operations. Prefer direct repository usage for administrative tooling or
 * cross-profile maintenance tasks that must bypass aggregate loaders yet still respect the
 * service executor model. Cached lookups remain synchronized with aggregate refresh logic, so
 * callers should invalidate or reload aggregate views after committing mutations through this
 * repository to keep in-memory projections consistent.
 * <p>
 * Consumers are required to log via {@link com.raindropcentral.rplatform.logging.CentralLogger
 * CentralLogger} whenever a cache miss occurs, before issuing batch updates or deletes, and after
 * those operations complete. Since this repository is commonly used by background jobs, error
 * handling must include structured log entries for any failed future or constraint violation so
 * operations staff can correlate the failure with upstream maintenance tasks.
 * </p>
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
        @NotNull ExecutorService executor,
        @NotNull EntityManagerFactory entityManagerFactory,
        @NotNull Class<RAbstractStatistic> entityClass,
        @NotNull Function<RAbstractStatistic, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
