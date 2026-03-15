package com.raindropcentral.core.listener;

import com.raindropcentral.core.RCore;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for managing player entities in the local database.
 *
 * <p>Creates or updates RPlayer entities when players join, and updates last seen
 * timestamp when they leave. This maintains local player data for plugin features.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public class PlayerJoinLeaveListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final RCore core;

    private RPlayerRepository playerRepository;

    /**
     * Constructs a new PlayerJoinLeaveListener.
     *
     * @throws NullPointerException if context is null
     */
    public PlayerJoinLeaveListener(final @NotNull RCore core) {
        this.core = core;
        this.playerRepository = this.core.getImpl().getPlayerRepository();
    }

    /**
     * Handles player join events to create/update player entities in local database.
 *
 * <p>Always creates or updates the RPlayer entity with current player information.
     * This maintains accurate local player data for plugin features.
     * </p>
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        playerRepository.findByUuidAsync(player.getUniqueId())
                .thenCompose(existingPlayer -> {
                    var rPlayer = existingPlayer.orElseGet(() -> {
                        LOGGER.fine("Creating new RPlayer for %s (%s)"
                                .formatted(player.getName(), player.getUniqueId()));
                        return new RPlayer(player);
                    });

                    rPlayer.updatePlayerName(player.getName());
                    return playerRepository.createOrUpdateAsync(rPlayer);
                })
                .thenAccept(savedPlayer ->
                        LOGGER.fine("Saved player %s to local database".formatted(player.getName()))
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to save player on join", throwable);
                    return null;
                });
    }

    /**
     * Handles player quit events to update the last seen timestamp.
 *
 * <p>Updates the player's lastSeen timestamp in the local database when they leave.
     * </p>
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();

        playerRepository.findByUuidAsync(player.getUniqueId())
                .thenCompose(rPlayer -> rPlayer
                        .map(p -> {
                            p.updateLastSeen();
                            return playerRepository.createOrUpdateAsync(p);
                        })
                        .orElseGet(() -> {
                            LOGGER.fine("Player %s not found in local database, skipping last seen update"
                                    .formatted(player.getName()));
                            return java.util.concurrent.CompletableFuture.completedFuture(null);
                        })
                )
                .thenAccept(savedPlayer -> {
                    if (savedPlayer != null) {
                        LOGGER.fine("Updated last seen for player %s in local database".formatted(player.getName()));
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to update player last seen", throwable);
                    return null;
                });
    }
}
