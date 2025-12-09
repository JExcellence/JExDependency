package com.raindropcentral.rdq2.service;

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

public class VampirePerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(VampirePerkListener.class);
    private static final String PERK_ID = "vampire";
    private static final double LIFESTEAL_PERCENTAGE = 0.25;
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.add(playerId);
        logger.debug("Vampire perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.remove(playerId);
        logger.debug("Vampire perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        logger.debug("Vampire perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        var damager = event.getDamager();
        
        if (!(damager instanceof Player player)) return;
        
        var playerId = player.getUniqueId();
        if (!activePlayers.contains(playerId)) return;
        
        var damage = event.getDamage();
        var heal = damage * LIFESTEAL_PERCENTAGE;
        var newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
        
        player.setHealth(newHealth);
        logger.debug("Vampire lifesteal triggered for player {}: healed {} health", 
            player.getName(), heal);
    }
}
