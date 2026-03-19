package com.raindropcentral.rdq.cache.quest;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Periodic task that auto-saves all dirty quest progress for crash protection.
 * <p>
 * This task runs every 5 minutes (6000 ticks) and saves all players with unsaved
 * quest progress changes. This ensures that progress is not lost in case of a
 * server crash or unexpected shutdown.
 * </p>
 *
 * <h3>Design Rationale</h3>
 * <ul>
 *   <li>5-minute interval balances safety and performance</li>
 *   <li>Only saves players with changes (dirty flag)</li>
 *   <li>Runs asynchronously to avoid blocking main thread</li>
 *   <li>Logs statistics for monitoring</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestProgressAutoSaveTask extends BukkitRunnable {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final PlayerQuestProgressCache cache;
    
    /**
     * Constructs a new QuestProgressAutoSaveTask.
     *
     * @param cache the quest progress cache to auto-save
     */
    public QuestProgressAutoSaveTask(@NotNull final PlayerQuestProgressCache cache) {
        this.cache = cache;
    }
    
    @Override
    public void run() {
        try {
            int savedCount = cache.autoSaveAll();
            
            if (savedCount > 0) {
                LOGGER.info(String.format("Quest progress auto-save: saved %d players", savedCount));
            } else {
                LOGGER.fine("Quest progress auto-save: no dirty players to save");
            }
            
        } catch (Exception e) {
            LOGGER.severe("Quest progress auto-save failed: " + e.getMessage());
        }
    }
}
