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

import com.raindropcentral.rda.ActivationMode;
import com.raindropcentral.rda.PlayerBuildService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Listener that routes click-based active-ability casts through player trigger preferences.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
@SuppressWarnings("unused")
public final class ActiveAbilityInteractionListener implements Listener {

    private final RDA rda;

    /**
     * Creates the listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public ActiveAbilityInteractionListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Attempts to cast active abilities when a configured click trigger matches.
     *
     * @param event player interaction event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ActivationMode activationMode = this.resolveActivationMode(event);
        if (activationMode == null) {
            return;
        }

        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final Material heldType = heldItem == null ? Material.AIR : heldItem.getType();
        for (final SkillType skillType : this.rda.getEnabledSkills()) {
            final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
            if (skillConfig == null || skillConfig.getActiveAbility() == null || skillConfig.getActiveAbility().activeConfig() == null) {
                continue;
            }

            if (skillConfig.getActiveAbility().activeConfig().activatorItem() != heldType) {
                continue;
            }

            if (buildService.getActivationMode(player, skillType) != activationMode) {
                continue;
            }

            if (buildService.cast(player, skillType, activationMode)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Clears mana HUD state when players leave the server.
     *
     * @param event player quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService != null) {
            buildService.handlePlayerQuit(event.getPlayer());
        }
    }

    private @Nullable ActivationMode resolveActivationMode(final @NotNull PlayerInteractEvent event) {
        return switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> event.getPlayer().isSneaking()
                ? ActivationMode.SHIFT_LEFT_CLICK
                : ActivationMode.LEFT_CLICK;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> event.getPlayer().isSneaking()
                ? ActivationMode.SHIFT_RIGHT_CLICK
                : ActivationMode.RIGHT_CLICK;
            default -> null;
        };
    }
}
