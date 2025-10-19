package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RPlayerQuest;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Repository that provides cached access to {@link RPlayerQuest} entities.
 *
 * <p>
 * The repository delegates persistence operations to the
 * {@link GenericCachedRepository} base class while ensuring asynchronous query
 * execution through the provided {@link ExecutorService}.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RPlayerQuestRepository extends GenericCachedRepository<RPlayerQuest, Long, Long> {

    /**
     * Creates a new repository backed by the supplied executor and entity manager factory.
     *
     * @param executor the executor used to run asynchronous repository operations
     * @param entityManagerFactory the entity manager factory supplying persistence contexts
     */
    public RPlayerQuestRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerQuest.class, AbstractEntity::getId);
    }
}
