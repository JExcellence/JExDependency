package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.server.RServer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class RServerRepository extends GenericCachedRepository<RServer, Long, UUID> {
    
    public RServerRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RServer.class, RServer::getUniqueId);
    }
}
