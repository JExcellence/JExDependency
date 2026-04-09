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

package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.view.shop.ShopCustomerView;
import com.raindropcentral.rds.view.shop.ShopOverviewView;
import me.devnatan.inventoryframework.View;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;

/**
 * Handles player click events.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class PlayerClickListener implements Listener {

    private final RDS rds;

    /**
     * Creates a new player click listener.
     *
     * @param rds plugin instance
     */
    public PlayerClickListener(RDS rds) {
        this.rds = rds;
    }

    /**
     * Handles the player click callback.
     *
     * @param event triggering event
     */
    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event == null) return;
        final var player = event.getPlayer();
        final var block = event.getClickedBlock();
        if (block == null) return;
        final var location = block.getLocation();
        final Shop shop = this.rds.getShopRepository().findByLocation(location);
        if (shop == null) return;
        if (event.getAction().isRightClick()) event.setCancelled(true);
        final boolean canAccessOverview = this.rds.getTownShopService() == null
                ? shop.canAccessOverview(player.getUniqueId())
                : this.rds.getTownShopService().canAccessOverview(player, shop);
        final Class<? extends View> viewClass = canAccessOverview
                ? ShopOverviewView.class
                : ShopCustomerView.class;
        this.rds.getViewFrame().open(
                viewClass,
                player,
                Map.of(
                        "plugin",
                        this.rds,
                        "shopLocation",
                        shop.getShopLocation()
                )
        );
    }
}
