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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Store for pending arrival tokens used by cross-server action handoff.
 */
public interface PendingArrivalActionStore {

    /**
     * Issues a new pending arrival token.
     *
     * @param playerUuid player UUID targeted by this token
     * @param moduleId owning module identifier
     * @param actionId destination action identifier
     * @param destination destination network location
     * @param ttl token lifetime
     * @param payload optional destination payload
     * @return issued pending arrival token
     */
    @NotNull PendingArrivalToken issueToken(
        @NotNull UUID playerUuid,
        @NotNull String moduleId,
        @NotNull String actionId,
        @NotNull NetworkLocation destination,
        @NotNull Duration ttl,
        @NotNull Map<String, String> payload
    );

    /**
     * Consumes one token by token identifier.
     *
     * @param tokenId token identifier
     * @return consumed token when present
     */
    @NotNull Optional<PendingArrivalToken> consumeToken(@NotNull String tokenId);

    /**
     * Consumes the first matching token for one player/module/action on one destination server.
     *
     * @param playerUuid player UUID
     * @param moduleId module identifier
     * @param actionId action identifier
     * @param destinationServerId destination server route identifier
     * @return consumed matching token when present
     */
    @NotNull Optional<PendingArrivalToken> consumeFirstForPlayer(
        @NotNull UUID playerUuid,
        @NotNull String moduleId,
        @NotNull String actionId,
        @NotNull String destinationServerId
    );

    /**
     * Removes expired tokens and returns the number removed.
     *
     * @param nowEpochMilli comparison timestamp in epoch milliseconds
     * @return count of removed tokens
     */
    int cleanupExpired(long nowEpochMilli);
}
