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

package com.raindropcentral.rplatform.requirement.plugin;

import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link ProtectionTownPluginIntegrationBridge}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class ProtectionTownPluginIntegrationBridgeTest {

    @Test
    void getValueDelegatesTownLevel() {
        final PluginIntegrationBridge bridge = new ProtectionTownPluginIntegrationBridge(
            "towny",
            new FakeProtectionBridge(true, 4.0D)
        );

        assertEquals(4.0D, bridge.getValue(mock(Player.class), "town_level"));
        assertEquals(4.0D, bridge.getValue(mock(Player.class), "Town-Level"));
    }

    @Test
    void getValueReturnsZeroForUnknownKeys() {
        final PluginIntegrationBridge bridge = new ProtectionTownPluginIntegrationBridge(
            "husktowns",
            new FakeProtectionBridge(true, 2.0D)
        );

        assertEquals(0.0D, bridge.getValue(mock(Player.class), "chunk_level"));
    }

    @Test
    void isAvailableDelegatesToProtectionBridge() {
        final PluginIntegrationBridge availableBridge = new ProtectionTownPluginIntegrationBridge(
            "towny",
            new FakeProtectionBridge(true, 1.0D)
        );
        final PluginIntegrationBridge unavailableBridge = new ProtectionTownPluginIntegrationBridge(
            "husktowns",
            new FakeProtectionBridge(false, 1.0D)
        );

        assertTrue(availableBridge.isAvailable());
        assertFalse(unavailableBridge.isAvailable());
    }

    private static final class FakeProtectionBridge implements RProtectionBridge {

        private final boolean available;
        private final double townLevel;

        private FakeProtectionBridge(final boolean available, final double townLevel) {
            this.available = available;
            this.townLevel = townLevel;
        }

        @Override
        public String getPluginName() {
            return "TestTownPlugin";
        }

        @Override
        public boolean isAvailable() {
            return this.available;
        }

        @Override
        public boolean isPlayerInTown(final Player player) {
            return this.townLevel > 0.0D;
        }

        @Override
        public boolean isPlayerStandingInOwnTown(final Player player) {
            return false;
        }

        @Override
        public boolean isPlayerTownMayor(final Player player) {
            return false;
        }

        @Override
        public String getPlayerTownIdentifier(final Player player) {
            return null;
        }

        @Override
        public String getPlayerTownDisplayName(final Player player) {
            return null;
        }

        @Override
        public double getPlayerTownLevel(final Player player) {
            return this.townLevel;
        }

        @Override
        public boolean depositToTownBank(final Player player, final double amount) {
            return false;
        }

        @Override
        public boolean withdrawFromTownBank(final Player player, final double amount) {
            return false;
        }
    }
}
