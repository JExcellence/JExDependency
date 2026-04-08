package de.jexcellence.oneblock.database.entity.infrastructure;

import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.oneblock.database.entity.storage.StorageTier;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Island Infrastructure - Complete automation and storage system
 * Database entity for persistent island upgrades and automation
 */
@Setter
@Getter
@Entity
@Table(name = "island_infrastructure")
public class IslandInfrastructure extends BaseEntity {

    @Column(name = "island_id", unique = true, nullable = false)
    private Long islandId;
    
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_tier", nullable = false)
    private StorageTier storageTier = StorageTier.BASIC;
    
    @ElementCollection
    @CollectionTable(name = "island_storage_items", joinColumns = @JoinColumn(name = "island_id"))
    @MapKeyColumn(name = "material")
    @Column(name = "amount")
    private Map<Material, Long> storedItems = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "island_automation_modules", joinColumns = @JoinColumn(name = "island_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "module_type")
    private Set<AutomationModule> automationModules = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "island_processors", joinColumns = @JoinColumn(name = "island_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "level")
    private Map<ProcessorType, Integer> processors = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "island_generators", joinColumns = @JoinColumn(name = "island_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "level")
    private Map<GeneratorType, Integer> generators = new HashMap<>();
    
    @Column(name = "energy_capacity")
    private long energyCapacity = 1000L;
    
    @Column(name = "current_energy")
    private long currentEnergy = 1000L;
    
    @Column(name = "energy_generation_rate")
    private double energyGenerationRate = 1.0;
    
    @Column(name = "passive_xp_multiplier")
    private double passiveXpMultiplier = 1.0;
    
    @Column(name = "passive_drop_multiplier")
    private double passiveDropMultiplier = 1.0;
    
    @Column(name = "auto_mining_speed")
    private double autoMiningSpeed = 0.0;
    
    @Column(name = "auto_smelting_enabled")
    private boolean autoSmeltingEnabled = false;
    
    @Column(name = "auto_crafting_enabled")
    private boolean autoCraftingEnabled = false;
    
    @Column(name = "auto_selling_enabled")
    private boolean autoSellingEnabled = false;
    
    @Column(name = "quantum_efficiency")
    private double quantumEfficiency = 1.0;
    
    @Column(name = "dimensional_storage_unlocked")
    private boolean dimensionalStorageUnlocked = false;
    
    @Column(name = "last_passive_reward")
    private LocalDateTime lastPassiveReward = LocalDateTime.now();
    
    @Column(name = "total_blocks_mined")
    private long totalBlocksMined = 0L;
    
    @Column(name = "total_energy_generated")
    private long totalEnergyGenerated = 0L;
    
    @Column(name = "prestige_level")
    private int prestigeLevel = 0;
    
    @ElementCollection
    @CollectionTable(name = "island_upgrade_materials", joinColumns = @JoinColumn(name = "island_id"))
    @MapKeyColumn(name = "material")
    @Column(name = "amount")
    private Map<Material, Long> upgradeBank = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "island_crafting_queue", joinColumns = @JoinColumn(name = "island_id"))
    private List<CraftingTask> craftingQueue = new ArrayList<>();

    // Constructors
    public IslandInfrastructure() {}
    
    public IslandInfrastructure(Long islandId, UUID ownerId) {
        this.islandId = islandId;
        this.ownerId = ownerId;
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Initialize basic processors
        processors.put(ProcessorType.BASIC_MINER, 0);
        processors.put(ProcessorType.ADVANCED_SMELTER, 0);
        processors.put(ProcessorType.QUANTUM_CRAFTER, 0);
        processors.put(ProcessorType.DIMENSIONAL_PROCESSOR, 0);
        
        // Initialize basic generators
        generators.put(GeneratorType.COAL_GENERATOR, 0);
        generators.put(GeneratorType.SOLAR_PANEL, 0);
        generators.put(GeneratorType.NUCLEAR_REACTOR, 0);
        generators.put(GeneratorType.FUSION_CORE, 0);
        generators.put(GeneratorType.COSMIC_HARVESTER, 0);
    }
    
    /**
     * Calculates total storage capacity for a specific rarity
     */
    public long getStorageCapacity(EEvolutionRarityType rarity) {
        long baseCapacity = storageTier.getCapacityForRarity(rarity);
        
        // Apply automation module bonuses
        double multiplier = 1.0;
        if (automationModules.contains(AutomationModule.DIMENSIONAL_STORAGE)) {
            multiplier *= 10.0; // 10x capacity with dimensional storage
        }
        if (automationModules.contains(AutomationModule.QUANTUM_COMPRESSOR)) {
            multiplier *= 5.0; // 5x capacity with quantum compression
        }
        
        return (long) (baseCapacity * multiplier);
    }
    
    /**
     * Calculates current energy generation per second
     */
    public double calculateEnergyGeneration() {
        double totalGeneration = 0.0;
        
        for (Map.Entry<GeneratorType, Integer> entry : generators.entrySet()) {
            if (entry.getValue() > 0) {
                totalGeneration += entry.getKey().getEnergyPerSecond() * entry.getValue();
            }
        }
        
        // Apply quantum efficiency bonus
        totalGeneration *= quantumEfficiency;
        
        return totalGeneration;
    }
    
    /**
     * Calculates total storage capacity across all rarities
     */
    public long getTotalStorageCapacity() {
        long total = storageTier.getTotalCapacity();
        
        // Apply automation module bonuses
        if (automationModules.contains(AutomationModule.DIMENSIONAL_STORAGE)) {
            total = total == Long.MAX_VALUE ? total : total * 10;
        }
        if (automationModules.contains(AutomationModule.QUANTUM_COMPRESSOR)) {
            total = total == Long.MAX_VALUE ? total : total * 5;
        }
        
        return total;
    }
    
    /**
     * Calculates current energy consumption per second
     */
    public double calculateEnergyConsumption() {
        double totalConsumption = 0.0;
        
        // Processor consumption
        for (Map.Entry<ProcessorType, Integer> entry : processors.entrySet()) {
            if (entry.getValue() > 0) {
                totalConsumption += entry.getKey().getEnergyConsumption() * entry.getValue();
            }
        }
        
        // Automation module consumption
        for (AutomationModule module : automationModules) {
            totalConsumption += module.getEnergyConsumption();
        }
        
        return totalConsumption;
    }
    
    /**
     * Checks if infrastructure can support a new upgrade
     */
    public boolean canSupportUpgrade(ProcessorType processor, int newLevel) {
        // Calculate energy consumption with new upgrade
        double newConsumption = calculateEnergyConsumption();
        newConsumption += processor.getEnergyConsumption() * (newLevel - processors.getOrDefault(processor, 0));
        
        return calculateEnergyGeneration() >= newConsumption;
    }
    
    /**
     * Upgrades a processor to the next level
     */
    public boolean upgradeProcessor(ProcessorType processor) {
        int currentLevel = processors.getOrDefault(processor, 0);
        int nextLevel = currentLevel + 1;
        
        if (nextLevel <= processor.getMaxLevel() && canSupportUpgrade(processor, nextLevel)) {
            processors.put(processor, nextLevel);
            updatePassiveBonuses();
            return true;
        }
        return false;
    }
    
    /**
     * Upgrades a generator to the next level
     */
    public boolean upgradeGenerator(GeneratorType generator) {
        int currentLevel = generators.getOrDefault(generator, 0);
        int nextLevel = currentLevel + 1;
        
        if (nextLevel <= generator.getMaxLevel()) {
            generators.put(generator, nextLevel);
            updateEnergyCapacity();
            return true;
        }
        return false;
    }
    
    /**
     * Adds an automation module
     */
    public boolean addAutomationModule(AutomationModule module) {
        if (!automationModules.contains(module) && canSupportUpgrade(null, 0)) {
            automationModules.add(module);
            updatePassiveBonuses();
            return true;
        }
        return false;
    }
    
    private void updatePassiveBonuses() {
        // Calculate passive bonuses based on infrastructure
        passiveXpMultiplier = storageTier.getPassiveXpMultiplier();
        passiveDropMultiplier = storageTier.getPassiveDropMultiplier();
        
        // Apply processor bonuses
        if (processors.getOrDefault(ProcessorType.QUANTUM_CRAFTER, 0) > 0) {
            passiveXpMultiplier *= 1.5;
        }
        if (processors.getOrDefault(ProcessorType.DIMENSIONAL_PROCESSOR, 0) > 0) {
            passiveDropMultiplier *= 2.0;
        }
        
        // Apply automation module bonuses
        if (automationModules.contains(AutomationModule.EXPERIENCE_AMPLIFIER)) {
            passiveXpMultiplier *= 3.0;
        }
        if (automationModules.contains(AutomationModule.QUANTUM_MULTIPLIER)) {
            passiveDropMultiplier *= 5.0;
        }
        
        // Apply prestige bonuses
        passiveXpMultiplier *= (1.0 + (prestigeLevel * 0.1));
        passiveDropMultiplier *= (1.0 + (prestigeLevel * 0.05));
    }
    
    private void updateEnergyCapacity() {
        energyCapacity = 1000L; // Base capacity
        
        for (Map.Entry<GeneratorType, Integer> entry : generators.entrySet()) {
            energyCapacity += entry.getKey().getEnergyCapacity() * entry.getValue();
        }
        
        // Ensure current energy doesn't exceed new capacity
        if (currentEnergy > energyCapacity) {
            currentEnergy = energyCapacity;
        }
    }
}