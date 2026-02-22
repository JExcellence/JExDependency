package com.raindropcentral.rdq.perk.cache;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener for managing perk cache lifecycle.
 * Loads cache on player join and saves on player quit.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkCacheListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final PlayerPerkCache cache;
    
    /**
     * Creates a new PerkCacheListener.
     *
     * @param cache the player perk cache
     */
    public PerkCacheListener(@NotNull final PlayerPerkCache cache) {
        this.cache = cache;
    }
    
    /**
     * Handles player join event by loading their perk cache.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LOGGER.log(Level.INFO, "Loading perk cache for player {0}", player.getName());
        
        cache.loadPlayerCache(playerId)
                .thenRun(() -> {
                    LOGGER.log(Level.INFO, "Perk cache loaded for player {0}", player.getName());
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to load perk cache for player " + player.getName(), throwable);
                    return null;
                });
    }
    
    /**
     * Handles player quit event by saving and unloading their perk cache.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        LOGGER.log(Level.INFO, "Saving and unloading perk cache for player {0}", player.getName());
        
        cache.saveAndUnloadPlayerCache(playerId)
                .thenRun(() -> {
                    LOGGER.log(Level.INFO, "Perk cache saved for player {0}", player.getName());
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to save perk cache for player " + player.getName(), throwable);
                    return null;
                });
    }
}
