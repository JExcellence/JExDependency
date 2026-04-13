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

package com.raindropcentral.rda.listeners;

import com.raindropcentral.rda.PartyService;
import com.raindropcentral.rda.RDA;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Listener that keeps persistent party leadership valid across player disconnects.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
@SuppressWarnings("unused")
public final class PartyLifecycleListener implements Listener {

    private final RDA rda;

    /**
     * Creates a listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public PartyLifecycleListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Transfers leadership when the current party leader disconnects.
     *
     * @param event player quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final PartyService partyService = this.rda.getPartyService();
        if (partyService != null) {
            partyService.handlePlayerQuit(event.getPlayer().getUniqueId());
        }
    }
}
