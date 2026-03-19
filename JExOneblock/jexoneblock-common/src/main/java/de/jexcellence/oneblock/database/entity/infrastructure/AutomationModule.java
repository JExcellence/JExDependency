package de.jexcellence.oneblock.database.entity.infrastructure;

import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

/**
 * Automation Modules - Advanced automation systems for islands
 * Each module provides unique automation capabilities and bonuses
 */
public enum AutomationModule {
    
    // Tier 1: Basic Automation (Stages 11-20)
    AUTO_COLLECTOR("Auto Collector", 11, 50.0,
        "Automatically collects items from OneBlock breaking",
        Arrays.asList(
            new CraftingIngredient(Material.HOPPER, 16),
            new CraftingIngredient(Material.CHEST, 8),
            new CraftingIngredient(Material.REDSTONE_BLOCK, 4),
            new CraftingIngredient(Material.IRON_BLOCK, 32)
        ), 500000L),
    
    AUTO_SMELTER("Auto Smelter", 15, 75.0,
        "Automatically smelts ores and raw materials",
        Arrays.asList(
            new CraftingIngredient(Material.BLAST_FURNACE, 8),
            new CraftingIngredient(Material.LAVA_BUCKET, 16),
            new CraftingIngredient(Material.GOLD_BLOCK, 16),
            new CraftingIngredient(Material.DIAMOND_BLOCK, 8)
        ), 1000000L),
    
    // Tier 2: Advanced Automation (Stages 21-30)
    AUTO_CRAFTER("Auto Crafter", 21, 100.0,
        "Automatically crafts items based on recipes",
        Arrays.asList(
            new CraftingIngredient(Material.CRAFTER, 4),
            new CraftingIngredient(Material.CRAFTING_TABLE, 16),
            new CraftingIngredient(Material.EMERALD_BLOCK, 32),
            new CraftingIngredient(Material.NETHERITE_INGOT, 8)
        ), 2500000L),
    
    AUTO_SELLER("Auto Seller", 25, 125.0,
        "Automatically sells items for currency",
        Arrays.asList(
            new CraftingIngredient(Material.GOLD_BLOCK, 64),
            new CraftingIngredient(Material.EMERALD_BLOCK, 32),
            new CraftingIngredient(Material.BEACON, 2),
            new CraftingIngredient(Material.NETHER_STAR, 4)
        ), 5000000L),
    
    // Tier 3: Industrial Automation (Stages 31-40)
    QUANTUM_COMPRESSOR("Quantum Compressor", 31, 200.0,
        "Compresses items to save 5x storage space",
        Arrays.asList(
            new CraftingIngredient(Material.NETHERITE_BLOCK, 16),
            new CraftingIngredient(Material.END_CRYSTAL, 8),
            new CraftingIngredient(Material.DRAGON_EGG, 1),
            new CraftingIngredient(Material.SCULK_CATALYST, 32)
        ), 25000000L),
    
    EXPERIENCE_AMPLIFIER("Experience Amplifier", 35, 300.0,
        "Triples all passive XP generation",
        Arrays.asList(
            new CraftingIngredient(Material.EXPERIENCE_BOTTLE, 1000),
            new CraftingIngredient(Material.ENCHANTED_BOOK, 64),
            new CraftingIngredient(Material.BEACON, 8),
            new CraftingIngredient(Material.NETHER_STAR, 16)
        ), 50000000L),
    
    // Tier 4: Cosmic Automation (Stages 41-50)
    DIMENSIONAL_STORAGE("Dimensional Storage", 41, 500.0,
        "Unlocks 10x storage capacity across all rarities",
        Arrays.asList(
            new CraftingIngredient(Material.ENDER_CHEST, 64),
            new CraftingIngredient(Material.SHULKER_BOX, 128),
            new CraftingIngredient(Material.END_CRYSTAL, 32),
            new CraftingIngredient(Material.DRAGON_EGG, 4)
        ), 100000000L),
    
    QUANTUM_MULTIPLIER("Quantum Multiplier", 45, 750.0,
        "Multiplies all drop rates by 5x",
        Arrays.asList(
            new CraftingIngredient(Material.HEAVY_CORE, 8),
            new CraftingIngredient(Material.MACE, 4),
            new CraftingIngredient(Material.TRIAL_KEY, 100),
            new CraftingIngredient(Material.OMINOUS_TRIAL_KEY, 50)
        ), 250000000L),
    
    REALITY_PROCESSOR("Reality Processor", 49, 1000.0,
        "Processes items at quantum speeds - instant crafting",
        Arrays.asList(
            new CraftingIngredient(Material.COMMAND_BLOCK, 16),
            new CraftingIngredient(Material.STRUCTURE_BLOCK, 8),
            new CraftingIngredient(Material.KNOWLEDGE_BOOK, 32),
            new CraftingIngredient(Material.DEBUG_STICK, 4)
        ), 500000000L),
    
    // Tier 5: Prestige Automation (Prestige Only)
    INFINITY_ENGINE("Infinity Engine", 1, 2000.0,
        "Generates infinite energy - no power limitations",
        Arrays.asList(
            new CraftingIngredient(Material.NETHER_STAR, 1000),
            new CraftingIngredient(Material.DRAGON_EGG, 100),
            new CraftingIngredient(Material.BEACON, 500),
            new CraftingIngredient(Material.END_CRYSTAL, 1000)
        ), 1000000000L, true),
    
    OMNIPOTENT_CORE("Omnipotent Core", 1, 5000.0,
        "Ultimate automation - all processes become instant and free",
        Arrays.asList(
            new CraftingIngredient(Material.HEAVY_CORE, 100),
            new CraftingIngredient(Material.KNOWLEDGE_BOOK, 1000),
            new CraftingIngredient(Material.BUNDLE, 10000),
            new CraftingIngredient(Material.COMMAND_BLOCK, 500)
        ), Long.MAX_VALUE, true);
    
    private final String displayName;
    private final int requiredStage;
    private final double energyConsumption;
    private final String description;
    private final List<CraftingIngredient> craftingRecipe;
    private final long craftingCost;
    private final boolean prestigeOnly;
    
    AutomationModule(String displayName, int requiredStage, double energyConsumption,
                    String description, List<CraftingIngredient> craftingRecipe, long craftingCost) {
        this(displayName, requiredStage, energyConsumption, description, craftingRecipe, craftingCost, false);
    }
    
    AutomationModule(String displayName, int requiredStage, double energyConsumption,
                    String description, List<CraftingIngredient> craftingRecipe, long craftingCost, boolean prestigeOnly) {
        this.displayName = displayName;
        this.requiredStage = requiredStage;
        this.energyConsumption = energyConsumption;
        this.description = description;
        this.craftingRecipe = craftingRecipe;
        this.craftingCost = craftingCost;
        this.prestigeOnly = prestigeOnly;
    }
    
    /**
     * Checks if this module can be crafted at the given evolution level
     */
    public boolean canCraftAtEvolution(int evolution, boolean hasPrestige) {
        if (prestigeOnly && !hasPrestige) {
            return false;
        }
        return evolution >= requiredStage;
    }
    
    /**
     * Checks if this module can be crafted at the given stage
     */
    public boolean canCraftAtStage(int stage, boolean hasPrestige) {
        if (prestigeOnly && !hasPrestige) {
            return false;
        }
        return stage >= requiredStage;
    }
    
    /**
     * Gets the total material cost for crafting this module
     */
    public long getTotalMaterialValue() {
        return craftingRecipe.stream()
            .mapToLong(ingredient -> ingredient.getAmount() * getMaterialValue(ingredient.getMaterial()))
            .sum();
    }
    
    private long getMaterialValue(Material material) {
        // Simplified material value calculation
        return switch (material) {
            case DIRT, COBBLESTONE, STONE -> 1L;
            case IRON_INGOT, COPPER_INGOT -> 10L;
            case GOLD_INGOT, DIAMOND -> 100L;
            case NETHERITE_INGOT, ANCIENT_DEBRIS -> 1000L;
            case NETHER_STAR, DRAGON_EGG -> 10000L;
            case BEACON, CONDUIT -> 50000L;
            case END_CRYSTAL, TOTEM_OF_UNDYING -> 25000L;
            case HEAVY_CORE, MACE -> 100000L;
            case KNOWLEDGE_BOOK, BUNDLE -> 500000L;
            default -> 50L;
        };
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public int getRequiredEvolution() { return requiredStage; }
    public double getEnergyConsumption() { return energyConsumption; }
    public String getDescription() { return description; }
    public List<CraftingIngredient> getCraftingRecipe() { return craftingRecipe; }
    public long getCraftingCost() { return craftingCost; }
    public boolean isPrestigeOnly() { return prestigeOnly; }
    
    /**
     * Crafting ingredient for automation modules
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