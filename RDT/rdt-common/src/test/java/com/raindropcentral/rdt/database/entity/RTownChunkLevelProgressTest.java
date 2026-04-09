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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import com.raindropcentral.rdt.utils.TownProtections;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests chunk-level saved progression bookkeeping on {@link RTownChunk}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RTownChunkLevelProgressTest {

    @Test
    void levelRequirementProgressCanBeStoredAndClearedByPrefix() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);
        final RTownChunk townChunk = new RTownChunk(town, "world", 1, 2, ChunkType.SECURITY);
        final ItemStack levelTwoProgress = this.mockItemStack(8);
        final ItemStack levelThreeProgress = this.mockItemStack(2);

        townChunk.setLevelCurrencyProgress(" security.level.2.vault ", 250.0D);
        townChunk.setLevelItemProgress("security.level.2.item", levelTwoProgress);
        townChunk.setLevelItemProgress("security.level.3.item", levelThreeProgress);

        assertEquals(250.0D, townChunk.getLevelCurrencyProgress("security.level.2.vault"));
        assertEquals(8, townChunk.getLevelItemProgress("security.level.2.item").getAmount());

        townChunk.clearLevelRequirementProgress("security.level.2");

        assertEquals(0.0D, townChunk.getLevelCurrencyProgress("security.level.2.vault"));
        assertNull(townChunk.getLevelItemProgress("security.level.2.item"));
        assertEquals(2, townChunk.getLevelItemProgress("security.level.3.item").getAmount());
    }

    @Test
    void changingChunkTypeOnlyUpdatesTypeUntilStateResetIsRequested() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);
        final RTownChunk townChunk = new RTownChunk(town, "world", 1, 2, ChunkType.SECURITY);
        final ItemStack seedBoxItem = this.mockItemStack(12);
        final ItemStack levelProgressItem = this.mockItemStack(3);
        final World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("world");

        townChunk.setChunkLevel(4);
        townChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        townChunk.setSeedBoxLocation(new Location(world, 18.0D, 64.0D, 34.0D));
        townChunk.setSeedBoxContents(Map.of("0", seedBoxItem));
        townChunk.setFarmGrowthEnabled(Boolean.TRUE);
        townChunk.setFarmAutoReplantEnabled(Boolean.FALSE);
        townChunk.setFarmReplantPriority(FarmReplantPriority.SEED_BOX_FIRST);
        townChunk.setLevelCurrencyProgress("security.level.4.vault", 500.0D);
        townChunk.setLevelItemProgress("security.level.4.item", levelProgressItem);

        townChunk.setChunkType(ChunkType.DEFAULT);

        assertEquals(ChunkType.DEFAULT, townChunk.getChunkType());
        assertEquals(4, townChunk.getChunkLevel());
        assertEquals(RTown.MAYOR_ROLE_ID, townChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertTrue(townChunk.hasSeedBox());
        assertEquals(1, townChunk.getSeedBoxContents().size());
        assertTrue(townChunk.isFarmGrowthEnabled(false));
        assertTrue(townChunk.getFarmAutoReplantEnabledValue() == Boolean.FALSE);
        assertEquals(FarmReplantPriority.SEED_BOX_FIRST, townChunk.getFarmReplantPriorityValue());
        assertEquals(500.0D, townChunk.getLevelCurrencyProgress("security.level.4.vault"));
        assertEquals(3, townChunk.getLevelItemProgress("security.level.4.item").getAmount());
    }

    @Test
    void resetChunkTypeStateClearsSavedProgressAndResetsChunkState() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);
        final RTownChunk townChunk = new RTownChunk(town, "world", 1, 2, ChunkType.SECURITY);
        final ItemStack seedBoxItem = this.mockItemStack(12);
        final ItemStack levelProgressItem = this.mockItemStack(3);
        final ItemStack fuelItem = this.mockItemStack(16);
        final World world = Mockito.mock(World.class);
        Mockito.when(world.getName()).thenReturn("world");

        townChunk.setChunkLevel(4);
        townChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        townChunk.setSeedBoxLocation(new Location(world, 18.0D, 64.0D, 34.0D));
        townChunk.setSeedBoxContents(Map.of("0", seedBoxItem));
        townChunk.setFarmGrowthEnabled(Boolean.TRUE);
        townChunk.setFarmAutoReplantEnabled(Boolean.FALSE);
        townChunk.setFarmReplantPriority(FarmReplantPriority.SEED_BOX_FIRST);
        townChunk.setLevelCurrencyProgress("security.level.4.vault", 500.0D);
        townChunk.setLevelItemProgress("security.level.4.item", levelProgressItem);
        townChunk.setFuelTankLocation(new Location(world, 16.0D, 64.0D, 32.0D));
        townChunk.setFuelTankContents(Map.of("0", fuelItem));
        townChunk.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, Boolean.TRUE);

        townChunk.resetChunkTypeState();

        assertEquals(1, townChunk.getChunkLevel());
        assertTrue(townChunk.getProtectionRoleIds().isEmpty());
        assertNull(townChunk.getSeedBoxLocation());
        assertTrue(townChunk.getSeedBoxContents().isEmpty());
        assertNull(townChunk.getFarmGrowthEnabledValue());
        assertNull(townChunk.getFarmAutoReplantEnabledValue());
        assertNull(townChunk.getFarmReplantPriorityValue());
        assertTrue(townChunk.getLevelCurrencyProgress().isEmpty());
        assertTrue(townChunk.getLevelItemProgress().isEmpty());
        assertNull(townChunk.getFuelTankLocation());
        assertTrue(townChunk.getFuelTankContents().isEmpty());
        assertTrue(townChunk.getAlliedProtectionStates().isEmpty());
    }

    private ItemStack mockItemStack(final int amount) {
        final ItemStack itemStack = Mockito.mock(ItemStack.class);
        Mockito.when(itemStack.isEmpty()).thenReturn(false);
        Mockito.when(itemStack.getAmount()).thenReturn(amount);
        Mockito.when(itemStack.clone()).thenReturn(itemStack);
        return itemStack;
    }
}
