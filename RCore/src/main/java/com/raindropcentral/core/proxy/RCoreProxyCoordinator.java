/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.proxy;

import com.raindropcentral.rplatform.proxy.InMemoryPendingArrivalActionStore;
import com.raindropcentral.rplatform.proxy.PendingArrivalActionStore;
import com.raindropcentral.rplatform.proxy.PlayerPresenceSnapshot;
import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyActionHandler;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory proxy coordinator used by RCore for handler routing and request correlation.
 */
public final class RCoreProxyCoordinator implements ProxyService {

    /**
     * Transfer executor used to perform actual proxy transfer operations.
     */
    @FunctionalInterface
    public interface TransferExecutor {
        /**
         * Requests one player transfer.
         *
         * @param transferRequest transfer request
         * @return async transfer result
         */
        @NotNull CompletableFuture<Boolean> requestTransfer(@NotNull ProxyTransferRequest transferRequest);
    }

    private final ProxyHostConfig config;
    private final PendingArrivalActionStore pendingArrivalActionStore;
    private final ConcurrentHashMap<String, ProxyActionHandler> actionHandlers;
    private final ConcurrentHashMap<UUID, PlayerPresenceSnapshot> presenceSnapshots;
    private final ConcurrentHashMap<UUID, CompletableFuture<ProxyActionResult>> correlatedRequests;
    private final ConcurrentHashMap<UUID, Long> correlatedRequestDeadlines;

    private volatile TransferExecutor transferExecutor;

    /**
     * Creates a new proxy coordinator.
     *
     * @param config proxy host configuration
     */
    public RCoreProxyCoordinator(final @NotNull ProxyHostConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.pendingArrivalActionStore = new InMemoryPendingArrivalActionStore();
        this.actionHandlers = new ConcurrentHashMap<>();
        this.presenceSnapshots = new ConcurrentHashMap<>();
        this.correlatedRequests = new ConcurrentHashMap<>();
        this.correlatedRequestDeadlines = new ConcurrentHashMap<>();
        this.transferExecutor = transferRequest -> CompletableFuture.completedFuture(false);
    }

    /**
     * Returns the immutable configuration snapshot backing this coordinator.
     *
     * @return proxy host config
     */
    public @NotNull ProxyHostConfig config() {
        return this.config;
    }

    /**
     * Replaces the active transfer executor.
     *
     * @param transferExecutor transfer executor
     */
    public void setTransferExecutor(final @NotNull TransferExecutor transferExecutor) {
        this.transferExecutor = Objects.requireNonNull(transferExecutor, "transferExecutor cannot be null");
    }

    /**
     * Updates one player's presence snapshot.
     *
     * @param playerUuid player UUID
     * @param serverId server route identifier
     * @param online whether player is online
     */
    public void updatePresence(
        final @NotNull UUID playerUuid,
        final @NotNull String serverId,
        final boolean online
    ) {
        if (!online) {
            this.presenceSnapshots.remove(playerUuid);
            return;
        }
        this.presenceSnapshots.put(
            playerUuid,
            PlayerPresenceSnapshot.online(playerUuid, this.config.resolveRoute(serverId), System.currentTimeMillis())
        );
    }

    /**
     * Removes one player's presence snapshot.
     *
     * @param playerUuid player UUID
     */
    public void removePresence(final @NotNull UUID playerUuid) {
        this.presenceSnapshots.remove(playerUuid);
    }

    /**
     * Opens a correlated request future tracked by request identifier.
     *
     * @param requestId request identifier
     * @return correlated request future
     */
    public @NotNull CompletableFuture<ProxyActionResult> openCorrelatedRequest(final @NotNull UUID requestId) {
        final CompletableFuture<ProxyActionResult> future = new CompletableFuture<>();
        this.correlatedRequests.put(requestId, future);
        this.correlatedRequestDeadlines.put(
            requestId,
            System.currentTimeMillis() + this.config.requestTimeoutMillis()
        );
        return future;
    }

    /**
     * Completes one correlated request when still pending.
     *
     * @param requestId request identifier
     * @param result action result
     * @return {@code true} when completion succeeded
     */
    public boolean completeCorrelatedRequest(
        final @NotNull UUID requestId,
        final @NotNull ProxyActionResult result
    ) {
        final CompletableFuture<ProxyActionResult> future = this.correlatedRequests.remove(requestId);
        this.correlatedRequestDeadlines.remove(requestId);
        if (future == null) {
            return false;
        }
        future.complete(result);
        return true;
    }

    /**
     * Expires pending correlated requests whose timeout deadline has elapsed.
     *
     * @param nowEpochMilli comparison timestamp in epoch milliseconds
     * @return count of timed-out requests
     */
    public int expireTimedOutRequests(final long nowEpochMilli) {
        int timedOutCount = 0;
        for (final Map.Entry<UUID, Long> entry : this.correlatedRequestDeadlines.entrySet()) {
            if (nowEpochMilli < entry.getValue()) {
                continue;
            }
            final UUID requestId = entry.getKey();
            final CompletableFuture<ProxyActionResult> future = this.correlatedRequests.remove(requestId);
            if (future != null) {
                future.complete(ProxyActionResult.failure("timeout", "Proxy request timed out."));
                timedOutCount++;
            }
            this.correlatedRequestDeadlines.remove(requestId);
        }
        return timedOutCount;
    }

    /**
     * Returns pending correlated request count.
     *
     * @return pending correlated request count
     */
    public int pendingRequestCount() {
        return this.correlatedRequests.size();
    }

    /**
     * Returns registered action-handler count.
     *
     * @return registered action-handler count
     */
    public int handlerCount() {
        return this.actionHandlers.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int protocolVersion() {
        return this.config.protocolVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String channelName() {
        return this.config.channelName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(final @NotNull UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(this.presenceSnapshots.get(playerUuid)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(
        final @NotNull Collection<UUID> playerUuids
    ) {
        final Map<UUID, PlayerPresenceSnapshot> snapshots = new LinkedHashMap<>();
        for (final UUID playerUuid : Objects.requireNonNull(playerUuids, "playerUuids cannot be null")) {
            final PlayerPresenceSnapshot snapshot = this.presenceSnapshots.get(playerUuid);
            if (snapshot != null) {
                snapshots.put(playerUuid, snapshot);
            }
        }
        return CompletableFuture.completedFuture(Map.copyOf(snapshots));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Boolean> requestPlayerTransfer(final @NotNull ProxyTransferRequest transferRequest) {
        return this.transferExecutor.requestTransfer(transferRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<ProxyActionResult> sendAction(final @NotNull ProxyActionEnvelope envelope) {
        if (envelope.protocolVersion() != this.protocolVersion()) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("protocol_mismatch", "Proxy protocol version mismatch.")
            );
        }

        final ProxyActionHandler handler = this.actionHandlers.get(actionKey(envelope.moduleId(), envelope.actionId()));
        if (handler == null) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("handler_missing", "No proxy action handler registered.")
            );
        }

        try {
            return handler.handle(envelope)
                .orTimeout(this.config.requestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(throwable ->
                    ProxyActionResult.failure("timeout", "Proxy action handling timed out or failed."))
                .thenApply(result -> result == null
                    ? ProxyActionResult.failure("handler_failed", "Proxy handler returned no result.")
                    : result
                );
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("handler_exception", "Proxy action handler threw an exception.")
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerActionHandler(
        final @NotNull String moduleId,
        final @NotNull String actionId,
        final @NotNull ProxyActionHandler handler
    ) {
        this.actionHandlers.put(actionKey(moduleId, actionId), handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterActionHandler(final @NotNull String moduleId, final @NotNull String actionId) {
        this.actionHandlers.remove(actionKey(moduleId, actionId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull PendingArrivalActionStore pendingArrivals() {
        return this.pendingArrivalActionStore;
    }

    private static @NotNull String actionKey(final @NotNull String moduleId, final @NotNull String actionId) {
        return moduleId.trim().toLowerCase(java.util.Locale.ROOT)
            + '#'
            + actionId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
