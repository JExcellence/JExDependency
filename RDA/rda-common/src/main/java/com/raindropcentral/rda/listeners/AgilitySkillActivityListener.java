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

import com.raindropcentral.rda.AgilityChunkVisitService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener that awards agility XP from movement and first-time chunk exploration.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class AgilitySkillActivityListener implements Listener {

    private static final double MAX_CONTIGUOUS_MOVE_DISTANCE = 6.0D;

    private final RDA rda;
    private final Map<UUID, Double> pendingTravelDistance = new ConcurrentHashMap<>();

    /**
     * Creates an agility listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public AgilitySkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Awards agility travel and exploration XP from valid movement events.
     *
     * @param event movement event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if (to == null || from.getWorld() == null || to.getWorld() == null || from.getWorld() != to.getWorld()) {
            this.pendingTravelDistance.remove(player.getUniqueId());
            return;
        }

        if (from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.AGILITY);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.AGILITY);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final double distance = from.distance(to);
        if (distance <= 0.0D || distance > MAX_CONTIGUOUS_MOVE_DISTANCE) {
            this.pendingTravelDistance.remove(player.getUniqueId());
            return;
        }

        final SkillConfig.RateDefinition travelRate = skillConfig.getRatesByTrigger(SkillTriggerType.BLOCK_TRAVEL)
            .stream()
            .findFirst()
            .orElse(null);
        if (travelRate != null) {
            final double threshold = travelRate.distance() <= 0.0D ? 1.0D : travelRate.distance();
            double totalDistance = this.pendingTravelDistance.getOrDefault(player.getUniqueId(), 0.0D) + distance;
            while (totalDistance >= threshold) {
                totalDistance -= threshold;
                progressionService.awardXp(player, travelRate, 1.0D, travelRate.label());
            }
            if (totalDistance <= 0.0D) {
                this.pendingTravelDistance.remove(player.getUniqueId());
            } else {
                this.pendingTravelDistance.put(player.getUniqueId(), totalDistance);
            }
        }

        if (from.getChunk().getX() == to.getChunk().getX() && from.getChunk().getZ() == to.getChunk().getZ()) {
            return;
        }

        final AgilityChunkVisitService chunkVisitService = this.rda.getAgilityChunkVisitService();
        final SkillConfig.RateDefinition chunkRate = skillConfig.getRatesByTrigger(SkillTriggerType.CHUNK_DISCOVERY)
            .stream()
            .findFirst()
            .orElse(null);
        if (chunkVisitService != null && chunkRate != null && chunkVisitService.markVisited(player, to.getChunk())) {
            progressionService.awardXp(player, chunkRate, 1.0D, chunkRate.label());
        }
    }

    /**
     * Clears pending travel accumulation after teleports.
     *
     * @param event teleport event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(final @NotNull PlayerTeleportEvent event) {
        this.pendingTravelDistance.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Clears pending travel accumulation when players leave.
     *
     * @param event quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        this.pendingTravelDistance.remove(event.getPlayer().getUniqueId());
    }
}
