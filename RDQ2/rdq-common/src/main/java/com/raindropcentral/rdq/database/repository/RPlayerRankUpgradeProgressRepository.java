package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository responsible for cached persistence of
 * {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress}
 * entities.
 *
 * <p>
 * This repository delegates the heavy lifting to the underlying
 * {@link de.jexcellence.hibernate.repository.GenericCachedRepository} while
 * ensuring rank upgrade progress records can be retrieved and stored using the
 * configured asynchronous executor and entity manager factory.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class RPlayerRankUpgradeProgressRepository extends GenericCachedRepository<RPlayerRankUpgradeProgress, Long, Long> {

    /**
     * Creates a repository for managing
     * {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress}
     * entities.
     *
     * @param executor             the {@link ExecutorService} responsible for running
     *                             asynchronous repository operations
     * @param entityManagerFactory the {@link EntityManagerFactory} used to create and
     *                             manage entity managers
     */
    public RPlayerRankUpgradeProgressRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(
                executor,
                entityManagerFactory,
                RPlayerRankUpgradeProgress.class,
                RPlayerRankUpgradeProgress::getId
        );
    }
}