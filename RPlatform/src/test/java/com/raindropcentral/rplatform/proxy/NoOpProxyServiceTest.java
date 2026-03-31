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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link NoOpProxyService}.
 */
class NoOpProxyServiceTest {

    @Test
    void returnsUnavailableWhenNoHandlerExists() {
        final NoOpProxyService service = NoOpProxyService.createDefault();
        final ProxyActionEnvelope envelope = new ProxyActionEnvelope(
            UUID.randomUUID(),
            service.protocolVersion(),
            "rdt",
            "town_spawn",
            UUID.randomUUID(),
            "lobby",
            "spawn",
            System.currentTimeMillis()
        );

        final ProxyActionResult result = service.sendAction(envelope).join();
        assertFalse(result.success());
        assertEquals("proxy_unavailable", result.statusCode());
    }

    @Test
    void executesLocalHandlerWhenRegistered() {
        final NoOpProxyService service = new NoOpProxyService("raindrop:proxy", 2, true);
        service.registerActionHandler("rdr", "ping", envelope -> CompletableFuture.completedFuture(
            ProxyActionResult.success("pong", Map.of("source", envelope.sourceServerId()))
        ));

        final ProxyActionEnvelope envelope = new ProxyActionEnvelope(
            UUID.randomUUID(),
            2,
            "rdr",
            "ping",
            UUID.randomUUID(),
            "alpha",
            "beta",
            System.currentTimeMillis()
        );
        final ProxyActionResult result = service.sendAction(envelope).join();

        assertTrue(result.success());
        assertEquals("ok", result.statusCode());
        assertEquals("alpha", result.payload().get("source"));
    }

    @Test
    void rejectsTransferRequestsAndSupportsPendingTokens() {
        final NoOpProxyService service = NoOpProxyService.createDefault();
        final ProxyTransferRequest transferRequest = new ProxyTransferRequest(
            UUID.randomUUID(),
            "alpha",
            "beta",
            "token-1"
        );
        assertFalse(service.requestPlayerTransfer(transferRequest).join());

        final PendingArrivalToken token = service.pendingArrivals().issueToken(
            UUID.randomUUID(),
            "rdt",
            "town_spawn",
            new NetworkLocation("beta", "world", 1.0D, 70.0D, 1.0D, 0.0F, 0.0F),
            Duration.ofSeconds(10L),
            Map.of()
        );
        assertTrue(service.pendingArrivals().consumeToken(token.tokenId()).isPresent());
    }
}
