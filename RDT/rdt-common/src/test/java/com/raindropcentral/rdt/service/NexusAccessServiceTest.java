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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NexusAccessServiceTest {

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private TownService townService;

    @Mock
    private TownRuntimeService townRuntimeService;

    @Mock
    private Player player;

    @Mock
    private RTown town;

    @Mock
    private World playerWorld;

    @Mock
    private World nexusWorld;

    @Mock
    private Location playerLocation;

    @Mock
    private Location nexusLocation;

    @Test
    void sessionValidationUsesBlockCoordinatesWithoutFetchingChunks() throws ReflectiveOperationException {
        final UUID playerUuid = UUID.randomUUID();
        final UUID townUuid = UUID.randomUUID();
        final RDT plugin = new RDT(this.javaPlugin, "Test", this.townService);

        this.setTownRuntimeService(plugin, this.townRuntimeService);
        when(this.townRuntimeService.getTown(townUuid)).thenReturn(this.town);
        when(this.player.getUniqueId()).thenReturn(playerUuid);
        when(this.player.getWorld()).thenReturn(this.playerWorld);
        when(this.playerWorld.getName()).thenReturn("world");
        when(this.player.getLocation()).thenReturn(this.playerLocation);
        when(this.playerLocation.getBlockX()).thenReturn(-1);
        when(this.playerLocation.getBlockZ()).thenReturn(-17);
        when(this.town.getTownUUID()).thenReturn(townUuid);
        when(this.town.getNexusLocation()).thenReturn(this.nexusLocation);
        when(this.nexusLocation.getWorld()).thenReturn(this.nexusWorld);
        when(this.nexusWorld.getName()).thenReturn("world");
        when(this.nexusLocation.getBlockX()).thenReturn(-1);
        when(this.nexusLocation.getBlockZ()).thenReturn(-17);

        final NexusAccessService service = new NexusAccessService(plugin);

        service.openSession(this.player, this.town);

        final NexusAccessService.NexusSession session = service.getSession(playerUuid);
        assertNotNull(session);
        assertEquals(-1, session.chunkX());
        assertEquals(-2, session.chunkZ());
        assertTrue(service.validate(this.player, townUuid));
        verify(this.playerLocation, never()).getChunk();
        verify(this.nexusLocation, never()).getChunk();
    }

    private void setTownRuntimeService(final RDT plugin, final TownRuntimeService runtimeService)
        throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField("townRuntimeService");
        field.setAccessible(true);
        field.set(plugin, runtimeService);
    }
}
