package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Task handler for ENCHANT_ITEM quest tasks.
 * <p>
 * This handler listens to {@link EnchantItemEvent} and updates progress for
 * ENCHANT_ITEM tasks when players enchant items. It tracks:
 * <ul>
 *   <li>Item material being enchanted</li>
 *   <li>Enchantments applied (type and level)</li>
 *   <li>Experience level used</li>
 * </ul>
 * <p>
 * Only successful enchantments are counted. Cancelled enchantments are ignored.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class EnchantItemTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new enchant item task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public EnchantItemTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "ENCHANT_ITEM";
    }
    
    /**
     * Handles enchant item events.
     * <p>
     * This method is called when a player enchants an item. It checks if:
     * <ul>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The enchantment is valid</li>
     *   <li>The item is valid</li>
     * </ul>
     * If all conditions are met, it updates progress for matching ENCHANT_ITEM tasks.
     *
     * @param event the enchant item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(@NotNull final EnchantItemEvent event) {
        final Player player = event.getEnchanter();
        
        // Handle the event
        handleEvent(event, player);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof EnchantItemEvent enchantEvent)) {
            return false;
        }
        
        // Check if item is valid
        final ItemStack item = enchantEvent.getItem();
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        // Check if enchantments were applied
        return !enchantEvent.getEnchantsToAdd().isEmpty();
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final EnchantItemEvent enchantEvent = (EnchantItemEvent) event;
        final ItemStack item = enchantEvent.getItem();
        
        // Build criteria map
        final Map<String, Object> criteria = new HashMap<>();
        criteria.put("item_type", item.getType().name());
        criteria.put("exp_level", enchantEvent.getExpLevelCost());
        
        // Add enchantments as a formatted string
        final String enchantments = enchantEvent.getEnchantsToAdd().entrySet().stream()
                .map(entry -> entry.getKey().getKey().getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        criteria.put("enchantments", enchantments);
        
        return criteria;
    }
}
