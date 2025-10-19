package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Listens for player join events so the RDQ plugin can prepare player rank data as soon as the
 * player connects to the server.
 *
 * <p>The listener currently captures the joining player's unique identifier which can be used by
 * subsequent rank loading routines. Keeping this listener lightweight ensures join handling remains
 * responsive even as additional initialization logic is introduced.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PlayerRankJoinListener implements Listener {

    private final RDQ rdq;

    /**
     * Creates a new listener that operates within the provided RDQ plugin context.
     *
     * @param rdq the RDQ plugin instance supplying rank management services
     */
    public PlayerRankJoinListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Handles the {@link PlayerJoinEvent} emitted when a player connects to the server.
     *
     * @param event the join event containing information about the connecting player
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final UUID playerUUID = event.getPlayer().getUniqueId();
    }
}