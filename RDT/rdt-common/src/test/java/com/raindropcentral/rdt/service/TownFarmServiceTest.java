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
import com.raindropcentral.rdt.configs.FarmConfigSection;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownFarmServiceTest {

    @Test
    void growthBoostRespectsFarmUnlocksAndToggleState() {
        final TownFarmService service = new TownFarmService(this.createPlugin(FarmConfigSection.createDefault()));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);

        assertEquals(1.0D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(0, service.resolveAdditionalGrowthStages(farmChunk, 0.0D));

        farmChunk.setChunkLevel(2);
        assertEquals(2.0D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(1, service.resolveAdditionalGrowthStages(farmChunk, 0.0D));

        farmChunk.setFarmGrowthEnabled(Boolean.FALSE);
        assertEquals(1.0D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(0, service.resolveAdditionalGrowthStages(farmChunk, 0.0D));

        farmChunk.setChunkLevel(4);
        farmChunk.setFarmGrowthEnabled(Boolean.TRUE);
        assertEquals(3.0D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(2, service.resolveAdditionalGrowthStages(farmChunk, 0.0D));
        assertEquals(1, service.resolveHarvestMultiplier(farmChunk));

        farmChunk.setChunkLevel(5);
        assertEquals(2, service.resolveHarvestMultiplier(farmChunk));
    }

    @Test
    void fractionalGrowthSpeedMultiplierUsesRandomRollForPartialStage() {
        final FarmConfigSection farmConfig = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            growth:
              tier_1_growth_speed_multiplier: 1.5
              tier_2_growth_speed_multiplier: 2.25
            """.getBytes(StandardCharsets.UTF_8)));
        final TownFarmService service = new TownFarmService(this.createPlugin(farmConfig));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);

        farmChunk.setChunkLevel(2);
        assertEquals(1.5D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(1, service.resolveAdditionalGrowthStages(farmChunk, 0.25D));
        assertEquals(0, service.resolveAdditionalGrowthStages(farmChunk, 0.75D));

        farmChunk.setChunkLevel(4);
        assertEquals(2.25D, service.resolveGrowthSpeedMultiplier(farmChunk));
        assertEquals(2, service.resolveAdditionalGrowthStages(farmChunk, 0.20D));
        assertEquals(1, service.resolveAdditionalGrowthStages(farmChunk, 0.80D));
    }

    @Test
    void cropFailureUsesConfiguredProbabilityOnlyOutsideFarmChunks() {
        final TownFarmService service = new TownFarmService(this.createPlugin(FarmConfigSection.createDefault()));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);
        final RTownChunk defaultChunk = new RTownChunk(town, "world", 4, 5, ChunkType.DEFAULT);

        assertFalse(service.isCropFailureEnabled(farmChunk));
        assertEquals(0.0D, service.resolveCropFailureRate(farmChunk));
        assertFalse(service.shouldFailCropGrowth(farmChunk, 0.0D));

        assertTrue(service.isCropFailureEnabled(defaultChunk));
        assertEquals(0.5D, service.resolveCropFailureRate(defaultChunk));
        assertTrue(service.shouldFailCropGrowth(defaultChunk, 0.25D));
        assertFalse(service.shouldFailCropGrowth(defaultChunk, 0.75D));
        assertTrue(service.isCropFailureEnabled(null));
        assertEquals(0.5D, service.resolveCropFailureRate(null));
        assertTrue(service.shouldFailCropGrowth(null, 0.25D));
    }

    @Test
    void cropFailureCanBeDisabledByConfig() {
        final FarmConfigSection farmConfig = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            crop_failure:
              enabled: false
              failure_rate: 1.0
            """.getBytes(StandardCharsets.UTF_8)));
        final TownFarmService service = new TownFarmService(this.createPlugin(farmConfig));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);

        assertFalse(service.isCropFailureEnabled(farmChunk));
        assertEquals(0.0D, service.resolveCropFailureRate(farmChunk));
        assertFalse(service.shouldFailCropGrowth(farmChunk, 0.0D));
    }

    @Test
    void consumeReplantSeedUsesInventoryFirstByDefault() {
        final TownFarmService service = new TownFarmService(this.createPlugin(FarmConfigSection.createDefault()));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);
        final InventoryContext inventoryContext = this.createInventoryContext(this.mockItemStack(Material.CARROT, 2));

        farmChunk.setChunkLevel(3);

        assertTrue(service.consumeReplantSeed(inventoryContext.player(), farmChunk, Material.CARROTS));
        assertEquals(1, inventoryContext.contents().get()[0].getAmount());
    }

    @Test
    void consumeReplantSeedFallsBackToSeedBoxStorage() {
        final TownFarmService service = new TownFarmService(this.createPlugin(FarmConfigSection.createDefault()));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);
        final InventoryContext inventoryContext = this.createInventoryContext();
        final World world = Mockito.mock(World.class);

        Mockito.when(world.getName()).thenReturn("world");
        Mockito.when(world.isChunkLoaded(2, 3)).thenReturn(false);
        farmChunk.setChunkLevel(3);
        farmChunk.setChunkBlockLocation(new Location(world, 32.0D, 64.0D, 48.0D));
        farmChunk.setSeedBoxLocation(new Location(world, 33.0D, 64.0D, 49.0D));
        farmChunk.setSeedBoxContents(Map.of("0", this.mockItemStack(Material.CARROT, 2)));

        assertTrue(service.consumeReplantSeed(inventoryContext.player(), farmChunk, Material.CARROTS));
        assertEquals(1, farmChunk.getSeedBoxContents().get("0").getAmount());
    }

    @Test
    void consumeReplantSeedCanPrioritizeSeedBoxBeforeInventory() {
        final FarmConfigSection farmConfig = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            replant:
              unlock_level: 3
              default_source_priority: SEED_BOX_FIRST
            """.getBytes(StandardCharsets.UTF_8)));
        final TownFarmService service = new TownFarmService(this.createPlugin(farmConfig));
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Town", null);
        final RTownChunk farmChunk = new RTownChunk(town, "world", 2, 3, ChunkType.FARM);
        final InventoryContext inventoryContext = this.createInventoryContext(this.mockItemStack(Material.CARROT, 2));
        final World world = Mockito.mock(World.class);

        Mockito.when(world.getName()).thenReturn("world");
        Mockito.when(world.isChunkLoaded(2, 3)).thenReturn(false);
        farmChunk.setChunkLevel(3);
        farmChunk.setChunkBlockLocation(new Location(world, 32.0D, 64.0D, 48.0D));
        farmChunk.setSeedBoxLocation(new Location(world, 33.0D, 64.0D, 49.0D));
        farmChunk.setSeedBoxContents(Map.of("0", this.mockItemStack(Material.CARROT, 2)));

        assertEquals(FarmReplantPriority.SEED_BOX_FIRST, service.resolveReplantPriority(farmChunk));
        assertTrue(service.consumeReplantSeed(inventoryContext.player(), farmChunk, Material.CARROTS));
        assertEquals(2, inventoryContext.contents().get()[0].getAmount());
        assertEquals(1, farmChunk.getSeedBoxContents().get("0").getAmount());
    }

    private RDT createPlugin(final FarmConfigSection farmConfig) {
        return new RDT(Mockito.mock(JavaPlugin.class), "test", Mockito.mock(TownService.class)) {
            @Override
            public FarmConfigSection getFarmConfig() {
                return farmConfig;
            }
        };
    }

    private InventoryContext createInventoryContext(final ItemStack... contents) {
        final Player player = Mockito.mock(Player.class);
        final PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        final AtomicReference<ItemStack[]> inventoryContents = new AtomicReference<>(contents);

        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(inventory.getContents()).thenAnswer(invocation -> inventoryContents.get());
        Mockito.doAnswer(invocation -> {
            inventoryContents.set(invocation.getArgument(0, ItemStack[].class));
            return null;
        }).when(inventory).setContents(Mockito.any(ItemStack[].class));

        return new InventoryContext(player, inventoryContents);
    }

    private ItemStack mockItemStack(final Material material, final int initialAmount) {
        final ItemStack itemStack = Mockito.mock(ItemStack.class);
        final AtomicInteger amount = new AtomicInteger(initialAmount);

        Mockito.lenient().when(itemStack.getType()).thenReturn(material);
        Mockito.lenient().when(itemStack.getAmount()).thenAnswer(invocation -> amount.get());
        Mockito.lenient().when(itemStack.isEmpty()).thenAnswer(invocation -> amount.get() <= 0);
        Mockito.lenient().doAnswer(invocation -> {
            amount.set(invocation.getArgument(0, Integer.class));
            return null;
        }).when(itemStack).setAmount(Mockito.anyInt());
        Mockito.lenient().when(itemStack.clone()).thenReturn(itemStack);
        return itemStack;
    }

    private record InventoryContext(Player player, AtomicReference<ItemStack[]> contents) {
    }
}
