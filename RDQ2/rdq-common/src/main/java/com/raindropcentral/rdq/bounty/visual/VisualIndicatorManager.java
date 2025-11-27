package com.raindropcentral.rdq.bounty.visual;

import com.raindropcentral.rdq.RDQ;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages visual indicators for players with active bounties.
 * Handles tab prefixes, name colors, and particle effects.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4
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
    
    // Track players with active indicators
    private final Map<UUID, PlayerIndicatorState> activeIndicators = new ConcurrentHashMap<>();
    
    // Particle task
    private @Nullable BukkitTask particleTask;
    
    /**
     * Creates a new VisualIndicatorManager.
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
        
        // Check if already applied
        if (activeIndicators.containsKey(playerId)) {
            LOGGER.fine("Indicators already applied to " + player.getName());
            return;
        }
        
        try {
            // Store original display name and tab list name
            Component originalDisplayName = player.displayName();
            Component originalTabListName = player.playerListName();
            
            // Apply tab prefix (Requirement 14.1)
            Component prefixComponent = MINI_MESSAGE.deserialize(tabPrefix);
            Component newTabListName = prefixComponent.append(Component.text(player.getName()));
            player.playerListName(newTabListName);
            
            // Apply name color (Requirement 14.2)
            Component coloredName = MINI_MESSAGE.deserialize(nameColor + player.getName());
            player.displayName(coloredName);
            
            // Store state
            PlayerIndicatorState state = new PlayerIndicatorState(
                    originalDisplayName,
                    originalTabListName,
                    System.currentTimeMillis()
            );
            activeIndicators.put(playerId, state);
            
            LOGGER.fine("Applied visual indicators to " + player.getName());
            
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
            // Restore original names
            player.displayName(state.originalDisplayName());
            player.playerListName(state.originalTabListName());
            
            LOGGER.fine("Removed visual indicators from " + player.getName());
            
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
            // Just remove from tracking if player is offline
            activeIndicators.remove(playerId);
            LOGGER.fine("Removed indicator tracking for offline player " + playerId);
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
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        particleTask = Bukkit.getScheduler().runTaskTimer(
                rdq.getPlugin(),
                this::spawnParticles,
                particleIntervalTicks,
                particleIntervalTicks
        );
        
        LOGGER.info("Started particle task with interval " + particleIntervalTicks + " ticks");
    }
    
    /**
     * Spawns particles around all players with active indicators.
     */
    private void spawnParticles() {
        for (UUID playerId : activeIndicators.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            try {
                Location loc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(
                        particleType,
                        loc,
                        5,  // count
                        0.3, // offsetX
                        0.5, // offsetY
                        0.3, // offsetZ
                        0.01 // speed
                );
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to spawn particles for " + player.getName(), e);
            }
        }
    }
    
    /**
     * Shuts down the manager and cleans up resources.
     * 
     * Requirements:
     * - 14.4: Stop particles when bounty claimed/expired
     */
    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        
        // Remove all indicators
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
     * Stores the state of a player's visual indicators.
     */
    private record PlayerIndicatorState(
            Component originalDisplayName,
            Component originalTabListName,
            long appliedTimestamp
    ) {}
}
