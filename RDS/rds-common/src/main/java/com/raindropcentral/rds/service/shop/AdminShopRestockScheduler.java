package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import org.jetbrains.annotations.NotNull;

/**
 * Schedules admin shop restock operations.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class AdminShopRestockScheduler {

    private final RDS plugin;
    private boolean running = false;

    /**
     * Creates a new admin shop restock scheduler.
     *
     * @param plugin plugin instance
     */
    public AdminShopRestockScheduler(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts admin shop restock scheduler processing.
     */
    public void start() {
        if (this.running) {
            return;
        }

        final long periodTicks = this.plugin.getDefaultConfig().getAdminShops().getRestockCheckPeriodTicks();
        this.plugin.getScheduler().runRepeating(this::restockAdminShops, periodTicks, periodTicks);
        this.running = true;
    }

    public boolean restockShop(
            final @NotNull Shop shop
    ) {
        final boolean changed = AdminShopStockSupport.applyRestock(this.plugin, shop);
        if (changed) {
            this.plugin.getShopRepository().update(shop);
        }
        return changed;
    }

    private void restockAdminShops() {
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (!shop.isAdminShop()) {
                continue;
            }

            this.restockShop(shop);
        }
    }
}
