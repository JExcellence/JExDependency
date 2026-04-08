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
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
     * Enforces switch, container, and right-click item-use protections.
     *
     * @param event interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            final Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) {
                return;
            }

            final TownProtections protection = resolveInteractProtection(clickedBlock);
            if (protection != TownProtections.PRESSURE_PLATES) {
                return;
            }

            if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), clickedBlock.getLocation(), protection)) {
                event.setCancelled(true);
                this.sendDeniedMessage(event.getPlayer(), "interact");
            }
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND
            || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        final TownProtections interactProtection = clickedBlock == null ? null : resolveInteractProtection(clickedBlock);
        if (interactProtection != null) {
            if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), clickedBlock.getLocation(), interactProtection)) {
                event.setCancelled(true);
                this.sendDeniedMessage(event.getPlayer(), "interact");
            }
            return;
        }

        final TownProtections itemUseProtection = resolveItemUseProtection(event.getItem());
        if (itemUseProtection == null) {
            return;
        }

        final Location interactionLocation = clickedBlock == null ? event.getPlayer().getLocation() : clickedBlock.getLocation();
        if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), interactionLocation, itemUseProtection)) {
            event.setCancelled(true);
            this.sendDeniedMessage(event.getPlayer(), "interact");
        }
    }

    /**
     * Enforces entity-interaction and item-use protections for entities such as minecarts, boats,
     * and lead targets.
     *
     * @param event entity-interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(final @NotNull PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final TownProtections heldItemProtection = resolveEntityItemUseProtection(
            event.getRightClicked(),
            event.getPlayer().getInventory().getItemInMainHand()
        );
        final TownProtections protection = heldItemProtection == null
            ? resolveEntityInteractProtection(event.getRightClicked())
            : heldItemProtection;
        if (protection == null) {
            return;
        }

        if (!this.plugin.getTownRuntimeService().isPlayerAllowed(event.getPlayer(), event.getRightClicked().getLocation(), protection)) {
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
        final TownProtections protection = this.resolveProtectionEntity(event.getEntity());
        if (protection == null) {
            return;
        }

        if (!this.plugin.getTownRuntimeService().isWorldActionAllowed(event.getLocation(), protection)) {
            event.setCancelled(true);
        }
    }

    /**
     * Removes protected mobs when they cross into a newly restricted claimed chunk.
     *
     * @param event entity movement event
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityMove(final @NotNull EntityMoveEvent event) {
        final TownProtections protection = this.resolveProtectionEntity(event.getEntity());
        if (protection == null || !this.hasChangedChunk(event.getFrom(), event.getTo())) {
            return;
        }

        final var townRuntimeService = this.plugin.getTownRuntimeService();
        if (townRuntimeService == null) {
            return;
        }

        if (!townRuntimeService.isWorldActionAllowed(event.getTo(), protection)) {
            event.getEntity().remove();
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

    static @Nullable TownProtections resolveInteractProtection(final @NotNull Block block) {
        final Material material = block.getType();
        final String materialName = material.name();
        if (materialName.equals(Material.SHULKER_BOX.name()) || materialName.endsWith("_SHULKER_BOX")) {
            return TownProtections.SHULKER_BOXES;
        }
        if (materialName.endsWith("_BUTTON")) {
            return TownProtections.BUTTONS;
        }
        if (materialName.endsWith("_PRESSURE_PLATE")) {
            return TownProtections.PRESSURE_PLATES;
        }
        if (materialName.endsWith("_FENCE_GATE")) {
            return TownProtections.FENCE_GATES;
        }
        if (materialName.endsWith("_TRAPDOOR")) {
            return TownProtections.TRAPDOORS;
        }
        if (isWoodDoor(materialName)) {
            return TownProtections.WOOD_DOORS;
        }
        return switch (material) {
            case CHEST -> TownProtections.CHEST;
            case TRAPPED_CHEST -> TownProtections.TRAPPED_CHEST;
            case FURNACE -> TownProtections.FURNACE;
            case BLAST_FURNACE -> TownProtections.BLAST_FURNACE;
            case DISPENSER -> TownProtections.DISPENSER;
            case HOPPER -> TownProtections.HOPPER;
            case DROPPER -> TownProtections.DROPPER;
            case JUKEBOX -> TownProtections.JUKEBOX;
            case STONECUTTER -> TownProtections.STONECUTTER;
            case SMITHING_TABLE -> TownProtections.SMITHING_TABLE;
            case FLETCHING_TABLE -> TownProtections.FLETCHING_TABLE;
            case SMOKER -> TownProtections.SMOKER;
            case LOOM -> TownProtections.LOOM;
            case GRINDSTONE -> TownProtections.GRINDSTONE;
            case COMPOSTER -> TownProtections.COMPOSTER;
            case CARTOGRAPHY_TABLE -> TownProtections.CARTOGRAPHY_TABLE;
            case BELL -> TownProtections.BELL;
            case BARREL -> TownProtections.BARREL;
            case BREWING_STAND -> TownProtections.BREWING_STAND;
            case LEVER -> TownProtections.LEVER;
            case LODESTONE -> TownProtections.LODESTONE;
            case RESPAWN_ANCHOR -> TownProtections.RESPAWN_ANCHOR;
            case TARGET -> TownProtections.TARGET;
            default -> block.getState() instanceof Container
                ? TownProtections.CONTAINER_ACCESS
                : block.getBlockData() instanceof Openable || block.getBlockData() instanceof Powerable
                    ? TownProtections.SWITCH_ACCESS
                    : null;
        };
    }

    static @Nullable TownProtections resolveItemUseProtection(final @Nullable ItemStack itemStack) {
        return resolveItemUseProtection(itemStack == null ? null : itemStack.getType());
    }

    static @Nullable TownProtections resolveItemUseProtection(final @Nullable Material material) {
        if (material == null || material == Material.AIR || material.name().endsWith("_AIR")) {
            return null;
        }

        final String materialName = material.name();
        if (materialName.endsWith("_CHEST_BOAT") || materialName.endsWith("_BOAT")) {
            return TownProtections.BOATS;
        }
        if (materialName.endsWith("MINECART")) {
            return TownProtections.MINECARTS;
        }
        return switch (material) {
            case ENDER_PEARL -> TownProtections.ENDER_PEARL;
            case FIRE_CHARGE -> TownProtections.FIREBALL;
            case CHORUS_FRUIT -> TownProtections.CHORUS_FRUIT;
            case LEAD -> TownProtections.LEAD;
            default -> null;
        };
    }

    static @Nullable TownProtections resolveEntityItemUseProtection(
        final @NotNull Entity entity,
        final @Nullable ItemStack itemStack
    ) {
        return resolveEntityItemUseProtection(entity, itemStack == null ? null : itemStack.getType());
    }

    static @Nullable TownProtections resolveEntityItemUseProtection(
        final @NotNull Entity entity,
        final @Nullable Material heldMaterial
    ) {
        return heldMaterial == Material.LEAD
            && (entity instanceof LivingEntity || entity.getType() == EntityType.LEASH_KNOT)
            ? TownProtections.LEAD
            : null;
    }

    static @Nullable TownProtections resolveEntityInteractProtection(final @NotNull Entity entity) {
        final String entityTypeName = entity.getType().name();
        if (entityTypeName.endsWith("MINECART")) {
            return TownProtections.MINECARTS;
        }
        if (entityTypeName.equals("BOAT") || entityTypeName.equals("CHEST_BOAT") || entityTypeName.endsWith("_BOAT")) {
            return TownProtections.BOATS;
        }
        return null;
    }

    private static boolean isWoodDoor(final @NotNull String materialName) {
        return materialName.endsWith("_DOOR")
            && !materialName.equals(Material.IRON_DOOR.name())
            && !materialName.contains("COPPER");
    }

    private @Nullable TownProtections resolveProtectionEntity(final @NotNull Entity entity) {
        if (entity instanceof Monster) {
            return TownProtections.TOWN_HOSTILE_ENTITIES;
        }
        if (entity instanceof Animals) {
            return TownProtections.TOWN_PASSIVE_ENTITIES;
        }
        return null;
    }

    private boolean hasChangedChunk(final @NotNull Location from, final @NotNull Location to) {
        if (from.getWorld() == null || to.getWorld() == null) {
            return true;
        }
        return !from.getWorld().getUID().equals(to.getWorld().getUID())
            || Math.floorDiv(from.getBlockX(), 16) != Math.floorDiv(to.getBlockX(), 16)
            || Math.floorDiv(from.getBlockZ(), 16) != Math.floorDiv(to.getBlockZ(), 16);
    }

    private void sendDeniedMessage(final @NotNull org.bukkit.entity.Player player, final @NotNull String actionKey) {
        new I18n.Builder("town_protection_listener." + actionKey + ".denied", player)
            .includePrefix()
            .build()
            .sendMessage();
    }
}
