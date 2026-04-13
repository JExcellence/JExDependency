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

package com.raindropcentral.rda.listeners;

import com.raindropcentral.rda.PlacedTrackedBlockService;
import com.raindropcentral.rda.PlayerBuildService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener that tracks placed blocks and awards block-based skill XP.
 *
 * <p>The listener routes mining, woodcutting, farming, excavation, and foraging through the shared
 * skill framework while preserving natural-block suppression where configured.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class BlockSkillActivityListener implements Listener {

    private final RDA rda;
    private final Set<String> chainedBreakKeys = ConcurrentHashMap.newKeySet();

    /**
     * Creates a block skill listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public BlockSkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Records placed blocks for skills that suppress player-placed materials.
     *
     * @param event block placement event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final SkillType skillType = this.rda.getBlockBreakSkillOwner(event.getBlockPlaced().getType());
        if (skillType == null || !this.rda.usesNaturalBlockSuppression(skillType)) {
            return;
        }

        final PlacedTrackedBlockService<?> placedBlockService = this.rda.getPlacedTrackedBlockService(skillType);
        if (placedBlockService != null) {
            placedBlockService.trackPlacedBlock(event.getBlockPlaced());
        }
    }

    /**
     * Awards XP for matching block-break skill rates and consumes suppression markers.
     *
     * @param event block break event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final Material material = event.getBlock().getType();
        final SkillType skillType = this.rda.getBlockBreakSkillOwner(material);
        if (skillType == null) {
            return;
        }

        final boolean chainedBreak = this.chainedBreakKeys.remove(this.toBlockKey(event.getBlock()));

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(skillType);
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final PlacedTrackedBlockService<?> placedBlockService = this.rda.getPlacedTrackedBlockService(skillType);
        if (placedBlockService != null && placedBlockService.consumePlacedBlock(event.getBlock())) {
            return;
        }

        final ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        final Material toolType = heldItem == null ? Material.AIR : heldItem.getType();
        final boolean fullyGrown = !(event.getBlock().getBlockData() instanceof Ageable ageable)
            || ageable.getAge() >= ageable.getMaximumAge();

        for (final SkillConfig.RateDefinition rate : skillConfig.getRatesByTrigger(SkillTriggerType.BLOCK_BREAK)) {
            if (!rate.matchesBlock(material, toolType, fullyGrown)) {
                continue;
            }

            progressionService.awardXp(event.getPlayer(), rate, 1.0D, rate.label());
            if (!chainedBreak) {
                this.tryTriggerActiveBreak(skillType, event.getBlock(), event.getPlayer(), heldItem, skillConfig);
            }
            return;
        }
    }

    private void tryTriggerActiveBreak(
        final @NotNull SkillType skillType,
        final @NotNull Block origin,
        final @NotNull org.bukkit.entity.Player player,
        final @NotNull ItemStack heldItem,
        final @NotNull SkillConfig skillConfig
    ) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null || !buildService.isSkillActive(player, skillType)) {
            return;
        }

        switch (skillType) {
            case WOODCUTTING -> this.breakConnectedMaterials(
                origin,
                heldItem,
                skillConfig.getTrackedMaterials(),
                24 + (int) Math.round(buildService.getAbilityPotency(player, skillType, "tree_feller"))
            );
            case MINING -> this.breakConnectedMaterials(
                origin,
                heldItem,
                Set.of(origin.getType()),
                8 + (int) Math.round(buildService.getAbilityPotency(player, skillType, "super_breaker"))
            );
            case EXCAVATION -> this.breakNearbyPlane(
                origin,
                heldItem,
                skillConfig.getTrackedMaterials(),
                this.resolvePlaneRadius(buildService.getAbilityPotency(player, skillType, "giga_drill_breaker"))
            );
            case FARMING -> this.breakNearbyPlane(
                origin,
                heldItem,
                skillConfig.getTrackedMaterials(),
                this.resolvePlaneRadius(buildService.getAbilityPotency(player, skillType, "green_terra"))
            );
            default -> {
            }
        }
    }

    private int resolvePlaneRadius(final double potency) {
        return Math.max(1, Math.min(3, 1 + (int) Math.floor(Math.max(0.0D, potency) / 6.0D)));
    }

    private void breakConnectedMaterials(
        final @NotNull Block origin,
        final @NotNull ItemStack tool,
        final @NotNull Set<Material> trackedMaterials,
        final int maxExtraBlocks
    ) {
        final ArrayDeque<Block> queue = new ArrayDeque<>();
        final Set<String> visited = new HashSet<>();
        queue.add(origin);
        visited.add(this.toBlockKey(origin));
        int brokenBlocks = 0;
        while (!queue.isEmpty() && brokenBlocks < maxExtraBlocks) {
            final Block current = queue.removeFirst();
            for (final Block neighbor : this.getAdjacentBlocks(current)) {
                final String blockKey = this.toBlockKey(neighbor);
                if (!visited.add(blockKey)
                    || neighbor.getType().isAir()
                    || !trackedMaterials.contains(neighbor.getType())) {
                    continue;
                }

                this.chainedBreakKeys.add(blockKey);
                neighbor.breakNaturally(tool);
                brokenBlocks++;
                if (brokenBlocks >= maxExtraBlocks) {
                    return;
                }
                queue.add(neighbor);
            }
        }
    }

    private void breakNearbyPlane(
        final @NotNull Block origin,
        final @NotNull ItemStack tool,
        final @NotNull Set<Material> trackedMaterials,
        final int radius
    ) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                final Block target = origin.getRelative(x, 0, z);
                if (target.equals(origin)
                    || target.getType().isAir()
                    || !trackedMaterials.contains(target.getType())) {
                    continue;
                }
                this.chainedBreakKeys.add(this.toBlockKey(target));
                target.breakNaturally(tool);
            }
        }
    }

    private @NotNull Set<Block> getAdjacentBlocks(final @NotNull Block block) {
        final Set<Block> adjacent = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    adjacent.add(block.getRelative(x, y, z));
                }
            }
        }
        return adjacent;
    }

    private @NotNull String toBlockKey(final @NotNull Block block) {
        return block.getWorld().getName()
            + ':'
            + block.getX()
            + ':'
            + block.getY()
            + ':'
            + block.getZ();
    }
}
