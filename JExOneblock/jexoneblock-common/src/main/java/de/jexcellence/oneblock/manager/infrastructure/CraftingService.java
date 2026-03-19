package de.jexcellence.oneblock.manager.infrastructure;

import de.jexcellence.oneblock.database.entity.infrastructure.*;
import de.jexcellence.oneblock.database.entity.infrastructure.CraftingTask.CraftingTaskType;

import java.util.ArrayList;

/**
 * Crafting Service - Handles all crafting operations and queue management
 * Clean, modern Java - no verbose garbage
 */
public class CraftingService {
    
    /**
     * Processes the crafting queue for an infrastructure
     */
    public void processCraftingQueue(IslandInfrastructure infrastructure) {
        var queue = infrastructure.getCraftingQueue();
        var completedTasks = new ArrayList<CraftingTask>();
        
        for (var task : queue) {
            task.updateProgress();
            
            if (task.isCompleted()) {
                completeCraftingTask(infrastructure, task);
                completedTasks.add(task);
            }
        }
        
        queue.removeAll(completedTasks);
    }
    
    /**
     * Instantly completes a crafting task using energy
     */
    public boolean instantComplete(IslandInfrastructure infrastructure, CraftingTask task) {
        if (!task.canInstantComplete(infrastructure.getCurrentEnergy())) {
            return false;
        }
        
        infrastructure.setCurrentEnergy(infrastructure.getCurrentEnergy() - task.getEnergyCost());
        task.instantComplete(infrastructure.getCurrentEnergy());
        completeCraftingTask(infrastructure, task);
        infrastructure.getCraftingQueue().remove(task);
        
        return true;
    }
    
    /**
     * Validates if a crafting recipe can be started
     */
    public boolean canStartCrafting(IslandInfrastructure infrastructure, AutomationModule module) {
        return hasRequiredMaterials(infrastructure, module.getCraftingRecipe()) &&
               hasRequiredCurrency(infrastructure, module.getCraftingCost()) &&
               !infrastructure.getAutomationModules().contains(module);
    }
    
    /**
     * Validates if a processor upgrade can be started
     */
    public boolean canStartProcessorUpgrade(IslandInfrastructure infrastructure, ProcessorType processor, int level) {
        var upgrade = processor.getUpgrade(level);
        return upgrade != null &&
               hasRequiredMaterials(infrastructure, upgrade.getMaterials()) &&
               hasRequiredCurrency(infrastructure, upgrade.getCost()) &&
               infrastructure.getProcessors().getOrDefault(processor, 0) < level;
    }
    
    private void completeCraftingTask(IslandInfrastructure infrastructure, CraftingTask task) {
        switch (task.getTaskType()) {
            case AUTOMATION_MODULE -> {
                var module = AutomationModule.valueOf(task.getTargetItem());
                infrastructure.getAutomationModules().add(module);
            }
            case PROCESSOR_UPGRADE -> {
                var processor = ProcessorType.valueOf(task.getTargetItem());
                infrastructure.getProcessors().put(processor, task.getTargetLevel());
            }
            case GENERATOR_UPGRADE -> {
                var generator = GeneratorType.valueOf(task.getTargetItem());
                infrastructure.getGenerators().put(generator, task.getTargetLevel());
            }
        }
    }
    
    private boolean hasRequiredMaterials(IslandInfrastructure infrastructure, java.util.List<?> materials) {
        return true; // Placeholder - implement material checking
    }
    
    private boolean hasRequiredCurrency(IslandInfrastructure infrastructure, long cost) {
        return true; // Placeholder - implement currency checking
    }
}
