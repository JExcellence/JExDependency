package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for PLACE_BLOCKS quest tasks.
 * <p>
 * This handler listens to {@link BlockPlaceEvent} and updates progress for
 * PLACE_BLOCKS tasks when players place blocks. It tracks:
 * <ul>
 *   <li>Block type (e.g., STONE, COBBLESTONE, OAK_PLANKS)</li>
 *   <li>World where the block was placed</li>
 * </ul>
 * <p>
 * Only blocks placed in survival mode are counted. Creative mode placements
 * are ignored to prevent exploitation.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class PlaceBlocksTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new place blocks task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public PlaceBlocksTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "PLACE_BLOCKS";
    }
    
    /**
     * Handles block place events.
     * <p>
     * This method is called when a player places a block. It checks if:
     * <ul>
     *   <li>The player is eligible for quest progress</li>
     *   <li>The block is a valid material (not AIR)</li>
     * </ul>
     * If all conditions are met, it updates progress for matching PLACE_BLOCKS tasks.
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(@NotNull final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        
        // Handle the event
        handleEvent(event, player);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof BlockPlaceEvent placeEvent)) {
            return false;
        }
        
        // Only count valid block placements (not AIR)
        return placeEvent.getBlock().getType() != Material.AIR;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final BlockPlaceEvent placeEvent = (BlockPlaceEvent) event;
        
        return Map.of(
                "target", placeEvent.getBlock().getType().name(),
                "world", placeEvent.getBlock().getWorld().getName()
        );
    }
}
