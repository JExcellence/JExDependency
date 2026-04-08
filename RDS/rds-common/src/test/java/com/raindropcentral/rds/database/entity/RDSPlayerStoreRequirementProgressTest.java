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

package com.raindropcentral.rds.database.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests r d s player store requirement progress behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RDSPlayerStoreRequirementProgressTest {

    @Test
    void storesCurrencyProgressInDedicatedEntries() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());

        player.setStoreCurrencyProgress("1:vault_purchase", 125.5D);

        assertEquals(125.5D, player.getStoreCurrencyProgress("1:vault_purchase"));
        assertEquals(1, player.getStoreCurrencyProgress().size());
    }

    @Test
    void clearsProgressEntriesWhenRequirementPrefixIsRemoved() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());

        player.setStoreCurrencyProgress("2:coins_purchase", 75.0D);
        player.setStoreCurrencyProgress("3:vault_purchase", 10.0D);

        player.clearStoreRequirementProgress("2:");

        assertEquals(0.0D, player.getStoreCurrencyProgress("2:coins_purchase"));
        assertEquals(10.0D, player.getStoreCurrencyProgress("3:vault_purchase"));
        assertTrue(player.getStoreCurrencyProgress().containsKey("3:vault_purchase"));
    }

    @Test
    void storesNormalizedSidebarScoreboardType() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());

        assertFalse(player.hasShopSidebarScoreboard());

        player.setShopSidebarScoreboardType("  STOCK  ");

        assertTrue(player.hasShopSidebarScoreboard());
        assertEquals("stock", player.getShopSidebarScoreboardType().orElseThrow());

        player.clearShopSidebarScoreboardType();

        assertFalse(player.hasShopSidebarScoreboard());
    }
}
