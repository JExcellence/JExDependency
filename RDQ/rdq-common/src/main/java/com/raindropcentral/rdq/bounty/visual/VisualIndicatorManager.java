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

package com.raindropcentral.rdq.bounty.visual;

import com.raindropcentral.rdq.RDQ;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages visual indicators for players with active bounties.
 * Handles tab prefixes, name colors, and particle effects using modern Kyori Adventure MiniMessage.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class VisualIndicatorManager {

    private static final Logger LOGGER = Logger.getLogger(VisualIndicatorManager.class.getName());
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final RDQ rdq;
    private final String tabPrefix;
    private final String nameColor;
    private final boolean particlesEnabled;
    private final Particle particleType;
    private final int particleIntervalTicks;

    private final Map<UUID, PlayerIndicatorState> activeIndicators = new ConcurrentHashMap<>();

    /**
     * Creates a new VisualIndicatorManager with default configuration.
     *
     * @param rdq the RDQ plugin instance
     */
    public VisualIndicatorManager(@NotNull RDQ rdq) {
        this(
            rdq,
            "<gradient:#ff6b6b:#ff8e8e>💀 BOUNTY </gradient>",
            "<gradient:#ff4444:#ff6666>",
            true,
            Particle.FLAME,
            30
        );
    }

    /**
     * Creates a new VisualIndicatorManager with custom configuration.
     *
     * @param rdq the RDQ plugin instance
     * @param tabPrefix the tab prefix to apply (MiniMessage format)
     * @param nameColor the name color to apply (MiniMessage format)
     * @param particlesEnabled whether particles are enabled
     * @param particleType the particle type to spawn
     * @param particleIntervalTicks the interval between particle spawns in ticks
     */
    public VisualIndicatorManager(
            @NotNull RDQ rdq,
            @NotNull String tabPrefix,
            @NotNull String nameColor,
            boolean particlesEnabled,
            @NotNull Particle particleType,
            int particleIntervalTicks
    ) {
        this.rdq = rdq;
        this.tabPrefix = tabPrefix;
        this.nameColor = nameColor;
        this.particlesEnabled = particlesEnabled;
        this.particleType = particleType;
        this.particleIntervalTicks = particleIntervalTicks;

        if (particlesEnabled) {
            startParticleTask();
        }

        startPeriodicRefreshTask();
    }

    /**
     * Applies visual indicators to a player with an active bounty.
     * 
     * Requirements:
     * - 14.1: Apply tab prefix to players with active bounties
     * - 14.2: Apply name color to players with active bounties
     * - 14.3: Spawn particles around players with active bounties
     *
     * @param player the player to apply indicators to
     */
    public void applyIndicators(@NotNull Player player) {
        UUID playerId = player.getUniqueId();

        if (activeIndicators.containsKey(playerId)) {
            removeIndicators(player);
        }

        try {
            Component originalDisplayName = player.displayName();
            Component originalTabListName = player.playerListName();

            Component prefixComponent = MINI_MESSAGE.deserialize(tabPrefix);
            Component playerNameComponent = Component.text(player.getName());
            Component newTabListName = prefixComponent.append(playerNameComponent);
            player.playerListName(newTabListName);

            String coloredNameString = nameColor + player.getName();
            Component coloredName = MINI_MESSAGE.deserialize(coloredNameString);
            player.displayName(coloredName);

            PlayerIndicatorState state = new PlayerIndicatorState(
                    originalDisplayName,
                    originalTabListName,
                    System.currentTimeMillis()
            );
            activeIndicators.put(playerId, state);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to apply visual indicators to " + player.getName(), e);
        }
    }

    /**
     * Removes visual indicators from a player.
     * 
     * Requirements:
     * - 14.4: Remove indicators when bounty claimed/expired
     *
     * @param player the player to remove indicators from
     */
    public void removeIndicators(@NotNull Player player) {
        UUID playerId = player.getUniqueId();

        PlayerIndicatorState state = activeIndicators.remove(playerId);
        if (state == null) {
            LOGGER.fine("No indicators to remove for " + player.getName());
            return;
        }

        try {
            player.displayName(state.originalDisplayName());
            player.playerListName(state.originalTabListName());

            LOGGER.info("Removed visual indicators from " + player.getName());

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove visual indicators from " + player.getName(), e);
        }
    }

    /**
     * Removes visual indicators from a player by UUID.
     * Useful when the player is offline.
     *
     * @param playerId the UUID of the player
     */
    public void removeIndicators(@NotNull UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            removeIndicators(player);
        } else {
            PlayerIndicatorState removed = activeIndicators.remove(playerId);
            if (removed != null) {
                LOGGER.fine("Removed indicator tracking for offline player " + playerId);
            }
        }
    }

    /**
     * Checks if a player has active indicators.
     *
     * @param playerId the UUID of the player
     * @return true if the player has active indicators
     */
    public boolean hasIndicators(@NotNull UUID playerId) {
        return activeIndicators.containsKey(playerId);
    }

    /**
     * Starts the particle spawning task.
     * 
     * Requirements:
     * - 14.3: Spawn particles around players with active bounties at configured interval
     */
    private void startParticleTask() {
        rdq.getPlatform().getScheduler().runRepeating(
                this::spawnParticles,
                particleIntervalTicks,
                particleIntervalTicks
        );
    }

    /**
     * Starts the periodic refresh task to ensure visual indicators persist.
     * This task runs every 30 seconds to reapply indicators that may have been overridden.
     */
    private void startPeriodicRefreshTask() {
        rdq.getPlatform().getScheduler().runRepeating(
                this::refreshIndicatorsForOnlinePlayers,
                600L,
                600L
        );
    }

    /**
     * Refreshes visual indicators for all online players with active bounties.
     * This ensures indicators persist even if they get overridden by other plugins.
     */
    private void refreshIndicatorsForOnlinePlayers() {
        int refreshCount = 0;
        for (UUID playerId : activeIndicators.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                try {
                    PlayerIndicatorState state = activeIndicators.get(playerId);
                    if (state != null) {
                        String currentDisplayName = player.displayName().toString();
                        if (!currentDisplayName.contains("BOUNTY") && !currentDisplayName.contains("💀")) {
                            LOGGER.fine("Refreshing lost visual indicators for " + player.getName());
                            applyIndicators(player);
                            refreshCount++;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to refresh indicators for " + player.getName(), e);
                }
            }
        }
        
        if (refreshCount > 0) {
            LOGGER.info("Refreshed visual indicators for " + refreshCount + " players during periodic check");
        }
    }

    /**
     * Spawns particles around all players with active indicators.
     * 
     * Requirements:
     * - 14.3: Spawn particles around players with active bounties
     */
    private void spawnParticles() {
        if (!particlesEnabled) {
            return;
        }

        int particleCount = 0;
        for (UUID playerId : activeIndicators.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            particleCount++;

            try {
                Location loc = player.getLocation().add(0, 2, 0);

                player.getWorld().spawnParticle(
                        particleType,
                        loc,
                        12,
                        0.5,
                        0.8,
                        0.5,
                        0.03
                );

                player.getWorld().spawnParticle(
                        Particle.SMOKE,
                        loc.add(0, 0.5, 0),
                        4,
                        0.3,
                        0.3,
                        0.3,
                        0.01
                );
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to spawn particles for " + player.getName(), e);
            }
        }

        if (particleCount > 0 && System.currentTimeMillis() % 10000 < particleIntervalTicks * 50) {
            LOGGER.fine("Spawned particles for " + particleCount + " players with active bounties");
        }
    }

    /**
     * Shuts down the manager and cleans up resources.
     * 
     * Requirements:
     * - 14.4: Stop particles when bounty claimed/expired
     */
    public void shutdown() {
        for (UUID playerId : activeIndicators.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeIndicators(player);
            }
        }

        activeIndicators.clear();
        LOGGER.info("VisualIndicatorManager shut down");
    }

    /**
     * Gets the number of players with active indicators.
     *
     * @return the count of active indicators
     */
    public int getActiveIndicatorCount() {
        return activeIndicators.size();
    }

    /**
     * Updates the visual indicators for all online players.
     * Useful for refreshing indicators after configuration changes.
     */
    public void refreshAllIndicators() {
        for (UUID playerId : activeIndicators.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                removeIndicators(player);
                applyIndicators(player);
            }
        }
        LOGGER.info("Refreshed visual indicators for " + activeIndicators.size() + " players");
    }

    /**
     * Forces reapplication of visual indicators for a specific player.
     * This method will remove existing indicators and reapply them.
     *
     * @param player the player to force refresh indicators for
     */
    public void forceRefreshIndicators(@NotNull Player player) {
        UUID playerId = player.getUniqueId();

        if (activeIndicators.containsKey(playerId)) {
            removeIndicators(player);
        }

        applyIndicators(player);
        updatePlayerDisplay(player);
        
        LOGGER.info("Force refreshed visual indicators for " + player.getName());
    }

    /**
     * Forces an immediate update of player display names and tab list.
     * This ensures visual changes are applied immediately.
     *
     * @param player the player to update
     */
    public void updatePlayerDisplay(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!activeIndicators.containsKey(playerId)) {
            return;
        }

        try {
            rdq.getPlatform().getScheduler().runSync(() -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.hidePlayer(rdq.getPlugin(), player);
                    onlinePlayer.showPlayer(rdq.getPlugin(), player);
                }
                
                LOGGER.fine("Forced display update for " + player.getName());
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update player display for " + player.getName(), e);
        }
    }

    /**
     * Handles bounty expiration by removing visual indicators.
     * This method should be called when a bounty expires.
     * 
     * Requirements:
     * - 14.4: Remove indicators when bounty claimed/expired
     *
     * @param targetPlayerId the UUID of the player whose bounty expired
     */
    public void handleBountyExpiration(@NotNull UUID targetPlayerId) {
        removeIndicators(targetPlayerId);
        LOGGER.info("Removed visual indicators for expired bounty on player: " + targetPlayerId);
    }

    /**
     * Stores the state of a player's visual indicators.
     */
    private record PlayerIndicatorState(
            Component originalDisplayName,
            Component originalTabListName,
            long appliedTimestamp
    ) {}
}