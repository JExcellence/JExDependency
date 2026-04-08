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
import com.raindropcentral.rdt.configs.SecurityConfigSection;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TownFuelServiceTest {

    @Test
    void storedFuelQueriesUsePersistedTankContentsWithoutLiveChunkChecks() {
        final World world = Mockito.mock(World.class);
        when(world.getName()).thenReturn("world");
        doThrow(new AssertionError("Stored fuel math should not inspect live chunk state."))
            .when(world).isChunkLoaded(2, 3);
        final ItemStack fuelStack = Mockito.mock(ItemStack.class);
        when(fuelStack.getType()).thenReturn(Material.REDSTONE);
        when(fuelStack.getAmount()).thenReturn(4);
        when(fuelStack.isEmpty()).thenReturn(false);
        when(fuelStack.clone()).thenReturn(fuelStack);

        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        securityChunk.setChunkBlockLocation(new Location(world, 32.0D, 64.0D, 48.0D));
        securityChunk.setFuelTankLocation(new Location(world, 34.0D, 64.0D, 50.0D));
        securityChunk.setFuelTankContents(Map.of("0", fuelStack));
        town.addChunk(securityChunk);

        final TownFuelService service = new TownFuelService(this.createPlugin());

        assertEquals(100.0D, service.getTankFuelUnits(securityChunk));
        assertEquals(100.0D, service.getStoredFuelUnits(town));
        verify(world, never()).isChunkLoaded(2, 3);
    }

    private RDT createPlugin() {
        final SecurityConfigSection securityConfig = SecurityConfigSection.createDefault();
        return new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class)) {
            @Override
            public SecurityConfigSection getSecurityConfig() {
                return securityConfig;
            }
        };
    }
}
