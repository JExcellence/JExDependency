package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rplatform.logging.CentralLogger;
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
 * Listener for managing player quest progress cache lifecycle.
 * <p>
 * This listener loads quest progress into cache when a player joins
 * and saves it back to the database when they quit.
 * </p>
 *
 * <h3>Cache Lifecycle</h3>
 * <ul>
 *   <li>On join: Load all active quest progress from database into memory</li>
 *   <li>During gameplay: All progress updates happen in memory (instant access)</li>
 *   <li>On quit: Save all changes back to database and remove from cache</li>
 *   <li>Auto-save: Periodic saves every 5 minutes for crash protection</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestProgressCacheListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final RDQ plugin;
    private final PlayerQuestProgressCache cache;
    
    /**
     * Constructs a new quest progress cache listener.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestProgressCacheListener(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        this.cache = plugin.getPlayerQuestProgressCache();
    }
    
    /**
     * Loads quest progress into cache when a player joins.
     * Uses LOWEST priority to run before other quest-related listeners.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Load quest progress asynchronously
        cache.loadPlayerAsync(player.getUniqueId())
            .thenRun(() -> {
                LOGGER.fine("Loaded quest progress for player " + player.getName());
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to load quest progress for player " + 
                    player.getName(), ex);
                
                // Kick player if cache load fails (prevents data corruption)
                player.kickPlayer("§cFailed to load quest data. Please try again.");
                return null;
            });
    }
    
    /**
     * Saves quest progress from cache when a player quits.
     * Uses MONITOR priority to run after all other listeners.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        
        // Save quest progress synchronously on quit
        // This ensures data is persisted before player disconnects
        try {
            cache.savePlayer(player.getUniqueId());
            LOGGER.fine("Saved quest progress for player " + player.getName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save quest progress for player " + 
                player.getName(), e);
        }
    }
}
