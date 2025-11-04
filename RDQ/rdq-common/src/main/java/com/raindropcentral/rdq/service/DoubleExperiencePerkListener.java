package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for the Double Experience perk.
 * 
 * Consolidates functionality from DoubleExperiencePerkService.
 * 
 * Functionality:
 * - Doubles experience gained by players with the perk active
 * - Tracks active players
 * - Registers Bukkit event handlers
 * 
 * @since 1.0.0
 */
public class DoubleExperiencePerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(DoubleExperiencePerkListener.class);
    private static final String PERK_ID = "double_experience";
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        activePlayers.add(player.getUniqueId());
        logger.debug("Double Experience perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
        logger.debug("Double Experience perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
        // Double experience is triggered automatically via event listener
        // This method is not used for this perk
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        // Event handlers are registered via @EventHandler annotation
        // This is called during initialization for consistency
        logger.debug("Double Experience perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    /**
     * Handle player experience change events.
     * 
     * Doubles the experience if the player has the perk active.
     * 
     * @param event The PlayerExpChangeEvent
     */
    @EventHandler
    public void onPlayerExpChange(@NotNull PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        int originalExp = event.getAmount();
        int doubledExp = originalExp * 2;
        
        event.setAmount(doubledExp);
        logger.debug("Doubled experience for player {}: {} -> {}", 
            player.getName(), originalExp, doubledExp);
    }
}
