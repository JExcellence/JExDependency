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

package com.raindropcentral.rda.listeners;

import com.raindropcentral.rda.RDA;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PlayerJoinListener}.
 */
class PlayerJoinListenerTest {

    @Test
    void provisionsPlayerProfileOnJoin() {
        final RDA rda = mock(RDA.class);
        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final Player player = mock(Player.class);
        when(event.getPlayer()).thenReturn(player);

        new PlayerJoinListener(rda).onPlayerJoin(event);

        verify(rda).ensurePlayerProfile(player);
    }
}
