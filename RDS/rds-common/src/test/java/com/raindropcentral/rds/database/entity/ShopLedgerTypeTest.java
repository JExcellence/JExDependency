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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ShopLedgerType} enum stability.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopLedgerTypeTest {

    @Test
    void exposesExpectedLedgerTypesInStableOrder() {
        assertArrayEquals(
            new ShopLedgerType[]{
                ShopLedgerType.PURCHASE,
                ShopLedgerType.TAXATION
            },
            ShopLedgerType.values()
        );
    }

    @Test
    void resolvesEnumValueByCanonicalName() {
        assertEquals(ShopLedgerType.TAXATION, ShopLedgerType.valueOf("TAXATION"));
    }
}
