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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.SeedBox;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Enforces Farm seed-box placement and inventory rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public final class SeedBoxListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the Farm seed-box listener.
     *
     * @param plugin active plugin runtime
     */
    public SeedBoxListener(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Validates and binds placed seed-box chests.
     *
     * @param event placement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final Material placedMaterial = event.getBlockPlaced().getType();
        if (placedMaterial != Material.CHEST) {
            return;
        }

        if (SeedBox.equals(this.plugin, event.getItemInHand())) {
            this.handleSeedBoxPlacement(event);
            return;
        }

        if (this.isAdjacentToSeedBox(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.double_chest", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    /**
     * Prevents placed seed boxes from being mined directly.
     *
     * @param event block-break event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        if (this.plugin.getTownRuntimeService() == null) {
            return;
        }
        final RTownChunk seedBoxChunk = this.plugin.getTownRuntimeService().findSeedBoxChunk(event.getBlock().getLocation());
        if (seedBoxChunk == null) {
            return;
        }
        event.setCancelled(true);
        new I18n.Builder("seed_box.break.denied", event.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    /**
     * Cancels invalid player inventory actions against seed-box chests and persists valid changes.
     *
     * @param event click event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final @NotNull InventoryClickEvent event) {
        final RTownChunk seedBoxChunk = this.resolveSeedBoxChunk(event.getView().getTopInventory());
        if (seedBoxChunk == null) {
            return;
        }

        final Inventory topInventory = event.getView().getTopInventory();
        final boolean topInventoryTargeted = event.getRawSlot() >= 0 && event.getRawSlot() < topInventory.getSize();
        final ItemStack insertedItem = this.resolveInsertedItem(event, topInventoryTargeted);
        if (insertedItem != null && !this.isSeed(insertedItem)) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.inventory.only_seeds", (Player) event.getWhoClicked())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        this.scheduleSeedBoxSync(seedBoxChunk, topInventory);
    }

    /**
     * Cancels invalid drag operations into seed-box inventories.
     *
     * @param event drag event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(final @NotNull InventoryDragEvent event) {
        final RTownChunk seedBoxChunk = this.resolveSeedBoxChunk(event.getView().getTopInventory());
        if (seedBoxChunk == null) {
            return;
        }

        final int topSize = event.getView().getTopInventory().getSize();
        final boolean touchesSeedBox = event.getRawSlots().stream().anyMatch(rawSlot -> rawSlot >= 0 && rawSlot < topSize);
        if (!touchesSeedBox) {
            return;
        }

        if (!this.isSeed(event.getOldCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                new I18n.Builder("seed_box.inventory.only_seeds", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            }
            return;
        }

        this.scheduleSeedBoxSync(seedBoxChunk, event.getView().getTopInventory());
    }

    /**
     * Persists the latest seed-box contents when a player closes the chest.
     *
     * @param event close event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(final @NotNull InventoryCloseEvent event) {
        final RTownChunk seedBoxChunk = this.resolveSeedBoxChunk(event.getInventory());
        if (seedBoxChunk == null || this.plugin.getTownRuntimeService() == null) {
            return;
        }
        this.plugin.getTownRuntimeService().syncSeedBoxContents(seedBoxChunk, this.snapshotSeedContents(event.getInventory()));
    }

    /**
     * Rejects non-seed hopper insertion into bound seed boxes.
     *
     * @param event inventory transfer event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(final @NotNull InventoryMoveItemEvent event) {
        final RTownChunk seedBoxChunk = this.resolveSeedBoxChunk(event.getDestination());
        if (seedBoxChunk == null) {
            return;
        }
        if (!this.isSeed(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        this.scheduleSeedBoxSync(seedBoxChunk, event.getDestination());
    }

    /**
     * Restores authoritative persisted seed-box inventories when Farm chunks load.
     *
     * @param event chunk-load event
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        if (this.plugin.getTownRuntimeService() == null || this.plugin.getTownFarmService() == null) {
            return;
        }
        final RTownChunk townChunk = this.plugin.getTownRuntimeService().getChunk(
            event.getWorld().getName(),
            event.getChunk().getX(),
            event.getChunk().getZ()
        );
        if (townChunk == null || !townChunk.hasSeedBox()) {
            return;
        }
        this.plugin.getTownFarmService().syncLiveSeedBoxInventory(townChunk);
    }

    private void handleSeedBoxPlacement(final @NotNull BlockPlaceEvent event) {
        if (this.plugin.getTownRuntimeService() == null) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        if (!this.plugin.getTownRuntimeService().hasTownPermission(player, TownPermissions.CHANGE_CHUNK_TYPE)) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.denied", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final ItemStack item = event.getItemInHand();
        final UUID townUuid = SeedBox.getTownUUID(this.plugin, item);
        final String worldName = SeedBox.getWorldName(this.plugin, item);
        final Integer chunkX = SeedBox.getChunkX(this.plugin, item);
        final Integer chunkZ = SeedBox.getChunkZ(this.plugin, item);
        if (townUuid == null
            || worldName == null
            || chunkX == null
            || chunkZ == null
            || !this.isTownMember(player, townUuid)) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final RTownChunk townChunk = this.plugin.getTownRuntimeService().getTownChunk(townUuid, worldName, chunkX, chunkZ);
        if (townChunk == null || townChunk.getChunkType() != ChunkType.FARM) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (this.hasAdjacentChest(event.getBlockPlaced())) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.double_chest", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!this.plugin.getTownRuntimeService().isValidSeedBoxPlacement(townChunk, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.invalid_location", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!(event.getBlockPlaced().getState() instanceof Chest chest)) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        SeedBox.bindPlacedSeedBox(this.plugin, chest, townUuid, worldName, chunkX, chunkZ);
        if (!this.plugin.getTownRuntimeService().registerSeedBox(townChunk, chest.getLocation(), Map.of())) {
            event.setCancelled(true);
            new I18n.Builder("seed_box.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        new I18n.Builder("seed_box.place.success", player)
            .includePrefix()
            .build()
            .sendMessage();
        new I18n.Builder("seed_box.place.warning", player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @Nullable RTownChunk resolveSeedBoxChunk(final @Nullable Inventory inventory) {
        if (inventory == null || this.plugin.getTownRuntimeService() == null) {
            return null;
        }
        if (!(inventory.getHolder() instanceof Chest chest)) {
            return null;
        }
        return this.plugin.getTownRuntimeService().findSeedBoxChunk(chest.getLocation());
    }

    static boolean insertsIntoSeedBox(final boolean topInventoryTargeted, final @NotNull InventoryAction action) {
        return FuelTankListener.insertsIntoFuelTank(topInventoryTargeted, action);
    }

    private @Nullable ItemStack resolveInsertedItem(final @NotNull InventoryClickEvent event, final boolean topInventoryTargeted) {
        if (!insertsIntoSeedBox(topInventoryTargeted, event.getAction())) {
            return null;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            return event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return event.getWhoClicked().getInventory().getItemInOffHand();
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return event.getCurrentItem();
        }
        return event.getCursor();
    }

    private boolean isSeed(final @Nullable ItemStack itemStack) {
        return itemStack != null
            && !itemStack.isEmpty()
            && this.plugin.getFarmConfig().isAllowedSeedMaterial(itemStack.getType());
    }

    private @NotNull Map<String, ItemStack> snapshotSeedContents(final @NotNull Inventory inventory) {
        final Map<String, ItemStack> contents = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack itemStack = inventory.getItem(slot);
            if (!this.isSeed(itemStack)) {
                continue;
            }
            contents.put(String.valueOf(slot), itemStack.clone());
        }
        return contents;
    }

    private void scheduleSeedBoxSync(final @NotNull RTownChunk townChunk, final @NotNull Inventory inventory) {
        if (this.plugin.getTownRuntimeService() == null) {
            return;
        }

        final ISchedulerAdapter scheduler = this.resolvePlatformScheduler();
        if (scheduler == null) {
            return;
        }

        final Location syncLocation = this.resolveInventoryLocation(inventory, townChunk);
        scheduler.runDelayed(() -> {
            if (this.plugin.getTownRuntimeService() == null) {
                return;
            }
            final Runnable syncTask = () -> this.plugin.getTownRuntimeService().syncSeedBoxContents(
                townChunk,
                this.snapshotSeedContents(inventory)
            );
            if (syncLocation == null) {
                scheduler.runGlobal(syncTask);
                return;
            }
            scheduler.runAtLocation(syncLocation, syncTask);
        }, 1L);
    }

    private @Nullable ISchedulerAdapter resolvePlatformScheduler() {
        if (this.plugin.getPlatform() != null) {
            return this.plugin.getPlatform().getScheduler();
        }
        return this.plugin.getScheduler();
    }

    private @Nullable Location resolveInventoryLocation(final @NotNull Inventory inventory, final @NotNull RTownChunk townChunk) {
        if (inventory.getHolder() instanceof Chest chest) {
            return chest.getLocation();
        }
        return townChunk.getSeedBoxLocation();
    }

    private boolean hasAdjacentChest(final @NotNull Block block) {
        return this.isChest(block.getRelative(1, 0, 0))
            || this.isChest(block.getRelative(-1, 0, 0))
            || this.isChest(block.getRelative(0, 0, 1))
            || this.isChest(block.getRelative(0, 0, -1));
    }

    private boolean isAdjacentToSeedBox(final @NotNull Location location) {
        return this.isSeedBoxBlock(location.clone().add(1.0D, 0.0D, 0.0D))
            || this.isSeedBoxBlock(location.clone().add(-1.0D, 0.0D, 0.0D))
            || this.isSeedBoxBlock(location.clone().add(0.0D, 0.0D, 1.0D))
            || this.isSeedBoxBlock(location.clone().add(0.0D, 0.0D, -1.0D));
    }

    private boolean isSeedBoxBlock(final @NotNull Location location) {
        return this.plugin.getTownRuntimeService() != null
            && this.plugin.getTownRuntimeService().findSeedBoxChunk(location) != null;
    }

    private boolean isChest(final @NotNull Block block) {
        return block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST;
    }

    private boolean isTownMember(final @NotNull Player player, final @NotNull UUID townUuid) {
        final RDTPlayer playerData = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().getPlayerData(player.getUniqueId());
        return playerData != null && Objects.equals(playerData.getTownUUID(), townUuid);
    }
}
