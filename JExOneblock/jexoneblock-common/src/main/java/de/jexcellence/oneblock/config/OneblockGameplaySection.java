package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced configuration section for OneBlock gameplay mechanics.
 * Handles rarity systems, spawn rates, progression, and game balance.
 */
@CSAlways
@SuppressWarnings("ALL")
public class OneblockGameplaySection extends AConfigSection {

    // ==================== RARITY SYSTEM ====================
    private Map<String, Double> rarityWeights;
    private Map<String, Integer> rarityMultipliers;
    private Boolean enableDynamicRarity;
    private Double rarityBoostPerEvolution;
    private Integer maxRarityBoost;

    // ==================== SPAWN RATES ====================
    private Map<String, Double> itemSpawnRates;
    private Map<String, Double> entitySpawnRates;
    private Map<String, Double> chestSpawnRates;
    private Double baseItemSpawnChance;
    private Double baseEntitySpawnChance;
    private Double baseChestSpawnChance;

    // ==================== STARTING BLOCKS ====================
    private Map<String, Double> startingBlockWeights;
    private Integer startingBlockCount;
    private Boolean enableStartingBlockProgression;
    private Map<String, List<String>> evolutionBasedStartingBlocks;

    // ==================== PROGRESSION SYSTEM ====================
    private Map<String, Integer> evolutionExperienceRequirements;
    private Map<String, Double> experienceMultipliers;
    private Boolean enableExperienceBoosts;
    private Double experienceBoostPerRarity;
    private Integer maxExperienceBoost;

    // ==================== SPECIAL EVENTS ====================
    private Map<String, Double> specialEventChances;
    private Map<String, Object> eventRewards;
    private Boolean enableRandomEvents;
    private Integer eventCooldownMinutes;

    // ==================== BALANCE SETTINGS ====================
    private Double globalDropRateMultiplier;
    private Double globalExperienceMultiplier;
    private Boolean enableAntiGrinding;
    private Integer antiGrindingThreshold;
    private Double antiGrindingPenalty;

    // ==================== PERFORMANCE ====================
    private Integer maxConcurrentSpawns;
    private Boolean enableAsyncSpawning;
    private Integer spawnProcessingDelay;
    private Boolean enableSpawnOptimization;

    // ==================== ADVANCED FEATURES ====================
    private Map<String, Object> evolutionSettings;
    private Boolean enableEvolutionSystem;
    private Map<String, Integer> evolutionRequirements;
    private Map<String, Object> evolutionRewards;

    public OneblockGameplaySection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        validateConfiguration();
    }

    /**
     * Validates and normalizes configuration values.
     */
    private void validateConfiguration() {
        // Normalize rarity weights to ensure they sum to 100%
        if (this.rarityWeights != null && !this.rarityWeights.isEmpty()) {
            double totalWeight = this.rarityWeights.values().stream().mapToDouble(Double::doubleValue).sum();
            if (totalWeight != 100.0) {
                this.rarityWeights.replaceAll((k, v) -> (v / totalWeight) * 100.0);
            }
        }

        // Ensure spawn rates are within valid range (0-100)
        validateSpawnRates();

        // Ensure multipliers are positive
        if (this.globalDropRateMultiplier != null && this.globalDropRateMultiplier <= 0) {
            this.globalDropRateMultiplier = 1.0;
        }

        if (this.globalExperienceMultiplier != null && this.globalExperienceMultiplier <= 0) {
            this.globalExperienceMultiplier = 1.0;
        }
    }

    private void validateSpawnRates() {
        if (this.itemSpawnRates != null) {
            this.itemSpawnRates.replaceAll((k, v) -> Math.max(0.0, Math.min(100.0, v)));
        }
        if (this.entitySpawnRates != null) {
            this.entitySpawnRates.replaceAll((k, v) -> Math.max(0.0, Math.min(100.0, v)));
        }
        if (this.chestSpawnRates != null) {
            this.chestSpawnRates.replaceAll((k, v) -> Math.max(0.0, Math.min(100.0, v)));
        }
    }

    // ==================== RARITY SYSTEM GETTERS ====================

    public @NotNull Map<String, Double> getRarityWeights() {
        return this.rarityWeights != null ? this.rarityWeights : getDefaultRarityWeights();
    }

    private @NotNull Map<String, Double> getDefaultRarityWeights() {
        return Map.of(
            "COMMON", 50.0,
            "UNCOMMON", 25.0,
            "RARE", 15.0,
            "EPIC", 7.0,
            "LEGENDARY", 2.5,
            "MYTHIC", 0.4,
            "OMNIPOTENT", 0.1
        );
    }

    public @NotNull Map<String, Integer> getRarityMultipliers() {
        return this.rarityMultipliers != null ? this.rarityMultipliers : getDefaultRarityMultipliers();
    }

    private @NotNull Map<String, Integer> getDefaultRarityMultipliers() {
        return Map.of(
            "COMMON", 1,
            "UNCOMMON", 2,
            "RARE", 3,
            "EPIC", 5,
            "LEGENDARY", 8,
            "MYTHIC", 12,
            "OMNIPOTENT", 20
        );
    }

    public boolean isDynamicRarityEnabled() {
        return this.enableDynamicRarity != null ? this.enableDynamicRarity : true;
    }

    public @NotNull Double getRarityBoostPerEvolution() {
        return this.rarityBoostPerEvolution != null ? this.rarityBoostPerEvolution : 0.1;
    }

    public @NotNull Integer getMaxRarityBoost() {
        return this.maxRarityBoost != null ? this.maxRarityBoost : 50;
    }

    // ==================== SPAWN RATES GETTERS ====================

    public @NotNull Map<String, Double> getItemSpawnRates() {
        return this.itemSpawnRates != null ? this.itemSpawnRates : getDefaultItemSpawnRates();
    }

    private @NotNull Map<String, Double> getDefaultItemSpawnRates() {
        return Map.of(
            "COMMON", 15.0,
            "UNCOMMON", 10.0,
            "RARE", 7.5,
            "EPIC", 5.0,
            "LEGENDARY", 2.5,
            "MYTHIC", 1.0,
            "OMNIPOTENT", 0.5
        );
    }

    public @NotNull Map<String, Double> getEntitySpawnRates() {
        return this.entitySpawnRates != null ? this.entitySpawnRates : getDefaultEntitySpawnRates();
    }

    private @NotNull Map<String, Double> getDefaultEntitySpawnRates() {
        return Map.of(
            "COMMON", 8.0,
            "UNCOMMON", 5.0,
            "RARE", 3.0,
            "EPIC", 2.0,
            "LEGENDARY", 1.0,
            "MYTHIC", 0.5,
            "OMNIPOTENT", 0.1
        );
    }

    public @NotNull Map<String, Double> getChestSpawnRates() {
        return this.chestSpawnRates != null ? this.chestSpawnRates : getDefaultChestSpawnRates();
    }

    private @NotNull Map<String, Double> getDefaultChestSpawnRates() {
        return Map.of(
            "COMMON", 5.0,
            "UNCOMMON", 3.0,
            "RARE", 2.0,
            "EPIC", 1.5,
            "LEGENDARY", 1.0,
            "MYTHIC", 0.5,
            "OMNIPOTENT", 0.2
        );
    }

    public @NotNull Double getBaseItemSpawnChance() {
        return this.baseItemSpawnChance != null ? this.baseItemSpawnChance : 25.0;
    }

    public @NotNull Double getBaseEntitySpawnChance() {
        return this.baseEntitySpawnChance != null ? this.baseEntitySpawnChance : 15.0;
    }

    public @NotNull Double getBaseChestSpawnChance() {
        return this.baseChestSpawnChance != null ? this.baseChestSpawnChance : 8.0;
    }

    // ==================== STARTING BLOCKS GETTERS ====================

    public @NotNull Map<String, Double> getStartingBlockWeights() {
        return this.startingBlockWeights != null ? this.startingBlockWeights : getDefaultStartingBlocks();
    }

    private @NotNull Map<String, Double> getDefaultStartingBlocks() {
        return Map.of(
            "DIRT", 45.0,
            "OAK_LOG", 25.0,
            "GRASS_BLOCK", 15.0,
            "COBBLESTONE", 10.0,
            "SAND", 5.0
        );
    }

    public @NotNull Integer getStartingBlockCount() {
        return this.startingBlockCount != null ? this.startingBlockCount : 15;
    }

    public boolean isStartingBlockProgressionEnabled() {
        return this.enableStartingBlockProgression != null ? this.enableStartingBlockProgression : true;
    }

    public @NotNull Map<String, List<String>> getEvolutionBasedStartingBlocks() {
        return this.evolutionBasedStartingBlocks != null ? this.evolutionBasedStartingBlocks : getDefaultEvolutionBlocks();
    }

    private @NotNull Map<String, List<String>> getDefaultEvolutionBlocks() {
        return Map.of(
            "1-5", List.of("DIRT", "OAK_LOG", "GRASS_BLOCK"),
            "6-10", List.of("COBBLESTONE", "STONE", "COAL_ORE"),
            "11-20", List.of("IRON_ORE", "GOLD_ORE", "REDSTONE_ORE"),
            "21+", List.of("DIAMOND_ORE", "EMERALD_ORE", "NETHERITE_SCRAP")
        );
    }

    // ==================== PROGRESSION SYSTEM GETTERS ====================

    public @NotNull Map<String, Integer> getEvolutionExperienceRequirements() {
        return this.evolutionExperienceRequirements != null ? this.evolutionExperienceRequirements : getDefaultEvolutionRequirements();
    }

    private @NotNull Map<String, Integer> getDefaultEvolutionRequirements() {
        Map<String, Integer> requirements = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            requirements.put(String.valueOf(i), i * 100 + (i * i * 10));
        }
        return requirements;
    }

    public @NotNull Map<String, Double> getExperienceMultipliers() {
        return this.experienceMultipliers != null ? this.experienceMultipliers : getDefaultExperienceMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultExperienceMultipliers() {
        return Map.of(
            "COMMON", 1.0,
            "UNCOMMON", 1.5,
            "RARE", 2.0,
            "EPIC", 3.0,
            "LEGENDARY", 5.0,
            "MYTHIC", 8.0,
            "OMNIPOTENT", 15.0
        );
    }

    public boolean areExperienceBoostsEnabled() {
        return this.enableExperienceBoosts != null ? this.enableExperienceBoosts : true;
    }

    public @NotNull Double getExperienceBoostPerRarity() {
        return this.experienceBoostPerRarity != null ? this.experienceBoostPerRarity : 0.25;
    }

    public @NotNull Integer getMaxExperienceBoost() {
        return this.maxExperienceBoost != null ? this.maxExperienceBoost : 100;
    }

    // ==================== SPECIAL EVENTS GETTERS ====================

    public @NotNull Map<String, Double> getSpecialEventChances() {
        return this.specialEventChances != null ? this.specialEventChances : getDefaultEventChances();
    }

    private @NotNull Map<String, Double> getDefaultEventChances() {
        return Map.of(
            "DOUBLE_DROPS", 5.0,
            "TRIPLE_EXPERIENCE", 3.0,
            "RARE_BOOST", 2.0,
            "LUCKY_BREAK", 1.0,
            "TREASURE_RAIN", 0.5,
            "EVOLUTION_BOOST", 1.5,
            "MEGA_FORTUNE", 0.3
        );
    }

    public @NotNull Map<String, Object> getEventRewards() {
        return this.eventRewards != null ? this.eventRewards : getDefaultEventRewards();
    }

    private @NotNull Map<String, Object> getDefaultEventRewards() {
        return Map.of(
            "DOUBLE_DROPS", Map.of("duration", 300, "multiplier", 2.0),
            "TRIPLE_EXPERIENCE", Map.of("duration", 180, "multiplier", 3.0),
            "RARE_BOOST", Map.of("duration", 240, "rarity_boost", 25.0),
            "LUCKY_BREAK", Map.of("duration", 120, "luck_level", 3),
            "TREASURE_RAIN", Map.of("duration", 60, "item_count", 10),
            "EVOLUTION_BOOST", Map.of("duration", 300, "evolution_chance", 0.5),
            "MEGA_FORTUNE", Map.of("duration", 90, "fortune_level", 5)
        );
    }

    public boolean areRandomEventsEnabled() {
        return this.enableRandomEvents != null ? this.enableRandomEvents : true;
    }

    public @NotNull Integer getEventCooldownMinutes() {
        return this.eventCooldownMinutes != null ? this.eventCooldownMinutes : 30;
    }

    // ==================== BALANCE SETTINGS GETTERS ====================

    public @NotNull Double getGlobalDropRateMultiplier() {
        return this.globalDropRateMultiplier != null ? this.globalDropRateMultiplier : 1.0;
    }

    public @NotNull Double getGlobalExperienceMultiplier() {
        return this.globalExperienceMultiplier != null ? this.globalExperienceMultiplier : 1.0;
    }

    public boolean isAntiGrindingEnabled() {
        return this.enableAntiGrinding != null ? this.enableAntiGrinding : false;
    }

    public @NotNull Integer getAntiGrindingThreshold() {
        return this.antiGrindingThreshold != null ? this.antiGrindingThreshold : 100;
    }

    public @NotNull Double getAntiGrindingPenalty() {
        return this.antiGrindingPenalty != null ? this.antiGrindingPenalty : 0.5;
    }

    // ==================== PERFORMANCE GETTERS ====================

    public @NotNull Integer getMaxConcurrentSpawns() {
        return this.maxConcurrentSpawns != null ? this.maxConcurrentSpawns : 3;
    }

    public boolean isAsyncSpawningEnabled() {
        return this.enableAsyncSpawning != null ? this.enableAsyncSpawning : true;
    }

    public @NotNull Integer getSpawnProcessingDelay() {
        return this.spawnProcessingDelay != null ? this.spawnProcessingDelay : 1;
    }

    public boolean isSpawnOptimizationEnabled() {
        return this.enableSpawnOptimization != null ? this.enableSpawnOptimization : true;
    }

    // ==================== ADVANCED FEATURES GETTERS ====================

    public @NotNull Map<String, Object> getEvolutionSettings() {
        return this.evolutionSettings != null ? this.evolutionSettings : getDefaultEvolutionSettings();
    }

    private @NotNull Map<String, Object> getDefaultEvolutionSettings() {
        return Map.of(
            "auto_evolution", Map.of("enabled", true, "threshold", 1000),
            "evolution_notifications", Map.of("enabled", true, "broadcast", false),
            "evolution_rewards", Map.of("enabled", true, "bonus_multiplier", 1.5),
            "evolution_requirements", Map.of("blocks_broken", 100, "experience_gained", 500)
        );
    }

    public boolean isEvolutionSystemEnabled() {
        return this.enableEvolutionSystem != null ? this.enableEvolutionSystem : true;
    }

    public @NotNull Map<String, Integer> getEvolutionRequirements() {
        return this.evolutionRequirements != null ? this.evolutionRequirements : getDefaultEvolutionMaterialRequirements();
    }

    private @NotNull Map<String, Integer> getDefaultEvolutionMaterialRequirements() {
        return Map.of(
            "STONE", 100,
            "COAL", 250,
            "IRON", 500,
            "GOLD", 750,
            "DIAMOND", 1000,
            "EMERALD", 1500,
            "NETHERITE", 2500,
            "END", 5000,
            "COSMIC", 10000,
            "OMNIPOTENT", 25000
        );
    }

    public @NotNull Map<String, Object> getEvolutionRewards() {
        return this.evolutionRewards != null ? this.evolutionRewards : getDefaultEvolutionRewards();
    }

    private @NotNull Map<String, Object> getDefaultEvolutionRewards() {
        return Map.of(
            "STONE", Map.of("coins", 100, "experience", 50),
            "COAL", Map.of("coins", 250, "experience", 125),
            "IRON", Map.of("coins", 500, "experience", 250),
            "GOLD", Map.of("coins", 1000, "experience", 500),
            "DIAMOND", Map.of("coins", 2500, "experience", 1250),
            "EMERALD", Map.of("coins", 5000, "experience", 2500),
            "NETHERITE", Map.of("coins", 10000, "experience", 5000),
            "END", Map.of("coins", 25000, "experience", 12500),
            "COSMIC", Map.of("coins", 50000, "experience", 25000),
            "OMNIPOTENT", Map.of("coins", 100000, "experience", 50000)
        );
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculates the effective spawn rate for a specific rarity and type.
     */
    public double getEffectiveSpawnRate(@NotNull String rarity, @NotNull String spawnType) {
        Map<String, Double> rates = switch (spawnType.toLowerCase()) {
            case "item" -> getItemSpawnRates();
            case "entity" -> getEntitySpawnRates();
            case "chest" -> getChestSpawnRates();
            default -> new HashMap<>();
        };

        double baseRate = rates.getOrDefault(rarity, 0.0);
        return baseRate * getGlobalDropRateMultiplier();
    }

    /**
     * Calculates experience gained for a specific rarity.
     */
    public double calculateExperience(@NotNull String rarity, int baseExperience) {
        double multiplier = getExperienceMultipliers().getOrDefault(rarity, 1.0);
        return baseExperience * multiplier * getGlobalExperienceMultiplier();
    }

    /**
     * Gets the experience requirement for a specific evolution.
     */
    public int getExperienceRequirement(int evolution) {
        return getEvolutionExperienceRequirements().getOrDefault(String.valueOf(evolution), evolution * 100);
    }

    /**
     * Determines if an event should trigger based on chance.
     */
    public boolean shouldTriggerEvent(@NotNull String eventType) {
        if (!areRandomEventsEnabled()) {
            return false;
        }

        double chance = getSpecialEventChances().getOrDefault(eventType, 0.0);
        return Math.random() * 100.0 < chance;
    }

    /**
     * Calculates the rarity boost based on evolution progression.
     */
    public double calculateRarityBoost(int evolution) {
        if (!isDynamicRarityEnabled()) {
            return 0.0;
        }

        double boost = evolution * getRarityBoostPerEvolution();
        return Math.min(boost, getMaxRarityBoost() / 100.0);
    }

    /**
     * Gets the evolution requirement for a specific evolution type.
     */
    public int getEvolutionRequirement(@NotNull String evolutionType) {
        return getEvolutionRequirements().getOrDefault(evolutionType.toUpperCase(), 1000);
    }

    /**
     * Checks if a player meets the evolution requirements.
     */
    public boolean meetsEvolutionRequirements(@NotNull String evolutionType, int playerProgress) {
        int requirement = getEvolutionRequirement(evolutionType);
        return playerProgress >= requirement;
    }

    // ==================== LISTENER INTEGRATION METHODS ====================

    /**
     * Gets the evolution level rarity bonus multiplier.
     */
    public double getEvolutionLevelRarityBonus() {
        return 0.001; // 0.1% per evolution level
    }

    /**
     * Gets the prestige level rarity bonus multiplier.
     */
    public double getPrestigeLevelRarityBonus() {
        return 0.005; // 0.5% per prestige level
    }

    /**
     * Gets the streak rarity bonus multiplier.
     */
    public double getStreakRarityBonus() {
        return 0.0001; // 0.01% per streak count
    }

    /**
     * Gets the maximum streak bonus.
     */
    public double getMaxStreakBonus() {
        return 0.1; // Maximum 10% bonus from streaks
    }

    /**
     * Gets the base experience per break.
     */
    public double getBaseExperiencePerBreak() {
        return 1.0;
    }

    /**
     * Gets the evolution experience multiplier.
     */
    public double getEvolutionExperienceMultiplier() {
        return 0.01; // 1% per evolution level
    }

    /**
     * Gets the prestige experience multiplier.
     */
    public double getPrestigeExperienceMultiplier() {
        return 0.1; // 10% per prestige level
    }

    /**
     * Gets the required experience for a specific level.
     */
    public double getRequiredExperienceForLevel(int level) {
        return level * 100.0 + (level * level * 5.0);
    }

    /**
     * Gets the next evolution for a given evolution and level.
     * This method should work with the auto-registered evolution system.
     */
    public String getNextEvolution(String currentEvolution, int level) {
        // The evolution system auto-registers 50+ evolutions with specific levels
        // We need to find the next evolution based on the current level
        
        // Evolution progression based on the auto-registered evolutions:
        // Genesis(1) -> Terra(2) -> Aqua(3) -> Ignis(4) -> Ventus(5) -> Stone(6) -> Copper(7) -> Iron(8) -> Coal(9) -> Wood(10)...
        
        return switch (currentEvolution) {
            case "Genesis" -> level >= 2 ? "Terra" : null;
            case "Terra" -> level >= 3 ? "Aqua" : null;
            case "Aqua" -> level >= 4 ? "Ignis" : null;
            case "Ignis" -> level >= 5 ? "Ventus" : null;
            case "Ventus" -> level >= 6 ? "Stone" : null;
            case "Stone" -> level >= 7 ? "Copper" : null;
            case "Copper" -> level >= 8 ? "Iron" : null;
            case "Iron" -> level >= 9 ? "Coal" : null;
            case "Coal" -> level >= 10 ? "Wood" : null;
            case "Wood" -> level >= 11 ? "Bronze" : null;
            case "Bronze" -> level >= 12 ? "Gold" : null;
            case "Gold" -> level >= 13 ? "Diamond" : null;
            case "Diamond" -> level >= 14 ? "Nether" : null;
            case "Nether" -> level >= 15 ? "End" : null;
            case "End" -> level >= 16 ? "Knight" : null;
            case "Knight" -> level >= 17 ? "Castle" : null;
            case "Castle" -> level >= 18 ? "Artemis" : null;
            case "Artemis" -> level >= 19 ? "Dragon" : null;
            case "Dragon" -> level >= 20 ? "Crusader" : null;
            case "Crusader" -> level >= 21 ? "Explorer" : null;
            case "Explorer" -> level >= 22 ? "Helium" : null;
            case "Helium" -> level >= 23 ? "Artist" : null;
            case "Artist" -> level >= 24 ? "Argon" : null;
            case "Argon" -> level >= 25 ? "Krypton" : null;
            case "Krypton" -> level >= 26 ? "Hephaestus" : null;
            case "Hephaestus" -> level >= 27 ? "Electric" : null;
            case "Electric" -> level >= 28 ? "Factory" : null;
            case "Factory" -> level >= 29 ? "Earth" : null;
            case "Earth" -> level >= 30 ? "Moon" : null;
            case "Moon" -> level >= 31 ? "Cyber" : null;
            case "Cyber" -> level >= 32 ? "Nano" : null;
            case "Nano" -> level >= 33 ? "Bio" : null;
            case "Bio" -> level >= 34 ? "Quantum" : null;
            case "Quantum" -> level >= 35 ? "Digital" : null;
            case "Digital" -> level >= 36 ? "Solar" : null;
            case "Solar" -> level >= 37 ? "Void" : null;
            case "Void" -> level >= 38 ? "Nebula" : null;
            case "Nebula" -> level >= 39 ? "Supernova" : null;
            case "Supernova" -> level >= 40 ? "Black Hole" : null;
            case "Black Hole" -> level >= 41 ? "Stellar" : null;
            case "Stellar" -> level >= 42 ? "Galactic" : null;
            case "Galactic" -> level >= 43 ? "Multiverse" : null;
            case "Multiverse" -> level >= 44 ? "Infinity" : null;
            case "Infinity" -> level >= 45 ? "Cosmic" : null;
            case "Cosmic" -> level >= 46 ? "Eternity" : null;
            case "Eternity" -> level >= 47 ? "Omnipotence" : null;
            case "Omnipotence" -> level >= 48 ? "Dimensional" : null;
            case "Dimensional" -> level >= 49 ? "Universal" : null;
            case "Universal" -> level >= 50 ? "Eden" : null;
            default -> null; // No further evolution or unknown evolution
        };
    }

    // ==================== RARITY CHANCE METHODS ====================

    public double getOmnipotentChance() { return 0.0001; } // 0.01%
    public double getInfiniteChance() { return 0.0005; } // 0.05%
    public double getCosmicChance() { return 0.001; } // 0.1%
    public double getEtherealChance() { return 0.002; } // 0.2%
    public double getTranscendentChance() { return 0.005; } // 0.5%
    public double getCelestialChance() { return 0.01; } // 1%
    public double getDivineChance() { return 0.02; } // 2%
    public double getMythicalChance() { return 0.05; } // 5%
    public double getUniqueChance() { return 0.1; } // 10%
    public double getSpecialChance() { return 0.2; } // 20%
    public double getLegendaryChance() { return 0.3; } // 30%
    public double getEpicChance() { return 0.5; } // 50%
    public double getRareChance() { return 0.7; } // 70%
    public double getUncommonChance() { return 0.85; } // 85%

    // ==================== GENERATOR UNLOCK LEVELS ====================

    public int getOmnipotentGeneratorUnlockLevel() { return 1000; }
    public int getCosmicGeneratorUnlockLevel() { return 500; }
    public int getMythicalGeneratorUnlockLevel() { return 250; }
    public int getLegendaryGeneratorUnlockLevel() { return 100; }
    public int getAdvancedGeneratorUnlockLevel() { return 50; }

    // ==================== TRIGGER BLOCKS ====================

    public @NotNull List<org.bukkit.Material> getGeneratorTriggerBlocks() {
        return List.of(
            org.bukkit.Material.COBBLESTONE,
            org.bukkit.Material.STONE,
            org.bukkit.Material.DEEPSLATE,
            org.bukkit.Material.COAL_ORE,
            org.bukkit.Material.IRON_ORE,
            org.bukkit.Material.GOLD_ORE,
            org.bukkit.Material.DIAMOND_ORE,
            org.bukkit.Material.EMERALD_ORE
        );
    }

    // ==================== EVOLUTION BLOCKS ====================

    public @NotNull Map<String, Double> getEvolutionBlocks(String evolution, de.jexcellence.oneblock.type.EEvolutionRarityType rarity) {
        // Return blocks available for specific evolution and rarity
        Map<String, Double> blocks = new HashMap<>();
        
        switch (rarity) {
            case COMMON -> {
                blocks.put("COBBLESTONE", 40.0);
                blocks.put("STONE", 30.0);
                blocks.put("DIRT", 20.0);
                blocks.put("GRASS_BLOCK", 10.0);
            }
            case UNCOMMON -> {
                blocks.put("COAL_ORE", 50.0);
                blocks.put("IRON_ORE", 30.0);
                blocks.put("COPPER_ORE", 20.0);
            }
            case RARE -> {
                blocks.put("GOLD_ORE", 40.0);
                blocks.put("REDSTONE_ORE", 35.0);
                blocks.put("LAPIS_ORE", 25.0);
            }
            case EPIC -> {
                blocks.put("DIAMOND_ORE", 60.0);
                blocks.put("EMERALD_ORE", 40.0);
            }
            case LEGENDARY -> {
                blocks.put("NETHERITE_SCRAP", 70.0);
                blocks.put("ANCIENT_DEBRIS", 30.0);
            }
            default -> {
                blocks.put("BEDROCK", 100.0);
            }
        }
        
        return blocks;
    }

    // ==================== SOUND AND PARTICLE EFFECTS ====================

    public String getRaritySound(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "BLOCK_STONE_BREAK";
            case UNCOMMON -> "BLOCK_GRAVEL_BREAK";
            case RARE -> "BLOCK_METAL_BREAK";
            case EPIC -> "ENTITY_EXPERIENCE_ORB_PICKUP";
            case LEGENDARY -> "ENTITY_PLAYER_LEVELUP";
            case SPECIAL -> "BLOCK_BEACON_ACTIVATE";
            case UNIQUE -> "BLOCK_BEACON_POWER_SELECT";
            case MYTHICAL -> "BLOCK_END_PORTAL_FRAME_FILL";
            case DIVINE -> "ENTITY_ENDER_DRAGON_DEATH";
            case CELESTIAL -> "UI_TOAST_CHALLENGE_COMPLETE";
            default -> "BLOCK_ANVIL_LAND";
        };
    }

    public String getRarityParticle(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> "CRIT";
            case UNCOMMON -> "VILLAGER_HAPPY";
            case RARE -> "CRIT";
            case EPIC -> "ENCHANTED_HIT";
            case LEGENDARY -> "FIREWORK";
            case SPECIAL -> "DRAGON_BREATH";
            case UNIQUE -> "END_ROD";
            case MYTHICAL -> "PORTAL";
            case DIVINE -> "TOTEM_OF_UNDYING";
            case CELESTIAL -> "SOUL_FIRE_FLAME";
            default -> "EXPLOSION";
        };
    }

    public int getParticleCount(de.jexcellence.oneblock.type.EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 15;
            case EPIC -> 25;
            case LEGENDARY -> 40;
            case SPECIAL -> 60;
            case UNIQUE -> 80;
            case MYTHICAL -> 100;
            case DIVINE -> 150;
            case CELESTIAL -> 200;
            default -> 300;
        };
    }

    // ==================== SPECIAL EVENT CHANCES ====================

    public double getChestSpawnChance(de.jexcellence.oneblock.type.EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.01; // 1%
            case UNCOMMON -> 0.02; // 2%
            case RARE -> 0.05; // 5%
            case EPIC -> 0.1; // 10%
            case LEGENDARY -> 0.2; // 20%
            case SPECIAL -> 0.3; // 30%
            case UNIQUE -> 0.4; // 40%
            case MYTHICAL -> 0.5; // 50%
            case DIVINE -> 0.7; // 70%
            case CELESTIAL -> 0.8; // 80%
            default -> 0.9; // 90%
        };
    }

    public double getEntitySpawnChance(de.jexcellence.oneblock.type.EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.005; // 0.5%
            case UNCOMMON -> 0.01; // 1%
            case RARE -> 0.02; // 2%
            case EPIC -> 0.05; // 5%
            case LEGENDARY -> 0.1; // 10%
            case SPECIAL -> 0.15; // 15%
            case UNIQUE -> 0.2; // 20%
            case MYTHICAL -> 0.3; // 30%
            case DIVINE -> 0.4; // 40%
            case CELESTIAL -> 0.5; // 50%
            default -> 0.6; // 60%
        };
    }

    public double getSpecialItemChance(de.jexcellence.oneblock.type.EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.001; // 0.1%
            case UNCOMMON -> 0.005; // 0.5%
            case RARE -> 0.01; // 1%
            case EPIC -> 0.02; // 2%
            case LEGENDARY -> 0.05; // 5%
            case SPECIAL -> 0.1; // 10%
            case UNIQUE -> 0.15; // 15%
            case MYTHICAL -> 0.25; // 25%
            case DIVINE -> 0.4; // 40%
            case CELESTIAL -> 0.6; // 60%
            default -> 0.8; // 80%
        };
    }
}