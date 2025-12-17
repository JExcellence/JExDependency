package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.repository.RCentralServerRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for managing RaindropCentral server connection entities.
 * <p>
 * This service provides high-level operations for server connection management, handling
 * the persistence of server state and connection information to the RaindropCentral platform.
 * All operations are asynchronous to avoid blocking the main thread.
 * </p>
 * <p>
 * The service automatically injects the {@link RCentralServerRepository} via the
 * {@link de.jexcellence.hibernate.repository.RepositoryManager} when instantiated through
 * {@code createInstance()}.
 * </p>
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class RCentralServerService {

    @InjectRepository
    private RCentralServerRepository serverRepository;

    /**
     * Constructs a new RCentralServerService.
     * <p>
     * The repository will be automatically injected by the RepositoryManager when this service
     * is created via {@code RepositoryManager.getInstance().createInstance(RCentralServerService.class)}.
     * </p>
     */
    public RCentralServerService() {
        // Repository injected by RepositoryManager
    }

    /**
     * Finds a server by its unique server UUID.
     *
     * @param serverUuid the server UUID to search for
     * @return future containing an optional with the server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findByServerUuid(final @NotNull UUID serverUuid) {
        return serverRepository.findByServerUuid(serverUuid);
    }

    /**
     * Finds the currently connected server (if any).
     * <p>
     * Only one server should be in CONNECTED status at a time.
     * </p>
     *
     * @return future containing an optional with the connected server if found
     */
    public CompletableFuture<Optional<RCentralServer>> findConnectedServer() {
        return serverRepository.findConnectedServer();
    }

    /**
     * Creates or updates a server entity.
     * <p>
     * If a server with the same ID already exists, it will be updated.
     * Otherwise, a new server entity will be created.
     * </p>
     *
     * @param server the server entity to save
     * @return future containing the saved server entity
     */
    public CompletableFuture<RCentralServer> createOrUpdate(final @NotNull RCentralServer server) {
        if (server.getId() == null) {
            return serverRepository.createAsync(server);
        }
        return serverRepository.updateAsync(server);
    }

    /**
     * Marks a server as connected and ensures all other servers are disconnected.
     * <p>
     * This method handles the business logic of ensuring only one server is connected
     * at a time by disconnecting any currently connected server before connecting the new one.
     * </p>
     *
     * @param server the server to mark as connected
     * @return future containing the updated server entity
     */
    public CompletableFuture<RCentralServer> connect(final @NotNull RCentralServer server) {
        return findConnectedServer()
                .thenCompose(currentlyConnected -> {
                    // Disconnect any currently connected server
                    if (currentlyConnected.isPresent() && !currentlyConnected.get().equals(server)) {
                        final RCentralServer connectedServer = currentlyConnected.get();
                        connectedServer.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
                        return serverRepository.updateAsync(connectedServer);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(v -> {
                    // Connect the new server
                    server.setConnectionStatus(RCentralServer.ConnectionStatus.CONNECTED);
                    return createOrUpdate(server);
                });
    }

    /**
     * Marks a server as disconnected.
     *
     * @param server the server to disconnect
     * @return future containing the updated server entity
     */
    public CompletableFuture<RCentralServer> disconnect(final @NotNull RCentralServer server) {
        server.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
        return createOrUpdate(server);
    }

    /**
     * Marks a server as disconnected by its UUID.
     *
     * @param serverUuid the UUID of the server to disconnect
     * @return future that completes when the disconnection is finished
     */
    public CompletableFuture<Object> disconnectByUuid(final @NotNull UUID serverUuid) {
        return findByServerUuid(serverUuid)
                .thenCompose(serverOpt -> serverOpt
                        .map(server -> disconnect(server).thenApply(s -> null))
                        .orElseGet(() -> CompletableFuture.completedFuture(null))
                );
    }

    /**
     * Checks if a server is currently connected.
     *
     * @param serverUuid the server UUID to check
     * @return future containing true if the server is connected
     */
    public CompletableFuture<Boolean> isConnected(final @NotNull UUID serverUuid) {
        return findByServerUuid(serverUuid)
                .thenApply(serverOpt -> serverOpt
                        .map(server -> server.getConnectionStatus() == RCentralServer.ConnectionStatus.CONNECTED)
                        .orElse(false)
                );
    }

    /**
     * Deletes a server by its ID.
     *
     * @param serverId the server's database ID
     * @return future that completes when the deletion is finished
     */
    public CompletableFuture<Boolean> deleteById(final @NotNull Long serverId) {
        return serverRepository.deleteAsync(serverId);
    }

    /**
     * Gets the injected repository instance.
     * <p>
     * This is primarily for testing purposes or advanced use cases.
     * </p>
     *
     * @return the server repository
     */
    public RCentralServerRepository getRepository() {
        return serverRepository;
    }
}
