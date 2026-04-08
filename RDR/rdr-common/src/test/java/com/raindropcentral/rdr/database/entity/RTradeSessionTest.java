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

package com.raindropcentral.rdr.database.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests participant server snapshot metadata on {@link RTradeSession}.
 */
class RTradeSessionTest {

    @Test
    void initializesServerSnapshotsWithOfflineAndUnknownDefaults() {
        final RTradeSession session = new RTradeSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now().plusMinutes(1)
        );

        assertEquals("offline", session.getInitiatorLastKnownServerId());
        assertEquals("offline", session.getPartnerLastKnownServerId());
        assertEquals("unknown", session.getOriginServerId());
    }

    @Test
    void updatesParticipantServerSnapshotsAndPreservesOriginWhenBlank() {
        final UUID initiatorUuid = UUID.randomUUID();
        final UUID partnerUuid = UUID.randomUUID();
        final RTradeSession session = new RTradeSession(
            initiatorUuid,
            partnerUuid,
            LocalDateTime.now().plusMinutes(1)
        );

        session.setOriginServerId("alpha");
        session.refreshParticipantServerSnapshots("hub-1", "trade-2", " ");

        assertEquals("hub-1", session.getLastKnownServerIdForParticipant(initiatorUuid));
        assertEquals("trade-2", session.getLastKnownServerIdForParticipant(partnerUuid));
        assertEquals("alpha", session.getOriginServerId());
    }

    @Test
    void resolvesParticipantServerIdAndRejectsUnknownParticipant() {
        final UUID initiatorUuid = UUID.randomUUID();
        final UUID partnerUuid = UUID.randomUUID();
        final RTradeSession session = new RTradeSession(
            initiatorUuid,
            partnerUuid,
            LocalDateTime.now().plusMinutes(1)
        );

        session.setLastKnownServerIdForParticipant(initiatorUuid, "spawn-a");
        session.setLastKnownServerIdForParticipant(partnerUuid, "spawn-b");

        assertEquals("spawn-a", session.getLastKnownServerIdForParticipant(initiatorUuid));
        assertEquals("spawn-b", session.getLastKnownServerIdForParticipant(partnerUuid));
        assertThrows(
            IllegalArgumentException.class,
            () -> session.getLastKnownServerIdForParticipant(UUID.randomUUID())
        );
    }
}
