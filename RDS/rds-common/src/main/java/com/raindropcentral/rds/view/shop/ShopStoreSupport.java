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

package com.raindropcentral.rds.view.shop;

/**
 * Provides support utilities for shop store.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
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
