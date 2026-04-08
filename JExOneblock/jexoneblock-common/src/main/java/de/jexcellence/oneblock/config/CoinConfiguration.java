package de.jexcellence.oneblock.config;

import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Configuration class for the coin earning system
 * Handles dynamic coin ranges, multipliers, and bonus chances
 * 
 * @author JExcellence
 * @since 1.0.10
 */
public class CoinConfiguration {
    
    private final Random random = new Random();
    
    // Coin ranges by rarity
    private final Map<EEvolutionRarityType, CoinRange> coinRanges = new EnumMap<>(EEvolutionRarityType.class);
    
    // Multipliers
    private double evolutionLevelMultiplier = 0.05;
    private double prestigeLevelMultiplier = 0.25;
    private double streakMultiplier = 0.02;
    private double maxStreakMultiplier = 0.5;
    
    // Bonus chances
    private double doubleCoinChance = 0.05;
    private double tripleCoinChance = 0.01;
    private double luckyCoinChance = 0.02;
    private double luckyCoinMinMultiplier = 1.5;
    private double luckyCoinMaxMultiplier = 3.0;
    
    // System enabled
    private boolean enabled = true;
    
    /**
     * Loads coin configuration from the provided config
     */
    public void loadFromConfig(@NotNull FileConfiguration config) {
        ConfigurationSection coinSection = config.getConfigurationSection("coins");
        if (coinSection == null) {
            loadDefaults();
            return;
        }
        
        enabled = coinSection.getBoolean("enabled", true);
        
        // Load coin ranges
        ConfigurationSection rangesSection = coinSection.getConfigurationSection("ranges");
        if (rangesSection != null) {
            for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
                String rarityKey = rarity.name().toLowerCase();
                ConfigurationSection raritySection = rangesSection.getConfigurationSection(rarityKey);
                
                if (raritySection != null) {
                    int min = raritySection.getInt("min", getDefaultMin(rarity));
                    int max = raritySection.getInt("max", getDefaultMax(rarity));
                    double weight = raritySection.getDouble("weight", getDefaultWeight(rarity));
                    
                    coinRanges.put(rarity, new CoinRange(min, max, weight));
                } else {
                    // Use defaults
                    coinRanges.put(rarity, new CoinRange(
                        getDefaultMin(rarity), 
                        getDefaultMax(rarity), 
                        getDefaultWeight(rarity)
                    ));
                }
            }
        } else {
            loadDefaultRanges();
        }
        
        // Load multipliers
        ConfigurationSection multipliersSection = coinSection.getConfigurationSection("multipliers");
        if (multipliersSection != null) {
            evolutionLevelMultiplier = multipliersSection.getDouble("evolutionLevelMultiplier", 0.05);
            prestigeLevelMultiplier = multipliersSection.getDouble("prestigeLevelMultiplier", 0.25);
            streakMultiplier = multipliersSection.getDouble("streakMultiplier", 0.02);
            maxStreakMultiplier = multipliersSection.getDouble("maxStreakMultiplier", 0.5);
        }
        
        // Load bonuses
        ConfigurationSection bonusesSection = coinSection.getConfigurationSection("bonuses");
        if (bonusesSection != null) {
            doubleCoinChance = bonusesSection.getDouble("doubleCoinChance", 0.05);
            tripleCoinChance = bonusesSection.getDouble("tripleCoinChance", 0.01);
            luckyCoinChance = bonusesSection.getDouble("luckyCoinMultiplier.chance", 0.02);
            luckyCoinMinMultiplier = bonusesSection.getDouble("luckyCoinMultiplier.min", 1.5);
            luckyCoinMaxMultiplier = bonusesSection.getDouble("luckyCoinMultiplier.max", 3.0);
        }
    }
    
    /**
     * Calculates coins for a given rarity with weighted randomization
     */
    public long calculateCoins(@NotNull EEvolutionRarityType rarity, int evolutionLevel, int prestigeLevel, int breakStreak) {
        if (!enabled) {
            return 0;
        }
        
        CoinRange range = coinRanges.get(rarity);
        if (range == null) {
            return 1; // Fallback
        }
        
        // Calculate base coins using weighted distribution
        long baseCoins = range.getWeightedRandomValue(random);
        
        // Apply multipliers
        double totalMultiplier = 1.0;
        
        // Evolution level multiplier
        totalMultiplier += evolutionLevel * evolutionLevelMultiplier;
        
        // Prestige level multiplier
        totalMultiplier += prestigeLevel * prestigeLevelMultiplier;
        
        // Break streak multiplier (capped)
        double streakBonus = Math.min((breakStreak / 10) * streakMultiplier, maxStreakMultiplier);
        totalMultiplier += streakBonus;
        
        // Calculate final base coins
        long finalCoins = Math.round(baseCoins * totalMultiplier);
        
        // Apply bonus chances
        finalCoins = applyBonuses(finalCoins);
        
        return Math.max(1, finalCoins); // Minimum 1 coin
    }
    
    /**
     * Applies bonus multipliers based on chance
     */
    private long applyBonuses(long baseCoins) {
        double multiplier = 1.0;
        
        // Check for triple coins first (rarest)
        if (random.nextDouble() < tripleCoinChance) {
            multiplier = 3.0;
        }
        // Check for double coins
        else if (random.nextDouble() < doubleCoinChance) {
            multiplier = 2.0;
        }
        // Check for lucky coin multiplier
        else if (random.nextDouble() < luckyCoinChance) {
            multiplier = luckyCoinMinMultiplier + 
                        (random.nextDouble() * (luckyCoinMaxMultiplier - luckyCoinMinMultiplier));
        }
        
        return Math.round(baseCoins * multiplier);
    }
    
    /**
     * Loads default configuration values
     */
    private void loadDefaults() {
        enabled = true;
        loadDefaultRanges();
    }
    
    /**
     * Loads default coin ranges
     */
    private void loadDefaultRanges() {
        coinRanges.clear();
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            coinRanges.put(rarity, new CoinRange(
                getDefaultMin(rarity), 
                getDefaultMax(rarity), 
                getDefaultWeight(rarity)
            ));
        }
    }
    
    /**
     * Gets default minimum coins for a rarity
     */
    private int getDefaultMin(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case EPIC -> 8;
            case LEGENDARY -> 15;
            case SPECIAL -> 30;
            case UNIQUE -> 60;
            case MYTHICAL -> 120;
            case DIVINE -> 250;
            case CELESTIAL -> 500;
            case TRANSCENDENT -> 1000;
            case ETHEREAL -> 2000;
            case COSMIC -> 4000;
            case INFINITE -> 8000;
            case OMNIPOTENT -> 15000;
            default -> 1;
        };
    }
    
    /**
     * Gets default maximum coins for a rarity
     */
    private int getDefaultMax(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 3;
            case UNCOMMON -> 6;
            case RARE -> 12;
            case EPIC -> 25;
            case LEGENDARY -> 50;
            case SPECIAL -> 100;
            case UNIQUE -> 200;
            case MYTHICAL -> 400;
            case DIVINE -> 800;
            case CELESTIAL -> 1500;
            case TRANSCENDENT -> 3000;
            case ETHEREAL -> 6000;
            case COSMIC -> 12000;
            case INFINITE -> 25000;
            case OMNIPOTENT -> 50000;
            default -> 3;
        };
    }
    
    /**
     * Gets default weight for a rarity (tendency toward lower values)
     */
    private double getDefaultWeight(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 0.8;
            case UNCOMMON -> 0.75;
            case RARE -> 0.7;
            case EPIC -> 0.65;
            case LEGENDARY -> 0.6;
            case SPECIAL -> 0.55;
            case UNIQUE -> 0.5;
            case MYTHICAL -> 0.45;
            case DIVINE -> 0.4;
            case CELESTIAL -> 0.35;
            case TRANSCENDENT -> 0.3;
            case ETHEREAL -> 0.25;
            case COSMIC -> 0.2;
            case INFINITE -> 0.15;
            case OMNIPOTENT -> 0.1;
            default -> 0.8;
        };
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public double getEvolutionLevelMultiplier() { return evolutionLevelMultiplier; }
    public double getPrestigeLevelMultiplier() { return prestigeLevelMultiplier; }
    public double getStreakMultiplier() { return streakMultiplier; }
    public double getMaxStreakMultiplier() { return maxStreakMultiplier; }
    public double getDoubleCoinChance() { return doubleCoinChance; }
    public double getTripleCoinChance() { return tripleCoinChance; }
    public double getLuckyCoinChance() { return luckyCoinChance; }
    
    /**
     * Inner class representing a coin range with weighted distribution
     */
    public static class CoinRange {
        private final int min;
        private final int max;
        private final double weight; // Weight toward lower values (0.0 = uniform, 1.0 = always min)
        
        public CoinRange(int min, int max, double weight) {
            this.min = min;
            this.max = max;
            this.weight = Math.max(0.0, Math.min(1.0, weight)); // Clamp between 0 and 1
        }
        
        /**
         * Gets a weighted random value from the range
         * Higher weight means more tendency toward lower values
         */
        public long getWeightedRandomValue(Random random) {
            if (min == max) {
                return min;
            }
            
            // Generate a weighted random value
            double randomValue = random.nextDouble();
            
            // Apply weight curve - higher weight curves toward 0
            double weightedValue = Math.pow(randomValue, 1.0 / (1.0 - weight + 0.1));
            
            // Map to range (inverted so lower values are more likely)
            long result = min + Math.round((max - min) * (1.0 - weightedValue));
            
            return Math.max(min, Math.min(max, result));
        }
        
        // Getters
        public int getMin() { return min; }
        public int getMax() { return max; }
        public double getWeight() { return weight; }
        
        @Override
        public String toString() {
            return String.format("CoinRange{min=%d, max=%d, weight=%.2f}", min, max, weight);
        }
    }
}