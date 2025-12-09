package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.utility.BountyFactory;
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
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class  BountyPlayerQuit implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyPlayerQuit.class.getName());

    private final BountyFactory bountyFactory;

    public BountyPlayerQuit(@NotNull RDQ rdq) {
        this.bountyFactory = rdq.getBountyFactory();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();

        bountyFactory.getDamageTracker().clearDamage(player.getUniqueId());
        LOGGER.fine("Cleaned up temporary bounty data for player " + player.getName());
    }
}
