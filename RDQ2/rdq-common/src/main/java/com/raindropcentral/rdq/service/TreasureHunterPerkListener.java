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

public class TreasureHunterPerkListener implements PerkServiceListener, Listener {
    
    private static final Logger logger = LoggerFactory.getLogger(TreasureHunterPerkListener.class);
    private static final String PERK_ID = "treasure_hunter";
    private static final double BONUS_MULTIPLIER = 1.5;
    
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Random random = new Random();
    
    @Override
    public void onActivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.add(playerId);
        logger.debug("Treasure Hunter perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activePlayers.remove(playerId);
        logger.debug("Treasure Hunter perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        logger.debug("Treasure Hunter perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
    
    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        
        if (!activePlayers.contains(playerId)) return;
        
        if (random.nextDouble() < 0.3) {
            logger.debug("Treasure Hunter bonus triggered for player: {}", player.getName());
        }
    }
}
