package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.server.RServer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository responsible for {@link RServer} persistence. Utilizes caching keyed by the server
 * UUID and delegates query execution to the provided executor.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RServerRepository extends GenericCachedRepository<RServer, Long, UUID> {

    /**
     * Creates the repository and binds it to the shared executor and entity manager factory.
     *
     * @param executor             executor powering asynchronous operations
     * @param entityManagerFactory JPA factory creating entity managers
     */
    public RServerRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RServer.class, RServer::getUniqueId);
    }
}
