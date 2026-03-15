package com.raindropcentral.core.service.central;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.repository.RCentralServerRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Context holder for the currently authenticated server.
 *
 * <p>Maintains the server UUID, authentication state, and cached server entity.
 * Provides access to the current server for other components that need to
 * associate data with the authenticated server.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class ServerContext {

    private final RCentralServerRepository serverRepository;
    private UUID serverUuid;
    private boolean authenticated;
    private RCentralServer cachedServer;

    /**
     * Constructs a new ServerContext.
     *
     * @param serverRepository repository for loading server entities
     * @throws NullPointerException if serverRepository is null
     */
    public ServerContext(final @NotNull RCentralServerRepository serverRepository) {
        this.serverRepository = Objects.requireNonNull(serverRepository, "serverRepository cannot be null");
        this.authenticated = false;
    }

    /**
     * Marks the server as authenticated and loads the server entity.
 *
 * <p>This method should be called after successful authentication to establish
     * the server context for the current session.
     * </p>
     *
     * @param serverUuid the UUID of the authenticated server
     * @return CompletableFuture that completes when the server entity is loaded
     * @throws NullPointerException if serverUuid is null
     */
    public CompletableFuture<Void> setAuthenticated(final @NotNull UUID serverUuid) {
        Objects.requireNonNull(serverUuid, "serverUuid cannot be null");

        this.serverUuid = serverUuid;
        this.authenticated = true;

        // Load server entity from repository
        return serverRepository.findByServerUuid(serverUuid)
                .thenAccept(serverOpt -> {
                    this.cachedServer = serverOpt.orElse(null);
                });
    }

    /**
     * Clears the authentication state and cached server entity.
 *
 * <p>This method should be called when the server disconnects from the platform.
     * </p>
     */
    public void clearAuthentication() {
        this.serverUuid = null;
        this.authenticated = false;
        this.cachedServer = null;
    }

    /**
     * Checks if the server is currently authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Gets the server UUID if authenticated.
     *
     * @return Optional containing the server UUID if authenticated, empty otherwise
     */
    public @NotNull Optional<UUID> getServerUuid() {
        return Optional.ofNullable(serverUuid);
    }

    /**
     * Gets the current authenticated server entity.
 *
 * <p>Returns the cached server entity if available. If the cache is stale,
     * consider calling {@link #refreshServer()} to reload from the database.
     * </p>
     *
     * @return Optional containing the server entity if authenticated, empty otherwise
     */
    public @NotNull Optional<RCentralServer> getCurrentServer() {
        return Optional.ofNullable(cachedServer);
    }

    /**
     * Refreshes the cached server entity from the database.
 *
 * <p>Useful when the server entity may have been updated by another process
     * and the cache needs to be refreshed.
     * </p>
     *
     * @return CompletableFuture that completes when the server is refreshed
     */
    public CompletableFuture<Void> refreshServer() {
        if (!authenticated || serverUuid == null) {
            return CompletableFuture.completedFuture(null);
        }

        return serverRepository.findByServerUuid(serverUuid)
                .thenAccept(serverOpt -> {
                    this.cachedServer = serverOpt.orElse(null);
                });
    }

    /**
     * Gets the cached server entity without wrapping in Optional.
 *
 * <p>Returns null if not authenticated or server not loaded.
     * </p>
     *
     * @return the cached server entity, or null if not available
     */
    public @Nullable RCentralServer getCachedServer() {
        return cachedServer;
    }
}
