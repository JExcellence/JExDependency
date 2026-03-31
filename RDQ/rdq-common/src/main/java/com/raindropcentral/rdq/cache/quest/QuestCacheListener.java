package com.raindropcentral.rdq.cache.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.quest.sidebar.QuestProgressSidebarService;
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
 * <p>
 * This listener automatically loads quest data when a player joins,
 * enables the quest progress sidebar if the player has active quests,
 * and saves data when they quit.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCacheListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(QuestCacheListener.class.getName());

    private final QuestCacheManager cacheManager;
    private final RDQ rdq;

    public QuestCacheListener(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.cacheManager = rdq.getQuestCacheManager();
    }

    /**
     * Handles player join event — loads quest data and restores the sidebar if the
     * player has active quests.
     * <p>
     * Uses LOWEST priority to load data before other listeners access it.
     * </p>
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        cacheManager.loadPlayerAsync(player.getUniqueId())
            .thenRun(() -> {
                // Enable the sidebar a tick later so the player is fully initialised
                final QuestProgressSidebarService sidebarService = this.rdq.getQuestProgressSidebarService();
                if (sidebarService == null) {
                    return;
                }

                final boolean hasActiveQuests = !cacheManager.getPlayerQuests(player.getUniqueId()).isEmpty();
                if (!hasActiveQuests) {
                    return;
                }

                // Schedule on main thread — scoreboard manipulation must happen sync
                this.rdq.getPlatform().getScheduler().runDelayed(() -> {
                    if (player.isOnline()) {
                        sidebarService.enable(player);
                    }
                }, 20L);
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to load quest data for " + player.getName(), ex);
                return null;
            });
    }

    /**
     * Handles player quit event — disables the sidebar and saves quest data.
     * <p>
     * Uses MONITOR priority to save data after other plugins are done.
     * </p>
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Disable quest sidebar
        final QuestProgressSidebarService sidebarService = this.rdq.getQuestProgressSidebarService();
        if (sidebarService != null) {
            sidebarService.disable(player);
        }

        // Save quest data synchronously on quit to ensure data is persisted
        try {
            cacheManager.savePlayer(player.getUniqueId()).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save quest data for " + player.getName(), e);
        }
    }
}

