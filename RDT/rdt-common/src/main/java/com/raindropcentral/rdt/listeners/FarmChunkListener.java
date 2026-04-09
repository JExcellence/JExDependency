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
import com.raindropcentral.rdt.service.TownFarmService;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies non-Farm crop-growth failure plus Farm chunk growth boosts, auto-replant, and double-harvest enhancements.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public final class FarmChunkListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the Farm chunk enhancement listener.
     *
     * @param plugin active plugin runtime
     */
    public FarmChunkListener(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Applies natural crop-growth failure outside Farm chunks and growth boosts inside Farm chunks.
     *
     * @param event natural block-growth event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(final @NotNull BlockGrowEvent event) {
        final TownFarmService farmService = this.plugin.getTownFarmService();
        final RTownChunk townChunk = this.resolveTownChunk(event.getBlock().getLocation());
        if (farmService == null || !(event.getNewState().getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (farmService.shouldFailCropGrowth(townChunk, ThreadLocalRandom.current().nextDouble())) {
            event.setCancelled(true);
            return;
        }

        final int additionalGrowthStages = farmService.resolveAdditionalGrowthStages(
            townChunk,
            ThreadLocalRandom.current().nextDouble()
        );
        if (additionalGrowthStages <= 0 || ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }

        ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + additionalGrowthStages));
        event.getNewState().setBlockData(ageable);
    }

    /**
     * Consumes one matching seed and schedules replanting after eligible mature Farm crops are harvested.
     *
     * @param event crop-break event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final TownFarmService farmService = this.plugin.getTownFarmService();
        final Block harvestedBlock = event.getBlock();
        final RTownChunk townChunk = this.resolveFarmChunk(harvestedBlock.getLocation());
        if (farmService == null || townChunk == null || !farmService.isSupportedMatureCrop(harvestedBlock)) {
            return;
        }
        if (!farmService.consumeReplantSeed(event.getPlayer(), townChunk, harvestedBlock.getType())) {
            return;
        }

        final ISchedulerAdapter scheduler = this.resolvePlatformScheduler();
        if (scheduler == null) {
            return;
        }

        final Location replantLocation = harvestedBlock.getLocation();
        final Material replantedMaterial = harvestedBlock.getType();
        scheduler.runDelayed(() -> scheduler.runAtLocation(replantLocation, () -> this.replantCrop(replantLocation, replantedMaterial)), 1L);
    }

    /**
     * Duplicates eligible harvested crop drops inside unlocked Farm chunks.
     *
     * @param event block-drop event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDropItem(final @NotNull BlockDropItemEvent event) {
        final TownFarmService farmService = this.plugin.getTownFarmService();
        final RTownChunk townChunk = this.resolveFarmChunk(event.getBlockState().getLocation());
        if (farmService == null
            || townChunk == null
            || !farmService.isSupportedHarvestCrop(event.getBlockState().getType())
            || !(event.getBlockState().getBlockData() instanceof Ageable ageable)
            || ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        final int multiplier = farmService.resolveHarvestMultiplier(townChunk);
        if (multiplier <= 1) {
            return;
        }

        final Location dropLocation = event.getBlockState().getLocation();
        if (dropLocation.getWorld() == null) {
            return;
        }
        for (final Item droppedItem : event.getItems()) {
            if (droppedItem == null || !droppedItem.isValid()) {
                continue;
            }
            for (int multiplierIndex = 1; multiplierIndex < multiplier; multiplierIndex++) {
                dropLocation.getWorld().dropItemNaturally(dropLocation, droppedItem.getItemStack().clone());
            }
        }
    }

    private void replantCrop(final @NotNull Location location, final @NotNull Material cropMaterial) {
        final Block block = location.getBlock();
        if (block.getType() != Material.AIR || block.getRelative(0, -1, 0).getType() != Material.FARMLAND) {
            return;
        }

        block.setType(cropMaterial, false);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }
    }

    private @Nullable RTownChunk resolveFarmChunk(final @Nullable Location location) {
        final RTownChunk townChunk = this.resolveTownChunk(location);
        return townChunk != null && townChunk.getChunkType() == com.raindropcentral.rdt.utils.ChunkType.FARM ? townChunk : null;
    }

    private @Nullable RTownChunk resolveTownChunk(final @Nullable Location location) {
        if (location == null || this.plugin.getTownRuntimeService() == null) {
            return null;
        }
        return this.plugin.getTownRuntimeService().getChunkAt(location);
    }

    private @Nullable ISchedulerAdapter resolvePlatformScheduler() {
        if (this.plugin.getPlatform() != null) {
            return this.plugin.getPlatform().getScheduler();
        }
        return this.plugin.getScheduler();
    }
}
