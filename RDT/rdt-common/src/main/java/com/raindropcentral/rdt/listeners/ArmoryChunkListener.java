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
import com.raindropcentral.rdt.items.RepairBlock;
import com.raindropcentral.rdt.items.SalvageBlock;
import com.raindropcentral.rdt.service.TownArmoryService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Enforces Armory chunk block placement, block recovery, salvage/repair use, and furnace doubling.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public final class ArmoryChunkListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the Armory chunk listener.
     *
     * @param plugin active plugin runtime
     */
    public ArmoryChunkListener(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Validates and registers placed Armory salvage and repair blocks.
     *
     * @param event placement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        if (SalvageBlock.equals(this.plugin, event.getItemInHand())) {
            this.handleSalvageBlockPlacement(event);
            return;
        }
        if (RepairBlock.equals(this.plugin, event.getItemInHand())) {
            this.handleRepairBlockPlacement(event);
        }
    }

    /**
     * Replaces direct salvage or repair block breaking with bound-item recovery.
     *
     * @param event block-break event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return;
        }

        final RTownChunk salvageChunk = runtimeService.findSalvageBlockChunk(event.getBlock().getLocation());
        if (salvageChunk != null) {
            event.setCancelled(true);
            event.setDropItems(false);
            this.handleArmoryBlockBreakResult(
                event.getPlayer(),
                runtimeService.breakSalvageBlock(event.getPlayer(), salvageChunk),
                "salvage_block.break"
            );
            return;
        }

        final RTownChunk repairChunk = runtimeService.findRepairBlockChunk(event.getBlock().getLocation());
        if (repairChunk != null) {
            event.setCancelled(true);
            event.setDropItems(false);
            this.handleArmoryBlockBreakResult(
                event.getPlayer(),
                runtimeService.breakRepairBlock(event.getPlayer(), repairChunk),
                "repair_block.break"
            );
        }
    }

    /**
     * Handles salvage and repair block right-click use.
     *
     * @param event interact event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final TownArmoryService townArmoryService = this.plugin.getTownArmoryService();
        if (runtimeService == null || townArmoryService == null) {
            return;
        }

        final RTownChunk salvageChunk = runtimeService.findSalvageBlockChunk(clickedBlock.getLocation());
        if (salvageChunk != null) {
            event.setCancelled(true);
            this.handleSalvageResult(
                event.getPlayer(),
                townArmoryService.salvageHeldItem(event.getPlayer(), salvageChunk)
            );
            return;
        }

        final RTownChunk repairChunk = runtimeService.findRepairBlockChunk(clickedBlock.getLocation());
        if (repairChunk != null) {
            event.setCancelled(true);
            this.handleRepairResult(
                event.getPlayer(),
                townArmoryService.repairHeldItem(event.getPlayer(), repairChunk)
            );
        }
    }

    /**
     * Applies the configured Armory double-smelt burn multiplier to furnace fuel items.
     *
     * @param event furnace burn event
     */
    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBurn(final @NotNull FurnaceBurnEvent event) {
        if (!this.supportsDoubleSmelt(event.getBlock())) {
            return;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final TownArmoryService townArmoryService = this.plugin.getTownArmoryService();
        if (runtimeService == null || townArmoryService == null) {
            return;
        }

        final RTownChunk townChunk = runtimeService.getChunkAt(event.getBlock().getLocation());
        event.setBurnTime(townArmoryService.resolveAdjustedBurnTime(townChunk, event.getBurnTime()));
    }

    /**
     * Applies Armory furnace and blast-furnace result doubling when enough fuel is available.
     *
     * @param event furnace smelt event
     */
    @EventHandler(ignoreCancelled = true)
    public void onFurnaceSmelt(final @NotNull FurnaceSmeltEvent event) {
        if (!this.supportsDoubleSmelt(event.getBlock())) {
            return;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final TownArmoryService townArmoryService = this.plugin.getTownArmoryService();
        if (runtimeService == null || townArmoryService == null) {
            return;
        }

        final RTownChunk townChunk = runtimeService.getChunkAt(event.getBlock().getLocation());
        final Furnace furnace = event.getBlock().getState() instanceof Furnace liveFurnace ? liveFurnace : null;
        final TownArmoryService.DoubleSmeltResult result = townArmoryService.applyDoubleSmelt(
            townChunk,
            furnace,
            event.getResult()
        );
        if (result.result() != null) {
            event.setResult(result.result());
        }
    }

    private void handleSalvageBlockPlacement(final @NotNull BlockPlaceEvent event) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        if (!runtimeService.hasTownPermission(player, TownPermissions.CHANGE_CHUNK_TYPE)) {
            event.setCancelled(true);
            this.send(player, "salvage_block.place.denied");
            return;
        }

        final RTownChunk townChunk = this.resolveBoundArmoryChunk(
            player,
            SalvageBlock.getTownUUID(this.plugin, event.getItemInHand()),
            SalvageBlock.getWorldName(this.plugin, event.getItemInHand()),
            SalvageBlock.getChunkX(this.plugin, event.getItemInHand()),
            SalvageBlock.getChunkZ(this.plugin, event.getItemInHand())
        );
        if (townChunk == null) {
            event.setCancelled(true);
            this.send(player, "salvage_block.place.failed");
            return;
        }
        if (!runtimeService.isValidSalvageBlockPlacement(townChunk, event.getBlockPlaced().getLocation())
            || !runtimeService.registerSalvageBlock(townChunk, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            this.send(player, "salvage_block.place.invalid_location");
            return;
        }

        this.send(player, "salvage_block.place.success");
        this.send(player, "salvage_block.place.warning");
    }

    private void handleRepairBlockPlacement(final @NotNull BlockPlaceEvent event) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        if (!runtimeService.hasTownPermission(player, TownPermissions.CHANGE_CHUNK_TYPE)) {
            event.setCancelled(true);
            this.send(player, "repair_block.place.denied");
            return;
        }

        final RTownChunk townChunk = this.resolveBoundArmoryChunk(
            player,
            RepairBlock.getTownUUID(this.plugin, event.getItemInHand()),
            RepairBlock.getWorldName(this.plugin, event.getItemInHand()),
            RepairBlock.getChunkX(this.plugin, event.getItemInHand()),
            RepairBlock.getChunkZ(this.plugin, event.getItemInHand())
        );
        if (townChunk == null) {
            event.setCancelled(true);
            this.send(player, "repair_block.place.failed");
            return;
        }
        if (!runtimeService.isValidRepairBlockPlacement(townChunk, event.getBlockPlaced().getLocation())
            || !runtimeService.registerRepairBlock(townChunk, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            this.send(player, "repair_block.place.invalid_location");
            return;
        }

        this.send(player, "repair_block.place.success");
        this.send(player, "repair_block.place.warning");
    }

    private @Nullable RTownChunk resolveBoundArmoryChunk(
        final @NotNull Player player,
        final @Nullable UUID townUuid,
        final @Nullable String worldName,
        final @Nullable Integer chunkX,
        final @Nullable Integer chunkZ
    ) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null
            || townUuid == null
            || worldName == null
            || chunkX == null
            || chunkZ == null
            || !this.isTownMember(player, townUuid)) {
            return null;
        }

        final RTownChunk townChunk = runtimeService.getTownChunk(townUuid, worldName, chunkX, chunkZ);
        return townChunk != null && townChunk.getChunkType() == ChunkType.ARMORY ? townChunk : null;
    }

    private void handleArmoryBlockBreakResult(
        final @NotNull Player player,
        final @NotNull TownRuntimeService.ArmoryBlockBreakResult result,
        final @NotNull String keyPrefix
    ) {
        final String key = switch (result.status()) {
            case SUCCESS -> "success";
            case NO_PERMISSION -> "denied";
            case INVALID_CHUNK, NO_BLOCK, FAILED -> "failed";
        };
        this.send(player, keyPrefix + '.' + key);
    }

    private void handleSalvageResult(
        final @NotNull Player player,
        final @NotNull TownArmoryService.SalvageResult result
    ) {
        final I18n.Builder builder = switch (result.status()) {
            case SUCCESS -> new I18n.Builder("town_armory_shared.messages.salvage_success", player)
                .withPlaceholder("recovered_material_count", result.recoveredMaterialCount());
            case SUCCESS_EMPTY -> new I18n.Builder("town_armory_shared.messages.salvage_success_empty", player);
            case UNSUPPORTED_ITEM -> new I18n.Builder("town_armory_shared.messages.salvage_unsupported_item", player);
            case NO_PERMISSION -> new I18n.Builder("town_armory_shared.messages.armory_use_denied", player);
            case INVALID_CHUNK, LOCKED, NO_BLOCK, FAILED -> new I18n.Builder("town_armory_shared.messages.salvage_failed", player);
        };
        builder.includePrefix().build().sendMessage();
    }

    private void handleRepairResult(
        final @NotNull Player player,
        final @NotNull TownArmoryService.RepairResult result
    ) {
        final I18n.Builder builder = switch (result.status()) {
            case SUCCESS -> new I18n.Builder("town_armory_shared.messages.repair_success", player)
                .withPlaceholders(Map.of(
                    "repair_material", this.formatMaterial(result.repairMaterial()),
                    "material_cost", result.materialCost(),
                    "repair_percent", this.formatDecimal(result.repairPercent())
                ));
            case FULLY_REPAIRED -> new I18n.Builder("town_armory_shared.messages.repair_fully_repaired", player);
            case NOT_ENOUGH_MATERIALS -> new I18n.Builder("town_armory_shared.messages.repair_not_enough_materials", player)
                .withPlaceholders(Map.of(
                    "repair_material", this.formatMaterial(result.repairMaterial()),
                    "material_cost", result.materialCost()
                ));
            case UNSUPPORTED_ITEM -> new I18n.Builder("town_armory_shared.messages.repair_unsupported_item", player);
            case NO_PERMISSION -> new I18n.Builder("town_armory_shared.messages.armory_use_denied", player);
            case INVALID_CHUNK, LOCKED, NO_BLOCK, FAILED -> new I18n.Builder("town_armory_shared.messages.repair_failed", player);
        };
        builder.includePrefix().build().sendMessage();
    }

    private void send(final @NotNull Player player, final @NotNull String key) {
        new I18n.Builder(key, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private boolean isTownMember(final @NotNull Player player, final @NotNull UUID townUuid) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null && Objects.equals(playerData.getTownUUID(), townUuid);
    }

    private boolean supportsDoubleSmelt(final @NotNull Block block) {
        return block.getType() == Material.FURNACE || block.getType() == Material.BLAST_FURNACE;
    }

    private @NotNull String formatMaterial(final @Nullable Material material) {
        if (material == null) {
            return "Unknown";
        }
        final String[] words = material.name().toLowerCase(java.util.Locale.ROOT).split("_");
        final StringBuilder formatted = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.length() == 0 ? material.name() : formatted.toString();
    }

    private @NotNull String formatDecimal(final double value) {
        final String formatted = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (formatted.endsWith("00")) {
            return formatted.substring(0, formatted.length() - 3);
        }
        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }
}
