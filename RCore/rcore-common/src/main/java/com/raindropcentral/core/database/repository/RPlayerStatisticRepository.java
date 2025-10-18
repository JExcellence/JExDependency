package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public class RPlayerStatisticRepository extends GenericCachedRepository<RPlayerStatistic, Long, Long> {
    
    public RPlayerStatisticRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerStatistic.class, AbstractEntity::getId);
    }
}
