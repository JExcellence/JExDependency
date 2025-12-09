package com.raindropcentral.rdq2.service;

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

public class PreventDeathPerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(PreventDeathPerkListener.class);
    private static final String PERK_ID = "prevent_death";
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.add(playerId);
        logger.debug("Prevent Death perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.remove(playerId);
        logger.debug("Prevent Death perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        logger.debug("Prevent Death perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        var player = event.getEntity();
        var playerId = player.getUniqueId();
        
        if (!activePlayers.contains(playerId)) return;
        
        event.setCancelled(true);
        player.setHealth(player.getMaxHealth());
        logger.debug("Prevented death for player: {}", player.getName());
    }
}
