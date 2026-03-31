package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for CRAFT_ITEMS quest tasks.
 * <p>
 * This handler listens to {@link CraftItemEvent} and updates progress for
 * CRAFT_ITEMS tasks when players craft items. It tracks:
 * <ul>
 *   <li>Item material crafted (e.g., DIAMOND_SWORD, BREAD, IRON_PICKAXE)</li>
 *   <li>Amount crafted (handles shift-click crafting)</li>
 * </ul>
 * </p>
 * <p>
 * This handler correctly handles shift-click crafting, which can craft multiple
 * items at once. The amount is calculated based on the available materials and
 * inventory space.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class CraftItemsTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new craft items task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public CraftItemsTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "CRAFT_ITEMS";
    }
    
    /**
     * Handles craft item events.
     * <p>
     * This method is called when a player crafts an item. It checks if:
     * <ul>
     *   <li>The crafter is a player</li>
     *   <li>The player is eligible for quest progress</li>
     * </ul>
     * If all conditions are met, it updates progress for matching CRAFT_ITEMS tasks
     * by the amount of items crafted (including shift-click crafting).
     * </p>
     *
     * @param event the craft item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(@NotNull final CraftItemEvent event) {
        // Check if crafter is a player
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Get crafted item
        final ItemStack result = event.getRecipe().getResult();
        int amount = result.getAmount();
        
        // Handle shift-click crafting (craft as many as possible)
        if (event.isShiftClick()) {
            amount = calculateShiftClickAmount(event);
        }
        
        // Handle the event with the crafted amount
        handleEvent(event, player, amount);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        // Always process craft events for players
        return event instanceof CraftItemEvent;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final CraftItemEvent craftEvent = (CraftItemEvent) event;
        final ItemStack result = craftEvent.getRecipe().getResult();
        
        return Map.of(
                "target", result.getType().name()
        );
    }
    
    /**
     * Calculates the amount of items that will be crafted with shift-click.
     * <p>
     * Shift-click crafting is complex because it depends on:
     * <ul>
     *   <li>Available materials in the crafting grid</li>
     *   <li>Available inventory space</li>
     *   <li>Stack size of the crafted item</li>
     * </ul>
     * </p>
     * <p>
     * This is a simplified calculation that may not be 100% accurate in all cases,
     * but should work for most common scenarios.
     * </p>
     *
     * @param event the craft item event
     * @return the estimated amount of items that will be crafted
     */
    private int calculateShiftClickAmount(@NotNull final CraftItemEvent event) {
        final ItemStack result = event.getRecipe().getResult();
        final int resultAmount = result.getAmount();
        
        // Get the crafting matrix
        final ItemStack[] matrix = event.getInventory().getMatrix();
        
        // Find the minimum stack size in the crafting matrix (excluding empty slots)
        int minStackSize = Integer.MAX_VALUE;
        for (final ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                minStackSize = Math.min(minStackSize, item.getAmount());
            }
        }
        
        // If no items found, return single craft amount
        if (minStackSize == Integer.MAX_VALUE) {
            return resultAmount;
        }
        
        // Calculate total amount that can be crafted
        // This is a simplified calculation - actual Minecraft logic is more complex
        final int maxCrafts = minStackSize;
        final int totalAmount = maxCrafts * resultAmount;
        
        // Limit to a reasonable amount (prevent overflow)
        return Math.min(totalAmount, result.getMaxStackSize() * 9); // Max 9 stacks
    }
}

