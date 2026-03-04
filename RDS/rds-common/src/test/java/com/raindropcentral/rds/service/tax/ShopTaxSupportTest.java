/*
 * ShopTaxSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.database.entity.Shop;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests tax support shop taxability rules.
 */
class ShopTaxSupportTest {

    @Test
    void excludesAdminShopsFromTaxableSet() {
        final UUID ownerId = UUID.randomUUID();
        final Shop adminShop = this.createShop(ownerId, true);
        final Shop playerShop = this.createShop(ownerId, false);

        assertFalse(ShopTaxSupport.isTaxableShop(adminShop));
        assertTrue(ShopTaxSupport.isTaxableShop(playerShop));
    }

    @Test
    void requiresMatchingOwnerForTaxableOwnerCheck() {
        final UUID ownerId = UUID.randomUUID();
        final UUID otherOwnerId = UUID.randomUUID();
        final Shop playerShop = this.createShop(ownerId, false);
        final Shop adminShop = this.createShop(ownerId, true);

        assertTrue(ShopTaxSupport.isTaxableShopForOwner(playerShop, ownerId));
        assertFalse(ShopTaxSupport.isTaxableShopForOwner(playerShop, otherOwnerId));
        assertFalse(ShopTaxSupport.isTaxableShopForOwner(adminShop, ownerId));
    }

    private Shop createShop(
            final UUID ownerId,
            final boolean adminShop
    ) {
        final Shop shop = new Shop(ownerId, null);
        shop.setAdminShop(adminShop);
        return shop;
    }
}
