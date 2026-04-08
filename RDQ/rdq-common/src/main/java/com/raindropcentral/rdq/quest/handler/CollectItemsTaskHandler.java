package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for COLLECT_ITEMS quest tasks.
 * <p>
 * This handler listens to {@link EntityPickupItemEvent} and updates progress for
 * COLLECT_ITEMS tasks when players pick up items. It tracks:
 * <ul>
 *   <li>Item material (e.g., DIAMOND, IRON_INGOT, WHEAT)</li>
 *   <li>Item amount picked up</li>
 * </ul>
 * <p>
 * Progress is incremented by the number of items picked up, not just by 1.
 * For example, picking up a stack of 64 wheat will add 64 to the progress.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class CollectItemsTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new collect items task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public CollectItemsTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "COLLECT_ITEMS";
    }
    
    /**
     * Handles item pickup events.
     * <p>
     * This method is called when any entity picks up an item. It checks if:
     * <ul>
     *   <li>The entity is a player</li>
     *   <li>The player is eligible for quest progress</li>
     * </ul>
     * If all conditions are met, it updates progress for matching COLLECT_ITEMS tasks
     * by the amount of items picked up.
     *
     * @param event the item pickup event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(@NotNull final EntityPickupItemEvent event) {
        // Check if entity is a player
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Get item amount
        final ItemStack item = event.getItem().getItemStack();
        final int amount = item.getAmount();
        
        // Handle the event with the item amount
        handleEvent(event, player, amount);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        // Always process item pickups for players
        return event instanceof EntityPickupItemEvent;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final EntityPickupItemEvent pickupEvent = (EntityPickupItemEvent) event;
        final ItemStack item = pickupEvent.getItem().getItemStack();
        
        return Map.of(
                "target", item.getType().name(),
                "amount", item.getAmount()
        );
    }
}

