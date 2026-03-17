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
 * ShopItemAvailabilityModeTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests shop-item availability mode behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopItemAvailabilityModeTest {

    @Test
    void cyclesAvailabilityModesInExpectedOrder() {
        assertEquals(ShopItem.AvailabilityMode.ROTATE, ShopItem.AvailabilityMode.ALWAYS.next());
        assertEquals(ShopItem.AvailabilityMode.NEVER, ShopItem.AvailabilityMode.ROTATE.next());
        assertEquals(ShopItem.AvailabilityMode.ALWAYS, ShopItem.AvailabilityMode.NEVER.next());
    }
}
