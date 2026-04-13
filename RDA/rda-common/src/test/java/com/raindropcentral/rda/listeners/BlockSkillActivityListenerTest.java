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

import com.raindropcentral.rda.PlacedTrackedBlockService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BlockSkillActivityListener}.
 */
class BlockSkillActivityListenerTest {

    @Test
    void tracksPlacedBlocksForSuppressedSkills() {
        final RDA rda = mock(RDA.class);
        final PlacedTrackedBlockService<?> placedBlockService = mock(PlacedTrackedBlockService.class);
        final BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        final Block block = mock(Block.class);

        when(event.getBlockPlaced()).thenReturn(block);
        when(block.getType()).thenReturn(Material.OAK_LOG);
        when(rda.getBlockBreakSkillOwner(Material.OAK_LOG)).thenReturn(SkillType.WOODCUTTING);
        when(rda.usesNaturalBlockSuppression(SkillType.WOODCUTTING)).thenReturn(true);
        doReturn(placedBlockService).when(rda).getPlacedTrackedBlockService(SkillType.WOODCUTTING);

        new BlockSkillActivityListener(rda).onBlockPlace(event);

        verify(placedBlockService).trackPlacedBlock(block);
    }

    @Test
    void awardsXpForNaturalTrackedBlocks() {
        final RDA rda = mock(RDA.class);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);
        final PlacedTrackedBlockService<?> placedBlockService = mock(PlacedTrackedBlockService.class);
        final BlockBreakEvent event = mock(BlockBreakEvent.class);
        final Block block = mock(Block.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final ItemStack itemInHand = mock(ItemStack.class);
        final World world = mock(World.class);
        final SkillConfig skillConfig = this.createWoodcuttingConfig();

        when(itemInHand.getType()).thenReturn(Material.DIAMOND_AXE);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(itemInHand);
        when(block.getType()).thenReturn(Material.OAK_LOG);
        when(block.getBlockData()).thenReturn(mock(BlockData.class));
        when(block.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(0);
        when(rda.getBlockBreakSkillOwner(Material.OAK_LOG)).thenReturn(SkillType.WOODCUTTING);
        doReturn(progressionService).when(rda).getSkillProgressionService(SkillType.WOODCUTTING);
        doReturn(skillConfig).when(rda).getSkillConfig(SkillType.WOODCUTTING);
        doReturn(placedBlockService).when(rda).getPlacedTrackedBlockService(SkillType.WOODCUTTING);
        when(placedBlockService.consumePlacedBlock(block)).thenReturn(false);

        new BlockSkillActivityListener(rda).onBlockBreak(event);

        verify(progressionService).awardXp(eq(player), eq(skillConfig.getRates().getFirst()), eq(1.0D), eq("Logs"));
    }

    @Test
    void skipsXpForPlayerPlacedTrackedBlocks() {
        final RDA rda = mock(RDA.class);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);
        final PlacedTrackedBlockService<?> placedBlockService = mock(PlacedTrackedBlockService.class);
        final BlockBreakEvent event = mock(BlockBreakEvent.class);
        final Block block = mock(Block.class);
        final World world = mock(World.class);

        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.OAK_LOG);
        when(block.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(block.getX()).thenReturn(0);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(0);
        when(rda.getBlockBreakSkillOwner(Material.OAK_LOG)).thenReturn(SkillType.WOODCUTTING);
        doReturn(progressionService).when(rda).getSkillProgressionService(SkillType.WOODCUTTING);
        doReturn(this.createWoodcuttingConfig()).when(rda).getSkillConfig(SkillType.WOODCUTTING);
        doReturn(placedBlockService).when(rda).getPlacedTrackedBlockService(SkillType.WOODCUTTING);
        when(placedBlockService.consumePlacedBlock(block)).thenReturn(true);

        new BlockSkillActivityListener(rda).onBlockBreak(event);

        verify(progressionService, never()).awardXp(any(), any(), anyDouble(), any());
    }

    private SkillConfig createWoodcuttingConfig() {
        return new SkillConfig(
            SkillType.WOODCUTTING,
            true,
            Material.DIAMOND_AXE,
            1000,
            10,
            SkillConfig.PrestigeTrigger.MANUAL,
            10,
            new SkillConfig.LevelFormula(
                SkillConfig.LevelFormulaType.POWER,
                50.0D,
                4.0D,
                1.2D,
                SkillConfig.LevelFormulaRounding.FLOOR,
                "test"
            ),
            List.of(new SkillConfig.RateDefinition(
                "logs",
                "Logs",
                "Break logs.",
                null,
                Material.OAK_LOG,
                SkillTriggerType.BLOCK_BREAK,
                5,
                Set.of(Material.OAK_LOG),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                false,
                false,
                false,
                0.0D,
                SkillConfig.ToolRequirement.ANY
            )),
            0,
            List.of(),
            null
        );
    }
}
