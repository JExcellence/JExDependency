package com.raindropcentral.rds.database.entity;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests r d s player store requirement progress behavior.
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