package com.raindropcentral.rdq.quest.cache;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for managing quest cache on player join/quit.
 *
 * <p>This listener automatically loads quest data when a player joins
 * and saves it when they quit.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCacheListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(QuestCacheListener.class.getName());

    private final QuestCacheManager cacheManager;
    private final RDQ rdq;

    /**
     * Executes QuestCacheListener.
     */
    public QuestCacheListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        // TODO: Implement getQuestCacheManager() in RDQ.java
        this.cacheManager = null; // rdq.getQuestCacheManager();
    }
    
    /**
     * Handles player join event - loads quest data.
 *
 * <p>Uses LOWEST priority to load data before other plugins access it.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Load quest data asynchronously
        cacheManager.loadPlayerAsync(player.getUniqueId())
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to load quest data for " + player.getName(), ex);
                    return null;
                });
    }
    
    /**
     * Handles player quit event - saves and unloads quest data.
 *
 * <p>Uses MONITOR priority to save data after other plugins are done.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        
        // Save quest data synchronously on quit
        // This blocks to ensure data is saved before player disconnects
        try {
            cacheManager.savePlayer(player.getUniqueId()).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save quest data for " + player.getName(), e);
        }
    }
}
