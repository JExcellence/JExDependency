package de.jexcellence.oneblock.listener;

import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class OneblockPlayerListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private final JExOneblock plugin;
    
    public OneblockPlayerListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        plugin.getOneblockPlayerRepository().findByUuidAsync(player.getUniqueId())
            .thenAccept(existingPlayerOpt -> {
                if (existingPlayerOpt.isPresent()) {
                    var existingPlayer = existingPlayerOpt.get();
                    existingPlayer.setPlayerName(player.getName());
                    existingPlayer.updateLastSeen();
                    existingPlayer.setActive(true);
                    
                    plugin.getOneblockPlayerRepository().updateAsync(existingPlayer)
                        .thenAccept(updatedPlayer -> LOGGER.fine("Updated OneblockPlayer for " + player.getName()))
                        .exceptionally(throwable -> {
                            LOGGER.warning("Failed to update OneblockPlayer for " + player.getName() + ": " + throwable.getMessage());
                            return null;
                        });
                } else {
                    var newPlayer = new OneblockPlayer(player);
                    plugin.getOneblockPlayerRepository().createAsync(newPlayer)
                        .thenAccept(savedPlayer -> LOGGER.info("Created OneblockPlayer for: " + player.getName()))
                        .exceptionally(throwable -> {
                            LOGGER.warning("Failed to create OneblockPlayer for " + player.getName() + ": " + throwable.getMessage());
                            return null;
                        });
                }
            })
            .exceptionally(throwable -> {
                LOGGER.warning("Error handling player join for " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
    }
}