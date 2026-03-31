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
import com.raindropcentral.rplatform.cookie.CookieBoostLookup;
import com.raindropcentral.rplatform.cookie.CookieBoostType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the double-drop droplet cookie effect to player-caused mob kills.
 */
public final class DropletDoubleDropListener implements Listener {

    private final CookieBoostLookup cookieBoostLookup;

    /**
     * Creates the listener.
     *
     * @param plugin active RCore implementation
     */
    public DropletDoubleDropListener(final @NotNull RCoreImpl plugin) {
        this.cookieBoostLookup = plugin.getPlatform()
                .getServiceRegistry()
                .get(CookieBoostLookup.class)
                .orElse(plugin.getActiveCookieBoostService());
    }

    /**
     * Duplicates the final item drop list when the killer has an active double-drop boost.
     *
     * @param event entity death event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(final @NotNull EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null || event.getDrops().isEmpty()) {
            return;
        }

        final double multiplier = this.cookieBoostLookup.getMultiplier(
                killer.getUniqueId(),
                CookieBoostType.DOUBLE_DROP,
                null,
                null
        );
        if (multiplier <= 1.0D) {
            return;
        }

        final List<ItemStack> extraDrops = new ArrayList<>(event.getDrops().size());
        for (final ItemStack itemStack : event.getDrops()) {
            if (itemStack == null || itemStack.getType().isAir()) {
                continue;
            }
            extraDrops.add(itemStack.clone());
        }

        if (!extraDrops.isEmpty()) {
            event.getDrops().addAll(extraDrops);
        }
    }
}
