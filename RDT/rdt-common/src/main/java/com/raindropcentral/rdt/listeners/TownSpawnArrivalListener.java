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

package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.service.TownSpawnService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Consumes pending town-spawn arrival actions after cross-server player join.
 */
@SuppressWarnings("unused")
public final class TownSpawnArrivalListener implements Listener {

    private final RDT plugin;

    /**
     * Creates one town-spawn arrival listener.
     *
     * @param plugin active RDT runtime
     */
    public TownSpawnArrivalListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to consume and execute one pending town-spawn arrival token after player join.
     *
     * @param event player join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final TownSpawnService townSpawnService = this.plugin.getTownSpawnService();
        if (townSpawnService == null) {
            return;
        }
        townSpawnService.consumePendingArrivalOnJoin(event.getPlayer());
    }
}
