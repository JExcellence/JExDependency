package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for the Prevent Death perk.
 * 
 * Consolidates functionality from PreventDeathPerkService.
 * 
 * Functionality:
 * - Prevents death for players with the perk active
 * - Restores health instead of killing the player
 * - Tracks active players
 * 
 * @since 1.0.0
 */
public class PreventDeathPerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(PreventDeathPerkListener.class);
    private static final String PERK_ID = "prevent_death";
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        activePlayers.add(player.getUniqueId());
        logger.debug("Prevent Death perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
        logger.debug("Prevent Death perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
        // Prevent death is triggered automatically via event listener
        // This method is not used for this perk
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        // Event handlers are registered via @EventHandler annotation
        // This is called during initialization for consistency
        logger.debug("Prevent Death perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    /**
     * Handle player death events.
     * 
     * Prevents death if the player has the perk active by canceling the event
     * and restoring health.
     * 
     * @param event The PlayerDeathEvent
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        event.setCancelled(true);
        player.setHealth(player.getMaxHealth());
        logger.debug("Prevented death for player: {}", player.getName());
    }
}
