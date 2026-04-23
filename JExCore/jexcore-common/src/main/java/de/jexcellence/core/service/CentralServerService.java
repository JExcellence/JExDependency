package de.jexcellence.core.service;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.repository.CentralServerRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service facade over {@link CentralServerRepository}.
 */
public class CentralServerService {

    private final CentralServerRepository repo;
    private final JExLogger logger;

    public CentralServerService(@NotNull CentralServerRepository repo, @NotNull JExLogger logger) {
        this.repo = repo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<Optional<CentralServer>> findByUuid(@NotNull UUID serverUuid) {
        return this.repo.findByServerUuidAsync(serverUuid).exceptionally(ex -> {
            this.logger.error("findByUuid failed for {}: {}", serverUuid, ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<Optional<CentralServer>> findConnected() {
        return this.repo.findConnectedAsync().exceptionally(ex -> {
            this.logger.error("findConnected failed: {}", ex.getMessage());
            return Optional.empty();
        });
    }

    public @NotNull CompletableFuture<CentralServer> register(@NotNull UUID serverUuid) {
        return this.repo.findByServerUuidAsync(serverUuid)
                .thenCompose(opt -> opt
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> this.repo.createAsync(new CentralServer(serverUuid))))
                .exceptionally(ex -> {
                    this.logger.error("register failed for {}: {}", serverUuid, ex.getMessage());
                    return null;
                });
    }

    public @NotNull CentralServerRepository repository() {
        return this.repo;
    }
}
