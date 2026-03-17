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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for damage events related to the bounty system.
 * Tracks damage dealt to players with active bounties for claim attribution.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyDamageListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyDamageListener.class.getName());

    private final RDQ rdq;

    /**
     * Executes BountyDamageListener.
     */
    public BountyDamageListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Executes onEntityDamageByEntity.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)  // Changed to catch cancelled events too
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        // Only track player-on-player damage
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // Always record damage for all player-vs-player combat
        // The ClaimHandler will check for bounties later during death processing
        double damage = event.getFinalDamage();
        rdq.getBountyFactory().getDamageTracker().recordDamage(
            victim.getUniqueId(),
            attacker.getUniqueId(),
            damage
        );
    }
}
