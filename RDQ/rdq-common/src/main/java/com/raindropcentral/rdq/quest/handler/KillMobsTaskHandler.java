package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for KILL_MOBS quest tasks.
 * <p>
 * This handler listens to {@link EntityDeathEvent} and updates progress for
 * KILL_MOBS tasks when players kill mobs. It tracks:
 * <ul>
 *   <li>Entity type (e.g., ZOMBIE, SKELETON, COW)</li>
 *   <li>World where the kill occurred</li>
 * </ul>
 * <p>
 * Only kills of monsters and animals are counted. Player kills, armor stand
 * destruction, and other entity deaths are ignored.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class KillMobsTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new kill mobs task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public KillMobsTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "KILL_MOBS";
    }
    
    /**
     * Handles entity death events.
     * <p>
     * This method is called when any entity dies. It checks if:
     * <ul>
     *   <li>The entity was killed by a player</li>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The entity is a mob (monster or animal)</li>
     * </ul>
     * If all conditions are met, it updates progress for matching KILL_MOBS tasks.
     *
     * @param event the entity death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull final EntityDeathEvent event) {
        try {
            // Check if killed by a player
            final Player killer = event.getEntity().getKiller();
            if (killer == null) {
                return;
            }

            Bukkit.getLogger().info("[KillMobs] " + killer.getName() + " killed " + event.getEntityType().name());
            Bukkit.getLogger().info("[KillMobs] About to call handleEvent...");

            // Handle the event
            handleEvent(event, killer);
            
            Bukkit.getLogger().info("[KillMobs] handleEvent completed");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[KillMobs] Exception in onEntityDeath: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof EntityDeathEvent deathEvent)) {
            return false;
        }
        
        // Only count monster and animal kills
        final boolean isValidMob = deathEvent.getEntity() instanceof Monster || 
               deathEvent.getEntity() instanceof Animals;
        
        if (!isValidMob) {
            logger.finest("[KillMobs] Entity " + deathEvent.getEntityType() + " is not a monster or animal");
        }
        
        return isValidMob;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        
        final Map<String, Object> criteria = Map.of(
                "target", deathEvent.getEntityType().name(),
                "world", deathEvent.getEntity().getWorld().getName()
        );
        
        logger.fine("[KillMobs] Extracted criteria: " + criteria);
        
        return criteria;
    }
}

