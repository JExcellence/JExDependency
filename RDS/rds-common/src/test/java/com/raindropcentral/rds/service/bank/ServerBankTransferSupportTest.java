/*
 * ServerBankTransferSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.bank;

import com.raindropcentral.rds.database.entity.Shop;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests admin-shop bank transfer support helpers.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ServerBankTransferSupportTest {

    @Test
    void returnsEmptyTransferSetForPlayerShops() {
        final Shop playerShop = new Shop(UUID.randomUUID(), null);
        playerShop.addBank("vault", 100D);

        final Map<String, Double> transferable = ServerBankTransferSupport.collectTransferableBalances(playerShop);
        assertTrue(transferable.isEmpty());
    }

    @Test
    void collectsPositiveAdminShopBankBalancesByCurrency() {
        final Shop adminShop = new Shop(UUID.randomUUID(), null);
        adminShop.setAdminShop(true);
        adminShop.addBank("vault", 200D);
        adminShop.addBank("coins", 15D);
        adminShop.addBank("coins", 5D);

        final Map<String, Double> transferable = ServerBankTransferSupport.collectTransferableBalances(adminShop);
        assertEquals(2, transferable.size());
        assertEquals(200D, transferable.get("vault"), 1.0E-9D);
        assertEquals(20D, transferable.get("coins"), 1.0E-9D);
        assertEquals(200D, adminShop.getBankAmount("vault"), 1.0E-9D);
        assertEquals(20D, adminShop.getBankAmount("coins"), 1.0E-9D);
    }
}
