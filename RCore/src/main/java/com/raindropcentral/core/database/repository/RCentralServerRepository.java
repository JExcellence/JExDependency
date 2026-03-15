package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing RCentralServer entities.
 * Handles persistence of server connection state to RaindropCentral platform.
 */
public class RCentralServerRepository extends CachedRepository<RCentralServer, Long, Long> {

    /**
     * Executes RCentralServerRepository.
     */
    public RCentralServerRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RCentralServer> entityClass,
            @NotNull Function<RCentralServer, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds a server by its unique server UUID.
     *
     * @param serverUuid the server UUID to search for
     * @return CompletableFuture containing the server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findByServerUuid(final @NotNull UUID serverUuid) {
        return CompletableFuture.supplyAsync(
            () -> findByAttribute("serverUuid", serverUuid),
            getExecutorService()
        );
    }

    /**
     * Finds the currently connected server (if any).
     *
     * @return CompletableFuture containing the connected server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findConnectedServer() {
        return CompletableFuture.supplyAsync(
            () -> findByAttribute("connectionStatus", RCentralServer.ConnectionStatus.CONNECTED),
            getExecutorService()
        );
    }
}
