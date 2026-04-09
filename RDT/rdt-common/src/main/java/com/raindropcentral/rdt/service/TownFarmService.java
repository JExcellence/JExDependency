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
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.SeedBox;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maintains Farm chunk enhancement state such as growth boosts, seed-box syncing, and auto-replant sources.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownFarmService {

    private final RDT plugin;

    /**
     * Creates the Farm chunk enhancement service.
     *
     * @param plugin active plugin runtime
     */
    public TownFarmService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns whether the supplied material is one of the supported same-block Farm harvest crops.
     *
     * @param material material to inspect
     * @return {@code true} when the crop supports auto-replant and double harvest
     */
    public boolean isSupportedHarvestCrop(final @Nullable Material material) {
        return resolveSeedMaterial(material) != null;
    }

    /**
     * Returns whether the supplied block is one of the supported mature Farm harvest crops.
     *
     * @param block block to inspect
     * @return {@code true} when the block is a supported mature crop
     */
    public boolean isSupportedMatureCrop(final @Nullable Block block) {
        if (block == null || !this.isSupportedHarvestCrop(block.getType()) || !(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    /**
     * Returns the effective configured growth-speed multiplier for one Farm chunk.
     *
     * @param townChunk chunk to inspect
     * @return growth-speed multiplier, or {@code 1.0} when growth is locked or disabled
     */
    public double resolveGrowthSpeedMultiplier(final @Nullable RTownChunk townChunk) {
        if (!this.isFarmChunk(townChunk)) {
            return 1.0D;
        }

        final FarmConfigSection.GrowthSettings growthSettings = this.plugin.getFarmConfig().getGrowth();
        if (!growthSettings.isUnlocked(townChunk.getChunkLevel())
            || !townChunk.isFarmGrowthEnabled(growthSettings.enabledByDefault())) {
            return 1.0D;
        }
        return growthSettings.resolveGrowthSpeedMultiplier(townChunk.getChunkLevel());
    }

    /**
     * Resolves the additional crop stages one natural growth event should apply for one Farm chunk.
     *
     * <p>The vanilla event already advances the crop by one stage. This method returns only the
     * extra stages needed to approximate the configured growth-speed multiplier.</p>
     *
     * @param townChunk chunk to inspect
     * @param fractionalRoll random fractional roll in the range {@code [0, 1)}
     * @return additional growth stages to apply for this event
     */
    public int resolveAdditionalGrowthStages(final @Nullable RTownChunk townChunk, final double fractionalRoll) {
        final double speedMultiplier = this.resolveGrowthSpeedMultiplier(townChunk);
        if (speedMultiplier <= 1.0D) {
            return 0;
        }

        final double additionalStages = Math.max(0.0D, speedMultiplier - 1.0D);
        final int guaranteedStages = (int) Math.floor(additionalStages);
        final double fractionalStage = additionalStages - guaranteedStages;
        return guaranteedStages + (fractionalRoll < fractionalStage ? 1 : 0);
    }

    /**
     * Returns whether natural crop-growth failure is currently active outside Farm chunks.
     *
     * @param townChunk chunk to inspect, or {@code null} when the location is unclaimed
     * @return {@code true} when natural crop-growth failure is enabled at this location
     */
    public boolean isCropFailureEnabled(final @Nullable RTownChunk townChunk) {
        return this.resolveCropFailureRate(townChunk) > 0.0D;
    }

    /**
     * Returns the effective configured crop-failure rate outside Farm chunks.
     *
     * @param townChunk chunk to inspect, or {@code null} when the location is unclaimed
     * @return crop-failure rate, or {@code 0.0} when the feature is disabled or the location is a Farm chunk
     */
    public double resolveCropFailureRate(final @Nullable RTownChunk townChunk) {
        if (this.isFarmChunk(townChunk)) {
            return 0.0D;
        }

        final FarmConfigSection.CropFailureSettings cropFailureSettings = this.plugin.getFarmConfig().getCropFailure();
        return cropFailureSettings.enabled() ? cropFailureSettings.failureRate() : 0.0D;
    }

    /**
     * Returns whether one natural crop-growth tick should fail outside Farm chunks.
     *
     * @param townChunk chunk to inspect, or {@code null} when the location is unclaimed
     * @param roll random roll in the range {@code [0, 1)}
     * @return {@code true} when the natural grow tick should fail at this location
     */
    public boolean shouldFailCropGrowth(final @Nullable RTownChunk townChunk, final double roll) {
        final double cropFailureRate = this.resolveCropFailureRate(townChunk);
        return cropFailureRate > 0.0D && roll < cropFailureRate;
    }

    /**
     * Returns whether auto-replanting is currently active for one Farm chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when auto-replanting is unlocked and enabled
     */
    public boolean isAutoReplantEnabled(final @Nullable RTownChunk townChunk) {
        if (!this.isFarmChunk(townChunk)) {
            return false;
        }

        final FarmConfigSection.ReplantSettings replantSettings = this.plugin.getFarmConfig().getReplant();
        return replantSettings.isUnlocked(townChunk.getChunkLevel())
            && townChunk.isFarmAutoReplantEnabled(replantSettings.enabledByDefault());
    }

    /**
     * Returns the effective Farm seed-consumption priority for one chunk.
     *
     * @param townChunk chunk to inspect
     * @return effective seed-consumption priority
     */
    public @NotNull FarmReplantPriority resolveReplantPriority(final @Nullable RTownChunk townChunk) {
        final FarmReplantPriority fallback = this.plugin.getFarmConfig().getReplant().defaultSourcePriority();
        if (!this.isFarmChunk(townChunk)) {
            return fallback;
        }
        return townChunk.getFarmReplantPriority(fallback);
    }

    /**
     * Returns the effective configured harvest multiplier for one Farm chunk.
     *
     * @param townChunk chunk to inspect
     * @return harvest multiplier, or {@code 1} when locked
     */
    public int resolveHarvestMultiplier(final @Nullable RTownChunk townChunk) {
        if (!this.isFarmChunk(townChunk)) {
            return 1;
        }

        final FarmConfigSection.DoubleHarvestSettings doubleHarvestSettings = this.plugin.getFarmConfig().getDoubleHarvest();
        return doubleHarvestSettings.isUnlocked(townChunk.getChunkLevel())
            ? Math.max(1, doubleHarvestSettings.multiplier())
            : 1;
    }

    /**
     * Consumes one matching replant seed for a supported harvested crop using the chunk's configured priority.
     *
     * @param player harvesting player
     * @param townChunk Farm chunk being harvested
     * @param harvestedMaterial harvested crop material
     * @return {@code true} when one matching seed was consumed and replanting may proceed
     */
    public boolean consumeReplantSeed(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull Material harvestedMaterial
    ) {
        final Material seedMaterial = resolveSeedMaterial(harvestedMaterial);
        if (seedMaterial == null || !this.isAutoReplantEnabled(townChunk)) {
            return false;
        }

        return switch (this.resolveReplantPriority(townChunk)) {
            case INVENTORY_FIRST -> this.consumeInventorySeed(player, seedMaterial)
                || this.consumeSeedBoxSeed(townChunk, seedMaterial);
            case SEED_BOX_FIRST -> this.consumeSeedBoxSeed(townChunk, seedMaterial)
                || this.consumeInventorySeed(player, seedMaterial);
        };
    }

    /**
     * Returns whether one Farm chunk has a currently valid seed box.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when the tracked seed box is valid for storage access
     */
    public boolean hasValidSeedBox(final @Nullable RTownChunk townChunk) {
        if (!this.hasTrackedSeedBox(townChunk)) {
            return false;
        }

        final Location seedBoxLocation = townChunk.getSeedBoxLocation();
        if (seedBoxLocation == null || seedBoxLocation.getWorld() == null) {
            return false;
        }

        final World world = seedBoxLocation.getWorld();
        if (!world.isChunkLoaded(townChunk.getX(), townChunk.getZ())) {
            return true;
        }
        if (!(seedBoxLocation.getBlock().getState() instanceof Chest chest) || !SeedBox.isPlacedSeedBox(this.plugin, chest)) {
            return false;
        }
        return Objects.equals(SeedBox.getTownUUID(this.plugin, chest), townChunk.getTown().getTownUUID())
            && Objects.equals(SeedBox.getWorldName(this.plugin, chest), townChunk.getWorldName())
            && Objects.equals(SeedBox.getChunkX(this.plugin, chest), townChunk.getX())
            && Objects.equals(SeedBox.getChunkZ(this.plugin, chest), townChunk.getZ());
    }

    /**
     * Restores authoritative persisted seed-box inventories when Farm chunks load.
     *
     * @param townChunk seed-box-owning Farm chunk
     */
    public void syncLiveSeedBoxInventory(final @NotNull RTownChunk townChunk) {
        if (!this.hasValidSeedBox(townChunk)) {
            return;
        }

        final Location seedBoxLocation = townChunk.getSeedBoxLocation();
        if (seedBoxLocation == null
            || seedBoxLocation.getWorld() == null
            || !seedBoxLocation.getWorld().isChunkLoaded(townChunk.getX(), townChunk.getZ())) {
            return;
        }
        if (!(seedBoxLocation.getBlock().getState() instanceof Chest chest)) {
            return;
        }

        chest.getBlockInventory().clear();
        for (final Map.Entry<String, ItemStack> entry : this.sanitizeSeedContents(townChunk).entrySet()) {
            try {
                final int slot = Integer.parseInt(entry.getKey());
                if (slot >= 0 && slot < chest.getBlockInventory().getSize()) {
                    chest.getBlockInventory().setItem(slot, entry.getValue().clone());
                }
            } catch (final NumberFormatException ignored) {
            }
        }
        chest.update(true, false);
    }

    /**
     * Resolves the matching seed material for one supported harvested crop.
     *
     * @param harvestedMaterial harvested crop material
     * @return matching seed material, or {@code null} when the crop is unsupported
     */
    public static @Nullable Material resolveSeedMaterial(final @Nullable Material harvestedMaterial) {
        if (harvestedMaterial == null) {
            return null;
        }
        return switch (harvestedMaterial) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            default -> null;
        };
    }

    /**
     * Formats one configured Farm growth-speed multiplier for player-facing text.
     *
     * @param multiplier configured growth-speed multiplier
     * @return normalized multiplier string without unnecessary trailing zeroes
     */
    public static @NotNull String formatGrowthSpeedMultiplier(final double multiplier) {
        return BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString();
    }

    private boolean isFarmChunk(final @Nullable RTownChunk townChunk) {
        return townChunk != null && townChunk.getChunkType() == ChunkType.FARM;
    }

    private boolean consumeInventorySeed(final @NotNull Player player, final @NotNull Material seedMaterial) {
        final ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType() != seedMaterial) {
                continue;
            }

            if (itemStack.getAmount() <= 1) {
                contents[slot] = null;
            } else {
                itemStack.setAmount(itemStack.getAmount() - 1);
            }
            player.getInventory().setContents(contents);
            return true;
        }
        return false;
    }

    private boolean consumeSeedBoxSeed(final @NotNull RTownChunk townChunk, final @NotNull Material seedMaterial) {
        if (!this.hasTrackedSeedBox(townChunk) || !this.plugin.getFarmConfig().isAllowedSeedMaterial(seedMaterial)) {
            return false;
        }

        final Location seedBoxLocation = townChunk.getSeedBoxLocation();
        if (seedBoxLocation != null
            && seedBoxLocation.getWorld() != null
            && seedBoxLocation.getWorld().isChunkLoaded(townChunk.getX(), townChunk.getZ())
            && seedBoxLocation.getBlock().getState() instanceof Chest chest
            && SeedBox.isPlacedSeedBox(this.plugin, chest)) {
            for (int slot = 0; slot < chest.getBlockInventory().getSize(); slot++) {
                final ItemStack itemStack = chest.getBlockInventory().getItem(slot);
                if (itemStack == null || itemStack.isEmpty() || itemStack.getType() != seedMaterial) {
                    continue;
                }

                if (itemStack.getAmount() <= 1) {
                    chest.getBlockInventory().setItem(slot, null);
                } else {
                    final ItemStack updatedStack = itemStack.clone();
                    updatedStack.setAmount(itemStack.getAmount() - 1);
                    chest.getBlockInventory().setItem(slot, updatedStack);
                }
                chest.update(true, false);
                this.persistSeedBoxSnapshot(townChunk, this.snapshotSeedContents(chest.getBlockInventory()));
                return true;
            }
            return false;
        }

        final LinkedHashMap<String, ItemStack> updatedContents = new LinkedHashMap<>(this.sanitizeSeedContents(townChunk));
        for (final Map.Entry<String, ItemStack> entry : updatedContents.entrySet()) {
            final ItemStack itemStack = entry.getValue();
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType() != seedMaterial) {
                continue;
            }

            if (itemStack.getAmount() <= 1) {
                updatedContents.remove(entry.getKey());
            } else {
                final ItemStack updatedStack = itemStack.clone();
                updatedStack.setAmount(itemStack.getAmount() - 1);
                updatedContents.put(entry.getKey(), updatedStack);
            }
            this.persistSeedBoxSnapshot(townChunk, updatedContents);
            return true;
        }
        return false;
    }

    private void persistSeedBoxSnapshot(
        final @NotNull RTownChunk townChunk,
        final @NotNull Map<String, ItemStack> seedContents
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService != null) {
            runtimeService.syncSeedBoxContents(townChunk, seedContents);
            return;
        }

        townChunk.setSeedBoxContents(seedContents);
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(townChunk.getTown());
        }
    }

    private @NotNull Map<String, ItemStack> sanitizeSeedContents(final @NotNull RTownChunk townChunk) {
        final LinkedHashMap<String, ItemStack> sanitized = new LinkedHashMap<>();
        for (final Map.Entry<String, ItemStack> entry : townChunk.getSeedBoxContents().entrySet()) {
            final ItemStack itemStack = entry.getValue();
            if (entry.getKey() == null
                || itemStack == null
                || itemStack.isEmpty()
                || !this.plugin.getFarmConfig().isAllowedSeedMaterial(itemStack.getType())) {
                continue;
            }
            sanitized.put(entry.getKey(), itemStack.clone());
        }
        return sanitized;
    }

    private @NotNull Map<String, ItemStack> snapshotSeedContents(final @NotNull org.bukkit.inventory.Inventory inventory) {
        final Map<String, ItemStack> contents = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack itemStack = inventory.getItem(slot);
            if (itemStack == null || itemStack.isEmpty() || !this.plugin.getFarmConfig().isAllowedSeedMaterial(itemStack.getType())) {
                continue;
            }
            contents.put(String.valueOf(slot), itemStack.clone());
        }
        return contents;
    }

    private boolean hasTrackedSeedBox(final @Nullable RTownChunk townChunk) {
        if (townChunk == null || townChunk.getChunkType() != ChunkType.FARM || !townChunk.hasSeedBox()) {
            return false;
        }

        final Location seedBoxLocation = townChunk.getSeedBoxLocation();
        final Location markerLocation = townChunk.getChunkBlockLocation();
        if (seedBoxLocation == null
            || seedBoxLocation.getWorld() == null
            || markerLocation == null
            || markerLocation.getWorld() == null
            || !Objects.equals(seedBoxLocation.getWorld().getName(), townChunk.getWorldName())
            || !Objects.equals(markerLocation.getWorld().getName(), townChunk.getWorldName())
            || !Objects.equals(markerLocation.getWorld().getName(), seedBoxLocation.getWorld().getName())) {
            return false;
        }

        if (TownRuntimeService.toChunkCoordinate(seedBoxLocation.getBlockX()) != townChunk.getX()
            || TownRuntimeService.toChunkCoordinate(seedBoxLocation.getBlockZ()) != townChunk.getZ()) {
            return false;
        }

        final int radius = this.plugin.getFarmConfig().getSeedBox().placementRadiusBlocks();
        return distanceSquared(markerLocation, seedBoxLocation) <= ((double) radius * (double) radius);
    }

    private static double distanceSquared(final @NotNull Location first, final @NotNull Location second) {
        final double deltaX = first.getX() - second.getX();
        final double deltaY = first.getY() - second.getY();
        final double deltaZ = first.getZ() - second.getZ();
        return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
    }
}
