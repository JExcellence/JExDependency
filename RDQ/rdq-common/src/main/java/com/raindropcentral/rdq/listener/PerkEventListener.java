package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit event listener that triggers perk events.
 * Listens for game events and forwards them to the EventPerkHandler.
 */
public class PerkEventListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final RDQ rdq;
    
    /**
     * Executes PerkEventListener.
     */
    public PerkEventListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
    }
    
    /**
     * Executes onPlayerItemConsume.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemConsume(@NotNull final PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        
        LOGGER.log(Level.FINE, "PlayerItemConsumeEvent triggered for player {0}", player.getName());
        
        // Process event for all registered perks
        rdq.getPerkActivationService().getEventPerkHandler().processEvent(player, "PLAYER_ITEM_CONSUME", event);
    }
    
    /**
     * Executes onPlayerFish.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerFish(@NotNull final PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        LOGGER.log(Level.FINE, "PlayerFishEvent triggered for player {0}", player.getName());
        
        // Process event for all registered perks
        rdq.getPerkActivationService().getEventPerkHandler().processEvent(player, "PLAYER_FISH", event);
    }
}
