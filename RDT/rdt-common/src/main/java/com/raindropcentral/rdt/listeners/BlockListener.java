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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.ChunkBlock;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rdt.service.NexusAccessService;
import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.view.town.TownChunkView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles GUI-issued town marker placement and interaction rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class BlockListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the block listener.
     *
     * @param plugin active RDT runtime
     */
    public BlockListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Finalizes town creation and GUI-issued chunk claims when bound items are placed.
     *
     * @param event placement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final ItemStack item = event.getItemInHand();
        if (Nexus.equals(this.plugin, item)) {
            this.handleNexusPlacement(event, item);
            return;
        }
        if (ChunkBlock.equals(this.plugin, item)) {
            this.handleChunkPlacement(event, item);
        }
    }

    /**
     * Opens the appropriate town or chunk view when a marker block is right-clicked.
     *
     * @param event interaction event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        final Location location = event.getClickedBlock().getLocation();
        final RTown nexusTown = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().findNexusTown(location);
        if (nexusTown != null) {
            this.openNexusOverview(event.getPlayer(), nexusTown);
            event.setCancelled(true);
            return;
        }

        final RTownChunk townChunk = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().findChunkMarker(location);
        if (townChunk == null || !this.isTownMember(event.getPlayer(), townChunk.getTown().getTownUUID())) {
            return;
        }

        this.plugin.getViewFrame().open(
            TownChunkView.class,
            event.getPlayer(),
            Map.of(
                "plugin", this.plugin,
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
        event.setCancelled(true);
    }

    /**
     * Restricts breaking live marker blocks to the town runtime rules.
     *
     * @param event block-break event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final RTown nexusTown = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().findNexusTown(location);
        if (nexusTown != null) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.nexus.pickup.unsupported", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final RTownChunk townChunk = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().findChunkMarker(location);
        if (townChunk == null) {
            return;
        }

        if (!this.plugin.getTownRuntimeService().hasTownPermission(event.getPlayer(), TownPermissions.PICKUP_CHUNK)
            || !this.isTownMember(event.getPlayer(), townChunk.getTown().getTownUUID())) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.chunk.pickup.denied", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!this.plugin.getTownRuntimeService().unclaimChunk(townChunk)) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.chunk.pickup.failed", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        event.setDropItems(false);
        final ItemStack chunkBlock = ChunkBlock.getChunkBlockItem(
            this.plugin,
            event.getPlayer(),
            townChunk.getTown().getTownUUID(),
            townChunk.getTown().getMayorUUID(),
            townChunk.getWorldName(),
            townChunk.getX(),
            townChunk.getZ()
        );
        event.getPlayer().getInventory().addItem(chunkBlock)
            .values()
            .forEach(overflow -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), overflow));
        new I18n.Builder("block_listener.chunk.pickup.success", event.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of("chunk_x", townChunk.getX(), "chunk_z", townChunk.getZ()))
            .build()
            .sendMessage();
    }

    /**
     * Restricts who may pick up bound town marker items.
     *
     * @param event pickup event
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPickupItem(final @NotNull EntityPickupItemEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        final ItemStack item = event.getItem().getItemStack();
        if (Nexus.equals(this.plugin, item) && !this.canPickupNexusItem(player, item)) {
            event.setCancelled(true);
            return;
        }
        if (ChunkBlock.equals(this.plugin, item) && !this.canPickupChunkItem(player, item)) {
            event.setCancelled(true);
        }
    }

    private void handleNexusPlacement(final @NotNull BlockPlaceEvent event, final @NotNull ItemStack item) {
        final Player player = event.getPlayer();
        final UUID itemMayorUuid = Nexus.getMayorUUID(this.plugin, item);
        if (!Objects.equals(itemMayorUuid, player.getUniqueId())) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.nexus.place.denied", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final UUID townUuid = Nexus.getTownUUID(this.plugin, item);
        final String townName = Nexus.getTownName(this.plugin, item);
        final String townColor = Nexus.getTownColor(this.plugin, item);
        final RTown town = townUuid == null || townName == null || townColor == null
            ? null
            : this.plugin.getTownRuntimeService().finalizeTownCreation(
                player,
                event.getBlockPlaced().getLocation(),
                townUuid,
                townName,
                townColor
            );
        if (town == null) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.nexus.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        new I18n.Builder("block_listener.nexus.place.success", player)
            .includePrefix()
            .withPlaceholder("town_name", town.getTownName())
            .build()
            .sendMessage();
    }

    private void handleChunkPlacement(final @NotNull BlockPlaceEvent event, final @NotNull ItemStack item) {
        final Player player = event.getPlayer();
        if (!this.plugin.getTownRuntimeService().hasTownPermission(player, TownPermissions.PLACE_CHUNK)) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.chunk.place.denied", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final UUID townUuid = ChunkBlock.getTownUUID(this.plugin, item);
        final String worldName = ChunkBlock.getWorldName(this.plugin, item);
        final Integer chunkX = ChunkBlock.getXLoc(this.plugin, item);
        final Integer chunkZ = ChunkBlock.getZLoc(this.plugin, item);
        if (townUuid == null || worldName == null || chunkX == null || chunkZ == null || !this.isTownMember(player, townUuid)) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.chunk.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final RTownChunk claimedChunk = this.plugin.getTownRuntimeService().claimChunk(
            player,
            event.getBlockPlaced().getLocation(),
            townUuid,
            worldName,
            chunkX,
            chunkZ
        );
        if (claimedChunk == null) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.chunk.place.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        new I18n.Builder("block_listener.chunk.place.success", player)
            .includePrefix()
            .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
            .build()
            .sendMessage();
    }

    private void openNexusOverview(final @NotNull Player player, final @NotNull RTown town) {
        if (!this.isTownMember(player, town.getTownUUID())) {
            new I18n.Builder("block_listener.nexus.open.denied", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        this.plugin.getNexusAccessService().openSession(player, town);
        final NexusAccessService.NexusSession session = this.plugin.getNexusAccessService().getSession(player.getUniqueId());
        if (session == null) {
            new I18n.Builder("block_listener.nexus.open.failed", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        this.plugin.getViewFrame().open(
            TownOverviewView.class,
            player,
            Map.of(
                "plugin", this.plugin,
                "town_uuid", town.getTownUUID(),
                "access_mode", TownOverviewAccessMode.NEXUS,
                "nexus_session", session.sessionToken(),
                "nexus_world", session.worldName(),
                "nexus_chunk_x", session.chunkX(),
                "nexus_chunk_z", session.chunkZ()
            )
        );
    }

    private boolean canPickupNexusItem(final @NotNull Player player, final @Nullable ItemStack item) {
        final UUID mayorUuid = Nexus.getMayorUUID(this.plugin, item);
        if (Objects.equals(mayorUuid, player.getUniqueId())) {
            return true;
        }
        final UUID townUuid = Nexus.getTownUUID(this.plugin, item);
        return townUuid != null
            && this.isTownMember(player, townUuid)
            && this.plugin.getTownRuntimeService().hasTownPermission(player, TownPermissions.PICKUP_NEXUS);
    }

    private boolean canPickupChunkItem(final @NotNull Player player, final @Nullable ItemStack item) {
        final UUID townUuid = ChunkBlock.getTownUUID(this.plugin, item);
        return townUuid != null
            && this.isTownMember(player, townUuid)
            && this.plugin.getTownRuntimeService().hasTownPermission(player, TownPermissions.PICKUP_CHUNK);
    }

    private boolean isTownMember(final @NotNull Player player, final @NotNull UUID townUuid) {
        final RDTPlayer playerData = this.plugin.getTownRuntimeService().getPlayerData(player.getUniqueId());
        return playerData != null && Objects.equals(playerData.getTownUUID(), townUuid);
    }
}
