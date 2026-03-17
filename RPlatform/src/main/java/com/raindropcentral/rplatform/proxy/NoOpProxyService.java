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

package com.raindropcentral.rplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local fallback {@link ProxyService} implementation for non-proxy installs.
 *
 * <p>The service supports local in-process action handlers and pending-arrival tokens while
 * reporting transfer operations as unavailable.</p>
 */
public final class NoOpProxyService implements ProxyService {

    private static final String DEFAULT_CHANNEL_NAME = "raindrop:proxy";
    private static final int DEFAULT_PROTOCOL_VERSION = 1;

    private final String channelName;
    private final int protocolVersion;
    private final boolean available;
    private final PendingArrivalActionStore pendingArrivalActionStore;
    private final ConcurrentHashMap<String, ProxyActionHandler> actionHandlers;

    /**
     * Creates the default no-op proxy service.
     *
     * @return default no-op proxy service
     */
    public static @NotNull NoOpProxyService createDefault() {
        return new NoOpProxyService(DEFAULT_CHANNEL_NAME, DEFAULT_PROTOCOL_VERSION, false);
    }

    /**
     * Creates a no-op proxy service.
     *
     * @param channelName proxy channel name
     * @param protocolVersion protocol version
     * @param available availability flag exposed by {@link #isAvailable()}
     */
    public NoOpProxyService(
        final @NotNull String channelName,
        final int protocolVersion,
        final boolean available
    ) {
        this.channelName = channelName == null || channelName.isBlank() ? DEFAULT_CHANNEL_NAME : channelName.trim();
        this.protocolVersion = Math.max(1, protocolVersion);
        this.available = available;
        this.pendingArrivalActionStore = new InMemoryPendingArrivalActionStore();
        this.actionHandlers = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return this.available;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int protocolVersion() {
        return this.protocolVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String channelName() {
        return this.channelName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(final @NotNull UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(
        final @NotNull Collection<UUID> playerUuids
    ) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Boolean> requestPlayerTransfer(final @NotNull ProxyTransferRequest transferRequest) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<ProxyActionResult> sendAction(final @NotNull ProxyActionEnvelope envelope) {
        if (envelope.protocolVersion() != this.protocolVersion) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("protocol_mismatch", "Proxy protocol version mismatch.")
            );
        }

        final ProxyActionHandler handler = this.actionHandlers.get(actionKey(envelope.moduleId(), envelope.actionId()));
        if (handler == null) {
            return CompletableFuture.completedFuture(ProxyActionResult.failure(
                this.available ? "handler_missing" : "proxy_unavailable",
                this.available ? "No handler registered for proxy action." : "Proxy service is unavailable."
            ));
        }

        try {
            return handler.handle(envelope);
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("handler_exception", "Proxy action handler failed: " + exception.getMessage())
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
