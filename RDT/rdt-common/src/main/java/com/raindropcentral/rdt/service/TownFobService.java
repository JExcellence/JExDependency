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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Handles local-only {@code /rt fob} travel for the player's town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownFobService {

    private final RDT plugin;

    /**
     * Creates the town FOB travel service.
     *
     * @param plugin active plugin runtime
     */
    public TownFobService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Starts a FOB travel request for the supplied player.
     *
     * @param player player requesting travel
     * @return {@code true} when the request was accepted
     */
    public boolean teleportToTownFob(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RTown town = runtimeService == null ? null : runtimeService.getTownFor(player.getUniqueId());
        if (town == null) {
            return false;
        }

        final Location target = runtimeService.resolveFobTeleportLocation(town);
        if (target == null || this.plugin.getScheduler() == null) {
            return false;
        }

        final int delaySeconds = Math.max(0, this.plugin.getDefaultConfig().getTownFobTeleportDelaySeconds());
        new I18n.Builder("town_fob.started", player)
            .includePrefix()
            .withPlaceholder("seconds", delaySeconds)
            .build()
            .sendMessage();
        this.plugin.getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleport(target);
            new I18n.Builder("town_fob.teleported", player)
                .includePrefix()
                .withPlaceholder("town", town.getTownName())
                .build()
                .sendMessage();
        }, delaySeconds * 20L);
        return true;
    }
}
