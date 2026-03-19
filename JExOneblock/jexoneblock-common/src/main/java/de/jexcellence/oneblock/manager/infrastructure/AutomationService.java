package de.jexcellence.oneblock.manager.infrastructure;

import de.jexcellence.oneblock.database.entity.infrastructure.*;
import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Automation Service - Handles all automation module processing
 * Clean modern Java - no verbose patterns
 */
public class AutomationService {
    
    private final EnergyService energyService;
    
    public AutomationService(EnergyService energyService) {
        this.energyService = energyService;
    }
    
    /**
     * Processes all automation for an infrastructure
     */
    public void processAutomation(IslandInfrastructure infrastructure) {
        if (infrastructure.getCurrentEnergy() <= 0) return;
        
        for (var module : infrastructure.getAutomationModules()) {
            if (!energyService.hasEnoughEnergy(infrastructure, (long) module.getEnergyConsumption())) {
                continue;
            }
            
            processAutomationModule(infrastructure, module);
            energyService.consumeEnergy(infrastructure, (long) module.getEnergyConsumption());
        }
        
        processProcessors(infrastructure);
    }
    
    /**
     * Processes smelting automation
     */
    public void processSmelting(IslandInfrastructure infrastructure, Material material, int amount) {
        if (!infrastructure.getAutomationModules().contains(AutomationModule.AUTO_SMELTER)) {
            return;
        }
        
        var smeltedMaterial = getSmeltedResult(material);
        if (smeltedMaterial != null) {
            var smelterLevel = infrastructure.getProcessors().getOrDefault(ProcessorType.ADVANCED_SMELTER, 0);
            var multiplier = smelterLevel > 0 ? ProcessorType.ADVANCED_SMELTER.getProcessingSpeed(smelterLevel) : 1.0;
            
            var smeltedAmount = (int) (amount * multiplier);
            storeItem(infrastructure, smeltedMaterial, smeltedAmount);
        }
    }
    
    private void processAutomationModule(IslandInfrastructure infrastructure, AutomationModule module) {
        switch (module) {
            case AUTO_COLLECTOR -> processAutoCollection(infrastructure);
            case AUTO_SMELTER -> processAutoSmelting(infrastructure);
            case AUTO_CRAFTER -> processAutoCrafting(infrastructure);
            case AUTO_SELLER -> processAutoSelling(infrastructure);
            case QUANTUM_COMPRESSOR -> processQuantumCompression(infrastructure);
            case EXPERIENCE_AMPLIFIER -> processExperienceAmplification(infrastructure);
            case DIMENSIONAL_STORAGE -> processDimensionalStorage(infrastructure);
            case QUANTUM_MULTIPLIER -> processQuantumMultiplier(infrastructure);
            case REALITY_PROCESSOR -> processRealityProcessor(infrastructure);
            case INFINITY_ENGINE -> processInfinityEngine(infrastructure);
            case OMNIPOTENT_CORE -> processOmnipotentCore(infrastructure);
        }
    }
    
    private void processProcessors(IslandInfrastructure infrastructure) {
        var minerLevel = infrastructure.getProcessors().getOrDefault(ProcessorType.BASIC_MINER, 0);
        if (minerLevel > 0) {
            var miningSpeed = ProcessorType.BASIC_MINER.getProcessingSpeed(minerLevel);
            var blocksToMine = (int) (miningSpeed * ThreadLocalRandom.current().nextDouble(0.8, 1.2));
            
            for (int i = 0; i < blocksToMine; i++) {
                var material = generateRandomMaterial();
                storeItem(infrastructure, material, 1);
                infrastructure.setTotalBlocksMined(infrastructure.getTotalBlocksMined() + 1);
            }
        }
        
        var assemblerLevel = infrastructure.getProcessors().getOrDefault(ProcessorType.MOLECULAR_ASSEMBLER, 0);
        if (assemblerLevel > 0) {
            var speed = ProcessorType.MOLECULAR_ASSEMBLER.getProcessingSpeed(assemblerLevel);
            if (ThreadLocalRandom.current().nextDouble() < speed * 0.01) {
                var rareMaterial = generateRareMaterial();
                storeItem(infrastructure, rareMaterial, 1);
            }
        }
    }
    
    private void processAutoCollection(IslandInfrastructure infrastructure) {
        infrastructure.setPassiveDropMultiplier(infrastructure.getPassiveDropMultiplier() * 1.1);
    }
    
    private void processAutoSmelting(IslandInfrastructure infrastructure) {
        var rawMaterials = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT
        );
        
        for (var entry : rawMaterials.entrySet()) {
            var rawAmount = infrastructure.getStoredItems().getOrDefault(entry.getKey(), 0L);
            if (rawAmount > 0) {
                var toSmelt = Math.min(rawAmount, 64L);
                infrastructure.getStoredItems().put(entry.getKey(), rawAmount - toSmelt);
                storeItem(infrastructure, entry.getValue(), (int) toSmelt);
            }
        }
    }
    
    private void processAutoCrafting(IslandInfrastructure infrastructure) {
        var craftingRecipes = Map.of(
            Material.STICK, Map.of(Material.OAK_PLANKS, 2),
            Material.TORCH, Map.of(Material.STICK, 1, Material.COAL, 1),
            Material.IRON_BLOCK, Map.of(Material.IRON_INGOT, 9)
        );
        
        for (var recipe : craftingRecipes.entrySet()) {
            if (canCraft(infrastructure, recipe.getValue())) {
                consumeMaterials(infrastructure, recipe.getValue());
                storeItem(infrastructure, recipe.getKey(), 1);
            }
        }
    }
    
    private void processAutoSelling(IslandInfrastructure infrastructure) {
        var sellPrices = Map.of(
            Material.COBBLESTONE, 1L,
            Material.DIRT, 1L,
            Material.STONE, 2L,
            Material.COAL, 5L
        );
        
        for (var entry : sellPrices.entrySet()) {
            var amount = infrastructure.getStoredItems().getOrDefault(entry.getKey(), 0L);
            if (amount > 1000) {
                var toSell = amount / 2;
                infrastructure.getStoredItems().put(entry.getKey(), amount - toSell);
            }
        }
    }
    
    private void processQuantumCompression(IslandInfrastructure infrastructure) {
        // Handled in storage calculation
    }
    
    private void processExperienceAmplification(IslandInfrastructure infrastructure) {
        // Handled in passive calculation
    }
    
    private void processDimensionalStorage(IslandInfrastructure infrastructure) {
        infrastructure.setDimensionalStorageUnlocked(true);
    }
    
    private void processQuantumMultiplier(IslandInfrastructure infrastructure) {
        infrastructure.setPassiveDropMultiplier(infrastructure.getPassiveDropMultiplier() * 5.0);
    }
    
    private void processRealityProcessor(IslandInfrastructure infrastructure) {
        infrastructure.setQuantumEfficiency(Double.MAX_VALUE);
    }
    
    private void processInfinityEngine(IslandInfrastructure infrastructure) {
        infrastructure.setCurrentEnergy(infrastructure.getEnergyCapacity());
        infrastructure.setEnergyGenerationRate(Double.MAX_VALUE);
    }
    
    private void processOmnipotentCore(IslandInfrastructure infrastructure) {
        infrastructure.setQuantumEfficiency(Double.MAX_VALUE);
        infrastructure.setCurrentEnergy(infrastructure.getEnergyCapacity());
        infrastructure.setAutoMiningSpeed(Double.MAX_VALUE);
    }
    
    private Material getSmeltedResult(Material material) {
        return switch (material) {
            case RAW_IRON, IRON_ORE -> Material.IRON_INGOT;
            case RAW_GOLD, GOLD_ORE -> Material.GOLD_INGOT;
            case RAW_COPPER, COPPER_ORE -> Material.COPPER_INGOT;
            case COBBLESTONE -> Material.STONE;
            case SAND -> Material.GLASS;
            default -> null;
        };
    }
    
    private void storeItem(IslandInfrastructure infrastructure, Material material, int amount) {
        var current = infrastructure.getStoredItems().getOrDefault(material, 0L);
        infrastructure.getStoredItems().put(material, current + amount);
    }
    
    private Material generateRandomMaterial() {
        var commonMaterials = new Material[]{Material.COBBLESTONE, Material.DIRT, Material.STONE};
        return commonMaterials[ThreadLocalRandom.current().nextInt(commonMaterials.length)];
    }
    
    private Material generateRareMaterial() {
        var rareMaterials = new Material[]{Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT};
        return rareMaterials[ThreadLocalRandom.current().nextInt(rareMaterials.length)];
    }
    
    private boolean canCraft(IslandInfrastructure infrastructure, Map<Material, Integer> recipe) {
        return recipe.entrySet().stream().allMatch(entry -> 
            infrastructure.getStoredItems().getOrDefault(entry.getKey(), 0L) >= entry.getValue()
        );
    }
    
    private void consumeMaterials(IslandInfrastructure infrastructure, Map<Material, Integer> recipe) {
        recipe.forEach((material, amount) -> {
            var current = infrastructure.getStoredItems().getOrDefault(material, 0L);
            infrastructure.getStoredItems().put(material, current - amount);
        });
    }
}
