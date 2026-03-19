package de.jexcellence.oneblock.listener;

import de.jexcellence.oneblock.JExOneblock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class InfrastructurePlayerListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(InfrastructurePlayerListener.class.getName());
    
    private final JExOneblock plugin;
    
    public InfrastructurePlayerListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        var islandId = plugin.getOneblockService().getPlayerIslandId(player);
        if (islandId == null) return;
        
        plugin.getInfrastructureService().getInfrastructureAsync(islandId)
            .thenAccept(opt -> opt.ifPresent(infra -> {
                plugin.getInfrastructureService().getTickProcessor().register(infra);
                LOGGER.fine("Loaded infrastructure for player " + player.getName());
            }));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        
        var islandId = plugin.getOneblockService().getPlayerIslandId(player);
        if (islandId == null) return;
        
        LOGGER.fine("Player " + player.getName() + " quit, infrastructure remains active");
    }
}
