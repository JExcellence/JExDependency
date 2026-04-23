package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link CentralServer} entities. Looks servers up by their
 * UUID or by connection status.
 */
public class CentralServerRepository extends AbstractCrudRepository<CentralServer, Long> {

    public CentralServerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<CentralServer> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    public @NotNull CompletableFuture<Optional<CentralServer>> findByServerUuidAsync(@NotNull UUID serverUuid) {
        return query().and("serverUuid", serverUuid).firstAsync();
    }

    public @NotNull CompletableFuture<Optional<CentralServer>> findConnectedAsync() {
        return query()
                .and("connectionStatus", CentralServer.ConnectionStatus.CONNECTED)
                .firstAsync();
    }
}
