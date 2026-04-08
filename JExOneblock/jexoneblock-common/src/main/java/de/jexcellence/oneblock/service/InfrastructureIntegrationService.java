package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.infrastructure.AutomationModule;
import de.jexcellence.oneblock.database.entity.infrastructure.GeneratorType;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.entity.infrastructure.ProcessorType;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureManager;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureTickProcessor;
import de.jexcellence.oneblock.database.repository.IslandInfrastructureRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InfrastructureIntegrationService {
    
    private final InfrastructureManager manager;
    private final InfrastructureTickProcessor tickProcessor;
    private final IslandInfrastructureRepository repository;
    
    public InfrastructureIntegrationService(Plugin plugin, IslandInfrastructureRepository repository) {
        this.repository = repository;
        this.manager = new InfrastructureManager();
        this.tickProcessor = new InfrastructureTickProcessor(plugin, manager, repository);
    }
    
    public void initialize() {
        tickProcessor.start();
    }
    
    public void shutdown() {
        tickProcessor.stop();
    }
    
    public void handleBlockBreak(BlockBreakEvent event, Long islandId) {
        var player = event.getPlayer();
        var material = event.getBlock().getType();
        
        var infra = manager.getInfrastructure(islandId, player.getUniqueId());
        tickProcessor.register(infra);
        
        var dropMultiplier = infra.getPassiveDropMultiplier();
        var amount = (int) Math.ceil(1 * dropMultiplier);
        
        if (infra.getAutomationModules().contains(AutomationModule.AUTO_COLLECTOR)) {
            event.setDropItems(false);
            manager.handleBlockBreak(player, islandId, material, amount);
        }
        
        if (infra.getAutomationModules().contains(AutomationModule.AUTO_SMELTER)) {
            manager.getAutomationService().processSmelting(infra, material, amount);
        }
        
        infra.setTotalBlocksMined(infra.getTotalBlocksMined() + 1);
    }
    
    public void onEvolutionUnlock(Long islandId, UUID ownerId, int newStage) {
        var infra = manager.getInfrastructure(islandId, ownerId);
        
        for (var processor : ProcessorType.values()) {
            if (processor.canBuildAtStage(newStage) && 
                infra.getProcessors().getOrDefault(processor, 0) == 0) {
            }
        }
        
        for (var generator : GeneratorType.values()) {
            if (generator.canBuildAtStage(newStage, infra.getPrestigeLevel() > 0) &&
                infra.getGenerators().getOrDefault(generator, 0) == 0) {
            }
        }
        
        for (var module : AutomationModule.values()) {
            if (module.canCraftAtStage(newStage, infra.getPrestigeLevel() > 0) &&
                !infra.getAutomationModules().contains(module)) {
            }
        }
    }
    
    public void onPrestige(Long islandId, UUID ownerId, int newPrestigeLevel) {
        var infra = manager.getInfrastructure(islandId, ownerId);
        infra.setPrestigeLevel(newPrestigeLevel);
        
        var xpBonus = 1.0 + (newPrestigeLevel * 0.1);
        var dropBonus = 1.0 + (newPrestigeLevel * 0.05);
        
        infra.setPassiveXpMultiplier(infra.getPassiveXpMultiplier() * xpBonus);
        infra.setPassiveDropMultiplier(infra.getPassiveDropMultiplier() * dropBonus);
    }
    
    public IslandInfrastructure getInfrastructure(Long islandId, UUID ownerId) {
        return manager.getInfrastructure(islandId, ownerId);
    }
    
    public CompletableFuture<Optional<IslandInfrastructure>> getInfrastructureAsync(Long islandId) {
        return repository.findByIslandIdAsync(islandId);
    }
    
    public boolean canAffordCrafting(Player player, long cost) {
        return true;
    }
    
    public boolean deductCraftingCost(Player player, long cost) {
        return true;
    }
    
    public int getCurrentStage(UUID islandId) {
        return 1;
    }
    
    public boolean shouldAutoSmelt(Material material) {
        return switch (material) {
            case RAW_IRON, RAW_GOLD, RAW_COPPER, IRON_ORE, GOLD_ORE, COPPER_ORE,
                 COBBLESTONE, SAND, CLAY -> true;
            default -> false;
        };
    }
    
    public InfrastructureManager getManager() {
        return manager;
    }
    
    public InfrastructureTickProcessor getTickProcessor() {
        return tickProcessor;
    }
    
    public IslandInfrastructureRepository getRepository() {
        return repository;
    }
}
