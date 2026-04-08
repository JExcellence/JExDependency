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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.FuelTank;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maintains FE fuel drain and pooled Security tank supply across towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownFuelService {

    private static final Comparator<RTownChunk> TANK_ORDER = Comparator
        .comparing(RTownChunk::getWorldName, String.CASE_INSENSITIVE_ORDER)
        .thenComparingInt(RTownChunk::getX)
        .thenComparingInt(RTownChunk::getZ);

    private static final double EPSILON = 1.0E-6D;

    private final RDT plugin;
    private CancellableTaskHandle drainTask;

    /**
     * Creates the FE fuel service.
     *
     * @param plugin active plugin runtime
     */
    public TownFuelService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Starts periodic FE fuel drain ticks when fuel is enabled.
     */
    public void start() {
        if (this.plugin.getScheduler() == null || this.drainTask != null || !this.isEnabled()) {
            return;
        }
        final long intervalTicks = Math.max(20L, this.plugin.getSecurityConfig().getFuel().getCalculationIntervalSeconds() * 20L);
        this.drainTask = this.plugin.getScheduler().runRepeating(this::tickFuel, intervalTicks, intervalTicks);
    }

    /**
     * Stops the FE fuel service.
     */
    public void shutdown() {
        if (this.drainTask != null) {
            this.drainTask.cancel();
            this.drainTask = null;
        }
    }

    /**
     * Returns whether FE fuel is enabled in the active Security config.
     *
     * @return {@code true} when FE fuel is enabled
     */
    public boolean isEnabled() {
        return this.plugin.getSecurityConfig().getFuel().isEnabled();
    }

    /**
     * Returns whether FE drain is currently paused because offline protections are active.
     *
     * @param town town to inspect
     * @return {@code true} when offline protections pause FE drain
     */
    public boolean isDrainPaused(final @Nullable RTown town) {
        return town != null && this.isEnabled() && this.shouldPauseDrain(town);
    }

    /**
     * Returns whether a town currently has usable FE available.
     *
     * @param town town to inspect
     * @return {@code true} when the town has buffered FE or stored fuel items
     */
    public boolean isTownPowered(final @Nullable RTown town) {
        if (town == null) {
            return false;
        }
        return this.getTotalFuelUnits(town) > EPSILON;
    }

    /**
     * Returns the pooled FE currently available to a town.
     *
     * @param town town to inspect
     * @return pooled FE from the active buffer plus stored tank items
     */
    public double getTotalFuelUnits(final @Nullable RTown town) {
        if (town == null || !this.isEnabled()) {
            return 0.0D;
        }
        return town.getBufferedFuelUnits() + this.getStoredFuelUnits(town);
    }

    /**
     * Returns the FE currently stored as fuel items inside all valid town tanks.
     *
     * @param town town to inspect
     * @return FE stored inside valid tank inventories
     */
    public double getStoredFuelUnits(final @Nullable RTown town) {
        if (town == null || !this.isEnabled()) {
            return 0.0D;
        }

        double storedUnits = 0.0D;
        for (final RTownChunk securityChunk : this.getOrderedTrackedFuelChunks(town)) {
            for (final ItemStack itemStack : this.sanitizeFuelContents(securityChunk).values()) {
                final SecurityConfigSection.FuelDefinition fuelDefinition = this.plugin.getSecurityConfig().getFuelDefinition(itemStack.getType());
                if (fuelDefinition != null) {
                    storedUnits += fuelDefinition.units() * itemStack.getAmount();
                }
            }
        }
        return storedUnits;
    }

    /**
     * Returns the FE stored as fuel items inside one Security chunk tank.
     *
     * @param townChunk tank-owning Security chunk
     * @return FE stored inside that one tank inventory
     */
    public double getTankFuelUnits(final @Nullable RTownChunk townChunk) {
        if (!this.hasTrackedFuelTank(townChunk)) {
            return 0.0D;
        }

        double storedUnits = 0.0D;
        for (final ItemStack itemStack : this.sanitizeFuelContents(townChunk).values()) {
            final SecurityConfigSection.FuelDefinition fuelDefinition = this.plugin.getSecurityConfig().getFuelDefinition(itemStack.getType());
            if (fuelDefinition != null) {
                storedUnits += fuelDefinition.units() * itemStack.getAmount();
            }
        }
        return storedUnits;
    }

    /**
     * Returns the configured FE drain per hour for one town.
     *
     * @param town town to inspect
     * @return FE drain per hour
     */
    public double getFuelPerHour(final @Nullable RTown town) {
        if (town == null || !this.isEnabled()) {
            return 0.0D;
        }

        final List<RTownChunk> claimedChunks = town.getChunks();
        if (claimedChunks.isEmpty()) {
            return 0.0D;
        }

        final SecurityConfigSection.FuelSettings fuelSettings = this.plugin.getSecurityConfig().getFuel();
        double effectiveChunks = 0.0D;
        for (final RTownChunk chunk : claimedChunks) {
            final SecurityConfigSection.FuelChunkTypeDefinition chunkTypeDefinition =
                this.plugin.getSecurityConfig().getFuelChunkTypeDefinition(chunk.getChunkType());
            final int level = chunk.getChunkType() == ChunkType.NEXUS
                ? town.getNexusLevel()
                : chunk.getChunkLevel();
            effectiveChunks += chunkTypeDefinition.resolveWeight(level);
        }
        effectiveChunks = Math.max(
            effectiveChunks,
            fuelSettings.getMinimumEffectiveChunkRatio() * claimedChunks.size()
        );
        return fuelSettings.getBaseRate()
            * Math.pow(effectiveChunks, fuelSettings.getChunkExponent())
            * (1.0D + (fuelSettings.getTownLevelRate() * Math.max(0, town.getTownLevel() - 1)));
    }

    /**
     * Returns whether one Security chunk has a currently valid fuel tank.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when the tank is valid for FE supply
     */
    public boolean hasValidFuelTank(final @Nullable RTownChunk townChunk) {
        if (!this.hasTrackedFuelTank(townChunk)) {
            return false;
        }

        final Location tankLocation = townChunk.getFuelTankLocation();
        if (tankLocation == null || tankLocation.getWorld() == null) {
            return false;
        }

        final World world = tankLocation.getWorld();
        if (!world.isChunkLoaded(townChunk.getX(), townChunk.getZ())) {
            return true;
        }

        if (!(tankLocation.getBlock().getState() instanceof Chest chest) || !FuelTank.isPlacedFuelTank(this.plugin, chest)) {
            return false;
        }
        return Objects.equals(FuelTank.getTownUUID(this.plugin, chest), townChunk.getTown().getTownUUID())
            && Objects.equals(FuelTank.getWorldName(this.plugin, chest), townChunk.getWorldName())
            && Objects.equals(FuelTank.getChunkX(this.plugin, chest), townChunk.getX())
            && Objects.equals(FuelTank.getChunkZ(this.plugin, chest), townChunk.getZ());
    }

    /**
     * Syncs persisted tank contents back into the live chest when the chunk is loaded.
     *
     * @param townChunk tank-owning Security chunk
     */
    public void syncLiveFuelTankInventory(final @NotNull RTownChunk townChunk) {
        if (!this.hasValidFuelTank(townChunk)) {
            return;
        }

        final Location tankLocation = townChunk.getFuelTankLocation();
        if (tankLocation == null
            || tankLocation.getWorld() == null
            || !tankLocation.getWorld().isChunkLoaded(townChunk.getX(), townChunk.getZ())) {
            return;
        }
        if (!(tankLocation.getBlock().getState() instanceof Chest chest)) {
            return;
        }

        chest.getBlockInventory().clear();
        for (final Map.Entry<String, ItemStack> entry : this.sanitizeFuelContents(townChunk).entrySet()) {
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

    void tickFuel() {
        if (!this.isEnabled() || this.plugin.getTownRepository() == null || this.plugin.getTownRuntimeService() == null) {
            return;
        }

        for (final RTown town : this.plugin.getTownRuntimeService().getTowns()) {
            final boolean changed = this.processTownFuel(town);
            if (changed) {
                this.plugin.getTownRepository().update(town);
            }
        }
    }

    private boolean processTownFuel(final @NotNull RTown town) {
        boolean changed = this.plugin.getTownRuntimeService().ensureCompositeTownState(town);
        final boolean wasPowered = this.isTownPowered(town);

        if (this.shouldPauseDrain(town)) {
            if (!wasPowered && town.getBufferedFuelUnits() > EPSILON) {
                town.setBufferedFuelUnits(0.0D);
                changed = true;
            }
            return changed;
        }

        final double intervalDrain = this.getFuelPerHour(town)
            * this.plugin.getSecurityConfig().getFuel().getCalculationIntervalSeconds()
            / 3600.0D;
        if (intervalDrain <= EPSILON) {
            return changed;
        }

        final double previousBufferedFuelUnits = town.getBufferedFuelUnits();
        town.setBufferedFuelUnits(previousBufferedFuelUnits - intervalDrain);
        changed = changed || Math.abs(town.getBufferedFuelUnits() - previousBufferedFuelUnits) > EPSILON;

        while (town.getBufferedFuelUnits() <= EPSILON) {
            final ConsumedFuel consumedFuel = this.consumeNextFuelItem(town);
            if (!consumedFuel.consumed()) {
                town.setBufferedFuelUnits(0.0D);
                break;
            }
            town.setBufferedFuelUnits(town.getBufferedFuelUnits() + consumedFuel.units());
            this.scheduleLiveFuelTankInventorySync(consumedFuel.sourceChunk());
            changed = true;
        }

        if (town.getBufferedFuelUnits() <= EPSILON) {
            town.setBufferedFuelUnits(0.0D);
        }

        final boolean isPowered = this.isTownPowered(town);
        if (!wasPowered && isPowered) {
            this.plugin.getTownRuntimeService().reconcileLoadedProtectionEntities(town);
        }
        return changed;
    }

    private boolean shouldPauseDrain(final @NotNull RTown town) {
        final SecurityConfigSection.FuelSettings fuelSettings = this.plugin.getSecurityConfig().getFuel();
        return fuelSettings.isOfflineProtection() && !this.hasOnlineTownMember(town);
    }

    private boolean hasOnlineTownMember(final @NotNull RTown town) {
        for (final RDTPlayer member : town.getMembers()) {
            final Player onlinePlayer = Bukkit.getPlayer(member.getIdentifier());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                return true;
            }
        }
        return false;
    }

    private @NotNull List<RTownChunk> getOrderedTrackedFuelChunks(final @NotNull RTown town) {
        final List<RTownChunk> trackedFuelChunks = new ArrayList<>();
        for (final RTownChunk chunk : town.getChunks()) {
            if (this.hasTrackedFuelTank(chunk)) {
                trackedFuelChunks.add(chunk);
            }
        }
        trackedFuelChunks.sort(TANK_ORDER);
        return trackedFuelChunks;
    }

    private @NotNull Map<String, ItemStack> sanitizeFuelContents(final @NotNull RTownChunk townChunk) {
        final LinkedHashMap<String, ItemStack> sanitized = new LinkedHashMap<>();
        for (final Map.Entry<String, ItemStack> entry : townChunk.getFuelTankContents().entrySet()) {
            final ItemStack itemStack = entry.getValue();
            if (entry.getKey() == null
                || itemStack == null
                || itemStack.isEmpty()
                || !this.plugin.getSecurityConfig().isFuelMaterial(itemStack.getType())) {
                continue;
            }
            sanitized.put(entry.getKey(), itemStack.clone());
        }
        return sanitized;
    }

    private @NotNull ConsumedFuel consumeNextFuelItem(final @NotNull RTown town) {
        final List<RTownChunk> trackedFuelChunks = this.getOrderedTrackedFuelChunks(town);
        if (trackedFuelChunks.isEmpty()) {
            return ConsumedFuel.none();
        }

        for (final SecurityConfigSection.FuelDefinition fuelDefinition : this.plugin.getSecurityConfig().getFuel().getFuels().values()) {
            for (final RTownChunk chunk : trackedFuelChunks) {
                final LinkedHashMap<String, ItemStack> contents = new LinkedHashMap<>(this.sanitizeFuelContents(chunk));
                for (final Map.Entry<String, ItemStack> entry : contents.entrySet()) {
                    final ItemStack itemStack = entry.getValue();
                    if (itemStack.getType() != fuelDefinition.material() || itemStack.getAmount() <= 0) {
                        continue;
                    }

                    if (itemStack.getAmount() == 1) {
                        contents.remove(entry.getKey());
                    } else {
                        final ItemStack updatedStack = itemStack.clone();
                        updatedStack.setAmount(itemStack.getAmount() - 1);
                        contents.put(entry.getKey(), updatedStack);
                    }
                    chunk.setFuelTankContents(contents);
                    return new ConsumedFuel(true, fuelDefinition.units(), chunk);
                }
            }
        }
        return ConsumedFuel.none();
    }

    private boolean hasTrackedFuelTank(final @Nullable RTownChunk townChunk) {
        if (townChunk == null || townChunk.getChunkType() != ChunkType.SECURITY || !townChunk.hasFuelTank()) {
            return false;
        }

        final Location tankLocation = townChunk.getFuelTankLocation();
        final Location markerLocation = townChunk.getChunkBlockLocation();
        if (tankLocation == null
            || tankLocation.getWorld() == null
            || markerLocation == null
            || markerLocation.getWorld() == null
            || !Objects.equals(tankLocation.getWorld().getName(), townChunk.getWorldName())
            || !Objects.equals(markerLocation.getWorld().getName(), townChunk.getWorldName())
            || !Objects.equals(markerLocation.getWorld().getName(), tankLocation.getWorld().getName())) {
            return false;
        }

        if (TownRuntimeService.toChunkCoordinate(tankLocation.getBlockX()) != townChunk.getX()
            || TownRuntimeService.toChunkCoordinate(tankLocation.getBlockZ()) != townChunk.getZ()) {
            return false;
        }

        final int radius = this.plugin.getSecurityConfig().getFuel().getTankPlacementRadiusBlocks();
        return distanceSquared(markerLocation, tankLocation) <= ((double) radius * (double) radius);
    }

    private void scheduleLiveFuelTankInventorySync(final @Nullable RTownChunk townChunk) {
        if (townChunk == null || this.plugin.getScheduler() == null) {
            return;
        }

        final Location tankLocation = townChunk.getFuelTankLocation();
        if (tankLocation == null || tankLocation.getWorld() == null) {
            return;
        }

        this.plugin.getScheduler().runAtLocation(tankLocation, () -> this.syncLiveFuelTankInventory(townChunk));
    }

    private static double distanceSquared(final @NotNull Location first, final @NotNull Location second) {
        final double deltaX = first.getX() - second.getX();
        final double deltaY = first.getY() - second.getY();
        final double deltaZ = first.getZ() - second.getZ();
        return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ);
    }

    private record ConsumedFuel(boolean consumed, double units, @Nullable RTownChunk sourceChunk) {

        private static @NotNull ConsumedFuel none() {
            return new ConsumedFuel(false, 0.0D, null);
        }
    }
}
