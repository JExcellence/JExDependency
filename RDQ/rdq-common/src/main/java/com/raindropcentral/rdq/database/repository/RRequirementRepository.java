package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public final class RRequirementRepository extends GenericCachedRepository<RRequirement, Long, Long> {

    public RRequirementRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRequirement.class, AbstractEntity::getId);
    }
}