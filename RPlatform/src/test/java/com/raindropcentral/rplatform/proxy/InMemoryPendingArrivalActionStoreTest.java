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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link InMemoryPendingArrivalActionStore}.
 */
class InMemoryPendingArrivalActionStoreTest {

    @Test
    void issuesAndConsumesTokenByPlayerAndAction() {
        final InMemoryPendingArrivalActionStore store = new InMemoryPendingArrivalActionStore();
        final UUID playerUuid = UUID.randomUUID();
        final NetworkLocation destination = new NetworkLocation("hub", "world", 10.0D, 64.0D, 20.0D, 0.0F, 0.0F);
        final PendingArrivalToken token = store.issueToken(
            playerUuid,
            "rdt",
            "town_spawn",
            destination,
            Duration.ofSeconds(30L),
            Map.of("town_uuid", UUID.randomUUID().toString())
        );

        assertEquals(1, store.size());
        final Optional<PendingArrivalToken> consumed = store.consumeFirstForPlayer(playerUuid, "rdt", "town_spawn", "hub");
        assertTrue(consumed.isPresent());
        assertEquals(token.tokenId(), consumed.get().tokenId());
        assertEquals(0, store.size());
    }

    @Test
    void cleanupRemovesExpiredTokens() {
        final InMemoryPendingArrivalActionStore store = new InMemoryPendingArrivalActionStore();
        final PendingArrivalToken token = store.issueToken(
            UUID.randomUUID(),
            "rdt",
            "town_spawn",
            new NetworkLocation("spawn", "world", 0.0D, 80.0D, 0.0D, 0.0F, 0.0F),
            Duration.ofSeconds(1L),
            Map.of()
        );

        final int removed = store.cleanupExpired(token.expiresAtEpochMilli());
        assertEquals(1, removed);
        assertFalse(store.consumeToken(token.tokenId()).isPresent());
    }
}
