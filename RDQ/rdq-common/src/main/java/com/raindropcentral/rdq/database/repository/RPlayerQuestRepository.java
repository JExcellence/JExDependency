package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RPlayerQuest;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public final class RPlayerQuestRepository extends GenericCachedRepository<RPlayerQuest, Long, Long> {

    public RPlayerQuestRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerQuest.class, AbstractEntity::getId);
    }
}