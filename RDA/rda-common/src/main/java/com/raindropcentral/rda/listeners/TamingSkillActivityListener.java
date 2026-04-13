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
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Listener that awards taming XP from successful tames and tame final blows.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class TamingSkillActivityListener implements Listener {

    private final RDA rda;

    /**
     * Creates a taming listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public TamingSkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Awards taming XP for successful taming actions.
     *
     * @param event tame event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(final @NotNull EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.TAMING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.TAMING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = skillConfig.getRatesByTrigger(SkillTriggerType.TAME_SUCCESS)
            .stream()
            .findFirst()
            .orElse(null);
        if (rate != null) {
            progressionService.awardXp(player, rate, 1.0D, rate.label());
        }
        this.tryBuffTame(event.getEntity(), player);
    }

    /**
     * Awards taming XP when an owned tame lands the final blow.
     *
     * @param event death event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(final @NotNull EntityDeathEvent event) {
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            return;
        }

        if (!(damageByEntityEvent.getDamager() instanceof Tameable tameable)) {
            return;
        }

        if (!(tameable.getOwner() instanceof Player owner)) {
            return;
        }

        final Entity victim = event.getEntity();
        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.TAMING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.TAMING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        for (final SkillConfig.RateDefinition rate : skillConfig.getRatesByTrigger(SkillTriggerType.TAMED_KILL)) {
            final boolean hostile = victim instanceof org.bukkit.entity.Enemy;
            final boolean playerVictim = victim instanceof Player;
            if (!rate.matchesEntity(victim.getType(), hostile, playerVictim)) {
                continue;
            }

            if (victim instanceof Player victimPlayer && rate.excludeSameTown() && this.isSameTown(owner, victimPlayer)) {
                continue;
            }

            progressionService.awardXp(owner, rate, 1.0D, rate.label());
            this.tryBuffTame(tameable, owner);
            return;
        }
    }

    private void tryBuffTame(final @NotNull Entity entity, final @NotNull Player owner) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (!buildService.isSkillActive(owner, SkillType.TAMING)) {
            return;
        }

        final org.bukkit.attribute.AttributeInstance maxHealthAttribute =
            livingEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        final double maxHealth = maxHealthAttribute == null ? livingEntity.getHealth() + 4.0D : maxHealthAttribute.getValue();
        livingEntity.setHealth(Math.min(maxHealth, livingEntity.getHealth() + 4.0D));
        livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.STRENGTH,
            120,
            0,
            true,
            false,
            false
        ));
        livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE,
            120,
            0,
            true,
            false,
            false
        ));
    }

    private boolean isSameTown(final @NotNull Player first, final @NotNull Player second) {
        final RProtectionBridge protectionBridge = RProtectionBridge.getBridge();
        if (protectionBridge == null) {
            return false;
        }

        final String firstTownIdentifier = protectionBridge.getPlayerTownIdentifier(first);
        final String secondTownIdentifier = protectionBridge.getPlayerTownIdentifier(second);
        return firstTownIdentifier != null && firstTownIdentifier.equals(secondTownIdentifier);
    }
}
