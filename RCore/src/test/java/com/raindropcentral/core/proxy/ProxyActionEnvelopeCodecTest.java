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
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ProxyActionEnvelopeCodec}.
 */
class ProxyActionEnvelopeCodecTest {

    @Test
    void encodesAndDecodesEnvelopeRoundTrip() {
        final ProxyActionEnvelopeCodec codec = new ProxyActionEnvelopeCodec();
        final ProxyActionEnvelope envelope = new ProxyActionEnvelope(
            UUID.randomUUID(),
            3,
            "rdt",
            "town_spawn",
            UUID.randomUUID(),
            "alpha",
            "beta",
            "token-123",
            Map.of("town_uuid", UUID.randomUUID().toString()),
            System.currentTimeMillis()
        );

        final byte[] encoded = codec.encode(envelope);
        final ProxyActionEnvelope decoded = codec.decode(encoded);

        assertEquals(envelope.requestId(), decoded.requestId());
        assertEquals(envelope.protocolVersion(), decoded.protocolVersion());
        assertEquals(envelope.moduleId(), decoded.moduleId());
        assertEquals(envelope.actionId(), decoded.actionId());
        assertEquals(envelope.playerUuid(), decoded.playerUuid());
        assertEquals(envelope.sourceServerId(), decoded.sourceServerId());
        assertEquals(envelope.targetServerId(), decoded.targetServerId());
        assertEquals(envelope.actionToken(), decoded.actionToken());
        assertEquals(envelope.payload(), decoded.payload());
    }
}
