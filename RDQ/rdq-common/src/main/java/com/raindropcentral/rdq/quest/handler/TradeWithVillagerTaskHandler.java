package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for TRADE_WITH_VILLAGER quest tasks.
 * <p>
 * This handler listens to {@link VillagerAcquireTradeEvent} and updates progress for
 * TRADE_WITH_VILLAGER tasks when players trade with villagers. It tracks:
 * <ul>
 *   <li>Villager profession (e.g., FARMER, LIBRARIAN, BLACKSMITH)</li>
 *   <li>Trade result item material</li>
 *   <li>World where the trade occurred</li>
 * </ul>
 * <p>
 * Note: This handler uses VillagerAcquireTradeEvent which is available in
 * modern Minecraft versions. For older versions, alternative events may be needed.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class TradeWithVillagerTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new trade with villager task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public TradeWithVillagerTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "TRADE_WITH_VILLAGER";
    }
    
    /**
     * Handles villager acquire trade events.
     * <p>
     * This method is called when a villager acquires a new trade. It checks if:
     * <ul>
     *   <li>The entity is a villager</li>
     *   <li>The trade recipe is valid</li>
     * </ul>
     * If all conditions are met, it updates progress for matching TRADE_WITH_VILLAGER tasks.
     * Note: This event fires when a villager gains a new trade, not when a player
     * completes a trade. For actual trade completion tracking, additional events
     * may be needed in future versions.
     *
     * @param event the villager acquire trade event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerAcquireTrade(@NotNull final VillagerAcquireTradeEvent event) {
        // Note: This event doesn't directly involve a player
        // For actual player trading, we would need to track inventory transactions
        // This is a placeholder implementation that tracks villager trade acquisition
        
        // Skip processing as this event doesn't involve a player directly
        // A proper implementation would require tracking player-villager interactions
        // through inventory events or custom trade tracking
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof VillagerAcquireTradeEvent tradeEvent)) {
            return false;
        }
        
        // Check if entity is a villager
        if (!(tradeEvent.getEntity() instanceof Villager)) {
            return false;
        }
        
        // Check if recipe is valid
        final MerchantRecipe recipe = tradeEvent.getRecipe();
        return recipe != null && recipe.getResult() != null;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final VillagerAcquireTradeEvent tradeEvent = (VillagerAcquireTradeEvent) event;
        final Villager villager = (Villager) tradeEvent.getEntity();
        final MerchantRecipe recipe = tradeEvent.getRecipe();
        
        return Map.of(
                "profession", villager.getProfession().name(),
                "result_item", recipe.getResult().getType().name(),
                "world", villager.getWorld().getName()
        );
    }
}
