package de.jexcellence.oneblock.listener;

import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.infrastructure.AutomationModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class InfrastructureBlockBreakListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(InfrastructureBlockBreakListener.class.getName());
    
    private final JExOneblock plugin;
    
    public InfrastructureBlockBreakListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        var location = block.getLocation();
        
        var islandId = plugin.getOneblockService().getIslandIdAtLocation(location);
        if (islandId == null) return;
        
        if (!plugin.getOneblockService().isOneBlockLocation(location)) return;
        
        var infra = plugin.getInfrastructureService().getInfrastructure(islandId, player.getUniqueId());
        var material = block.getType();
        
        var dropMultiplier = infra.getPassiveDropMultiplier();
        var amount = (int) Math.ceil(1 * dropMultiplier);
        
        if (infra.getAutomationModules().contains(AutomationModule.AUTO_COLLECTOR)) {
            event.setDropItems(false);
            plugin.getInfrastructureService().getManager().handleBlockBreak(player, islandId, material, amount);
        }
        
        if (infra.getAutomationModules().contains(AutomationModule.AUTO_SMELTER)) {
            plugin.getInfrastructureService().getManager().getAutomationService()
                .processSmelting(infra, material, amount);
        }
        
        infra.setTotalBlocksMined(infra.getTotalBlocksMined() + 1);
        
        plugin.getInfrastructureService().getTickProcessor().register(infra);
    }
}
