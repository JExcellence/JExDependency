package com.raindropcentral.rds.service.shop;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;

/**
 * Utility methods for counting a player's active non-admin shops.
 *
 * <p>These counts are used for placement and admin-toggle limits. Purchased shop-block progress is
 * tracked separately on the player profile.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class ShopOwnershipSupport {

    private ShopOwnershipSupport() {
    }

    /**
     * Counts the owner's active non-admin shops.
     *
     * @param plugin plugin instance
     * @param ownerId owner UUID to count
     * @return number of active non-admin shops owned by the player
     */
    public static int countOwnedPlayerShops(
        final @NotNull RDS plugin,
        final @NotNull UUID ownerId
    ) {
        int ownedShops = 0;
        for (final Shop shop : plugin.getShopRepository().findAllShops()) {
            if (shop == null || shop.isAdminShop() || !shop.isOwner(ownerId)) {
                continue;
            }
            ownedShops++;
        }
        return ownedShops;
    }

    /**
     * Counts all active admin shops on the server.
     *
     * @param plugin plugin instance
     * @return number of active admin shops
     */
    public static int countAdminShops(final @NotNull RDS plugin) {
        int adminShops = 0;
        for (final Shop shop : plugin.getShopRepository().findAllShops()) {
            if (shop != null && shop.isAdminShop()) {
                adminShops++;
            }
        }
        return adminShops;
    }
}
