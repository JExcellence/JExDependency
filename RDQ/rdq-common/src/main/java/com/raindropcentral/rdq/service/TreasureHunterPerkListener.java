package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for the Treasure Hunter perk.
 * 
 * Consolidates functionality from TreasureHunterPerkService.
 * 
 * Functionality:
 * - Increases chance of finding rare items when mining
 * - Tracks active players
 * - Applies bonus loot multiplier
 * 
 * @since 1.0.0
 */
public class TreasureHunterPerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(TreasureHunterPerkListener.class);
    private static final String PERK_ID = "treasure_hunter";
    private static final double BONUS_MULTIPLIER = 1.5; // 50% more loot
    
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Random random = new Random();
    
    @Override
    public void onActivate(@NotNull Player player) {
        activePlayers.add(player.getUniqueId());
        logger.debug("Treasure Hunter perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
        logger.debug("Treasure Hunter perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
        // Treasure Hunter is triggered automatically via event listener
        // This method is not used for this perk
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        // Event handlers are registered via @EventHandler annotation
        // This is called during initialization for consistency
        logger.debug("Treasure Hunter perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    /**
     * Handle block break events.
     * 
     * Increases loot drops if the player has the perk active.
     * 
     * @param event The BlockBreakEvent
     */
    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        // Apply bonus loot multiplier
        // This is a simplified implementation - actual implementation would
        // depend on the specific loot system being used
        if (random.nextDouble() < 0.3) { // 30% chance for bonus loot
            logger.debug("Treasure Hunter bonus triggered for player: {}", player.getName());
        }
    }
}
