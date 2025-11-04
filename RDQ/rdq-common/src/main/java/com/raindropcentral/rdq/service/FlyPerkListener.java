package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener for the Fly perk.
 * 
 * Consolidates functionality from FlyPerkService.
 * 
 * Functionality:
 * - Enables flight for players with the perk active
 * - Tracks active players
 * - Manages flight state
 * 
 * @since 1.0.0
 */
public class FlyPerkListener implements PerkServiceListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FlyPerkListener.class);
    private static final String PERK_ID = "fly";
    
    private final Set<UUID> activePlayers = new HashSet<>();
    
    @Override
    public void onActivate(@NotNull Player player) {
        activePlayers.add(player.getUniqueId());
        player.setAllowFlight(true);
        player.setFlying(true);
        logger.debug("Fly perk activated for player: {}", player.getName());
    }
    
    @Override
    public void onDeactivate(@NotNull Player player) {
        activePlayers.remove(player.getUniqueId());
        player.setAllowFlight(false);
        player.setFlying(false);
        logger.debug("Fly perk deactivated for player: {}", player.getName());
    }
    
    @Override
    public void onTrigger(@NotNull Player player) {
        if (activePlayers.contains(player.getUniqueId())) {
            player.setFlying(!player.isFlying());
            logger.debug("Toggled flight for player: {}", player.getName());
        }
    }
    
    @Override
    public void registerEventHandlers(@NotNull PluginManager manager) {
        // No event handlers needed for fly perk
        logger.debug("Fly perk event handlers registered");
    }
    
    @Override
    public @NotNull String getPerkId() {
        return PERK_ID;
    }
}
