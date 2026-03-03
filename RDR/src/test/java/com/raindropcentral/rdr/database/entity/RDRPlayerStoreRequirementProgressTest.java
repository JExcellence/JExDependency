/*
 * RDRPlayerStoreRequirementProgressTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.database.entity;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RDRPlayerStoreRequirementProgressTest {

    @Test
    void storesCurrencyProgressInDedicatedEntries() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        player.setStoreCurrencyProgress("1:vault_purchase", 125.5D);

        assertEquals(125.5D, player.getStoreCurrencyProgress("1:vault_purchase"));
        assertEquals(1, player.getStoreCurrencyProgress().size());
    }

    @Test
    void clearsProgressEntriesWhenRequirementPrefixIsRemoved() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        player.setStoreCurrencyProgress("2:coins_purchase", 75.0D);
        player.setStoreCurrencyProgress("3:vault_purchase", 10.0D);

        player.clearStoreRequirementProgress("2:");

        assertEquals(0.0D, player.getStoreCurrencyProgress("2:coins_purchase"));
        assertEquals(10.0D, player.getStoreCurrencyProgress("3:vault_purchase"));
        assertTrue(player.getStoreCurrencyProgress().containsKey("3:vault_purchase"));
    }
}
