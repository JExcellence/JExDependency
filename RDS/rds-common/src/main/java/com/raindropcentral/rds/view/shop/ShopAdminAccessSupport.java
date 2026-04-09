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

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import me.devnatan.inventoryframework.context.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared admin-access helpers for shop views.
 *
 * <p>This utility centralizes owner-override checks used by admin shop tooling so all views
 * interpret the same context flag consistently.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ShopAdminAccessSupport {

    static final String ADMIN_OWNER_OVERRIDE_KEY = "adminOwnerOverride";
    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private ShopAdminAccessSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    static boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        Objects.requireNonNull(player, "player");
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    static boolean hasOwnerOverride(
            final @NotNull Context context
    ) {
        if (!hasAdminAccess(context.getPlayer())) {
            return false;
        }

        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return false;
        }

        final Object overrideValue = data.get(ADMIN_OWNER_OVERRIDE_KEY);
        return overrideValue instanceof Boolean enabled && enabled;
    }

    static boolean canActAsOwner(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        final UUID playerId = context.getPlayer().getUniqueId();
        return shop.isOwner(playerId) || hasOwnerOverride(context);
    }

    static boolean canActAsOwner(
            final @NotNull Context context,
            final @NotNull Shop shop,
            final @NotNull RDS plugin
    ) {
        if (hasOwnerOverride(context)) {
            return true;
        }
        if (shop.isPlayerShop()) {
            return shop.isOwner(context.getPlayer().getUniqueId());
        }

        return plugin.getTownShopService() != null
                && plugin.getTownShopService().canCloseShop(context.getPlayer(), shop);
    }

    static boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        final UUID playerId = context.getPlayer().getUniqueId();
        return shop.canManage(playerId) || hasOwnerOverride(context);
    }

    static boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop,
            final @NotNull RDS plugin
    ) {
        if (hasOwnerOverride(context)) {
            return true;
        }
        return plugin.getTownShopService() == null
                ? shop.canManage(context.getPlayer().getUniqueId())
                : plugin.getTownShopService().canManageShop(context.getPlayer(), shop);
    }

    static boolean canSupply(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        final UUID playerId = context.getPlayer().getUniqueId();
        return shop.canSupply(playerId) || hasOwnerOverride(context);
    }

    static boolean canSupply(
            final @NotNull Context context,
            final @NotNull Shop shop,
            final @NotNull RDS plugin
    ) {
        if (hasOwnerOverride(context)) {
            return true;
        }
        return plugin.getTownShopService() == null
                ? shop.canSupply(context.getPlayer().getUniqueId())
                : plugin.getTownShopService().canSupplyShop(context.getPlayer(), shop);
    }

    static boolean canAccessOverview(
            final @NotNull Player player,
            final @NotNull Shop shop,
            final @NotNull RDS plugin
    ) {
        return plugin.getTownShopService() == null
                ? shop.canAccessOverview(player.getUniqueId())
                : plugin.getTownShopService().canAccessOverview(player, shop);
    }

    static @NotNull String resolveOwnerName(
            final @NotNull Shop shop
    ) {
        if (shop.isTownShop()) {
            final String townDisplayName = shop.getTownDisplayName();
            if (townDisplayName != null && !townDisplayName.isBlank()) {
                return townDisplayName;
            }

            final String townIdentifier = shop.getTownIdentifier();
            return townIdentifier == null || townIdentifier.isBlank() ? "Unknown Town" : townIdentifier;
        }

        final UUID ownerId = shop.getOwner();
        return ownerId == null ? "Unknown" : ownerId.toString();
    }

    static @NotNull String resolveOwnerName(
            final @NotNull RDS plugin,
            final @NotNull Shop shop
    ) {
        if (plugin.getTownShopService() != null) {
            return plugin.getTownShopService().resolveOwnerName(shop);
        }

        return resolveOwnerName(shop);
    }
}
