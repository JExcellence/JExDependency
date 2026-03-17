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
