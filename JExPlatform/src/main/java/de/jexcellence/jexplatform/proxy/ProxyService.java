package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Shared proxy bridge consumed by Paper modules.
 *
 * <p>Provides cross-server presence lookups, player transfers, module action
 * dispatching, and pending-arrival token management.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface ProxyService {

    /**
     * Returns whether this proxy bridge is actively connected to a proxy coordinator.
     *
     * @return {@code true} when proxy-backed operations are available
     */
    boolean isAvailable();

    /**
     * Returns the active proxy protocol version.
     *
     * @return the protocol version
     */
    int protocolVersion();

    /**
     * Returns the proxy action channel identifier.
     *
     * @return the channel name
     */
    @NotNull String channelName();

    /**
     * Returns one player's presence snapshot when available.
     *
     * @param playerUuid the target player UUID
     * @return an async optional presence snapshot
     */
    @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(
            @NotNull UUID playerUuid);

    /**
     * Returns presence snapshots for a collection of UUIDs.
     *
     * @param playerUuids the target UUID collection
     * @return an async map of known presence snapshots
     */
    @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(
            @NotNull Collection<UUID> playerUuids);

    /**
     * Requests a proxy transfer for one player.
     *
     * @param transferRequest the transfer request
     * @return an async transfer result
     */
    @NotNull CompletableFuture<Boolean> requestPlayerTransfer(
            @NotNull ProxyTransferRequest transferRequest);

    /**
     * Sends one module action envelope.
     *
     * @param envelope the action envelope
     * @return an async action result
     */
    @NotNull CompletableFuture<ProxyActionResult> sendAction(
            @NotNull ProxyActionEnvelope envelope);

    /**
     * Registers one module action handler.
     *
     * @param moduleId the module identifier
     * @param actionId the action identifier
     * @param handler  the action handler
     */
    void registerActionHandler(@NotNull String moduleId, @NotNull String actionId,
                               @NotNull ProxyActionHandler handler);

    /**
     * Removes one module action handler.
     *
     * @param moduleId the module identifier
     * @param actionId the action identifier
     */
    void unregisterActionHandler(@NotNull String moduleId, @NotNull String actionId);

    /**
     * Returns the pending-arrival token store associated with this proxy bridge.
     *
     * @return the pending-arrival token store
     */
    @NotNull PendingArrivalActionStore pendingArrivals();
}
