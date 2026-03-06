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
