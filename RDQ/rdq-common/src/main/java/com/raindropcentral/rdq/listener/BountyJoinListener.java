package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listener that updates bounty player displays when a player joins the server.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class BountyJoinListener implements Listener {

    private final RDQ rdq;

    /**
     * Creates a new listener that updates bounty displays using the provided RDQ instance.
     *
     * @param rdq the RDQ plugin instance supplying bounty management services
     */
    public BountyJoinListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Handles player join events by refreshing the bounty display for the joining player.
     *
     * @param event the join event containing the player whose bounty display should be updated
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        this.rdq.getManager().getBountyManager().updateBountyPlayerDisplay(event.getPlayer().getUniqueId());
    }
}
