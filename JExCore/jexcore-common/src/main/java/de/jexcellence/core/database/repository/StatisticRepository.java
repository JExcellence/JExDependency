package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.statistic.AbstractStatistic;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Low-level repository over all {@link AbstractStatistic} rows. Use for
 * administrative tooling that bypasses {@link PlayerStatisticRepository}
 * aggregate loaders.
 */
public class StatisticRepository extends AbstractCrudRepository<AbstractStatistic, Long> {

    public StatisticRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<AbstractStatistic> entityClass
    ) {
        super(executor, emf, entityClass);
    }
}
