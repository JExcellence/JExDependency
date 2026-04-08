package de.jexcellence.oneblock.manager.infrastructure;

import de.jexcellence.oneblock.database.entity.infrastructure.*;
import de.jexcellence.oneblock.database.entity.infrastructure.CraftingTask.CraftingTaskType;
import de.jexcellence.oneblock.database.entity.storage.StorageTier;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Infrastructure Manager - Core service for island automation and infrastructure
 * Modern Java approach - no verbose nonsense, clean and efficient
 */
public class InfrastructureManager {
    
    private final Map<Long, IslandInfrastructure> infrastructureCache = new ConcurrentHashMap<>();
    private final CraftingService craftingService;
    private final EnergyService energyService;
    private final AutomationService automationService;
    
    public InfrastructureManager() {
        this.energyService = new EnergyService();
        this.automationService = new AutomationService(energyService);
        this.craftingService = new CraftingService();
    }
    
    /**
     * Gets or creates infrastructure for an island
     */
    public IslandInfrastructure getInfrastructure(Long islandId, UUID ownerId) {
        return infrastructureCache.computeIfAbsent(islandId, id -> 
            loadFromDatabase(id, ownerId).orElseGet(() -> new IslandInfrastructure(id, ownerId))
        );
    }
    
    /**
     * Handles OneBlock break with infrastructure integration
     */
    public void handleBlockBreak(Player player, Long islandId, Material material, int amount) {
        var infrastructure = getInfrastructure(islandId, player.getUniqueId());
        
        // Auto-collection if enabled
        if (infrastructure.getAutomationModules().contains(AutomationModule.AUTO_COLLECTOR)) {
            var overflow = storeItem(infrastructure, material, amount);
            if (overflow > 0) giveItemsToPlayer(player, material, overflow);
        } else {
            giveItemsToPlayer(player, material, amount);
        }
        
        // Auto-smelting if enabled
        if (infrastructure.getAutomationModules().contains(AutomationModule.AUTO_SMELTER)) {
            automationService.processSmelting(infrastructure, material, amount);
        }
        
        // Update statistics
        infrastructure.setTotalBlocksMined(infrastructure.getTotalBlocksMined() + amount);
        saveToDatabase(infrastructure);
    }
    
    /**
     * Upgrades storage tier
     */
    public UpgradeResult upgradeStorage(Player player, Long islandId, StorageTier newTier) {
        var infrastructure = getInfrastructure(islandId, player.getUniqueId());
        
        if (!canUpgradeStorage(infrastructure, newTier)) {
            return UpgradeResult.REQUIREMENTS_NOT_MET;
        }
        
        if (!consumeUpgradeResources(infrastructure, newTier)) {
            return UpgradeResult.INSUFFICIENT_RESOURCES;
        }
        
        infrastructure.setStorageTier(newTier);
        saveToDatabase(infrastructure);
        
        return UpgradeResult.SUCCESS;
    }
    
    /**
     * Crafts an automation module
     */
    public CraftingResult craftAutomationModule(Player player, Long islandId, AutomationModule module) {
        var infrastructure = getInfrastructure(islandId, player.getUniqueId());
        
        if (!canCraftModule(infrastructure, module)) {
            return CraftingResult.REQUIREMENTS_NOT_MET;
        }
        
        if (!consumeCraftingResources(infrastructure, module)) {
            return CraftingResult.INSUFFICIENT_RESOURCES;
        }
        
        var craftingTime = calculateCraftingTime(module);
        var task = new CraftingTask(
            CraftingTaskType.AUTOMATION_MODULE,
            module.name(),
            0,
            LocalDateTime.now().plusSeconds(craftingTime),
            module.getCraftingCost()
        );
        
        infrastructure.getCraftingQueue().add(task);
        saveToDatabase(infrastructure);
        
        return CraftingResult.QUEUED;
    }
    
    /**
     * Upgrades a processor
     */
    public CraftingResult upgradeProcessor(Player player, Long islandId, ProcessorType processor) {
        var infrastructure = getInfrastructure(islandId, player.getUniqueId());
        var currentLevel = infrastructure.getProcessors().getOrDefault(processor, 0);
        var nextLevel = currentLevel + 1;
        
        if (nextLevel > processor.getMaxLevel()) {
            return CraftingResult.MAX_LEVEL_REACHED;
        }
        
        if (!canUpgradeProcessor(infrastructure, processor, nextLevel)) {
            return CraftingResult.REQUIREMENTS_NOT_MET;
        }
        
        var upgrade = processor.getUpgrade(nextLevel);
        if (!consumeProcessorUpgradeResources(infrastructure, upgrade)) {
            return CraftingResult.INSUFFICIENT_RESOURCES;
        }
        
        var craftingTime = calculateProcessorUpgradeTime(processor, nextLevel);
        var task = new CraftingTask(
            CraftingTaskType.PROCESSOR_UPGRADE,
            processor.name(),
            nextLevel,
            LocalDateTime.now().plusSeconds(craftingTime),
            upgrade.getCost()
        );
        
        infrastructure.getCraftingQueue().add(task);
        saveToDatabase(infrastructure);
        
        return CraftingResult.QUEUED;
    }
    
    /**
     * Processes all infrastructure systems (called every tick)
     */
    public void processTick() {
        infrastructureCache.values().parallelStream().forEach(infrastructure -> {
            energyService.processEnergyGeneration(infrastructure);
            automationService.processAutomation(infrastructure);
            craftingService.processCraftingQueue(infrastructure);
            
            if (shouldSaveToDatabase(infrastructure)) {
                saveToDatabase(infrastructure);
            }
        });
    }
    
    /**
     * Gets infrastructure statistics for display
     */
    public InfrastructureStats getStats(UUID islandId) {
        var infrastructure = infrastructureCache.get(islandId);
        if (infrastructure == null) return InfrastructureStats.empty();
        
        return new InfrastructureStats(
            infrastructure.getCurrentEnergy(),
            infrastructure.getEnergyCapacity(),
            infrastructure.calculateEnergyGeneration(),
            infrastructure.calculateEnergyConsumption(),
            infrastructure.getStoredItems().size(),
            infrastructure.getTotalStorageCapacity(),
            infrastructure.getPassiveXpMultiplier(),
            infrastructure.getPassiveDropMultiplier(),
            infrastructure.getCraftingQueue().size(),
            infrastructure.getTotalBlocksMined(),
            infrastructure.getPrestigeLevel()
        );
    }
    
    // Getters for services
    public CraftingService getCraftingService() { return craftingService; }
    public EnergyService getEnergyService() { return energyService; }
    public AutomationService getAutomationService() { return automationService; }
    
    // Private helper methods
    private long storeItem(IslandInfrastructure infrastructure, Material material, int amount) {
        var rarity = getRarityForMaterial(material);
        var capacity = infrastructure.getStorageCapacity(rarity);
        var current = infrastructure.getStoredItems().getOrDefault(material, 0L);
        
        var canStore = Math.min(amount, capacity - current);
        if (canStore > 0) {
            infrastructure.getStoredItems().put(material, current + canStore);
        }
        
        return amount - canStore;
    }
    
    private void giveItemsToPlayer(Player player, Material material, long amount) {
        while (amount > 0) {
            var stackSize = (int) Math.min(amount, material.getMaxStackSize());
            var item = new org.bukkit.inventory.ItemStack(material, stackSize);
            var overflow = player.getInventory().addItem(item);
            overflow.values().forEach(dropped -> 
                player.getWorld().dropItem(player.getLocation(), dropped));
            amount -= stackSize;
        }
    }
    
    private boolean canUpgradeStorage(IslandInfrastructure infrastructure, StorageTier newTier) {
        return newTier.ordinal() == infrastructure.getStorageTier().ordinal() + 1;
    }
    
    private boolean canCraftModule(IslandInfrastructure infrastructure, AutomationModule module) {
        return !infrastructure.getAutomationModules().contains(module) &&
               hasRequiredPrestige(infrastructure, module.isPrestigeOnly());
    }
    
    private boolean canUpgradeProcessor(IslandInfrastructure infrastructure, ProcessorType processor, int level) {
        return infrastructure.canSupportUpgrade(processor, level);
    }
    
    private boolean consumeUpgradeResources(IslandInfrastructure infrastructure, StorageTier tier) {
        return true; // Placeholder - implement resource consumption
    }
    
    private boolean consumeCraftingResources(IslandInfrastructure infrastructure, AutomationModule module) {
        return true; // Placeholder - implement resource consumption
    }
    
    private boolean consumeProcessorUpgradeResources(IslandInfrastructure infrastructure, ProcessorType.ProcessorUpgrade upgrade) {
        return true; // Placeholder - implement resource consumption
    }
    
    private long calculateCraftingTime(AutomationModule module) {
        return switch (module) {
            case AUTO_COLLECTOR, AUTO_SMELTER -> 300;
            case AUTO_CRAFTER, AUTO_SELLER -> 900;
            case QUANTUM_COMPRESSOR, EXPERIENCE_AMPLIFIER -> 1800;
            case DIMENSIONAL_STORAGE, QUANTUM_MULTIPLIER -> 3600;
            case REALITY_PROCESSOR -> 7200;
            case INFINITY_ENGINE, OMNIPOTENT_CORE -> 14400;
        };
    }
    
    private long calculateProcessorUpgradeTime(ProcessorType processor, int level) {
        return 600L * level * (processor.ordinal() + 1);
    }
    
    private boolean hasRequiredPrestige(IslandInfrastructure infrastructure, boolean prestigeOnly) {
        return !prestigeOnly || infrastructure.getPrestigeLevel() > 0;
    }
    
    private EEvolutionRarityType getRarityForMaterial(Material material) {
        return EEvolutionRarityType.COMMON; // Placeholder - implement rarity mapping
    }
    
    private boolean shouldSaveToDatabase(IslandInfrastructure infrastructure) {
        return LocalDateTime.now().isAfter(infrastructure.getUpdatedAt().plusMinutes(5));
    }
    
    private Optional<IslandInfrastructure> loadFromDatabase(Long islandId, UUID ownerId) {
        return Optional.empty(); // Placeholder - implement database loading
    }
    
    private void saveToDatabase(IslandInfrastructure infrastructure) {
        infrastructure.setUpdatedAt(LocalDateTime.now());
        // Placeholder - implement database saving
    }
    
    // Result enums
    public enum UpgradeResult {
        SUCCESS, REQUIREMENTS_NOT_MET, INSUFFICIENT_RESOURCES, MAX_LEVEL_REACHED
    }
    
    public enum CraftingResult {
        SUCCESS, QUEUED, REQUIREMENTS_NOT_MET, INSUFFICIENT_RESOURCES, MAX_LEVEL_REACHED
    }
    
    // Statistics record
    public record InfrastructureStats(
        long currentEnergy,
        long maxEnergy,
        double energyGeneration,
        double energyConsumption,
        int storedItemTypes,
        long totalStorageCapacity,
        double passiveXpMultiplier,
        double passiveDropMultiplier,
        int queuedTasks,
        long totalBlocksMined,
        int prestigeLevel
    ) {
        public static InfrastructureStats empty() {
            return new InfrastructureStats(0, 0, 0, 0, 0, 0, 1.0, 1.0, 0, 0, 0);
        }
    }
}
