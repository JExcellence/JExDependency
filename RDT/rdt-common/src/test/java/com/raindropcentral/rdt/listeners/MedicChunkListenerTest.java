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
import com.raindropcentral.rdt.service.TownMedicService;
import com.raindropcentral.rdt.service.TownService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MedicChunkListenerTest {

    @Test
    void moveAcrossChunksForwardsToMedicService() {
        final TownMedicService townMedicService = Mockito.mock(TownMedicService.class);
        final MedicChunkListener listener = new MedicChunkListener(this.createPlugin(townMedicService));
        final Player player = Mockito.mock(Player.class);
        final World world = Mockito.mock(World.class);
        final Chunk fromChunk = Mockito.mock(Chunk.class);
        final Chunk toChunk = Mockito.mock(Chunk.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(fromChunk.getX()).thenReturn(0);
        when(fromChunk.getZ()).thenReturn(0);
        when(toChunk.getX()).thenReturn(1);
        when(toChunk.getZ()).thenReturn(0);
        final Location from = Mockito.mock(Location.class);
        final Location to = Mockito.mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(to.getWorld()).thenReturn(world);
        when(from.getChunk()).thenReturn(fromChunk);
        when(to.getChunk()).thenReturn(toChunk);
        final PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);

        listener.onPlayerMove(event);

        verify(townMedicService).handlePlayerMove(player, from, to);
    }

    @Test
    void moveWithinSameChunkDoesNotForwardToMedicService() {
        final TownMedicService townMedicService = Mockito.mock(TownMedicService.class);
        final MedicChunkListener listener = new MedicChunkListener(this.createPlugin(townMedicService));
        final World world = Mockito.mock(World.class);
        final Chunk chunk = Mockito.mock(Chunk.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(chunk.getX()).thenReturn(1);
        when(chunk.getZ()).thenReturn(0);
        final Location from = Mockito.mock(Location.class);
        final Location to = Mockito.mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(to.getWorld()).thenReturn(world);
        when(from.getChunk()).thenReturn(chunk);
        when(to.getChunk()).thenReturn(chunk);
        final PlayerMoveEvent event = Mockito.mock(PlayerMoveEvent.class);
        when(event.getFrom()).thenReturn(from);
        when(event.getTo()).thenReturn(to);

        listener.onPlayerMove(event);

        verifyNoInteractions(townMedicService);
    }

    @Test
    void joinTeleportRespawnAndQuitForwardToMedicService() {
        final TownMedicService townMedicService = Mockito.mock(TownMedicService.class);
        final MedicChunkListener listener = new MedicChunkListener(this.createPlugin(townMedicService));
        final Player player = Mockito.mock(Player.class);
        final World world = Mockito.mock(World.class);
        final Location destination = new Location(world, 32.0D, 64.0D, 48.0D);

        final PlayerJoinEvent joinEvent = Mockito.mock(PlayerJoinEvent.class);
        when(joinEvent.getPlayer()).thenReturn(player);
        listener.onPlayerJoin(joinEvent);

        final PlayerTeleportEvent teleportEvent = Mockito.mock(PlayerTeleportEvent.class);
        when(teleportEvent.getPlayer()).thenReturn(player);
        when(teleportEvent.getTo()).thenReturn(destination);
        listener.onPlayerTeleport(teleportEvent);

        final PlayerRespawnEvent respawnEvent = Mockito.mock(PlayerRespawnEvent.class);
        when(respawnEvent.getPlayer()).thenReturn(player);
        when(respawnEvent.getRespawnLocation()).thenReturn(destination);
        listener.onPlayerRespawn(respawnEvent);

        final PlayerQuitEvent quitEvent = Mockito.mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(player);
        listener.onPlayerQuit(quitEvent);

        verify(townMedicService).handlePlayerJoin(player);
        verify(townMedicService).handlePlayerTeleport(player, destination);
        verify(townMedicService).handlePlayerRespawn(player, destination);
        verify(townMedicService).handlePlayerQuit(player);
        verify(townMedicService, never()).handlePlayerMove(any(), any(), any());
    }

    private RDT createPlugin(final TownMedicService townMedicService) {
        return new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class)) {
            @Override
            public TownMedicService getTownMedicService() {
                return townMedicService;
            }
        };
    }
}
