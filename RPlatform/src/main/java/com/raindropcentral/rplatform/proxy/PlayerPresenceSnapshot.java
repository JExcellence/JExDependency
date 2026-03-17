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

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable cross-server presence snapshot for one player.
 *
 * @param playerUuid player UUID
 * @param serverId current server route identifier
 * @param online whether the player is currently online
 * @param capturedAtEpochMilli snapshot timestamp in epoch milliseconds
 */
public record PlayerPresenceSnapshot(
    @NotNull UUID playerUuid,
    @NotNull String serverId,
    boolean online,
    long capturedAtEpochMilli
) {

    /**
     * Creates a normalized presence snapshot.
     *
     * @param playerUuid player UUID
     * @param serverId current server route identifier
     * @param online whether the player is currently online
     * @param capturedAtEpochMilli snapshot timestamp in epoch milliseconds
     */
    public PlayerPresenceSnapshot {
        playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        serverId = normalizeServerId(serverId);
        capturedAtEpochMilli = Math.max(0L, capturedAtEpochMilli);
    }

    /**
     * Creates an online presence snapshot.
     *
     * @param playerUuid player UUID
     * @param serverId server route identifier
     * @param capturedAtEpochMilli snapshot timestamp
     * @return online snapshot
     */
    public static @NotNull PlayerPresenceSnapshot online(
        final @NotNull UUID playerUuid,
        final @NotNull String serverId,
        final long capturedAtEpochMilli
    ) {
        return new PlayerPresenceSnapshot(playerUuid, serverId, true, capturedAtEpochMilli);
    }

    /**
     * Creates an offline presence snapshot.
     *
     * @param playerUuid player UUID
     * @param capturedAtEpochMilli snapshot timestamp
     * @return offline snapshot
     */
    public static @NotNull PlayerPresenceSnapshot offline(
        final @NotNull UUID playerUuid,
        final long capturedAtEpochMilli
    ) {
        return new PlayerPresenceSnapshot(playerUuid, "offline", false, capturedAtEpochMilli);
    }

    private static @NotNull String normalizeServerId(final @NotNull String serverId) {
        final String normalized = Objects.requireNonNull(serverId, "serverId cannot be null").trim();
        if (normalized.isEmpty()) {
            return "offline";
        }
        return normalized;
    }
}
