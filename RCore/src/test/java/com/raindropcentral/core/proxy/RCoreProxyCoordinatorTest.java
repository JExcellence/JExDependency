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

import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link RCoreProxyCoordinator}.
 */
class RCoreProxyCoordinatorTest {

    @Test
    void routesActionToRegisteredHandler() {
        final RCoreProxyCoordinator coordinator = new RCoreProxyCoordinator(ProxyHostConfig.defaults("alpha"));
        coordinator.registerActionHandler("rdr", "ping", envelope -> CompletableFuture.completedFuture(
            ProxyActionResult.success("pong", Map.of("module", envelope.moduleId()))
        ));

        final ProxyActionResult result = coordinator.sendAction(new ProxyActionEnvelope(
            UUID.randomUUID(),
            coordinator.protocolVersion(),
            "rdr",
            "ping",
            UUID.randomUUID(),
            "alpha",
            "beta",
            System.currentTimeMillis()
        )).join();

        assertTrue(result.success());
        assertEquals("pong", result.message());
        assertEquals("rdr", result.payload().get("module"));
    }

    @Test
    void expiresCorrelatedRequestWhenDeadlinePasses() {
        final RCoreProxyCoordinator coordinator = new RCoreProxyCoordinator(
            new ProxyHostConfig("raindrop:proxy", 1, 5_000L, 120_000L, "alpha", Map.of())
        );
        final UUID requestId = UUID.randomUUID();
        final CompletableFuture<ProxyActionResult> pending = coordinator.openCorrelatedRequest(requestId);
        final int expiredCount = coordinator.expireTimedOutRequests(System.currentTimeMillis() + 6_000L);

        assertEquals(1, expiredCount);
        assertTrue(pending.isDone());
        assertEquals("timeout", pending.join().statusCode());
        assertEquals(0, coordinator.pendingRequestCount());
    }

    @Test
    void delegatesPlayerTransferToConfiguredExecutor() {
        final RCoreProxyCoordinator coordinator = new RCoreProxyCoordinator(ProxyHostConfig.defaults("alpha"));
        final ProxyTransferRequest transferRequest = new ProxyTransferRequest(
            UUID.randomUUID(),
            "alpha",
            "beta",
            "token-1"
        );
        assertFalse(coordinator.requestPlayerTransfer(transferRequest).join());

        coordinator.setTransferExecutor(request -> CompletableFuture.completedFuture(true));
        assertTrue(coordinator.requestPlayerTransfer(transferRequest).join());
    }
}
