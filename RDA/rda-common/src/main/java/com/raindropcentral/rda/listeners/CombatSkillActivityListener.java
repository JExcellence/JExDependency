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

import com.raindropcentral.rda.CoreStatType;
import com.raindropcentral.rda.PlayerBuildService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Listener that awards defense, archery, and fighting XP from combat events.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class CombatSkillActivityListener implements Listener {

    private final RDA rda;

    /**
     * Creates a combat listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public CombatSkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Awards archery or fighting XP from outgoing damage events.
     *
     * @param event outgoing combat damage event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(final @NotNull EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) {
            return;
        }

        if (event.getDamager() instanceof Projectile projectile
            && projectile instanceof AbstractArrow
            && projectile.getShooter() instanceof Player player) {
            this.applyProjectileDamageBonuses(event, player);
            this.handleArcheryDamage(player, projectile, event.getEntity(), event.getFinalDamage());
            return;
        }

        if (event.getDamager() instanceof Player player) {
            this.applyMeleeDamageBonuses(event, player);
            this.handleFightingDamage(player, event.getEntity(), event.getFinalDamage());
        }
    }

    /**
     * Awards defense XP from incoming damage and shield blocks.
     *
     * @param event incoming damage event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(final @NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        this.applyIncomingDamageReduction(event, player);

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.DEFENSE);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.DEFENSE);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final double finalDamage = Math.max(0.0D, event.getFinalDamage());
        if (finalDamage > 0.0D) {
            final SkillConfig.RateDefinition damageTakenRate = skillConfig.getRatesByTrigger(SkillTriggerType.DAMAGE_TAKEN)
                .stream()
                .findFirst()
                .orElse(null);
            if (damageTakenRate != null) {
                progressionService.awardXp(player, damageTakenRate, finalDamage, damageTakenRate.label());
            }
        }

        final double blockedDamage = Math.max(0.0D, event.getDamage() - event.getFinalDamage());
        if (player.isBlocking() && blockedDamage > 0.0D) {
            final SkillConfig.RateDefinition shieldBlockRate = skillConfig.getRatesByTrigger(SkillTriggerType.SHIELD_BLOCK)
                .stream()
                .findFirst()
                .orElse(null);
            if (shieldBlockRate != null) {
                progressionService.awardXp(player, shieldBlockRate, blockedDamage, shieldBlockRate.label());
            }
        }
    }

    private void handleArcheryDamage(
        final @NotNull Player attacker,
        final @NotNull Projectile projectile,
        final @NotNull Entity victim,
        final double damage
    ) {
        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.ARCHERY);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.ARCHERY);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        if (!this.isCombatEligibleVictim(victim)) {
            return;
        }

        final SkillConfig.ProjectileKind projectileKind = this.resolveProjectileKind(projectile);
        final boolean hostile = victim instanceof Enemy;
        final boolean playerVictim = victim instanceof Player;
        for (final SkillConfig.RateDefinition rate : skillConfig.getRatesByTrigger(SkillTriggerType.ENTITY_DAMAGE)) {
            if (!rate.matchesProjectileKind(projectileKind)
                || !rate.matchesEntity(victim.getType(), hostile, playerVictim)) {
                continue;
            }

            if (victim instanceof Player victimPlayer && rate.excludeSameTown() && this.isSameTown(attacker, victimPlayer)) {
                continue;
            }

            progressionService.awardXp(attacker, rate, damage, rate.label());
            return;
        }
    }

    private void handleFightingDamage(
        final @NotNull Player attacker,
        final @NotNull Entity victim,
        final double damage
    ) {
        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.FIGHTING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.FIGHTING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        if (!this.isCombatEligibleVictim(victim)) {
            return;
        }

        final boolean hostile = victim instanceof Enemy;
        final boolean playerVictim = victim instanceof Player;
        for (final SkillConfig.RateDefinition rate : skillConfig.getRatesByTrigger(SkillTriggerType.ENTITY_DAMAGE)) {
            if (!rate.matchesEntity(victim.getType(), hostile, playerVictim)) {
                continue;
            }

            if (victim instanceof Player victimPlayer && rate.excludeSameTown() && this.isSameTown(attacker, victimPlayer)) {
                continue;
            }

            progressionService.awardXp(attacker, rate, damage, rate.label());
            return;
        }
    }

    private boolean isCombatEligibleVictim(final @NotNull Entity victim) {
        return victim instanceof Enemy || victim instanceof Player;
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

    private @Nullable SkillConfig.ProjectileKind resolveProjectileKind(final @NotNull Projectile projectile) {
        if (projectile instanceof SpectralArrow) {
            return SkillConfig.ProjectileKind.SPECTRAL_ARROW;
        }

        if (projectile instanceof Arrow arrow) {
            final PotionType potionType = arrow.getBasePotionType();
            if ((potionType != null && potionType != PotionType.WATER) || !arrow.getCustomEffects().isEmpty()) {
                return SkillConfig.ProjectileKind.TIPPED_ARROW;
            }
            return SkillConfig.ProjectileKind.ARROW;
        }

        return null;
    }

    private void applyMeleeDamageBonuses(
        final @NotNull EntityDamageByEntityEvent event,
        final @NotNull Player attacker
    ) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null) {
            return;
        }

        double multiplier = 1.0D + buildService.getPassiveValue(attacker, CoreStatType.STR);
        multiplier += buildService.getAbilityPotency(attacker, SkillType.FIGHTING, "serrated_strikes");
        multiplier += buildService.getAbilityPotency(attacker, SkillType.FIGHTING, "combat_focus");
        if (buildService.isSkillActive(attacker, SkillType.FIGHTING)) {
            multiplier += buildService.getAbilityPotency(attacker, SkillType.FIGHTING, "berserk");
        }
        event.setDamage(event.getDamage() * Math.max(1.0D, multiplier));
    }

    private void applyProjectileDamageBonuses(
        final @NotNull EntityDamageByEntityEvent event,
        final @NotNull Player attacker
    ) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null) {
            return;
        }

        final double multiplier = 1.0D
            + buildService.getPassiveValue(attacker, CoreStatType.DEX)
            + buildService.getAbilityPotency(attacker, SkillType.ARCHERY, "draw_mastery")
            + buildService.getAbilityPotency(attacker, SkillType.ARCHERY, "steady_aim")
            + (buildService.isSkillActive(attacker, SkillType.ARCHERY)
                ? buildService.getAbilityPotency(attacker, SkillType.ARCHERY, "rapid_fire")
                : 0.0D);
        event.setDamage(event.getDamage() * Math.max(1.0D, multiplier));
    }

    private void applyIncomingDamageReduction(
        final @NotNull EntityDamageEvent event,
        final @NotNull Player player
    ) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null) {
            return;
        }

        double reduction = buildService.getAbilityPotency(player, SkillType.DEFENSE, "iron_skin");
        if (player.isBlocking()) {
            reduction += buildService.getAbilityPotency(player, SkillType.DEFENSE, "guard_stance");
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            reduction += buildService.getPassiveValue(player, CoreStatType.AGI)
                + buildService.getAbilityPotency(player, SkillType.AGILITY, "sure_footing");
        }
        if (buildService.isSkillActive(player, SkillType.DEFENSE)) {
            reduction += buildService.getAbilityPotency(player, SkillType.DEFENSE, "iron_guard");
        }

        final double clampedReduction = Math.max(0.0D, Math.min(0.85D, reduction));
        if (clampedReduction > 0.0D) {
            event.setDamage(event.getDamage() * (1.0D - clampedReduction));
        }
    }
}
