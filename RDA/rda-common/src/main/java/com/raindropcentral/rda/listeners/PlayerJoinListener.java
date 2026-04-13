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

import com.raindropcentral.rda.RDA;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Player join listener that provisions persistent RDA player rows and child skill states.
 *
 * <p>Provisioning is intentionally lazy and asynchronous so existing players migrate without a
 * blocking one-time data step.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final RDA rda;

    /**
     * Creates a listener bound to the active plugin runtime.
     *
     * @param rda active RDA runtime
     * @throws NullPointerException if {@code rda} is {@code null}
     */
    public PlayerJoinListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Creates a new persisted player row when needed.
     *
     * @param event Bukkit player join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.rda.ensurePlayerProfile(player);
    }
}
