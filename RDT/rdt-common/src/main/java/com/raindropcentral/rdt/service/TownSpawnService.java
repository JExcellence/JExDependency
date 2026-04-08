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
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Handles {@code /rt spawn} travel for the player's town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownSpawnService {

    private final RDT plugin;

    /**
     * Creates the town-spawn service.
     *
     * @param plugin active plugin runtime
     */
    public TownSpawnService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Starts a town-spawn travel request for the supplied player.
     *
     * @param player player requesting travel
     * @return {@code true} when the request was accepted
     */
    public boolean teleportToTownSpawn(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RTown town = runtimeService == null ? null : runtimeService.getTownFor(player.getUniqueId());
        if (town == null) {
            return false;
        }

        final ConfigSection config = this.plugin.getDefaultConfig();
        final String localServerId = config.getProxyServerRouteId();
        final String targetServerId = town.resolveTownSpawnServerId(localServerId);
        if (config.isProxyEnabled()
            && config.isProxyTownSpawnEnabled()
            && targetServerId != null
            && !targetServerId.isBlank()
            && localServerId != null
            && !localServerId.isBlank()
            && !targetServerId.equalsIgnoreCase(localServerId)
            && this.plugin.getProxyService().isAvailable()) {
            final boolean queued = this.plugin.getProxyService().requestPlayerTransfer(
                new ProxyTransferRequest(player.getUniqueId(), localServerId, targetServerId, "")
            ).join();
            if (queued) {
                new I18n.Builder("town_spawn.transfer_requested", player)
                    .includePrefix()
                    .withPlaceholder("target_server", targetServerId)
                    .build()
                    .sendMessage();
            }
            return queued;
        }

        final Location target = this.resolveTargetLocation(town);
        if (target == null || this.plugin.getScheduler() == null) {
            return false;
        }

        final int delaySeconds = Math.max(0, config.getTownSpawnTeleportDelaySeconds());
        new I18n.Builder("town_spawn.started", player)
            .includePrefix()
            .withPlaceholder("seconds", delaySeconds)
            .build()
            .sendMessage();
        this.plugin.getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleport(target);
            new I18n.Builder("town_spawn.teleported", player)
                .includePrefix()
                .withPlaceholder("town", town.getTownName())
                .build()
                .sendMessage();
        }, delaySeconds * 20L);
        return true;
    }

    private Location resolveTargetLocation(final @NotNull RTown town) {
        final Location target = town.getTownSpawnLocation() == null
            ? town.getNexusLocation()
            : town.getTownSpawnLocation();
        if (target == null || target.getWorld() == null) {
            return null;
        }
        return target.clone().add(0.5D, 1.0D, 0.5D);
    }
}
