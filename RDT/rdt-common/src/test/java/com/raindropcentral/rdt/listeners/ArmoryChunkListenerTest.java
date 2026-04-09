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
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.service.TownArmoryService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.service.TownService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArmoryChunkListenerTest {

    @Test
    void smokerBurnEventDoesNotInvokeDoubleSmeltLogic() {
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        final TownArmoryService townArmoryService = Mockito.mock(TownArmoryService.class);
        final ArmoryChunkListener listener = new ArmoryChunkListener(this.createPlugin(runtimeService, townArmoryService));
        final Block block = Mockito.mock(Block.class);
        final FurnaceBurnEvent event = Mockito.mock(FurnaceBurnEvent.class);

        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.SMOKER);

        listener.onFurnaceBurn(event);

        verifyNoInteractions(runtimeService, townArmoryService);
        verify(event, never()).setBurnTime(anyInt());
    }

    @Test
    void furnaceBurnEventAppliesAdjustedBurnTime() {
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        final TownArmoryService townArmoryService = Mockito.mock(TownArmoryService.class);
        final ArmoryChunkListener listener = new ArmoryChunkListener(this.createPlugin(runtimeService, townArmoryService));
        final Block block = Mockito.mock(Block.class);
        final Location location = Mockito.mock(Location.class);
        final RTownChunk townChunk = Mockito.mock(RTownChunk.class);
        final FurnaceBurnEvent event = Mockito.mock(FurnaceBurnEvent.class);

        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.FURNACE);
        when(block.getLocation()).thenReturn(location);
        when(event.getBurnTime()).thenReturn(200);
        when(runtimeService.getChunkAt(location)).thenReturn(townChunk);
        when(townArmoryService.resolveAdjustedBurnTime(townChunk, 200)).thenReturn(100);

        listener.onFurnaceBurn(event);

        verify(townArmoryService).resolveAdjustedBurnTime(townChunk, 200);
        verify(event).setBurnTime(100);
    }

    @Test
    void smokerSmeltEventDoesNotInvokeDoubleSmeltLogic() {
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        final TownArmoryService townArmoryService = Mockito.mock(TownArmoryService.class);
        final ArmoryChunkListener listener = new ArmoryChunkListener(this.createPlugin(runtimeService, townArmoryService));
        final Block block = Mockito.mock(Block.class);
        final FurnaceSmeltEvent event = Mockito.mock(FurnaceSmeltEvent.class);

        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.SMOKER);

        listener.onFurnaceSmelt(event);

        verifyNoInteractions(runtimeService, townArmoryService);
        verify(event, never()).setResult(Mockito.any());
    }

    @Test
    void blastFurnaceSmeltEventAppliesDoubleSmeltResult() {
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        final TownArmoryService townArmoryService = Mockito.mock(TownArmoryService.class);
        final ArmoryChunkListener listener = new ArmoryChunkListener(this.createPlugin(runtimeService, townArmoryService));
        final Block block = Mockito.mock(Block.class);
        final Furnace furnace = Mockito.mock(Furnace.class);
        final Location location = Mockito.mock(Location.class);
        final RTownChunk townChunk = Mockito.mock(RTownChunk.class);
        final ItemStack baseResult = Mockito.mock(ItemStack.class);
        final ItemStack doubledResult = Mockito.mock(ItemStack.class);
        final FurnaceSmeltEvent event = Mockito.mock(FurnaceSmeltEvent.class);

        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.BLAST_FURNACE);
        when(block.getLocation()).thenReturn(location);
        when(block.getState()).thenReturn(furnace);
        when(event.getResult()).thenReturn(baseResult);
        when(runtimeService.getChunkAt(location)).thenReturn(townChunk);
        when(townArmoryService.applyDoubleSmelt(townChunk, furnace, baseResult))
            .thenReturn(new TownArmoryService.DoubleSmeltResult(doubledResult, true, false));

        listener.onFurnaceSmelt(event);

        verify(townArmoryService).applyDoubleSmelt(townChunk, furnace, baseResult);
        verify(event).setResult(doubledResult);
    }

    private RDT createPlugin(
        final TownRuntimeService runtimeService,
        final TownArmoryService townArmoryService
    ) {
        return new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class)) {
            @Override
            public TownRuntimeService getTownRuntimeService() {
                return runtimeService;
            }

            @Override
            public TownArmoryService getTownArmoryService() {
                return townArmoryService;
            }
        };
    }
}
