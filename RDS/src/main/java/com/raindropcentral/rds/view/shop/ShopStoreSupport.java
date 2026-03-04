/*
 * ShopStoreSupport.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

/**
 * Provides support utilities for shop store.
 */
final class ShopStoreSupport {

    private ShopStoreSupport() {}

    static boolean hasReachedShopLimit(
        final int ownedShops,
        final int maxShops
    ) {
        return maxShops > 0 && ownedShops >= maxShops;
    }

    static int getNextPurchaseNumber(final int ownedShops) {
        return Math.max(ownedShops, 0) + 1;
    }
}