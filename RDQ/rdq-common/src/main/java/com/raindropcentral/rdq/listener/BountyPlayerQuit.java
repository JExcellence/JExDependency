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

package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for player quit events related to the bounty system.
 * Handles preserving bounty state and cleaning up temporary data when players log out.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class  BountyPlayerQuit implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyPlayerQuit.class.getName());

    private final RDQ rdq;

    /**
     * Executes BountyPlayerQuit.
     */
    public BountyPlayerQuit(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Executes onPlayerQuit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();

        rdq.getBountyFactory().getDamageTracker().clearDamage(player.getUniqueId());
        LOGGER.fine("Cleaned up temporary bounty data for player " + player.getName());
    }
}
