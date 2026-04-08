package com.raindropcentral.rdq.quest.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that manages quest cache lifecycle for players.
 * <p>
 * Loads active quest data into the in-memory cache on join and
 * flushes it back to the database on quit.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestPlayerListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(QuestPlayerListener.class.getName());

    private final QuestCacheManager questCacheManager;

    /**
     * Constructs a new quest player listener.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestPlayerListener(@NotNull final RDQ plugin) {
        this.questCacheManager = plugin.getQuestCacheManager();
    }

    /**
     * Loads the player's active quest data into cache on join.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final var playerId = event.getPlayer().getUniqueId();
        questCacheManager.loadPlayerAsync(playerId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to load quest cache for " + event.getPlayer().getName(), ex);
                    return null;
                });
    }

    /**
     * Saves the player's active quest data from cache to DB on quit.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final var playerId = event.getPlayer().getUniqueId();
        questCacheManager.savePlayer(playerId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to save quest cache for " + event.getPlayer().getName(), ex);
                    return null;
                });
    }
}
