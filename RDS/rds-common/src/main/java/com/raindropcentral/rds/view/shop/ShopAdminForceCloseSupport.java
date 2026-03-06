package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import me.devnatan.inventoryframework.ViewFrame;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.context.IFRenderContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Helpers for force-closing RDS views and shops from admin tooling.
 *
 * <p>These operations bypass owner checks and are intended for privileged moderation tasks.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ShopAdminForceCloseSupport {

    private ShopAdminForceCloseSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    static int closeAllRdsViews(
            final @NotNull RDS plugin
    ) {
        final ViewFrame viewFrame = plugin.getViewFrame();
        if (viewFrame == null) {
            return 0;
        }

        int closedViews = 0;
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final Viewer viewer = viewFrame.getViewer(onlinePlayer);
            if (viewer == null || !isRdsShopView(viewer.getCurrentContext())) {
                continue;
            }
            viewer.close();
            closedViews++;
        }
        return closedViews;
    }

    static int closeShopViews(
            final @NotNull RDS plugin,
            final @NotNull Shop shop
    ) {
        final ViewFrame viewFrame = plugin.getViewFrame();
        if (viewFrame == null) {
            return 0;
        }

        int closedViews = 0;
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final Viewer viewer = viewFrame.getViewer(onlinePlayer);
            if (viewer == null || !isViewerOnShop(viewer.getCurrentContext(), shop)) {
                continue;
            }

            viewer.close();
            closedViews++;
        }

        return closedViews;
    }

    static int forceCloseShop(
            final @NotNull RDS plugin,
            final @NotNull Shop shop
    ) {
        final int closedViews = closeShopViews(plugin, shop);

        plugin.getShopRepository().deleteEntity(shop);
        clearShopBlock(shop.getShopLocation());
        clearShopBlock(shop.getSecondaryShopLocation());
        return closedViews;
    }

    private static boolean isViewerOnShop(
            final IFRenderContext context,
            final @NotNull Shop shop
    ) {
        if (!isRdsShopView(context)) {
            return false;
        }

        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return false;
        }

        final Object locationObject = data.get("shopLocation");
        if (!(locationObject instanceof Location viewedLocation)) {
            return false;
        }

        final Location primary = shop.getShopLocation();
        final Location secondary = shop.getSecondaryShopLocation();
        return Objects.equals(primary, viewedLocation) || Objects.equals(secondary, viewedLocation);
    }

    private static boolean isRdsShopView(
            final IFRenderContext context
    ) {
        if (context == null || context.getRoot() == null) {
            return false;
        }

        return context.getRoot().getClass().getPackageName()
                .startsWith("com.raindropcentral.rds.view.shop");
    }

    private static void clearShopBlock(
            final Location location
    ) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        location.getBlock().setType(Material.AIR);
    }
}
