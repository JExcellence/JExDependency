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
import com.raindropcentral.rdt.utils.TownProtections;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enforces town protection thresholds against player and world actions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class TownProtectionListener implements Listener {

    private final RDT plugin;

    /**
     * Creates the protection listener.
     *
     * @param plugin active RDT runtime
     */
    public TownProtectionListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Enforces block-break protections.
     *
     * @param event break event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), event.getBlock().getLocation(), TownProtections.BREAK_BLOCK)) {
            event.setCancelled(true);
            this.sendDeniedMessage(event.getPlayer(), "break");
        }
    }

    /**
     * Enforces block-place protections.
     *
     * @param event place event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), event.getBlockPlaced().getLocation(), TownProtections.PLACE_BLOCK)) {
            event.setCancelled(true);
            this.sendDeniedMessage(event.getPlayer(), "place");
        }
    }

    /**
     * Enforces switch and container protections.
     *
     * @param event interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        final TownProtections protection = this.resolveInteractProtection(event.getClickedBlock());
        if (protection == null) {
            return;
        }

        if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), event.getClickedBlock().getLocation(), protection)) {
            event.setCancelled(true);
            this.sendDeniedMessage(event.getPlayer(), "interact");
        }
    }

    /**
     * Enforces hostile and passive mob-spawn protections.
     *
     * @param event spawn event
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(final @NotNull CreatureSpawnEvent event) {
        final TownProtections protection;
        if (event.getEntity() instanceof Monster) {
            protection = TownProtections.TOWN_HOSTILE_ENTITIES;
        } else if (event.getEntity() instanceof Animals) {
            protection = TownProtections.TOWN_PASSIVE_ENTITIES;
        } else {
            return;
        }

        if (!this.plugin.getTownRuntimeService().isWorldActionAllowed(event.getLocation(), protection)) {
            event.setCancelled(true);
        }
    }

    /**
     * Reconciles loaded entities when a claimed chunk is brought back into memory.
     *
     * @param event chunk-load event
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        final var townRuntimeService = this.plugin.getTownRuntimeService();
        if (townRuntimeService == null) {
            return;
        }

        final var townChunk = townRuntimeService.getChunk(
            event.getWorld().getName(),
            event.getChunk().getX(),
            event.getChunk().getZ()
        );
        if (townChunk == null) {
            return;
        }
        townRuntimeService.reconcileLoadedProtectionEntities(townChunk);
    }

    /**
     * Enforces fire-spread protections.
     *
     * @param event spread event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(final @NotNull BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE
            && !this.plugin.getTownRuntimeService().isWorldActionAllowed(event.getBlock().getLocation(), TownProtections.TOWN_FIRE)) {
            event.setCancelled(true);
        }
    }

    /**
     * Enforces water and lava flow protections.
     *
     * @param event fluid-flow event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(final @NotNull BlockFromToEvent event) {
        final Material type = event.getBlock().getType();
        final TownProtections protection;
        if (type == Material.WATER || type == Material.KELP || type == Material.SEAGRASS) {
            protection = TownProtections.TOWN_WATER;
        } else if (type == Material.LAVA) {
            protection = TownProtections.TOWN_LAVA;
        } else {
            return;
        }

        if (!this.plugin.getTownRuntimeService().isWorldActionAllowed(event.getToBlock().getLocation(), protection)) {
            event.setCancelled(true);
        }
    }

    private @Nullable TownProtections resolveInteractProtection(final @NotNull Block block) {
        if (block.getState() instanceof Container) {
            return TownProtections.CONTAINER_ACCESS;
        }

        final String materialName = block.getType().name();
        if (materialName.endsWith("_BUTTON")) {
            return TownProtections.BUTTONS;
        }
        if (materialName == null || materialName.isBlank()) {
            return null;
        }
        if (materialName.contains("TRAPDOOR")) {
            return TownProtections.TRAPDOORS;
        }
        if (materialName.contains("FENCE_GATE")) {
            return TownProtections.FENCE_GATES;
        }
        if (materialName.contains("DOOR")) {
            return TownProtections.WOOD_DOORS;
        }
        if (materialName == Material.LEVER.name()) {
            return TownProtections.LEVER;
        }
        return block.getBlockData() instanceof Openable || block.getBlockData() instanceof Powerable
            ? TownProtections.SWITCH_ACCESS
            : null;
    }

    private void sendDeniedMessage(final @NotNull org.bukkit.entity.Player player, final @NotNull String actionKey) {
        new I18n.Builder("town_protection_listener." + actionKey + ".denied", player)
            .includePrefix()
            .build()
            .sendMessage();
    }
}
