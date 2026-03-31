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
 * ShopBrowserSupportTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests shared shop browser helpers used by the directory and material search views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopBrowserSupportTest {

    @Test
    void resolvesMaterialQueriesIgnoringCaseAndSeparators() {
        assertEquals(Material.DIAMOND_BLOCK, ShopBrowserSupport.resolveMaterialQuery("diamond block"));
        assertEquals(Material.OAK_LOG, ShopBrowserSupport.resolveMaterialQuery("oak-log"));
    }

    @Test
    void returnsNullForUnknownMaterialQueries() {
        assertNull(ShopBrowserSupport.resolveMaterialQuery("totally_not_a_material"));
    }

    @Test
    void formatsMaterialNamesForResultTitlesAndLore() {
        assertEquals("Diamond Block", ShopBrowserSupport.formatMaterialName(Material.DIAMOND_BLOCK));
        assertEquals("Oak Log", ShopBrowserSupport.formatMaterialName(Material.OAK_LOG));
    }
}
