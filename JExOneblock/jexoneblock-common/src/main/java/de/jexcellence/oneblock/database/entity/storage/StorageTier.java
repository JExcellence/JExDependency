package de.jexcellence.oneblock.database.entity.storage;

import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Storage Tier System - Defines capacity and bonuses for each storage level
 * Tiers unlock as players progress through evolution levels
 */
public enum StorageTier {
    
    // Tier 1: Genesis Storage (Evolutions 1-10)
    BASIC("Basic Storage", 1, 10,
        1000L, 500L, 100L, 50L, 10L, 5L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
        1.0, 1.0, 0.1, 10.0,
        Arrays.asList(Material.CHEST, Material.BARREL),
        50000L, "Unlocked from the beginning"),
    
    // Tier 2: Primordial Storage (Evolutions 11-20)
    ADVANCED("Advanced Storage", 11, 20,
        5000L, 2500L, 1000L, 500L, 100L, 50L, 10L, 5L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
        1.2, 1.1, 0.2, 25.0,
        Arrays.asList(Material.ENDER_CHEST, Material.SHULKER_BOX),
        200000L, "Unlocked at Ancient tier"),
    
    // Tier 3: Ancient Storage (Evolutions 21-30)
    SUPERIOR("Superior Storage", 21, 30,
        25000L, 12500L, 5000L, 2500L, 1000L, 500L, 100L, 50L, 10L, 5L, 1L, 0L, 0L, 0L, 0L, 0L,
        1.5, 1.25, 0.3, 50.0,
        Arrays.asList(Material.BEACON, Material.CONDUIT),
        1000000L, "Unlocked at Renaissance tier"),
    
    // Tier 4: Medieval Storage (Evolutions 31-40)
    ELITE("Elite Storage", 31, 40,
        100000L, 50000L, 25000L, 12500L, 5000L, 2500L, 1000L, 500L, 100L, 50L, 10L, 5L, 1L, 0L, 0L, 0L,
        2.0, 1.5, 0.5, 100.0,
        Arrays.asList(Material.NETHER_STAR, Material.DRAGON_EGG),
        5000000L, "Unlocked at Modern tier"),
    
    // Tier 5: Cosmic Storage (Evolutions 41-50)
    COSMIC("Cosmic Storage", 41, 50,
        1000000L, 500000L, 250000L, 125000L, 50000L, 25000L, 10000L, 5000L, 2500L, 1000L, 500L, 250L, 100L, 50L, 25L, 10L,
        3.0, 2.0, 1.0, 500.0,
        Arrays.asList(Material.END_CRYSTAL, Material.TOTEM_OF_UNDYING),
        25000000L, "Unlocked at Cosmic tier"),
    
    // Tier 6: Transcendent Storage (Prestige Only)
    TRANSCENDENT("Transcendent Storage", 1, 50,
        10000000L, 5000000L, 2500000L, 1250000L, 500000L, 250000L, 100000L, 50000L, 25000L, 10000L, 5000L, 2500L, 1000L, 500L, 250L, 100L,
        5.0, 3.0, 2.0, 1000.0,
        Arrays.asList(Material.HEAVY_CORE, Material.MACE),
        100000000L, "Unlocked through prestige system"),
    
    // Tier 7: Infinite Storage (High Prestige)
    INFINITE("Infinite Storage", 1, 50,
        Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
        Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
        10.0, 5.0, 5.0, 2500.0,
        Arrays.asList(Material.KNOWLEDGE_BOOK, Material.BUNDLE),
        Long.MAX_VALUE, "Ultimate prestige storage - infinite capacity");
    
    private final String displayName;
    private final int minEvolution;
    private final int maxEvolution;
    private final long[] rarityCapacities; // Capacities for each rarity type
    private final double passiveXpMultiplier;
    private final double passiveDropMultiplier;
    private final double passiveEfficiency;
    private final double maxPassiveXpPerMinute;
    private final List<Material> requiredMaterials;
    private final long upgradeCost;
    private final String description;
    
    StorageTier(String displayName, int minEvolution, int maxEvolution,
                long common, long uncommon, long rare, long epic, long legendary, long special,
                long unique, long mythical, long divine, long celestial, long transcendent,
                long ethereal, long cosmic, long infinite, long omnipotent, long reserved,
                double passiveXpMultiplier, double passiveDropMultiplier, double passiveEfficiency,
                double maxPassiveXpPerMinute, List<Material> requiredMaterials, long upgradeCost, String description) {
        
        this.displayName = displayName;
        this.minEvolution = minEvolution;
        this.maxEvolution = maxEvolution;
        this.rarityCapacities = new long[]{
            common, uncommon, rare, epic, legendary, special, unique, mythical,
            divine, celestial, transcendent, ethereal, cosmic, infinite, omnipotent, reserved
        };
        this.passiveXpMultiplier = passiveXpMultiplier;
        this.passiveDropMultiplier = passiveDropMultiplier;
        this.passiveEfficiency = passiveEfficiency;
        this.maxPassiveXpPerMinute = maxPassiveXpPerMinute;
        this.requiredMaterials = requiredMaterials;
        this.upgradeCost = upgradeCost;
        this.description = description;
    }
    
    /**
     * Gets the storage capacity for a specific rarity type
     */
    public long getCapacityForRarity(EEvolutionRarityType rarity) {
        return rarityCapacities[rarity.ordinal()];
    }
    
    /**
     * Checks if this tier can be unlocked at the given stage
     */
    public boolean canUnlockAtStage(int stage) {
        return stage >= minEvolution && stage <= maxEvolution;
    }
    
    /**
     * Gets the next tier in progression
     */
    public StorageTier getNextTier() {
        StorageTier[] tiers = StorageTier.values();
        int currentIndex = this.ordinal();
        return currentIndex < tiers.length - 1 ? tiers[currentIndex + 1] : null;
    }
    
    /**
     * Calculates total storage capacity across all rarities
     */
    public long getTotalCapacity() {
        long total = 0;
        for (long capacity : rarityCapacities) {
            if (capacity != Long.MAX_VALUE) {
                total += capacity;
            } else {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public int getMinStage() { return minEvolution; }
    public int getMaxStage() { return maxEvolution; }
    public double getPassiveXpMultiplier() { return passiveXpMultiplier; }
    public double getPassiveDropMultiplier() { return passiveDropMultiplier; }
    public double getPassiveEfficiency() { return passiveEfficiency; }
    public double getMaxPassiveXpPerMinute() { return maxPassiveXpPerMinute; }
    public List<Material> getRequiredMaterials() { return requiredMaterials; }
    public long getUpgradeCost() { return upgradeCost; }
    public String getDescription() { return description; }
}