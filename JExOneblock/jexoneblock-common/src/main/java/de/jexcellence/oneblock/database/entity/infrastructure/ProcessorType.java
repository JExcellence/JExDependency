package de.jexcellence.oneblock.database.entity.infrastructure;

import lombok.Getter;
import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

/**
 * Processor Types - Advanced processing systems for islands
 * Each processor handles specific automation tasks and scales with levels
 */
@Getter
public enum ProcessorType {
    
    // Tier 1: Basic Processors (Stages 11-25)
    BASIC_MINER("Basic Auto Miner", 11, 10,
        25.0, 1.0, "Automatically mines the OneBlock",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.DIAMOND_PICKAXE, 1),
                new CraftingIngredient(Material.REDSTONE_BLOCK, 8),
                new CraftingIngredient(Material.IRON_BLOCK, 16)
            ), 100000L, 0.5), // 0.5 blocks per second
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.NETHERITE_PICKAXE, 1),
                new CraftingIngredient(Material.DIAMOND_BLOCK, 16),
                new CraftingIngredient(Material.GOLD_BLOCK, 32)
            ), 500000L, 1.0), // 1.0 blocks per second
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.BEACON, 2),
                new CraftingIngredient(Material.NETHER_STAR, 4),
                new CraftingIngredient(Material.EMERALD_BLOCK, 64)
            ), 2500000L, 2.0) // 2.0 blocks per second
        )),
    
    ADVANCED_SMELTER("Advanced Auto Smelter", 15, 8,
        50.0, 2.0, "Automatically smelts all ores and raw materials",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.BLAST_FURNACE, 4),
                new CraftingIngredient(Material.LAVA_BUCKET, 8),
                new CraftingIngredient(Material.GOLD_BLOCK, 16)
            ), 250000L, 1.0), // 1x smelting speed
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.NETHERITE_BLOCK, 8),
                new CraftingIngredient(Material.MAGMA_BLOCK, 64),
                new CraftingIngredient(Material.BLAZE_ROD, 32)
            ), 1000000L, 3.0), // 3x smelting speed
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.END_CRYSTAL, 4),
                new CraftingIngredient(Material.DRAGON_BREATH, 16),
                new CraftingIngredient(Material.FIRE_CHARGE, 128)
            ), 5000000L, 10.0) // 10x smelting speed
        )),
    
    // Tier 2: Advanced Processors (Stages 26-35)
    QUANTUM_CRAFTER("Quantum Auto Crafter", 26, 6,
        100.0, 5.0, "Automatically crafts items using stored materials",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.CRAFTER, 8),
                new CraftingIngredient(Material.CRAFTING_TABLE, 32),
                new CraftingIngredient(Material.EMERALD_BLOCK, 64)
            ), 1000000L, 1.0), // Basic crafting speed
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.BEACON, 4),
                new CraftingIngredient(Material.NETHER_STAR, 8),
                new CraftingIngredient(Material.END_CRYSTAL, 16)
            ), 5000000L, 5.0), // 5x crafting speed
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.DRAGON_EGG, 2),
                new CraftingIngredient(Material.TOTEM_OF_UNDYING, 8),
                new CraftingIngredient(Material.ELYTRA, 4)
            ), 25000000L, 25.0) // 25x crafting speed
        )),
    
    MOLECULAR_ASSEMBLER("Molecular Assembler", 32, 5,
        200.0, 10.0, "Assembles items at the molecular level - creates any material",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.SCULK_CATALYST, 16),
                new CraftingIngredient(Material.SCULK_SENSOR, 32),
                new CraftingIngredient(Material.CALIBRATED_SCULK_SENSOR, 16)
            ), 10000000L, 0.1), // 0.1 items per second
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.RECOVERY_COMPASS, 4),
                new CraftingIngredient(Material.ECHO_SHARD, 32),
                new CraftingIngredient(Material.DISC_FRAGMENT_5, 8)
            ), 50000000L, 0.5), // 0.5 items per second
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.TRIAL_KEY, 100),
                new CraftingIngredient(Material.OMINOUS_TRIAL_KEY, 50),
                new CraftingIngredient(Material.HEAVY_CORE, 8)
            ), 250000000L, 2.0) // 2.0 items per second
        )),
    
    // Tier 3: Cosmic Processors (Stages 41-50)
    DIMENSIONAL_PROCESSOR("Dimensional Processor", 41, 4,
        500.0, 25.0, "Processes items across multiple dimensions simultaneously",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.END_PORTAL_FRAME, 16),
                new CraftingIngredient(Material.ENDER_CHEST, 64),
                new CraftingIngredient(Material.SHULKER_BOX, 128)
            ), 100000000L, 10.0), // 10x processing speed
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.COMMAND_BLOCK, 8),
                new CraftingIngredient(Material.STRUCTURE_BLOCK, 4),
                new CraftingIngredient(Material.BARRIER, 32)
            ), 500000000L, 50.0), // 50x processing speed
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.KNOWLEDGE_BOOK, 64),
                new CraftingIngredient(Material.DEBUG_STICK, 8),
                new CraftingIngredient(Material.BUNDLE, 1000)
            ), 2500000000L, 250.0) // 250x processing speed
        )),
    
    REALITY_MANIPULATOR("Reality Manipulator", 49, 3,
        1000.0, 100.0, "Manipulates reality to create anything instantly",
        Arrays.asList(
            new ProcessorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.HEAVY_CORE, 16),
                new CraftingIngredient(Material.MACE, 8),
                new CraftingIngredient(Material.WIND_CHARGE, 1000)
            ), 1000000000L, 100.0), // 100x reality manipulation
            new ProcessorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.COMMAND_BLOCK, 64),
                new CraftingIngredient(Material.KNOWLEDGE_BOOK, 500),
                new CraftingIngredient(Material.DEBUG_STICK, 32)
            ), 5000000000L, 1000.0), // 1000x reality manipulation
            new ProcessorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.BUNDLE, 10000),
                new CraftingIngredient(Material.BARRIER, 1000),
                new CraftingIngredient(Material.BEDROCK, 100)
            ), Long.MAX_VALUE, Double.MAX_VALUE) // Infinite reality manipulation
        ));

    // Getters
    private final String displayName;
    private final int requiredStage;
    private final int maxLevel;
    private final double baseEnergyConsumption;
    private final double energyScaling;
    private final String description;
    private final List<ProcessorUpgrade> upgrades;
    
    ProcessorType(String displayName, int requiredStage, int maxLevel,
                 double baseEnergyConsumption, double energyScaling, String description,
                 List<ProcessorUpgrade> upgrades) {
        this.displayName = displayName;
        this.requiredStage = requiredStage;
        this.maxLevel = maxLevel;
        this.baseEnergyConsumption = baseEnergyConsumption;
        this.energyScaling = energyScaling;
        this.description = description;
        this.upgrades = upgrades;
    }
    
    /**
     * Gets energy consumption for a specific level
     */
    public double getEnergyConsumption(int level) {
        if (level <= 0) return 0.0;
        return baseEnergyConsumption * Math.pow(energyScaling, level - 1);
    }
    
    /**
     * Gets energy consumption for max level
     */
    public double getEnergyConsumption() {
        return getEnergyConsumption(maxLevel);
    }
    
    /**
     * Gets processing speed for a specific level
     */
    public double getProcessingSpeed(int level) {
        if (level <= 0 || level > upgrades.size()) return 0.0;
        return upgrades.get(level - 1).getProcessingSpeed();
    }
    
    /**
     * Gets upgrade requirements for a specific level
     */
    public ProcessorUpgrade getUpgrade(int level) {
        if (level <= 0 || level > upgrades.size()) return null;
        return upgrades.get(level - 1);
    }
    
    /**
     * Checks if this processor can be built at the given evolution level
     */
    public boolean canBuildAtEvolution(int evolution) {
        return evolution >= requiredStage;
    }
    
    /**
     * Checks if this processor can be built at the given stage
     */
    public boolean canBuildAtStage(int stage) {
        return stage >= requiredStage;
    }
    
    public int getRequiredEvolution() {
        return requiredStage;
    }

    /**
     * Processor upgrade definition
     */
    public static class ProcessorUpgrade {
        private final int level;
        private final List<CraftingIngredient> materials;
        private final long cost;
        private final double processingSpeed;
        
        public ProcessorUpgrade(int level, List<CraftingIngredient> materials, long cost, double processingSpeed) {
            this.level = level;
            this.materials = materials;
            this.cost = cost;
            this.processingSpeed = processingSpeed;
        }
        
        public int getLevel() { return level; }
        public List<CraftingIngredient> getMaterials() { return materials; }
        public long getCost() { return cost; }
        public double getProcessingSpeed() { return processingSpeed; }
    }
    
    /**
     * Crafting ingredient for processors
     */
    public static class CraftingIngredient {
        private final Material material;
        private final int amount;
        
        public CraftingIngredient(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
        
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
    }
}