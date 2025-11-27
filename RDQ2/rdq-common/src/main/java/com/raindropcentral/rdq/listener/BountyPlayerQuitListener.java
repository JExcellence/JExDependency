package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for player quit events related to the bounty system.
 * Handles preserving bounty state and cleaning up temporary data when players log out.
 * 
 * Requirements: 14.5
 */
public class BountyPlayerQuitListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(BountyPlayerQuitListener.class.getName());
    
    private final RDQ rdq;
    private final DamageTracker damageTracker;
    
    /**
     * Creates a new BountyPlayerQuitListener.
     * Constructor compatible with CommandFactory auto-registration.
     *
     * @param rdq the RDQ plugin instance
     */
    public BountyPlayerQuitListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
        var config = rdq.getBountyConfig();
        this.damageTracker = new DamageTracker(config.getDamageTrackingWindow());
    }
    
    /**
     * Handles player quit events to preserve bounty state and clean up temporary data.
     * 
     * Requirements:
     * - 14.5: Preserve bounty state on logout
     * 
     * Note: Bounty state is preserved automatically in the database, so no action is needed.
     * This listener primarily handles cleanup of temporary in-memory data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up damage tracking data for this player
        // This prevents memory leaks from tracking damage on players who are no longer online
        damageTracker.clearDamage(player.getUniqueId());
        
        // Note: Bounty state is persisted in the database and will be restored when the player rejoins
        // Visual indicators will be reapplied by BountyPlayerJoinListener
        
        LOGGER.fine("Cleaned up temporary bounty data for player " + player.getName());
    }
}
