package de.jexcellence.oneblock.manager.infrastructure;

import de.jexcellence.oneblock.database.entity.infrastructure.*;

/**
 * Energy Service - Handles all energy generation, consumption, and distribution
 * Modern Java - clean and efficient
 */
public class EnergyService {
    
    /**
     * Processes energy generation and consumption for an infrastructure
     */
    public void processEnergyGeneration(IslandInfrastructure infrastructure) {
        var generation = infrastructure.calculateEnergyGeneration();
        var consumption = infrastructure.calculateEnergyConsumption();
        var netEnergy = generation - consumption;
        
        var newEnergy = Math.max(0, Math.min(
            infrastructure.getEnergyCapacity(),
            infrastructure.getCurrentEnergy() + (long) netEnergy
        ));
        
        infrastructure.setCurrentEnergy(newEnergy);
        infrastructure.setTotalEnergyGenerated(infrastructure.getTotalEnergyGenerated() + (long) Math.max(0, generation));
        
        if (netEnergy < 0 && infrastructure.getCurrentEnergy() == 0) {
            handleEnergyShortage(infrastructure);
        }
    }
    
    /**
     * Checks if infrastructure has enough energy for an operation
     */
    public boolean hasEnoughEnergy(IslandInfrastructure infrastructure, long requiredEnergy) {
        return infrastructure.getCurrentEnergy() >= requiredEnergy;
    }
    
    /**
     * Consumes energy from infrastructure
     */
    public boolean consumeEnergy(IslandInfrastructure infrastructure, long amount) {
        if (!hasEnoughEnergy(infrastructure, amount)) {
            return false;
        }
        
        infrastructure.setCurrentEnergy(infrastructure.getCurrentEnergy() - amount);
        return true;
    }
    
    /**
     * Gets energy efficiency rating (0.0 to 1.0)
     */
    public double getEnergyEfficiency(IslandInfrastructure infrastructure) {
        var generation = infrastructure.calculateEnergyGeneration();
        var consumption = infrastructure.calculateEnergyConsumption();
        
        if (consumption == 0) return 1.0;
        return Math.min(1.0, generation / consumption);
    }
    
    /**
     * Calculates optimal generator configuration
     */
    public GeneratorRecommendation getGeneratorRecommendation(IslandInfrastructure infrastructure) {
        var currentGeneration = infrastructure.calculateEnergyGeneration();
        var currentConsumption = infrastructure.calculateEnergyConsumption();
        var deficit = currentConsumption - currentGeneration;
        
        if (deficit <= 0) {
            return new GeneratorRecommendation(GeneratorAction.SUFFICIENT, null, 0);
        }
        
        GeneratorType bestGenerator = null;
        int bestLevel = 0;
        double bestEfficiency = 0;
        
        for (var generator : GeneratorType.values()) {
            var currentLevel = infrastructure.getGenerators().getOrDefault(generator, 0);
            if (currentLevel >= generator.getMaxLevel()) continue;
            
            var nextLevel = currentLevel + 1;
            var upgrade = generator.getUpgrade(nextLevel);
            if (upgrade == null) continue;
            
            var efficiency = upgrade.getEnergyPerSecond() / upgrade.getCost();
            if (efficiency > bestEfficiency) {
                bestEfficiency = efficiency;
                bestGenerator = generator;
                bestLevel = nextLevel;
            }
        }
        
        return bestGenerator != null 
            ? new GeneratorRecommendation(GeneratorAction.UPGRADE, bestGenerator, bestLevel)
            : new GeneratorRecommendation(GeneratorAction.NO_OPTIONS, null, 0);
    }
    
    private void handleEnergyShortage(IslandInfrastructure infrastructure) {
        infrastructure.setAutoSmeltingEnabled(false);
        infrastructure.setAutoCraftingEnabled(false);
        
        var essentialModules = java.util.Set.of(
            AutomationModule.AUTO_COLLECTOR,
            AutomationModule.INFINITY_ENGINE
        );
        
        infrastructure.getAutomationModules().retainAll(essentialModules);
    }
    
    public record GeneratorRecommendation(
        GeneratorAction action,
        GeneratorType generator,
        int level
    ) {}
    
    public enum GeneratorAction {
        SUFFICIENT, UPGRADE, NO_OPTIONS
    }
}
