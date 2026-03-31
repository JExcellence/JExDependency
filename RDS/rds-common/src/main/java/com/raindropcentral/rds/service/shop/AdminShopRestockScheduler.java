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

    /**
     * Executes restockShop.
     */
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
