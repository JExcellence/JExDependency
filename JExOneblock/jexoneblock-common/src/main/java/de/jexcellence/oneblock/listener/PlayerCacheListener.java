package de.jexcellence.oneblock.listener;

import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.service.IslandCacheService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerCacheListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final IslandCacheService islandCacheService;
    private final JExOneblock plugin;
    
    public PlayerCacheListener(@NotNull JExOneblock oneblock) {
        this.plugin = oneblock;
        this.islandCacheService = oneblock.getIslandCacheService();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        islandCacheService.loadPlayerIsland(player.getUniqueId())
            .thenRun(() -> {
                LOGGER.log(Level.FINE, "Island cache loaded for player " + player.getName());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Failed to load island cache for player " + player.getName(), throwable);
                return null;
            });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        islandCacheService.saveAndUnloadPlayerIsland(player.getUniqueId())
            .thenRun(() -> {
                LOGGER.log(Level.FINE, "Island cache saved and unloaded for player " + player.getName());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to save island cache for player " + player.getName(), throwable);
                return null;
            });
        
        if (plugin.getIslandStorageManager() != null) {
            plugin.getIslandStorageManager().handlePlayerLeave(player.getUniqueId());
        }
    }
}