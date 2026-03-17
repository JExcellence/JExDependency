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
 * RDQPlayerScoreboardPreferenceTest.java
 *
 * @author RaindropCentral
 * @version 6.0.0
 */

package com.raindropcentral.rdq.database.entity.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies persistence-facing scoreboard preference state on {@link RDQPlayer}.
 *
 * @author RaindropCentral
 * @since 6.0.0
 * @version 6.0.0
 */
class RDQPlayerScoreboardPreferenceTest {

    @Test
    void perkSidebarScoreboardDefaultsToDisabled() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "TestPlayer");

        assertFalse(player.isPerkSidebarScoreboardEnabled());
    }

    @Test
    void perkSidebarScoreboardPreferenceCanBeToggled() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "TestPlayer");

        player.setPerkSidebarScoreboardEnabled(true);
        assertTrue(player.isPerkSidebarScoreboardEnabled());

        player.setPerkSidebarScoreboardEnabled(false);
        assertFalse(player.isPerkSidebarScoreboardEnabled());
    }
}
