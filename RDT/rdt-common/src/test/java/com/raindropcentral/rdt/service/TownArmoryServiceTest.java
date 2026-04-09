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
import com.raindropcentral.rdt.configs.ArmoryConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TownArmoryServiceTest {

    @Test
    void doubleSmeltUsesChunkDefaultToggleAndBurnMultiplier() {
        final ArmoryConfigSection armoryConfig = ArmoryConfigSection.fromInputStream(new ByteArrayInputStream("""
            double_smelt:
              enabled_by_default: true
              burn_faster_multiplier: 2.0
            """.getBytes(StandardCharsets.UTF_8)));
        final TownArmoryService service = new TownArmoryService(this.createPlugin(armoryConfig, null));
        final RTownChunk armoryChunk = this.createArmoryChunk(5);

        assertTrue(service.isDoubleSmeltActive(armoryChunk));
        assertEquals(100, service.resolveAdjustedBurnTime(armoryChunk, 200));

        armoryChunk.setArmoryDoubleSmeltEnabled(Boolean.FALSE);

        assertFalse(service.isDoubleSmeltActive(armoryChunk));
        assertEquals(200, service.resolveAdjustedBurnTime(armoryChunk, 200));
    }

    @Test
    void applyDoubleSmeltConsumesExtraFuelAndDoublesResultWhenEnabled() {
        final ArmoryConfigSection armoryConfig = ArmoryConfigSection.fromInputStream(new ByteArrayInputStream("""
            double_smelt:
              enabled_by_default: true
              extra_fuel_per_smelt_units: 20
            """.getBytes(StandardCharsets.UTF_8)));
        final TownArmoryService service = new TownArmoryService(this.createPlugin(armoryConfig, null));
        final RTownChunk armoryChunk = this.createArmoryChunk(5);
        final Furnace furnace = Mockito.mock(Furnace.class);
        final AtomicInteger burnTime = new AtomicInteger(100);
        final ItemStackContext resultItem = this.createCloneableItemStack(Material.IRON_INGOT, 1, 64);

        when(furnace.getBurnTime()).thenAnswer(invocation -> (short) burnTime.get());
        Mockito.doAnswer(invocation -> {
            burnTime.set(invocation.getArgument(0, Short.class));
            return null;
        }).when(furnace).setBurnTime(anyShort());

        final TownArmoryService.DoubleSmeltResult result = service.applyDoubleSmelt(
            armoryChunk,
            furnace,
            resultItem.original()
        );

        assertTrue(result.bonusApplied());
        assertFalse(result.insufficientFuel());
        assertSame(resultItem.clonedItem(), result.result());
        assertEquals(2, resultItem.cloneAmount().get());
        assertEquals(80, burnTime.get());
        verify(furnace).update(true, false);
    }

    @Test
    void applyDoubleSmeltLeavesResultNormalWhenExtraFuelIsMissing() {
        final ArmoryConfigSection armoryConfig = ArmoryConfigSection.fromInputStream(new ByteArrayInputStream("""
            double_smelt:
              enabled_by_default: true
              extra_fuel_per_smelt_units: 20
            """.getBytes(StandardCharsets.UTF_8)));
        final TownArmoryService service = new TownArmoryService(this.createPlugin(armoryConfig, null));
        final RTownChunk armoryChunk = this.createArmoryChunk(5);
        final Furnace furnace = Mockito.mock(Furnace.class);
        final AtomicInteger burnTime = new AtomicInteger(10);
        final ItemStackContext resultItem = this.createCloneableItemStack(Material.IRON_INGOT, 1, 64);

        when(furnace.getBurnTime()).thenAnswer(invocation -> (short) burnTime.get());

        final TownArmoryService.DoubleSmeltResult result = service.applyDoubleSmelt(
            armoryChunk,
            furnace,
            resultItem.original()
        );

        assertFalse(result.bonusApplied());
        assertTrue(result.insufficientFuel());
        assertSame(resultItem.clonedItem(), result.result());
        assertEquals(1, resultItem.cloneAmount().get());
        assertEquals(10, burnTime.get());
        verify(furnace, never()).setBurnTime(anyShort());
        verify(furnace, never()).update(true, false);
    }

    @Test
    void freeRepairCooldownReadsPersistedChunkUsage() {
        final ArmoryConfigSection armoryConfig = ArmoryConfigSection.fromInputStream(new ByteArrayInputStream("""
            free_repair:
              cooldown_seconds: 10
            """.getBytes(StandardCharsets.UTF_8)));
        final TownRuntimeService runtimeService = Mockito.mock(TownRuntimeService.class);
        final TownArmoryService service = new TownArmoryService(this.createPlugin(armoryConfig, runtimeService));
        final RTownChunk armoryChunk = this.createArmoryChunk(2);
        final Player player = Mockito.mock(Player.class);
        final UUID playerUuid = UUID.randomUUID();
        final RDTPlayer playerData = new RDTPlayer(playerUuid, armoryChunk.getTown().getTownUUID());

        playerData.setArmoryFreeRepairUsedAt(armoryChunk.getIdentifier(), System.currentTimeMillis() - 2_000L);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(runtimeService.getPlayerData(playerUuid)).thenReturn(playerData);

        final long remainingCooldown = service.getFreeRepairCooldownRemainingMillis(player, armoryChunk);

        assertTrue(remainingCooldown <= 8_000L);
        assertTrue(remainingCooldown >= 7_000L);
    }

    private RDT createPlugin(final ArmoryConfigSection armoryConfig, final TownRuntimeService runtimeService) {
        return new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class)) {
            @Override
            public ArmoryConfigSection getArmoryConfig() {
                return armoryConfig;
            }

            @Override
            public TownRuntimeService getTownRuntimeService() {
                return runtimeService;
            }
        };
    }

    private RTownChunk createArmoryChunk(final int level) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk townChunk = new RTownChunk(town, "world", 2, 3, ChunkType.ARMORY);
        townChunk.setChunkLevel(level);
        town.addChunk(townChunk);
        return townChunk;
    }

    private ItemStackContext createCloneableItemStack(
        final Material material,
        final int amount,
        final int maxStackSize
    ) {
        final ItemStack original = Mockito.mock(ItemStack.class);
        final ItemStack clone = Mockito.mock(ItemStack.class);
        final AtomicInteger cloneAmount = new AtomicInteger(amount);

        when(original.getType()).thenReturn(material);
        when(original.isEmpty()).thenReturn(false);
        when(original.clone()).thenReturn(clone);
        when(clone.getMaxStackSize()).thenReturn(maxStackSize);
        when(clone.getAmount()).thenAnswer(invocation -> cloneAmount.get());
        Mockito.doAnswer(invocation -> {
            cloneAmount.set(invocation.getArgument(0, Integer.class));
            return null;
        }).when(clone).setAmount(Mockito.anyInt());

        return new ItemStackContext(original, clone, cloneAmount);
    }

    private record ItemStackContext(
        ItemStack original,
        ItemStack clonedItem,
        AtomicInteger cloneAmount
    ) {
    }
}
