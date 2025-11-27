package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository implementation that provides cached access to {@link RRequirement} entities.
 * It leverages the shared {@link GenericCachedRepository} infrastructure to reuse entity
 * management and identifier resolution logic across RDQ repositories.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RRequirementRepository extends GenericCachedRepository<RRequirement, Long, Long> {

    /**
     * Creates a new repository instance backed by the provided executor and entity manager
     * factory.
     *
     * @param executor the asynchronous executor responsible for repository operations
     * @param entityManagerFactory the entity manager factory used to obtain persistence contexts
     */
    public RRequirementRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRequirement.class, AbstractEntity::getId);
    }
}