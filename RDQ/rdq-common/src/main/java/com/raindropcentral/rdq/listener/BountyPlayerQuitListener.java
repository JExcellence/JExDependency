package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for player quit events related to the bounty system.
 * Handles cleanup of visual indicators when players leave the server.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyPlayerQuitListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyPlayerQuitListener.class.getName());

    private final RDQ rdq;

    public BountyPlayerQuitListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();

        // Clean up visual indicators tracking when player quits
        // We use the UUID version to avoid restoring names since player is leaving
        if (rdq.getVisualIndicatorManager().hasIndicators(playerId)) {
            rdq.getVisualIndicatorManager().removeIndicators(playerId);
            LOGGER.fine("Cleaned up visual indicators tracking for " + player.getName() + " on quit");
        }
    }
}