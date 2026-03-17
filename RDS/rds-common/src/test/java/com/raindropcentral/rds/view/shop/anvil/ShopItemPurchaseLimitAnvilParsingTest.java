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

package com.raindropcentral.rds.view.shop.anvil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShopItemPurchaseLimitAnvilParsingTest {

    @Test
    void parsesAmountLimitAndDisableValue() {
        assertEquals(-1, ShopItemPurchaseLimitAnvilView.parseLimitAmount("-1"));
        assertEquals(8, ShopItemPurchaseLimitAnvilView.parseLimitAmount("8"));
        assertEquals(4, ShopItemPurchaseLimitAnvilView.parseLimitAmount(" 4 "));
    }

    @Test
    void rejectsInvalidAmountLimitInput() {
        assertNull(ShopItemPurchaseLimitAnvilView.parseLimitAmount("0"));
        assertNull(ShopItemPurchaseLimitAnvilView.parseLimitAmount("-2"));
        assertNull(ShopItemPurchaseLimitAnvilView.parseLimitAmount("abc"));
        assertNull(ShopItemPurchaseLimitAnvilView.parseLimitAmount("3:10"));
    }

    @Test
    void parsesWindowMinutesInput() {
        assertEquals(30, ShopItemPurchaseLimitMinutesAnvilView.parseLimitWindowMinutes("30"));
        assertEquals(5, ShopItemPurchaseLimitMinutesAnvilView.parseLimitWindowMinutes(" 5 "));
    }

    @Test
    void rejectsInvalidWindowMinutesInput() {
        assertNull(ShopItemPurchaseLimitMinutesAnvilView.parseLimitWindowMinutes("-1"));
        assertNull(ShopItemPurchaseLimitMinutesAnvilView.parseLimitWindowMinutes("0"));
        assertNull(ShopItemPurchaseLimitMinutesAnvilView.parseLimitWindowMinutes("abc"));
    }
}
