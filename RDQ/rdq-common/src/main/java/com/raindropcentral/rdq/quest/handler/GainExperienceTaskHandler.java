package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for GAIN_EXPERIENCE quest tasks.
 * <p>
 * This handler listens to {@link PlayerExpChangeEvent} and updates progress for
 * GAIN_EXPERIENCE tasks when players gain experience points. It tracks:
 * <ul>
 *   <li>Amount of experience gained</li>
 *   <li>World where the experience was gained</li>
 * </ul>
 * </p>
 * <p>
 * Only positive experience gains are counted. Experience loss is ignored.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class GainExperienceTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new gain experience task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public GainExperienceTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "GAIN_EXPERIENCE";
    }
    
    /**
     * Handles player experience change events.
     * <p>
     * This method is called when a player's experience changes. It checks if:
     * <ul>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The experience change is positive (gain, not loss)</li>
     * </ul>
     * If all conditions are met, it updates progress for matching GAIN_EXPERIENCE tasks.
     * </p>
     *
     * @param event the player experience change event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerExpChange(@NotNull final PlayerExpChangeEvent event) {
        final Player player = event.getPlayer();
        final int amount = event.getAmount();
        
        // Only process positive experience gains
        if (amount <= 0) {
            return;
        }
        
        // Handle the event with the experience amount
        handleEvent(event, player, amount);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof PlayerExpChangeEvent expEvent)) {
            return false;
        }
        
        // Only count positive experience gains
        return expEvent.getAmount() > 0;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final PlayerExpChangeEvent expEvent = (PlayerExpChangeEvent) event;
        
        return Map.of(
                "amount", expEvent.getAmount(),
                "world", expEvent.getPlayer().getWorld().getName()
        );
    }
}
