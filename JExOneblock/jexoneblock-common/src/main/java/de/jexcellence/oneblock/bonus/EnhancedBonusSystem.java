package de.jexcellence.oneblock.bonus;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.service.DynamicEvolutionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Enhanced Bonus System for Dynamic Evolution Bonuses
 * 
 * Provides dynamic bonus calculation based on evolution tier, level, and progression.
 * Integrates with the existing bonus system while adding tier-based scaling,
 * bonus stacking, and infrastructure integration.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class EnhancedBonusSystem {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final DynamicEvolutionService evolutionService;
    private final BonusManager bonusManager;
    
    // Bonus calculation caches
    private final Map<String, List<EnhancedBonus>> bonusCache = new ConcurrentHashMap<>();
    private final Map<String, BonusMultipliers> multiplierCache = new ConcurrentHashMap<>();
    
    // Evolution tier definitions for bonus scaling
    private static final Map<Integer, EvolutionTier> EVOLUTION_TIERS = Map.of(
        1, new EvolutionTier("Genesis", 1, 10, 1.0),
        2, new EvolutionTier("Primordial", 11, 20, 1.2),
        3, new EvolutionTier("Ancient", 21, 30, 1.5),
        4, new EvolutionTier("Medieval", 31, 40, 1.8),
        5, new EvolutionTier("Renaissance", 41, 50, 2.2),
        6, new EvolutionTier("Industrial", 51, 60, 2.7),
        7, new EvolutionTier("Modern", 61, 70, 3.3),
        8, new EvolutionTier("Stellar", 71, 80, 4.0),
        9, new EvolutionTier("Cosmic", 81, 90, 5.0),
        10, new EvolutionTier("Eternal", 91, 100, 6.0)
    );
    
    public EnhancedBonusSystem(@NotNull DynamicEvolutionService evolutionService, @NotNull BonusManager bonusManager) {
        this.evolutionService = evolutionService;
        this.bonusManager = bonusManager;
    }
    
    /**
     * Calculates enhanced bonuses for an island
     * 
     * @param island the island to calculate bonuses for
     * @return list of enhanced bonuses
     */
    @NotNull
    public List<EnhancedBonus> calculateEnhancedBonuses(@NotNull OneblockIsland island) {
        String cacheKey = getCacheKey(island);
        
        return bonusCache.computeIfAbsent(cacheKey, k -> {
            List<EnhancedBonus> bonuses = new ArrayList<>();
            
            String evolutionName = island.getCurrentEvolution();
            int evolutionLevel = island.getOneblock() != null ? island.getOneblock().getEvolutionLevel() : 1;
            
            OneblockEvolution evolution = evolutionService.getEvolution(evolutionName);
            if (evolution == null) {
                LOGGER.warning("Evolution not found: " + evolutionName);
                return bonuses;
            }
            
            EvolutionTier tier = getEvolutionTier(evolution.getLevel());
            
            // Calculate tier-based bonuses
            bonuses.addAll(calculateTierBonuses(tier, evolutionLevel, island));
            
            // Calculate progression bonuses
            bonuses.addAll(calculateProgressionBonuses(evolution, island));
            
            // Calculate infrastructure bonuses
            bonuses.addAll(calculateInfrastructureBonuses(island));
            
            // Calculate stacking bonuses
            bonuses.addAll(calculateStackingBonuses(bonuses, island));
            
            LOGGER.info("Calculated " + bonuses.size() + " enhanced bonuses for island: " + island.getIdentifier());
            return bonuses;
        });
    }
    
    /**
     * Gets the total bonus multiplier for a specific type
     * 
     * @param island the island
     * @param bonusType the bonus type
     * @return total multiplier (1.0 = no bonus)
     */
    public double getTotalBonusMultiplier(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        List<EnhancedBonus> bonuses = calculateEnhancedBonuses(island);
        
        return bonuses.stream()
            .filter(bonus -> bonus.getType() == bonusType)
            .filter(EnhancedBonus::isActive)
            .mapToDouble(EnhancedBonus::getCalculatedValue)
            .reduce(1.0, (a, b) -> a * b);
    }
    
    /**
     * Gets the total additive bonus for a specific type
     * 
     * @param island the island
     * @param bonusType the bonus type
     * @return total additive bonus (0.0 = no bonus)
     */
    public double getTotalBonusAdditive(@NotNull OneblockIsland island, @NotNull Bonus.Type bonusType) {
        List<EnhancedBonus> bonuses = calculateEnhancedBonuses(island);
        
        return bonuses.stream()
            .filter(bonus -> bonus.getType() == bonusType)
            .filter(EnhancedBonus::isActive)
            .mapToDouble(EnhancedBonus::getCalculatedValue)
            .sum();
    }
    
    /**
     * Gets bonus multipliers for all types
     * 
     * @param island the island
     * @return bonus multipliers container
     */
    @NotNull
    public BonusMultipliers getBonusMultipliers(@NotNull OneblockIsland island) {
        String cacheKey = getCacheKey(island);
        
        return multiplierCache.computeIfAbsent(cacheKey, k -> {
            BonusMultipliers multipliers = new BonusMultipliers();
            List<EnhancedBonus> bonuses = calculateEnhancedBonuses(island);
            
            // Calculate multipliers for each bonus type
            for (Bonus.Type type : Bonus.Type.values()) {
                double multiplier = bonuses.stream()
                    .filter(bonus -> bonus.getType() == type)
                    .filter(EnhancedBonus::isActive)
                    .mapToDouble(EnhancedBonus::getCalculatedValue)
                    .reduce(1.0, (a, b) -> a * b);
                
                multipliers.setMultiplier(type, multiplier);
            }
            
            return multipliers;
        });
    }
    
    /**
     * Calculates tier-based bonuses
     */
    @NotNull
    private List<EnhancedBonus> calculateTierBonuses(@NotNull EvolutionTier tier, int evolutionLevel, @NotNull OneblockIsland island) {
        List<EnhancedBonus> bonuses = new ArrayList<>();
        
        double tierMultiplier = tier.getBonusMultiplier();
        double levelProgress = (double) evolutionLevel / 100.0; // 0.0 to 1.0
        
        // Core bonuses that scale with tier
        bonuses.add(new EnhancedBonus(
            Bonus.Type.BLOCK_BREAK_SPEED,
            1.0 + (tierMultiplier * 0.1), // 10% per tier level
            BonusSource.TIER,
            "Tier " + tier.getName() + " Block Breaking"
        ));
        
        bonuses.add(new EnhancedBonus(
            Bonus.Type.EXPERIENCE_MULTIPLIER,
            1.0 + (tierMultiplier * 0.15), // 15% per tier level
            BonusSource.TIER,
            "Tier " + tier.getName() + " Experience"
        ));
        
        bonuses.add(new EnhancedBonus(
            Bonus.Type.RARE_DROPS,
            1.0 + (tierMultiplier * 0.05), // 5% per tier level
            BonusSource.TIER,
            "Tier " + tier.getName() + " Rare Drops"
        ));
        
        // Advanced tier bonuses
        if (tier.getMinLevel() >= 31) { // Medieval and above
            bonuses.add(new EnhancedBonus(
                Bonus.Type.ENERGY,
                1.0 + (tierMultiplier * 0.2),
                BonusSource.TIER,
                "Advanced Energy Generation"
            ));
        }
        
        if (tier.getMinLevel() >= 61) { // Modern and above
            bonuses.add(new EnhancedBonus(
                Bonus.Type.AUTOMATION_EFFICIENCY,
                1.0 + (tierMultiplier * 0.25),
                BonusSource.TIER,
                "Modern Automation"
            ));
        }
        
        if (tier.getMinLevel() >= 81) { // Cosmic and above
            bonuses.add(new EnhancedBonus(
                Bonus.Type.ALL_STATS,
                1.0 + (tierMultiplier * 0.1),
                BonusSource.TIER,
                "Cosmic Enhancement"
            ));
        }
        
        return bonuses;
    }
    
    /**
     * Calculates progression-based bonuses
     */
    @NotNull
    private List<EnhancedBonus> calculateProgressionBonuses(@NotNull OneblockEvolution evolution, @NotNull OneblockIsland island) {
        List<EnhancedBonus> bonuses = new ArrayList<>();
        
        if (island.getOneblock() == null) {
            return bonuses;
        }
        
        double experience = island.getOneblock().getEvolutionExperience();
        double requiredExperience = evolution.getExperienceToPass();
        double progressRatio = Math.min(1.0, experience / requiredExperience);
        
        // Experience-based bonuses
        if (progressRatio > 0.5) { // 50% progress
            bonuses.add(new EnhancedBonus(
                Bonus.Type.LUCK,
                1.0 + (progressRatio * 0.2),
                BonusSource.PROGRESSION,
                "Evolution Progress Luck"
            ));
        }
        
        if (progressRatio > 0.75) { // 75% progress
            bonuses.add(new EnhancedBonus(
                Bonus.Type.PROBABILITY,
                1.0 + (progressRatio * 0.15),
                BonusSource.PROGRESSION,
                "High Progress Probability"
            ));
        }
        
        if (progressRatio >= 1.0) { // 100% progress (ready to evolve)
            bonuses.add(new EnhancedBonus(
                Bonus.Type.EVOLUTION_PROGRESS,
                2.0, // Double evolution progress
                BonusSource.PROGRESSION,
                "Evolution Ready Bonus"
            ));
        }
        
        return bonuses;
    }
    
    /**
     * Calculates infrastructure-based bonuses
     */
    @NotNull
    private List<EnhancedBonus> calculateInfrastructureBonuses(@NotNull OneblockIsland island) {
        List<EnhancedBonus> bonuses = new ArrayList<>();
        
        // This would integrate with the infrastructure system
        // For now, add placeholder bonuses based on island level
        int islandLevel = island.getLevel();
        
        if (islandLevel >= 10) {
            bonuses.add(new EnhancedBonus(
                Bonus.Type.STORAGE_CAPACITY,
                1.0 + (islandLevel * 0.01), // 1% per level
                BonusSource.INFRASTRUCTURE,
                "Island Level Storage"
            ));
        }
        
        if (islandLevel >= 25) {
            bonuses.add(new EnhancedBonus(
                Bonus.Type.GENERATOR_SPEED,
                1.0 + (islandLevel * 0.005), // 0.5% per level
                BonusSource.INFRASTRUCTURE,
                "Island Level Generation"
            ));
        }
        
        return bonuses;
    }
    
    /**
     * Calculates stacking bonuses (bonuses that enhance other bonuses)
     */
    @NotNull
    private List<EnhancedBonus> calculateStackingBonuses(@NotNull List<EnhancedBonus> existingBonuses, @NotNull OneblockIsland island) {
        List<EnhancedBonus> stackingBonuses = new ArrayList<>();
        
        // Count bonuses by source
        long tierBonuses = existingBonuses.stream().filter(b -> b.getSource() == BonusSource.TIER).count();
        long progressionBonuses = existingBonuses.stream().filter(b -> b.getSource() == BonusSource.PROGRESSION).count();
        long infrastructureBonuses = existingBonuses.stream().filter(b -> b.getSource() == BonusSource.INFRASTRUCTURE).count();
        
        // Synergy bonuses for having multiple bonus sources
        if (tierBonuses >= 3 && progressionBonuses >= 2) {
            stackingBonuses.add(new EnhancedBonus(
                Bonus.Type.ALL_STATS,
                1.1, // 10% boost to all stats
                BonusSource.SYNERGY,
                "Tier-Progression Synergy"
            ));
        }
        
        if (infrastructureBonuses >= 2 && tierBonuses >= 2) {
            stackingBonuses.add(new EnhancedBonus(
                Bonus.Type.EFFICIENCY,
                1.15, // 15% efficiency boost
                BonusSource.SYNERGY,
                "Infrastructure-Tier Synergy"
            ));
        }
        
        // Ultimate synergy bonus
        if (tierBonuses >= 5 && progressionBonuses >= 3 && infrastructureBonuses >= 2) {
            stackingBonuses.add(new EnhancedBonus(
                Bonus.Type.ULTIMATE,
                1.25, // 25% ultimate bonus
                BonusSource.SYNERGY,
                "Ultimate Synergy"
            ));
        }
        
        return stackingBonuses;
    }
    
    /**
     * Gets the evolution tier for a given level
     */
    @NotNull
    private EvolutionTier getEvolutionTier(int level) {
        return EVOLUTION_TIERS.values().stream()
            .filter(tier -> level >= tier.getMinLevel() && level <= tier.getMaxLevel())
            .findFirst()
            .orElse(EVOLUTION_TIERS.get(1)); // Default to Genesis
    }
    
    /**
     * Generates cache key for an island
     */
    @NotNull
    private String getCacheKey(@NotNull OneblockIsland island) {
        String evolution = island.getCurrentEvolution();
        int level = island.getOneblock() != null ? island.getOneblock().getEvolutionLevel() : 1;
        int islandLevel = island.getLevel();
        
        return evolution + ":" + level + ":" + islandLevel;
    }
    
    /**
     * Clears bonus caches
     */
    public void clearCache() {
        bonusCache.clear();
        multiplierCache.clear();
        LOGGER.info("Enhanced bonus system cache cleared");
    }
    
    /**
     * Gets cache statistics
     */
    @NotNull
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("bonuses", bonusCache.size());
        stats.put("multipliers", multiplierCache.size());
        return stats;
    }
    
    /**
     * Evolution tier definition
     */
    private static class EvolutionTier {
        private final String name;
        private final int minLevel;
        private final int maxLevel;
        private final double bonusMultiplier;
        
        public EvolutionTier(String name, int minLevel, int maxLevel, double bonusMultiplier) {
            this.name = name;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.bonusMultiplier = bonusMultiplier;
        }
        
        public String getName() { return name; }
        public int getMinLevel() { return minLevel; }
        public int getMaxLevel() { return maxLevel; }
        public double getBonusMultiplier() { return bonusMultiplier; }
    }
    
    /**
     * Enhanced bonus with additional metadata
     */
    public static class EnhancedBonus implements Bonus {
        private final Type type;
        private final double baseValue;
        private final BonusSource source;
        private final String description;
        private final boolean active;
        private final long duration;
        
        public EnhancedBonus(Type type, double baseValue, BonusSource source, String description) {
            this(type, baseValue, source, description, true, -1L);
        }
        
        public EnhancedBonus(Type type, double baseValue, BonusSource source, String description, boolean active, long duration) {
            this.type = type;
            this.baseValue = baseValue;
            this.source = source;
            this.description = description;
            this.active = active;
            this.duration = duration;
        }
        
        @Override
        public @NotNull Type getType() { return type; }
        
        @Override
        public double getValue() { return baseValue; }
        
        public double getCalculatedValue() {
            // Apply any additional calculations here
            return baseValue;
        }
        
        @Override
        public @NotNull String getFormattedDescription() {
            double percentage = (baseValue - 1.0) * 100;
            return description + ": +" + String.format("%.1f", percentage) + "%";
        }
        
        @Override
        public boolean isActive() { return active; }
        
        @Override
        public long getDuration() { return duration; }
        
        public BonusSource getSource() { return source; }
        public String getDescription() { return description; }
    }
    
    /**
     * Bonus source enumeration
     */
    public enum BonusSource {
        TIER("Evolution Tier"),
        PROGRESSION("Evolution Progress"),
        INFRASTRUCTURE("Infrastructure"),
        SYNERGY("Bonus Synergy"),
        TEMPORARY("Temporary Effect");
        
        private final String displayName;
        
        BonusSource(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Container for bonus multipliers
     */
    public static class BonusMultipliers {
        private final Map<Bonus.Type, Double> multipliers = new EnumMap<>(Bonus.Type.class);
        
        public void setMultiplier(Bonus.Type type, double multiplier) {
            multipliers.put(type, multiplier);
        }
        
        public double getMultiplier(Bonus.Type type) {
            return multipliers.getOrDefault(type, 1.0);
        }
        
        public Map<Bonus.Type, Double> getAllMultipliers() {
            return new EnumMap<>(multipliers);
        }
        
        public boolean hasBonus(Bonus.Type type) {
            return getMultiplier(type) > 1.0;
        }
    }
}