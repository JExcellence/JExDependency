package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced configuration section for island level calculation settings.
 * Supports dynamic block values, multipliers, and optimization features.
 */
@CSAlways
@SuppressWarnings("ALL")
public class IslandLevelCalculationSection extends AConfigSection {

    // ==================== BASIC SETTINGS ====================
    private Boolean enabled;
    private Map<String, Double> blockExperience;
    private Double baseExperienceMultiplier;
    private Integer maxLevel;
    private Boolean useOptimizedCalculation;
    private Integer calculationBatchSize;

    // ==================== DYNAMIC MULTIPLIERS ====================
    private Map<String, Double> biomeMultipliers;
    private Map<String, Double> heightMultipliers;
    private Map<String, Double> rarityMultipliers;
    private Boolean enableDynamicMultipliers;

    // ==================== BONUS SYSTEMS ====================
    private Map<String, Object> bonusSettings;
    private Boolean enableBonusBlocks;
    private Double bonusBlockMultiplier;
    private Map<String, Double> specialBlockBonuses;

    // ==================== PERFORMANCE OPTIMIZATION ====================
    private Boolean enableCaching;
    private Integer cacheExpirationMinutes;
    private Boolean enableAsyncCalculation;
    private Integer maxCalculationThreads;
    private Boolean enableProgressiveScanning;

    // ==================== PRESTIGE INTEGRATION ====================
    private Map<String, Double> prestigeMultipliers;
    private Boolean enablePrestigeBonus;
    private Double maxPrestigeBonus;

    public IslandLevelCalculationSection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        initializeDefaults();
        validateConfiguration();
    }

    private void initializeDefaults() {
        if (this.blockExperience == null) {
            this.blockExperience = new HashMap<>();
            initializeDefaultBlockExperience();
        }
    }

    private void initializeDefaultBlockExperience() {
        // Basic blocks
        blockExperience.put("STONE", 1.0);
        blockExperience.put("COBBLESTONE", 1.0);
        blockExperience.put("DIRT", 1.0);
        blockExperience.put("GRASS_BLOCK", 1.5);
        blockExperience.put("SAND", 1.0);
        blockExperience.put("GRAVEL", 1.0);

        // Wood blocks
        blockExperience.put("OAK_LOG", 2.0);
        blockExperience.put("BIRCH_LOG", 2.0);
        blockExperience.put("SPRUCE_LOG", 2.0);
        blockExperience.put("JUNGLE_LOG", 2.0);
        blockExperience.put("ACACIA_LOG", 2.0);
        blockExperience.put("DARK_OAK_LOG", 2.0);
        blockExperience.put("CHERRY_LOG", 2.0);
        blockExperience.put("MANGROVE_LOG", 2.0);

        // Planks
        blockExperience.put("OAK_PLANKS", 1.5);
        blockExperience.put("BIRCH_PLANKS", 1.5);
        blockExperience.put("SPRUCE_PLANKS", 1.5);
        blockExperience.put("JUNGLE_PLANKS", 1.5);
        blockExperience.put("ACACIA_PLANKS", 1.5);
        blockExperience.put("DARK_OAK_PLANKS", 1.5);

        // Ores
        blockExperience.put("COAL_ORE", 5.0);
        blockExperience.put("COPPER_ORE", 8.0);
        blockExperience.put("IRON_ORE", 10.0);
        blockExperience.put("LAPIS_ORE", 15.0);
        blockExperience.put("GOLD_ORE", 20.0);
        blockExperience.put("REDSTONE_ORE", 25.0);
        blockExperience.put("EMERALD_ORE", 50.0);
        blockExperience.put("DIAMOND_ORE", 100.0);

        // Deepslate ores (higher value)
        blockExperience.put("DEEPSLATE_COAL_ORE", 7.0);
        blockExperience.put("DEEPSLATE_COPPER_ORE", 12.0);
        blockExperience.put("DEEPSLATE_IRON_ORE", 15.0);
        blockExperience.put("DEEPSLATE_LAPIS_ORE", 22.0);
        blockExperience.put("DEEPSLATE_GOLD_ORE", 30.0);
        blockExperience.put("DEEPSLATE_REDSTONE_ORE", 35.0);
        blockExperience.put("DEEPSLATE_EMERALD_ORE", 75.0);
        blockExperience.put("DEEPSLATE_DIAMOND_ORE", 150.0);

        // Nether blocks
        blockExperience.put("NETHERRACK", 2.0);
        blockExperience.put("NETHER_BRICKS", 3.0);
        blockExperience.put("BLACKSTONE", 3.0);
        blockExperience.put("BASALT", 3.0);
        blockExperience.put("NETHER_QUARTZ_ORE", 15.0);
        blockExperience.put("NETHER_GOLD_ORE", 25.0);
        blockExperience.put("ANCIENT_DEBRIS", 1000.0);

        // End blocks
        blockExperience.put("END_STONE", 5.0);
        blockExperience.put("PURPUR_BLOCK", 10.0);
        blockExperience.put("CHORUS_PLANT", 8.0);
        blockExperience.put("CHORUS_FLOWER", 15.0);

        // Valuable blocks
        blockExperience.put("IRON_BLOCK", 90.0);
        blockExperience.put("GOLD_BLOCK", 180.0);
        blockExperience.put("DIAMOND_BLOCK", 900.0);
        blockExperience.put("EMERALD_BLOCK", 450.0);
        blockExperience.put("NETHERITE_BLOCK", 9000.0);

        // Special blocks
        blockExperience.put("BEACON", 5000.0);
        blockExperience.put("DRAGON_EGG", 10000.0);
        blockExperience.put("ENCHANTING_TABLE", 500.0);
        blockExperience.put("ANVIL", 300.0);

        // Decorative blocks
        blockExperience.put("GLASS", 2.0);
        blockExperience.put("WOOL", 3.0);
        blockExperience.put("CONCRETE", 4.0);
        blockExperience.put("TERRACOTTA", 3.0);

        // Crops and plants
        blockExperience.put("WHEAT", 2.0);
        blockExperience.put("CARROTS", 2.0);
        blockExperience.put("POTATOES", 2.0);
        blockExperience.put("BEETROOTS", 2.0);
        blockExperience.put("SUGAR_CANE", 1.5);
        blockExperience.put("CACTUS", 2.0);
        blockExperience.put("PUMPKIN", 5.0);
        blockExperience.put("MELON", 3.0);
    }

    private void validateConfiguration() {
        if (this.baseExperienceMultiplier != null && this.baseExperienceMultiplier <= 0) {
            this.baseExperienceMultiplier = 1.0;
        }
        if (this.maxLevel != null && this.maxLevel < 1) {
            this.maxLevel = 1000;
        }
        if (this.calculationBatchSize != null && this.calculationBatchSize < 100) {
            this.calculationBatchSize = 1000;
        }
    }

    // ==================== BASIC SETTINGS GETTERS ====================

    public boolean isEnabled() {
        return this.enabled != null ? this.enabled : true;
    }

    public @NotNull Map<String, Double> getBlockExperience() {
        return this.blockExperience != null ? this.blockExperience : new HashMap<>();
    }

    public double getBaseExperienceMultiplier() {
        return this.baseExperienceMultiplier != null ? this.baseExperienceMultiplier : 1.0;
    }

    public int getMaxLevel() {
        return this.maxLevel != null ? this.maxLevel : 1000;
    }

    public boolean isUseOptimizedCalculation() {
        return this.useOptimizedCalculation != null ? this.useOptimizedCalculation : true;
    }

    public int getCalculationBatchSize() {
        return this.calculationBatchSize != null ? this.calculationBatchSize : 1000;
    }

    // ==================== DYNAMIC MULTIPLIERS GETTERS ====================

    public @NotNull Map<String, Double> getBiomeMultipliers() {
        return this.biomeMultipliers != null ? this.biomeMultipliers : getDefaultBiomeMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultBiomeMultipliers() {
        return Map.of(
            "NETHER_WASTES", 1.5,
            "THE_END", 2.0,
            "MUSHROOM_FIELDS", 1.3,
            "ICE_SPIKES", 1.2,
            "BADLANDS", 1.1,
            "JUNGLE", 1.1,
            "DESERT", 1.0,
            "PLAINS", 1.0
        );
    }

    public @NotNull Map<String, Double> getHeightMultipliers() {
        return this.heightMultipliers != null ? this.heightMultipliers : getDefaultHeightMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultHeightMultipliers() {
        return Map.of(
            "ABOVE_200", 1.2,    // Sky builds
            "150_TO_200", 1.1,   // High builds
            "100_TO_150", 1.0,   // Normal height
            "50_TO_100", 1.0,    // Standard builds
            "0_TO_50", 0.9,      // Low builds
            "BELOW_0", 1.3       // Underground builds
        );
    }

    public @NotNull Map<String, Double> getRarityMultipliers() {
        return this.rarityMultipliers != null ? this.rarityMultipliers : getDefaultRarityMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultRarityMultipliers() {
        return Map.of(
            "COMMON", 1.0,
            "UNCOMMON", 1.2,
            "RARE", 1.5,
            "EPIC", 2.0,
            "LEGENDARY", 3.0,
            "MYTHIC", 5.0,
            "OMNIPOTENT", 10.0
        );
    }

    public boolean areDynamicMultipliersEnabled() {
        return this.enableDynamicMultipliers != null ? this.enableDynamicMultipliers : true;
    }

    // ==================== BONUS SYSTEMS GETTERS ====================

    public @NotNull Map<String, Object> getBonusSettings() {
        return this.bonusSettings != null ? this.bonusSettings : getDefaultBonusSettings();
    }

    private @NotNull Map<String, Object> getDefaultBonusSettings() {
        return Map.of(
            "completion_bonus", Map.of("enabled", true, "percentage", 0.1), // 10% bonus for completed builds
            "symmetry_bonus", Map.of("enabled", true, "percentage", 0.05),  // 5% bonus for symmetrical builds
            "diversity_bonus", Map.of("enabled", true, "percentage", 0.15), // 15% bonus for block diversity
            "height_variation_bonus", Map.of("enabled", true, "percentage", 0.08) // 8% bonus for height variation
        );
    }

    public boolean areBonusBlocksEnabled() {
        return this.enableBonusBlocks != null ? this.enableBonusBlocks : true;
    }

    public double getBonusBlockMultiplier() {
        return this.bonusBlockMultiplier != null ? this.bonusBlockMultiplier : 1.5;
    }

    public @NotNull Map<String, Double> getSpecialBlockBonuses() {
        return this.specialBlockBonuses != null ? this.specialBlockBonuses : getDefaultSpecialBlockBonuses();
    }

    private @NotNull Map<String, Double> getDefaultSpecialBlockBonuses() {
        return Map.of(
            "SPAWNER", 2.0,           // Spawners give double points
            "CONDUIT", 1.8,           // Conduits give 80% bonus
            "BEACON", 1.5,            // Beacons give 50% bonus
            "END_PORTAL_FRAME", 3.0,  // End portal frames give triple points
            "DRAGON_HEAD", 2.5,       // Dragon heads give 150% bonus
            "PLAYER_HEAD", 1.3        // Player heads give 30% bonus
        );
    }

    // ==================== PERFORMANCE OPTIMIZATION GETTERS ====================

    public boolean isCachingEnabled() {
        return this.enableCaching != null ? this.enableCaching : true;
    }

    public int getCacheExpirationMinutes() {
        return this.cacheExpirationMinutes != null ? this.cacheExpirationMinutes : 30;
    }

    public boolean isAsyncCalculationEnabled() {
        return this.enableAsyncCalculation != null ? this.enableAsyncCalculation : true;
    }

    public int getMaxCalculationThreads() {
        return this.maxCalculationThreads != null ? this.maxCalculationThreads : 4;
    }

    public boolean isProgressiveScanningEnabled() {
        return this.enableProgressiveScanning != null ? this.enableProgressiveScanning : true;
    }

    // ==================== PRESTIGE INTEGRATION GETTERS ====================

    public @NotNull Map<String, Double> getPrestigeMultipliers() {
        return this.prestigeMultipliers != null ? this.prestigeMultipliers : getDefaultPrestigeMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultPrestigeMultipliers() {
        return Map.of(
            "PRESTIGE_1", 1.1,   // 10% bonus
            "PRESTIGE_2", 1.25,  // 25% bonus
            "PRESTIGE_3", 1.4,   // 40% bonus
            "PRESTIGE_4", 1.6,   // 60% bonus
            "PRESTIGE_5", 1.8,   // 80% bonus
            "PRESTIGE_MAX", 2.0  // 100% bonus
        );
    }

    public boolean isPrestigeBonusEnabled() {
        return this.enablePrestigeBonus != null ? this.enablePrestigeBonus : true;
    }

    public double getMaxPrestigeBonus() {
        return this.maxPrestigeBonus != null ? this.maxPrestigeBonus : 2.0;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Gets the experience value for a specific block type with all multipliers applied.
     */
    public double getBlockExperience(@NotNull String blockType) {
        return getBlockExperience().getOrDefault(blockType.toUpperCase(), 0.0) * getBaseExperienceMultiplier();
    }

    /**
     * Sets the experience value for a specific block type.
     */
    public void setBlockExperience(@NotNull String blockType, double experience) {
        getBlockExperience().put(blockType.toUpperCase(), Math.max(0.0, experience));
    }

    /**
     * Calculates the level from experience using the configured formula.
     */
    public int calculateLevel(double experience) {
        if (experience <= 0) {
            return 1;
        }

        // Enhanced level formula: floor(sqrt(experience / 100)) + 1
        final int calculatedLevel = (int) Math.floor(Math.sqrt(experience / 100.0)) + 1;
        return Math.min(calculatedLevel, getMaxLevel());
    }

    /**
     * Calculates the experience required for a specific level.
     */
    public double getExperienceForLevel(int level) {
        if (level <= 1) {
            return 0.0;
        }

        // Inverse of level formula: (level - 1)^2 * 100
        return Math.pow(level - 1, 2) * 100.0;
    }

    /**
     * Gets the experience required to reach the next level.
     */
    public double getExperienceForNextLevel(double currentExperience) {
        final int currentLevel = calculateLevel(currentExperience);
        final double nextLevelExp = getExperienceForLevel(currentLevel + 1);
        return Math.max(0.0, nextLevelExp - currentExperience);
    }

    /**
     * Calculates experience with biome multiplier applied.
     */
    public double calculateExperienceWithBiome(@NotNull String blockType, @NotNull String biome) {
        double baseExp = getBlockExperience(blockType);
        double biomeMultiplier = getBiomeMultipliers().getOrDefault(biome, 1.0);
        return baseExp * biomeMultiplier;
    }

    /**
     * Calculates experience with height multiplier applied.
     */
    public double calculateExperienceWithHeight(@NotNull String blockType, int height) {
        double baseExp = getBlockExperience(blockType);
        String heightCategory = getHeightCategory(height);
        double heightMultiplier = getHeightMultipliers().getOrDefault(heightCategory, 1.0);
        return baseExp * heightMultiplier;
    }

    private @NotNull String getHeightCategory(int height) {
        if (height > 200) return "ABOVE_200";
        if (height > 150) return "150_TO_200";
        if (height > 100) return "100_TO_150";
        if (height > 50) return "50_TO_100";
        if (height > 0) return "0_TO_50";
        return "BELOW_0";
    }

    /**
     * Calculates the total experience with all multipliers and bonuses applied.
     */
    public double calculateTotalExperience(@NotNull String blockType, @NotNull String biome, 
                                         int height, @NotNull String rarity, int prestigeLevel) {
        double baseExp = getBlockExperience(blockType);
        
        if (areDynamicMultipliersEnabled()) {
            // Apply biome multiplier
            double biomeMultiplier = getBiomeMultipliers().getOrDefault(biome, 1.0);
            baseExp *= biomeMultiplier;
            
            // Apply height multiplier
            String heightCategory = getHeightCategory(height);
            double heightMultiplier = getHeightMultipliers().getOrDefault(heightCategory, 1.0);
            baseExp *= heightMultiplier;
            
            // Apply rarity multiplier
            double rarityMultiplier = getRarityMultipliers().getOrDefault(rarity, 1.0);
            baseExp *= rarityMultiplier;
        }
        
        // Apply prestige bonus
        if (isPrestigeBonusEnabled() && prestigeLevel > 0) {
            String prestigeKey = "PRESTIGE_" + Math.min(prestigeLevel, 5);
            if (prestigeLevel >= 5) prestigeKey = "PRESTIGE_MAX";
            double prestigeMultiplier = getPrestigeMultipliers().getOrDefault(prestigeKey, 1.0);
            baseExp *= prestigeMultiplier;
        }
        
        // Apply special block bonuses
        double specialBonus = getSpecialBlockBonuses().getOrDefault(blockType, 1.0);
        baseExp *= specialBonus;
        
        return baseExp;
    }
}