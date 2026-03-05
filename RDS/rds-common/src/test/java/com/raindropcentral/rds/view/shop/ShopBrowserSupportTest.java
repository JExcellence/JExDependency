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
