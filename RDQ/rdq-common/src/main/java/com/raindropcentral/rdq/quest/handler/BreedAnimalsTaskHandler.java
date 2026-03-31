package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for BREED_ANIMALS quest tasks.
 * <p>
 * This handler listens to {@link EntityBreedEvent} and updates progress for
 * BREED_ANIMALS tasks when players breed animals. It tracks:
 * <ul>
 *   <li>Animal type (e.g., COW, SHEEP, CHICKEN)</li>
 *   <li>World where the breeding occurred</li>
 * </ul>
 * </p>
 * <p>
 * Only breeding initiated by players is counted. Natural breeding or breeding
 * by other means is ignored.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class BreedAnimalsTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new breed animals task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public BreedAnimalsTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "BREED_ANIMALS";
    }
    
    /**
     * Handles entity breed events.
     * <p>
     * This method is called when two animals breed. It checks if:
     * <ul>
     *   <li>The breeding was initiated by a player</li>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The entity is an animal</li>
     * </ul>
     * If all conditions are met, it updates progress for matching BREED_ANIMALS tasks.
     * </p>
     *
     * @param event the entity breed event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(@NotNull final EntityBreedEvent event) {
        // Check if breeding was initiated by a player
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }
        
        // Handle the event
        handleEvent(event, player);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof EntityBreedEvent breedEvent)) {
            return false;
        }
        
        // Only count animal breeding
        return breedEvent.getEntity() instanceof Animals;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final EntityBreedEvent breedEvent = (EntityBreedEvent) event;
        
        return Map.of(
                "animal_type", breedEvent.getEntityType().name(),
                "world", breedEvent.getEntity().getWorld().getName()
        );
    }
}
