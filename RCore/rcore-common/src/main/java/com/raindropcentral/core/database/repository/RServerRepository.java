package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.server.RServer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository responsible for {@link RServer} persistence.
 * <p>
 * The {@link GenericCachedRepository} base class maintains an in-memory cache keyed by the
 * {@link UUID} returned from {@link RServer#getUniqueId()}, allowing repeated lookups of the same
 * logical server without hitting the database. This keeps network-wide server metadata readily
 * available for callers orchestrating cross-server operations.
 * <p>
 * Instances of this repository are wired into higher-level lifecycle services that broadcast
 * server availability to other Raindrop Central plugins. Those services rely on the repository to
 * provide consistent persistence semantics while they coordinate synchronization and messaging
 * duties across the cluster.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RServerRepository extends GenericCachedRepository<RServer, Long, UUID> {

    /**
     * Creates the repository and binds it to the shared executor and entity manager factory.
     * <p>
     * The supplied {@link ExecutorService} should match the asynchronous contract expected by the
     * surrounding services—typically a dedicated persistence executor that keeps blocking JPA work
     * away from the Bukkit main thread. The resulting repository is injected into service layers
     * that aggregate server state and propagate updates to consuming plugins.
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
