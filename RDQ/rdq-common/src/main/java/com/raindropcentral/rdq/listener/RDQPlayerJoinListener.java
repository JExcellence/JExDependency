package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that creates or updates RDQPlayer entities when players join the server.
 * This ensures that every player has an RDQPlayer record in the database.
 */
public class RDQPlayerJoinListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private final RDQ rdq;
    
    public RDQPlayerJoinListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Run async to avoid blocking the main thread
        rdq.getExecutor().submit(() -> {
            try {
                // Check if player already exists
                var existing = rdq.getPlayerRepository().findByAttributes(
                    java.util.Map.of("uniqueId", player.getUniqueId())
                );
                
                if (existing.isEmpty()) {
                    // Create new RDQPlayer
                    RDQPlayer rdqPlayer = new RDQPlayer(
                        player.getUniqueId(),
                        player.getName()
                    );
                    
                    rdq.getPlayerRepository().create(rdqPlayer);
                    LOGGER.info("Created RDQPlayer for " + player.getName());
                } else {
                    // Update player name if it changed
                    RDQPlayer rdqPlayer = existing.get();
                    if (!rdqPlayer.getPlayerName().equals(player.getName())) {
                        rdqPlayer.setPlayerName(player.getName());
                        rdq.getPlayerRepository().update(rdqPlayer);
                        LOGGER.fine("Updated player name for " + player.getName());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create/update RDQPlayer for " + player.getName(), e);
            }
        });
    }
}
