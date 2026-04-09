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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.items.CacheChest;
import com.raindropcentral.rdt.service.TownBankService;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Enforces BANK cache-chest placement, locking, and storage synchronization.
 *
 * <p>The placed cache chest acts as the physical representation of one town-wide cache unlocked by
 * BANK progression. This listener validates placement, blocks direct break access, acquires the
 * shared town-bank viewer lock for physical opens, and keeps the live chest synchronized with the
 * persisted town cache snapshot.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public final class BankCacheListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the BANK cache listener.
     *
     * @param plugin active plugin runtime
     */
    public BankCacheListener(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Validates and binds placed cache chests while also preventing double-chest merges.
     *
     * @param event placement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        if (CacheChest.equals(this.plugin, event.getItemInHand())) {
            this.handleCachePlacement(event);
            return;
        }

        final Material placedMaterial = event.getBlockPlaced().getType();
        if ((placedMaterial == Material.CHEST || placedMaterial == Material.TRAPPED_CHEST)
            && this.isAdjacentToCache(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.double_chest", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    /**
     * Prevents the placed cache chest from being broken directly.
     *
     * @param event break event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        if (!this.isPlacedCacheChest(event.getBlock())) {
            return;
        }

        event.setCancelled(true);
        new I18n.Builder("cache_chest.break.denied", event.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    /**
     * Enforces bank-view permissions and the single-viewer lock when players open the physical
     * cache chest.
     *
     * @param event open event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(final @NotNull InventoryOpenEvent event) {
        final TownBankService bankService = this.plugin.getTownBankService();
        if (bankService == null || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        final RTown town = this.resolveCacheTown(event.getInventory());
        if (town == null) {
            return;
        }
        if (!bankService.canViewBank(player, town)) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.access.no_permission", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!bankService.acquireBankAccess(town.getTownUUID(), player.getUniqueId())) {
            event.setCancelled(true);
            new I18n.Builder("town_bank_shared.messages.in_use", player)
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    /**
     * Persists the latest cache contents and releases the bank-view lock when the physical chest
     * closes.
     *
     * @param event close event
     */
    @EventHandler
    public void onInventoryClose(final @NotNull InventoryCloseEvent event) {
        final TownBankService bankService = this.plugin.getTownBankService();
        final RTown town = bankService == null ? null : this.resolveCacheTown(event.getInventory());
        if (bankService == null || town == null || !(event.getPlayer() instanceof Player player)) {
            return;
        }

        bankService.saveCacheStorage(town.getTownUUID(), event.getInventory().getContents());
        bankService.releaseBankAccess(town.getTownUUID(), player.getUniqueId());
    }

    /**
     * Blocks hopper or automation transfer while the cache is locked and otherwise persists hopper
     * mutations back to town storage after the transfer completes.
     *
     * @param event inventory transfer event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(final @NotNull InventoryMoveItemEvent event) {
        final TownBankService bankService = this.plugin.getTownBankService();
        if (bankService == null) {
            return;
        }

        final RTown sourceTown = this.resolveCacheTown(event.getSource());
        final RTown destinationTown = this.resolveCacheTown(event.getDestination());
        if (sourceTown == null && destinationTown == null) {
            return;
        }

        final UUID townUuid = sourceTown != null ? sourceTown.getTownUUID() : destinationTown.getTownUUID();
        if (bankService.isBankAccessLocked(townUuid)) {
            event.setCancelled(true);
            return;
        }

        if (sourceTown != null) {
            this.scheduleCacheSync(sourceTown.getTownUUID(), event.getSource());
        }
        if (destinationTown != null && (sourceTown == null || !Objects.equals(sourceTown.getTownUUID(), destinationTown.getTownUUID()))) {
            this.scheduleCacheSync(destinationTown.getTownUUID(), event.getDestination());
        }
    }

    /**
     * Re-synchronizes the authoritative cache contents when the host chunk loads.
     *
     * @param event chunk-load event
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        final TownBankService bankService = this.plugin.getTownBankService();
        final var runtimeService = this.plugin.getTownRuntimeService();
        if (bankService == null || runtimeService == null) {
            return;
        }

        for (final RTown town : runtimeService.getTowns()) {
            final Location cacheLocation = town.getBankCacheLocation();
            if (cacheLocation == null
                || cacheLocation.getWorld() == null
                || !Objects.equals(cacheLocation.getWorld().getName(), event.getWorld().getName())) {
                continue;
            }
            if (cacheLocation.getChunk().getX() != event.getChunk().getX()
                || cacheLocation.getChunk().getZ() != event.getChunk().getZ()) {
                continue;
            }
            bankService.syncLiveCacheInventory(town);
        }
    }

    private void handleCachePlacement(final @NotNull BlockPlaceEvent event) {
        final TownBankService bankService = this.plugin.getTownBankService();
        if (bankService == null) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        final UUID townUuid = CacheChest.getTownUuid(this.plugin, event.getItemInHand());
        final RTown town = townUuid == null ? null : bankService.getTown(townUuid);
        if (town == null || !bankService.canManageCachePlacement(player, town)) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.denied", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!bankService.isCacheUnlocked(town)) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (bankService.hasPlacedCache(town)) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.already_placed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (this.hasAdjacentChest(event.getBlockPlaced())) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.double_chest", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!bankService.isValidCachePlacement(town, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.invalid_location", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!(event.getBlockPlaced().getState() instanceof Container cacheContainer)) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        CacheChest.bindPlacedCacheChest(this.plugin, cacheContainer, town.getTownUUID());
        if (!bankService.registerCacheChest(town.getTownUUID(), cacheContainer.getLocation())) {
            event.setCancelled(true);
            new I18n.Builder("cache_chest.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        new I18n.Builder("cache_chest.place.success", player)
            .includePrefix()
            .build()
            .sendMessage();
        new I18n.Builder("cache_chest.place.warning", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private void scheduleCacheSync(final @NotNull UUID townUuid, final @NotNull Inventory inventory) {
        final TownBankService bankService = this.plugin.getTownBankService();
        final ISchedulerAdapter scheduler = this.resolvePlatformScheduler();
        if (bankService == null || scheduler == null) {
            return;
        }

        final Location syncLocation = this.resolveInventoryLocation(inventory);
        scheduler.runDelayed(() -> {
            final TownBankService liveBankService = this.plugin.getTownBankService();
            if (liveBankService == null) {
                return;
            }
            final Runnable syncTask = () -> liveBankService.saveCacheStorage(townUuid, inventory.getContents());
            if (syncLocation == null) {
                scheduler.runGlobal(syncTask);
                return;
            }
            scheduler.runAtLocation(syncLocation, syncTask);
        }, 1L);
    }

    private @Nullable RTown resolveCacheTown(final @Nullable Inventory inventory) {
        final TownBankService bankService = this.plugin.getTownBankService();
        if (bankService == null || !(this.resolveCacheTileState(inventory) instanceof TileState tileState)) {
            return null;
        }

        final UUID townUuid = CacheChest.getTownUuid(this.plugin, tileState);
        return townUuid == null ? null : bankService.getTown(townUuid);
    }

    private @Nullable TileState resolveCacheTileState(final @Nullable Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        final InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof TileState tileState) || !CacheChest.isPlacedCacheChest(this.plugin, tileState)) {
            return null;
        }
        return tileState;
    }

    private @Nullable ISchedulerAdapter resolvePlatformScheduler() {
        if (this.plugin.getPlatform() != null) {
            return this.plugin.getPlatform().getScheduler();
        }
        return this.plugin.getScheduler();
    }

    private @Nullable Location resolveInventoryLocation(final @NotNull Inventory inventory) {
        final TileState tileState = this.resolveCacheTileState(inventory);
        return tileState == null ? null : tileState.getLocation();
    }

    private boolean isPlacedCacheChest(final @NotNull Block block) {
        return block.getState() instanceof TileState tileState && CacheChest.isPlacedCacheChest(this.plugin, tileState);
    }

    private boolean hasAdjacentChest(final @NotNull Block block) {
        return this.isChest(block.getRelative(1, 0, 0))
            || this.isChest(block.getRelative(-1, 0, 0))
            || this.isChest(block.getRelative(0, 0, 1))
            || this.isChest(block.getRelative(0, 0, -1));
    }

    private boolean isAdjacentToCache(final @NotNull Location location) {
        return this.isPlacedCacheChest(location.clone().add(1.0D, 0.0D, 0.0D).getBlock())
            || this.isPlacedCacheChest(location.clone().add(-1.0D, 0.0D, 0.0D).getBlock())
            || this.isPlacedCacheChest(location.clone().add(0.0D, 0.0D, 1.0D).getBlock())
            || this.isPlacedCacheChest(location.clone().add(0.0D, 0.0D, -1.0D).getBlock());
    }

    private boolean isChest(final @NotNull Block block) {
        return block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST;
    }
}
