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

package com.raindropcentral.core.listener;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.central.DropletClaimService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Routes tagged droplet cookies to the correct activation flow when used.
 */
public final class DropletCookieListener implements Listener {

    private final DropletClaimService dropletClaimService;

    /**
     * Creates the cookie listener.
     *
     * @param plugin active RCore implementation
     */
    public DropletCookieListener(final @NotNull RCoreImpl plugin) {
        this.dropletClaimService = plugin.getDropletClaimService();
    }

    /**
     * Intercepts right-click usage of tagged droplet cookies.
     *
     * @param event triggering interact event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerUseDropletCookie(final @NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!this.dropletClaimService.isDropletCookie(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        this.dropletClaimService.handleCookieUse(event.getPlayer(), event.getItem());
    }
}
