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

import org.jetbrains.annotations.NotNull;

/**
 * Action modes used by the shop-admin control browser.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
enum ShopAdminShopControlMode {
    OPEN_AS_OWNER,
    FORCE_CLOSE_SHOP;

    static @NotNull ShopAdminShopControlMode fromRaw(
            final @NotNull String rawValue
    ) {
        for (final ShopAdminShopControlMode value : values()) {
            if (value.name().equalsIgnoreCase(rawValue)) {
                return value;
            }
        }
        return OPEN_AS_OWNER;
    }
}
