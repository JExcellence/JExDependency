package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Task handler for BREAK_BLOCKS quest tasks.
 * <p>
 * This handler listens to {@link BlockBreakEvent} and updates progress for
 * BREAK_BLOCKS tasks when players break blocks. It tracks:
 * <ul>
 *   <li>Block type (e.g., STONE, DIAMOND_ORE, OAK_LOG)</li>
 *   <li>World where the block was broken</li>
 * </ul>
 * </p>
 * <p>
 * Only blocks broken in survival mode are counted. Creative mode breaks
 * are ignored to prevent exploitation.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class BreakBlocksTaskHandler extends BaseTaskHandler {
    
    /**
     * Constructs a new break blocks task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public BreakBlocksTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "MINE_BLOCKS";
    }
    
    /**
     * Handles block break events.
     * <p>
     * Fires progress for both {@code MINE_BLOCKS} and {@code BREAK_BLOCKS} task types
     * so that quest YAMLs using either convention are supported.
     * </p>
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (!isEligible(player) || !shouldProcess(event, player)) {
            return;
        }
        final Map<String, Object> criteria = extractCriteria(event);
        // Fire for both type aliases so either YAML convention works
        updateProgressForType(player, criteria, "MINE_BLOCKS");
        updateProgressForType(player, criteria, "BREAK_BLOCKS");
    }

    /**
     * Fires a progress update for a specific task type.
     *
     * @param player   the player
     * @param criteria the extracted criteria
     * @param taskType the task type to update
     */
    private void updateProgressForType(
            @NotNull final Player player,
            @NotNull final Map<String, Object> criteria,
            @NotNull final String taskType
    ) {
        final java.util.Map<String, Object> fullCriteria = new java.util.HashMap<>(criteria);
        fullCriteria.put("task_type", taskType);
        progressTracker.updateTaskProgress(player.getUniqueId(), taskType, fullCriteria, 1)
                .exceptionally(ex -> {
                    logger.log(java.util.logging.Level.WARNING,
                            "Failed to update " + taskType + " progress for " + player.getName(), ex);
                    return null;
                });
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof BlockBreakEvent breakEvent)) {
            return false;
        }
        
        // Only count valid block breaks (not AIR)
        return breakEvent.getBlock().getType() != Material.AIR;
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final BlockBreakEvent breakEvent = (BlockBreakEvent) event;
        
        return Map.of(
                "target", breakEvent.getBlock().getType().name(),
                "world", breakEvent.getBlock().getWorld().getName()
        );
    }
}
