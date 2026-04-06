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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks short-lived nexus-origin sessions for governance actions that must be performed in the
 * active nexus chunk.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NexusAccessService {

    private static final long SESSION_LIFETIME_MILLIS = 120_000L;

    private final RDT plugin;
    private final Map<UUID, NexusSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates the nexus-access validator.
     *
     * @param plugin active plugin runtime
     */
    public NexusAccessService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Opens or refreshes a nexus-governance session for a player.
     *
     * @param player player entering the nexus menu
     * @param town target town
     */
    public void openSession(final @NotNull Player player, final @NotNull RTown town) {
        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            this.sessions.remove(player.getUniqueId());
            return;
        }

        this.sessions.put(
            player.getUniqueId(),
            new NexusSession(
                town.getTownUUID(),
                nexusLocation.getWorld().getName(),
                TownRuntimeService.toChunkCoordinate(nexusLocation.getBlockX()),
                TownRuntimeService.toChunkCoordinate(nexusLocation.getBlockZ()),
                UUID.randomUUID(),
                System.currentTimeMillis() + SESSION_LIFETIME_MILLIS
            )
        );
    }

    /**
     * Invalidates any stored nexus session for a player.
     *
     * @param playerUuid player UUID whose session should be removed
     */
    public void invalidate(final @NotNull UUID playerUuid) {
        this.sessions.remove(Objects.requireNonNull(playerUuid, "playerUuid"));
    }

    /**
     * Returns whether the player still has valid nexus governance access for the supplied town.
     *
     * @param player player to validate
     * @param town town being managed
     * @return {@code true} when the player still has a valid nexus session
     */
    public boolean hasValidSession(final @NotNull Player player, final @NotNull RTown town) {
        return this.validate(player, town.getTownUUID());
    }

    /**
     * Returns whether the player still has valid nexus governance access for the supplied town ID.
     *
     * @param player player to validate
     * @param townUuid target town UUID
     * @return {@code true} when the player still has a valid nexus session
     */
    public boolean validate(final @NotNull Player player, final @NotNull UUID townUuid) {
        final NexusSession session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        if (session.expiresAtMillis() < System.currentTimeMillis()) {
            this.sessions.remove(player.getUniqueId());
            return false;
        }
        if (!Objects.equals(session.townUuid(), townUuid)) {
            return false;
        }

        final RTown town = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().getTown(townUuid);
        final Location nexusLocation = town == null ? null : town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }

        final Location playerLocation = player.getLocation();
        final int playerChunkX = TownRuntimeService.toChunkCoordinate(playerLocation.getBlockX());
        final int playerChunkZ = TownRuntimeService.toChunkCoordinate(playerLocation.getBlockZ());
        final int nexusChunkX = TownRuntimeService.toChunkCoordinate(nexusLocation.getBlockX());
        final int nexusChunkZ = TownRuntimeService.toChunkCoordinate(nexusLocation.getBlockZ());

        return Objects.equals(player.getWorld().getName(), session.worldName())
            && playerChunkX == session.chunkX()
            && playerChunkZ == session.chunkZ()
            && Objects.equals(nexusLocation.getWorld().getName(), session.worldName())
            && nexusChunkX == session.chunkX()
            && nexusChunkZ == session.chunkZ();
    }

    /**
     * Returns the currently tracked session for a player.
     *
     * @param playerUuid player UUID to inspect
     * @return active session, or {@code null} when none is stored
     */
    public @Nullable NexusSession getSession(final @NotNull UUID playerUuid) {
        return this.sessions.get(Objects.requireNonNull(playerUuid, "playerUuid"));
    }

    /**
     * Immutable nexus-governance session payload.
     *
     * @param townUuid target town UUID
     * @param worldName nexus world name
     * @param chunkX nexus chunk X
     * @param chunkZ nexus chunk Z
     * @param sessionToken unique short-lived view token
     * @param expiresAtMillis expiry timestamp
     */
    public record NexusSession(
        @NotNull UUID townUuid,
        @NotNull String worldName,
        int chunkX,
        int chunkZ,
        @NotNull UUID sessionToken,
        long expiresAtMillis
    ) {
    }
}
