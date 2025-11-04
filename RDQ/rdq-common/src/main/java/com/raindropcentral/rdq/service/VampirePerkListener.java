package com.raindropcentral.rdq.service;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for the Vampire perk.
 * 
 * Consolidates functionality from VampirePerkService.
 * 
 * Functionality:
 * - Heals player when dealing damage to entities
 * - Tracks active players
 * - Applies lifesteal effect
 * 
 * @since 1.0.0
 */
public class VampirePerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(VampirePerkListener.class);
    private static final String PERK_ID = "vampire";
    private static final double LIFESTEAL_PERCENTAGE = 0.25; // 25% of damage dealt
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        activePlayers.add(player.getUniqueId());
        logger.debug("Vampire perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
        logger.debug("Vampire perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
        // Vampire is triggered automatically via event listener
        // This method is not used for this perk
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        // Event handlers are registered via @EventHandler annotation
        // This is called during initialization for consistency
        logger.debug("Vampire perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    /**
     * Handle entity damage events.
     * 
     * Applies lifesteal effect if the damager has the perk active.
     * 
     * @param event The EntityDamageByEntityEvent
     */
    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        
        if (!(damager instanceof Player player)) {
            return;
        }
        
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        double damage = event.getDamage();
        double heal = damage * LIFESTEAL_PERCENTAGE;
        
        // Heal the player, but don't exceed max health
        double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
        player.setHealth(newHealth);
        
        logger.debug("Vampire lifesteal triggered for player {}: healed {} health", 
            player.getName(), heal);
    }
}
