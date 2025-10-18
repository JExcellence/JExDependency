package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public class RStatisticRepository extends GenericCachedRepository<RAbstractStatistic, Long, Long> {
    
    public RStatisticRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RAbstractStatistic.class, AbstractEntity::getId);
    }
}
