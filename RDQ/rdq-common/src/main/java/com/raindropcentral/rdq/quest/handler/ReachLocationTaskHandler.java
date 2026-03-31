package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Task handler for REACH_LOCATION quest tasks.
 * <p>
 * This handler listens to {@link PlayerMoveEvent} and updates progress for
 * REACH_LOCATION tasks when players reach specific locations. It tracks:
 * <ul>
 *   <li>Player location (X, Y, Z coordinates)</li>
 *   <li>World where the location is</li>
 * </ul>
 * </p>
 * <p>
 * This handler implements throttling to avoid excessive event processing.
 * Movement events are only processed once per second per player to reduce
 * server load.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class ReachLocationTaskHandler extends BaseTaskHandler {
    
    private static final long THROTTLE_INTERVAL_MS = 1000; // 1 second
    private final Map<UUID, Long> lastCheckTime = new HashMap<>();
    
    /**
     * Constructs a new reach location task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    public ReachLocationTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        super(progressTracker, cacheManager, progressCache);
    }
    
    @Override
    @NotNull
    protected String getTaskType() {
        return "REACH_LOCATION";
    }
    
    /**
     * Handles player move events with throttling.
     * <p>
     * This method is called when a player moves. It implements throttling to
     * avoid excessive processing:
     * <ul>
     *   <li>Only processes movement once per second per player</li>
     *   <li>Ignores small movements (same block)</li>
     *   <li>Checks if player is eligible for quest progress</li>
     * </ul>
     * If all conditions are met, it updates progress for matching REACH_LOCATION tasks.
     * </p>
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        
        // Throttle: Only check once per second
        final long currentTime = System.currentTimeMillis();
        final Long lastCheck = lastCheckTime.get(playerId);
        
        if (lastCheck != null && (currentTime - lastCheck) < THROTTLE_INTERVAL_MS) {
            return;
        }
        
        // Update last check time
        lastCheckTime.put(playerId, currentTime);
        
        // Handle the event
        handleEvent(event, player);
    }
    
    @Override
    protected boolean shouldProcess(@NotNull final Event event, @NotNull final Player player) {
        if (!(event instanceof PlayerMoveEvent moveEvent)) {
            return false;
        }
        
        // Only process if player moved to a different block
        final Location from = moveEvent.getFrom();
        final Location to = moveEvent.getTo();
        
        if (to == null) {
            return false;
        }
        
        // Check if block position changed
        return from.getBlockX() != to.getBlockX() ||
               from.getBlockY() != to.getBlockY() ||
               from.getBlockZ() != to.getBlockZ();
    }
    
    @Override
    @NotNull
    protected Map<String, Object> extractCriteria(@NotNull final Event event) {
        final PlayerMoveEvent moveEvent = (PlayerMoveEvent) event;
        final Location location = moveEvent.getTo();
        
        if (location == null) {
            return Map.of();
        }
        
        return Map.of(
                "x", location.getBlockX(),
                "y", location.getBlockY(),
                "z", location.getBlockZ(),
                "world", location.getWorld().getName()
        );
    }
}
