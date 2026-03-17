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

/*
 * PlayerPerkTriggerAccountingTest.java
 *
 * @author RaindropCentral
 * @version 6.0.0
 */

package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that player perk trigger counts are tracked independently from enabled session state.
 *
 * @author RaindropCentral
 * @since 6.0.0
 * @version 6.0.0
 */
class PlayerPerkTriggerAccountingTest {

    @Test
    void markActivatedDoesNotIncrementTriggerCount() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.markActivated();

        assertTrue(playerPerk.isActive());
        assertEquals(0, playerPerk.getActivationCount());
    }

    @Test
    void recordTriggerIncrementsTriggerCountWithoutChangingActiveState() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.recordTrigger();
        playerPerk.recordTrigger();

        assertFalse(playerPerk.isActive());
        assertEquals(2, playerPerk.getActivationCount());
    }

    @Test
    void markDeactivatedKeepsRecordedTriggerCount() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.markActivated();
        playerPerk.recordTrigger();
        playerPerk.markDeactivated();

        assertFalse(playerPerk.isActive());
        assertEquals(1, playerPerk.getActivationCount());
    }
}
