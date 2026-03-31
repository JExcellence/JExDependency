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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link PendingArrivalActionStore} implementation.
 *
 * <p>This implementation is process-local and best suited for no-proxy fallback or unit tests.</p>
 */
public final class InMemoryPendingArrivalActionStore implements PendingArrivalActionStore {

    private static final long MIN_TTL_MILLIS = 1_000L;

    private final ConcurrentHashMap<String, PendingArrivalToken> tokensById;

    /**
     * Creates an empty in-memory pending arrival store.
     */
    public InMemoryPendingArrivalActionStore() {
        this.tokensById = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull PendingArrivalToken issueToken(
        final @NotNull UUID playerUuid,
        final @NotNull String moduleId,
        final @NotNull String actionId,
        final @NotNull NetworkLocation destination,
        final @NotNull Duration ttl,
        final @NotNull Map<String, String> payload
    ) {
        final long now = System.currentTimeMillis();
        final long ttlMillis = Math.max(MIN_TTL_MILLIS, ttl.toMillis());
        final PendingArrivalToken token = new PendingArrivalToken(
            UUID.randomUUID().toString(),
            playerUuid,
            moduleId,
            actionId,
            destination,
            payload,
            now,
            now + ttlMillis
        );
        this.tokensById.put(token.tokenId(), token);
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<PendingArrivalToken> consumeToken(final @NotNull String tokenId) {
        return Optional.ofNullable(this.tokensById.remove(tokenId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<PendingArrivalToken> consumeFirstForPlayer(
        final @NotNull UUID playerUuid,
        final @NotNull String moduleId,
        final @NotNull String actionId,
        final @NotNull String destinationServerId
    ) {
        final ArrayList<PendingArrivalToken> candidates = new ArrayList<>(this.tokensById.values());
        candidates.sort(Comparator.comparingLong(PendingArrivalToken::issuedAtEpochMilli));

        for (final PendingArrivalToken token : candidates) {
            if (!token.matches(playerUuid, moduleId, actionId, destinationServerId)) {
                continue;
            }
            if (this.tokensById.remove(token.tokenId(), token)) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cleanupExpired(final long nowEpochMilli) {
        int removed = 0;
        for (final Map.Entry<String, PendingArrivalToken> entry : this.tokensById.entrySet()) {
            final PendingArrivalToken token = entry.getValue();
            if (!token.isExpired(nowEpochMilli)) {
                continue;
            }
            if (this.tokensById.remove(entry.getKey(), token)) {
                removed++;
            }
        }
        return removed;
    }

    int size() {
        return this.tokensById.size();
    }
}
