package de.jexcellence.oneblock.database.entity.infrastructure;

import lombok.Getter;
import org.bukkit.Material;
import java.util.Arrays;
import java.util.List;

/**
 * Generator Types - Energy generation systems for islands
 * Each generator provides power for processors and automation modules
 * 
 * Note: Some ingredients use CustomMaterial enum for fictional materials
 * that are implemented as custom items with NBT data.
 */
@Getter
public enum GeneratorType {
    
    // Tier 1: Basic Generators (Stages 6-20)
    COAL_GENERATOR("Coal Generator", 6, 5,
        10.0, 1000L, "Burns coal to generate energy",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.FURNACE, 8),
                new CraftingIngredient(Material.COAL_BLOCK, 32),
                new CraftingIngredient(Material.IRON_BLOCK, 16)
            ), 50000L, 5.0, 500L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.BLAST_FURNACE, 4),
                new CraftingIngredient(Material.COAL_BLOCK, 128),
                new CraftingIngredient(Material.GOLD_BLOCK, 32)
            ), 250000L, 15.0, 1500L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.SMOKER, 8),
                new CraftingIngredient(Material.LAVA_BUCKET, 16),
                new CraftingIngredient(Material.DIAMOND_BLOCK, 16)
            ), 1000000L, 50.0, 5000L)
        )),
    
    SOLAR_PANEL("Solar Panel", 12, 8,
        25.0, 2000L, "Harnesses solar energy during daylight",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.DAYLIGHT_DETECTOR, 16),
                new CraftingIngredient(Material.GOLD_BLOCK, 32),
                new CraftingIngredient(Material.GLOWSTONE, 64)
            ), 200000L, 15.0, 1000L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.BEACON, 2),
                new CraftingIngredient(Material.DIAMOND_BLOCK, 32),
                new CraftingIngredient(Material.SEA_LANTERN, 64)
            ), 1000000L, 50.0, 3000L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.CONDUIT, 4),
                new CraftingIngredient(Material.PRISMARINE_CRYSTALS, 128),
                new CraftingIngredient(Material.EMERALD_BLOCK, 64)
            ), 5000000L, 150.0, 10000L)
        )),
    
    // Tier 2: Advanced Generators (Stages 21-35)
    GEOTHERMAL_PLANT("Geothermal Plant", 21, 6,
        100.0, 5000L, "Extracts energy from the earth's core",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.MAGMA_BLOCK, 128),
                new CraftingIngredient(Material.OBSIDIAN, 64),
                new CraftingIngredient(Material.NETHERITE_INGOT, 16)
            ), 2500000L, 75.0, 2500L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(Material.LAVA_BUCKET, 64),
                new CraftingIngredient(Material.ANCIENT_DEBRIS, 32),
                new CraftingIngredient(Material.NETHERITE_BLOCK, 16)
            ), 10000000L, 250.0, 7500L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.FIRE_CHARGE, 256),
                new CraftingIngredient(Material.BLAZE_ROD, 128),
                new CraftingIngredient(Material.GHAST_TEAR, 64)
            ), 50000000L, 750.0, 25000L)
        )),
    
    NUCLEAR_REACTOR("Nuclear Reactor", 29, 5,
        500.0, 25000L, "Splits atoms to generate massive energy",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                // Custom materials represented by vanilla items with NBT
                new CraftingIngredient(CustomMaterial.URANIUM_ORE, 256),
                new CraftingIngredient(Material.IRON_BLOCK, 128),
                new CraftingIngredient(CustomMaterial.LEAD_ORE, 64)
            ), 25000000L, 400.0, 10000L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(CustomMaterial.URANIUM_BLOCK, 64),
                new CraftingIngredient(CustomMaterial.HEAVY_WATER_BUCKET, 32),
                new CraftingIngredient(CustomMaterial.REINFORCED_CONCRETE, 256)
            ), 100000000L, 1500.0, 40000L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(CustomMaterial.PLUTONIUM_INGOT, 32),
                new CraftingIngredient(CustomMaterial.CONTROL_ROD, 16),
                new CraftingIngredient(CustomMaterial.REACTOR_CORE, 4)
            ), 500000000L, 5000.0, 150000L)
        )),
    
    // Tier 3: Cosmic Generators (Stages 36-50)
    FUSION_CORE("Fusion Core", 36, 4,
        1000.0, 100000L, "Fuses hydrogen into helium for clean energy",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.END_CRYSTAL, 16),
                new CraftingIngredient(Material.DRAGON_BREATH, 64),
                new CraftingIngredient(Material.NETHER_STAR, 8)
            ), 250000000L, 2000.0, 50000L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(CustomMaterial.PLASMA_CELL, 32),
                new CraftingIngredient(Material.TOTEM_OF_UNDYING, 16),
                new CraftingIngredient(Material.ELYTRA, 8)
            ), 1000000000L, 8000.0, 200000L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(Material.BEACON, 64),
                new CraftingIngredient(Material.CONDUIT, 32),
                new CraftingIngredient(Material.HEART_OF_THE_SEA, 16)
            ), 5000000000L, 25000.0, 750000L)
        )),
    
    COSMIC_HARVESTER("Cosmic Harvester", 45, 3,
        2500.0, 500000L, "Harvests energy from cosmic radiation and dark matter",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.SCULK_CATALYST, 64),
                new CraftingIngredient(Material.SCULK_SENSOR, 128),
                new CraftingIngredient(Material.CALIBRATED_SCULK_SENSOR, 64)
            ), 1000000000L, 10000.0, 250000L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(CustomMaterial.DARK_MATTER_SHARD, 32),
                new CraftingIngredient(CustomMaterial.QUANTUM_CRYSTAL, 64),
                new CraftingIngredient(CustomMaterial.COSMIC_DUST, 2000)
            ), 10000000000L, 50000.0, 1000000L),
            new GeneratorUpgrade(3, Arrays.asList(
                new CraftingIngredient(CustomMaterial.STELLAR_CORE_FRAGMENT, 16),
                new CraftingIngredient(CustomMaterial.VOID_ESSENCE, 64),
                new CraftingIngredient(CustomMaterial.ANTIMATTER_CAPSULE, 8)
            ), Long.MAX_VALUE, 250000.0, 5000000L)
        )),
    
    // Tier 4: Transcendent Generators (Prestige Only)
    QUANTUM_SINGULARITY("Quantum Singularity", 1, 2,
        10000.0, 2500000L, "Harnesses the power of black holes",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(Material.NETHER_STAR, 1000),
                new CraftingIngredient(CustomMaterial.ANTIMATTER_CAPSULE, 100),
                new CraftingIngredient(Material.END_CRYSTAL, 500)
            ), 50000000000L, 100000.0, 1000000L),
            new GeneratorUpgrade(2, Arrays.asList(
                new CraftingIngredient(CustomMaterial.VOID_ESSENCE, 10000),
                new CraftingIngredient(CustomMaterial.STELLAR_CORE_FRAGMENT, 1000),
                new CraftingIngredient(CustomMaterial.QUANTUM_PROCESSOR, 100)
            ), Long.MAX_VALUE, 1000000.0, 25000000L)
        ), true),
    
    REALITY_ENGINE("Reality Engine", 1, 1,
        50000.0, 10000000L, "Generates infinite energy by manipulating reality",
        Arrays.asList(
            new GeneratorUpgrade(1, Arrays.asList(
                new CraftingIngredient(CustomMaterial.QUANTUM_PROCESSOR, 1000),
                new CraftingIngredient(CustomMaterial.NEURAL_MATRIX, 10000),
                new CraftingIngredient(CustomMaterial.ANTIMATTER_CAPSULE, 100000)
            ), Long.MAX_VALUE, Double.MAX_VALUE, Long.MAX_VALUE)
        ), true);

    private final String displayName;
    private final int requiredStage;
    private final int maxLevel;
    private final double baseEnergyPerSecond;
    private final long baseEnergyCapacity;
    private final String description;
    private final List<GeneratorUpgrade> upgrades;
    private final boolean prestigeOnly;
    
    GeneratorType(String displayName, int requiredStage, int maxLevel,
                 double baseEnergyPerSecond, long baseEnergyCapacity, String description,
                 List<GeneratorUpgrade> upgrades) {
        this(displayName, requiredStage, maxLevel, baseEnergyPerSecond, baseEnergyCapacity, description, upgrades, false);
    }
    
    GeneratorType(String displayName, int requiredStage, int maxLevel,
                 double baseEnergyPerSecond, long baseEnergyCapacity, String description,
                 List<GeneratorUpgrade> upgrades, boolean prestigeOnly) {
        this.displayName = displayName;
        this.requiredStage = requiredStage;
        this.maxLevel = maxLevel;
        this.baseEnergyPerSecond = baseEnergyPerSecond;
        this.baseEnergyCapacity = baseEnergyCapacity;
        this.description = description;
        this.upgrades = upgrades;
        this.prestigeOnly = prestigeOnly;
    }
    
    public double getEnergyPerSecond(int level) {
        if (level <= 0 || level > upgrades.size()) return 0.0;
        return upgrades.get(level - 1).getEnergyPerSecond();
    }
    
    public double getEnergyPerSecond() {
        return getEnergyPerSecond(maxLevel);
    }
    
    public long getEnergyCapacity(int level) {
        if (level <= 0 || level > upgrades.size()) return 0L;
        return upgrades.get(level - 1).getEnergyCapacity();
    }
    
    public long getEnergyCapacity() {
        return getEnergyCapacity(maxLevel);
    }
    
    public GeneratorUpgrade getUpgrade(int level) {
        if (level <= 0 || level > upgrades.size()) return null;
        return upgrades.get(level - 1);
    }
    
    public boolean canBuildAtEvolution(int evolution, boolean hasPrestige) {
        if (prestigeOnly && !hasPrestige) return false;
        return evolution >= requiredStage;
    }
    
    public boolean canBuildAtStage(int stage, boolean hasPrestige) {
        if (prestigeOnly && !hasPrestige) return false;
        return stage >= requiredStage;
    }
    
    public int getRequiredEvolution() {
        return requiredStage;
    }

    /**
     * Generator upgrade definition
     */
    @Getter
    public static class GeneratorUpgrade {
        private final int level;
        private final List<CraftingIngredient> materials;
        private final long cost;
        private final double energyPerSecond;
        private final long energyCapacity;
        
        public GeneratorUpgrade(int level, List<CraftingIngredient> materials, long cost, 
                              double energyPerSecond, long energyCapacity) {
            this.level = level;
            this.materials = materials;
            this.cost = cost;
            this.energyPerSecond = energyPerSecond;
            this.energyCapacity = energyCapacity;
        }
    }
    
    /**
     * Crafting ingredient - supports both vanilla Materials and CustomMaterials
     */
    @Getter
    public static class CraftingIngredient {
        private final Material material;
        private final CustomMaterial customMaterial;
        private final int amount;
        private final boolean isCustom;
        
        /**
         * Creates an ingredient from a vanilla Material
         */
        public CraftingIngredient(Material material, int amount) {
            this.material = material;
            this.customMaterial = null;
            this.amount = amount;
            this.isCustom = false;
        }
        
        /**
         * Creates an ingredient from a CustomMaterial
         */
        public CraftingIngredient(CustomMaterial customMaterial, int amount) {
            this.material = customMaterial.getBaseMaterial();
            this.customMaterial = customMaterial;
            this.amount = amount;
            this.isCustom = true;
        }
        
        /**
         * Gets the display name for this ingredient
         */
        public String getDisplayName() {
            if (isCustom && customMaterial != null) {
                return customMaterial.getDisplayName();
            }
            return formatMaterialName(material);
        }
        
        private String formatMaterialName(Material mat) {
            String name = mat.name().replace("_", " ").toLowerCase();
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;
            for (char c : name.toCharArray()) {
                if (c == ' ') {
                    capitalizeNext = true;
                    result.append(c);
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }
}
