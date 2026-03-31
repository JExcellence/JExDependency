package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for FISH_ITEMS quest tasks.
 * <p>
 * This handler listens to {@link PlayerFishEvent} and updates progress for
 * FISH_ITEMS tasks when players catch items while fishing. It tracks:
 * <ul>
 *   <li>Item type caught (e.g., COD, SALMON, TREASURE)</li>
 *   <li>World where the fishing occurred</li>
 * </ul>
 * </p>
 * <p>
 * Only successful catches are counted. Failed fishing attempts or other
 * fishing states are ignored.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class FishItemsTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new fish items task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public FishItemsTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "FISH_ITEMS";
    }
    
    /**
     * Handles player fish events.
     * <p>
     * This method is called when a player fishes. It checks if:
     * <ul>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The fishing state is CAUGHT_FISH</li>
     *   <li>An item was actually caught</li>
     * </ul>
     * If all conditions are met, it updates progress for matching FISH_ITEMS tasks.
     * </p>
     *
     * @param event the player fish event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(@NotNull final PlayerFishEvent event) {
        final Player player = event.getPlayer();
        
        // Only process successful catches
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        // Handle the event
        handleEvent(event, player);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof PlayerFishEvent fishEvent)) {
            return false;
        }
        
        // Only count successful fish catches
        if (fishEvent.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return false;
        }
        
        // Check if caught entity is an item
        return fishEvent.getCaught() instanceof Item;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final PlayerFishEvent fishEvent = (PlayerFishEvent) event;
        
        // Get the caught item
        String itemType = "UNKNOWN";
        if (fishEvent.getCaught() instanceof Item item) {
            final ItemStack itemStack = item.getItemStack();
            itemType = itemStack.getType().name();
        }
        
        return Map.of(
                "item_type", itemType,
                "world", fishEvent.getPlayer().getWorld().getName()
        );
    }
}
