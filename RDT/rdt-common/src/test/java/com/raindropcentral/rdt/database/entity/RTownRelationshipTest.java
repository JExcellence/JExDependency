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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownRelationshipState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unordered town-pair normalization and state helpers on {@link RTownRelationship}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RTownRelationshipTest {

    @Test
    void pairOrderingIsStableRegardlessOfConstructionOrder() {
        final UUID firstTownUuid = UUID.fromString("00000000-0000-0000-0000-000000000010");
        final UUID secondTownUuid = UUID.fromString("00000000-0000-0000-0000-000000000020");

        final RTownRelationship forward = new RTownRelationship(firstTownUuid, secondTownUuid);
        final RTownRelationship reverse = new RTownRelationship(secondTownUuid, firstTownUuid);

        assertEquals(forward.getPrimaryTownUuid(), reverse.getPrimaryTownUuid());
        assertEquals(forward.getSecondaryTownUuid(), reverse.getSecondaryTownUuid());
        assertEquals(forward.getPairKey(), reverse.getPairKey());
        assertTrue(forward.matchesTownPair(firstTownUuid, secondTownUuid));
        assertTrue(forward.matchesTownPair(secondTownUuid, firstTownUuid));
    }

    @Test
    void relationshipsDefaultToNeutralAndCanResolveTheOppositeTown() {
        final UUID firstTownUuid = UUID.fromString("00000000-0000-0000-0000-000000000011");
        final UUID secondTownUuid = UUID.fromString("00000000-0000-0000-0000-000000000022");
        final RTownRelationship relationship = new RTownRelationship(firstTownUuid, secondTownUuid);

        assertEquals(TownRelationshipState.NEUTRAL, relationship.getConfirmedState());
        assertEquals(secondTownUuid, relationship.getOtherTownUuid(firstTownUuid));
        assertEquals(firstTownUuid, relationship.getOtherTownUuid(secondTownUuid));
        assertNull(relationship.getOtherTownUuid(UUID.randomUUID()));
    }

    @Test
    void constructorRejectsRelationshipsBetweenTheSameTown() {
        final UUID townUuid = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new RTownRelationship(townUuid, townUuid));
    }
}
