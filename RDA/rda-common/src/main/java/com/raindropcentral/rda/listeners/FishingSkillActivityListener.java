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

import com.raindropcentral.rda.PlayerBuildService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Listener that awards fishing XP from successful catches.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class FishingSkillActivityListener implements Listener {

    private final RDA rda;

    /**
     * Creates a fishing listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public FishingSkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Awards fishing XP for successful catches.
     *
     * @param event fishing event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerFish(final @NotNull PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.FISHING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.FISHING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = skillConfig.getRatesByTrigger(SkillTriggerType.FISH_CATCH)
            .stream()
            .findFirst()
            .orElse(null);
        if (rate != null) {
            progressionService.awardXp(event.getPlayer(), rate, 1.0D, rate.label());
        }

        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService != null
            && buildService.isSkillActive(event.getPlayer(), SkillType.FISHING)
            && event.getCaught() instanceof Item caughtItem) {
            caughtItem.getWorld().dropItemNaturally(caughtItem.getLocation(), caughtItem.getItemStack().clone());
        }
    }
}
