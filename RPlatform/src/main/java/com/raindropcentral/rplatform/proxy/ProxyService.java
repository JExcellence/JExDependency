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

/**
 * Shared proxy bridge consumed by Paper modules.
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
     * @return protocol version
     */
    int protocolVersion();

    /**
     * Returns the proxy action channel identifier.
     *
     * @return channel name
     */
    @NotNull String channelName();

    /**
     * Returns one player's presence snapshot when available.
     *
     * @param playerUuid target player UUID
     * @return async optional presence snapshot
     */
    @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(@NotNull UUID playerUuid);

    /**
     * Returns presence snapshots for one UUID collection.
     *
     * @param playerUuids target UUID collection
     * @return async map of known presence snapshots
     */
    @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(@NotNull Collection<UUID> playerUuids);

    /**
     * Requests a proxy transfer for one player.
     *
     * @param transferRequest transfer request
     * @return async transfer result
     */
    @NotNull CompletableFuture<Boolean> requestPlayerTransfer(@NotNull ProxyTransferRequest transferRequest);

    /**
     * Sends one module action envelope.
     *
     * @param envelope action envelope
     * @return async action result
     */
    @NotNull CompletableFuture<ProxyActionResult> sendAction(@NotNull ProxyActionEnvelope envelope);

    /**
     * Registers one module action handler.
     *
     * @param moduleId module identifier
     * @param actionId action identifier
     * @param handler action handler
     */
    void registerActionHandler(@NotNull String moduleId, @NotNull String actionId, @NotNull ProxyActionHandler handler);

    /**
     * Removes one module action handler.
     *
     * @param moduleId module identifier
     * @param actionId action identifier
     */
    void unregisterActionHandler(@NotNull String moduleId, @NotNull String actionId);

    /**
     * Returns the pending-arrival token store associated with this proxy bridge.
     *
     * @return pending-arrival token store
     */
    @NotNull PendingArrivalActionStore pendingArrivals();
}
