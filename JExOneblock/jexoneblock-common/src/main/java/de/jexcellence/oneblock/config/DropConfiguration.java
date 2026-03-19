package de.jexcellence.oneblock.config;

import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Configuration class for the item drop system
 * Handles dynamic drop amounts, multipliers, and rarity-based ranges
 * 
 * @author JExcellence
 * @since 1.0.12
 */
public class DropConfiguration {
    
    private final Random random = new Random();
    
    // Drop amount ranges by rarity
    private final Map<EEvolutionRarityType, DropRange> dropRanges = new EnumMap<>(EEvolutionRarityType.class);
    
    // Multipliers
    private double evolutionLevelMultiplier = 0.02;
    private double prestigeLevelMultiplier = 0.05;
    private double maxMultiplier = 2.0;
    
    // System enabled
    private boolean enabled = true;
    
    /**
     * Loads drop configuration from the provided config
     */
    public void loadFromConfig(@NotNull FileConfiguration config) {
        ConfigurationSection dropSection = config.getConfigurationSection("drops");
        if (dropSection == null) {
            loadDefaults();
            return;
        }
        
        enabled = dropSection.getBoolean("enabled", true);
        
        // Load drop ranges
        ConfigurationSection amountsSection = dropSection.getConfigurationSection("amounts");
        if (amountsSection != null) {
            for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
                String rarityKey = rarity.name().toLowerCase();
                ConfigurationSection raritySection = amountsSection.getConfigurationSection(rarityKey);
                
                if (raritySection != null) {
                    int min = raritySection.getInt("min", getDefaultMin(rarity));
                    int max = raritySection.getInt("max", getDefaultMax(rarity));
                    
                    dropRanges.put(rarity, new DropRange(min, max));
                } else {
                    // Use defaults
                    dropRanges.put(rarity, new DropRange(
                        getDefaultMin(rarity), 
                        getDefaultMax(rarity)
                    ));
                }
            }
        } else {
            loadDefaultRanges();
        }
        
        // Load multipliers
        ConfigurationSection multipliersSection = dropSection.getConfigurationSection("multipliers");
        if (multipliersSection != null) {
            evolutionLevelMultiplier = multipliersSection.getDouble("evolutionLevelMultiplier", 0.02);
            prestigeLevelMultiplier = multipliersSection.getDouble("prestigeLevelMultiplier", 0.05);
            maxMultiplier = multipliersSection.getDouble("maxMultiplier", 2.0);
        }
    }
    
    /**
     * Calculates drop amount for a given rarity with multipliers
     */
    public int calculateDropAmount(@NotNull EEvolutionRarityType rarity, int evolutionLevel, int prestigeLevel) {
        if (!enabled) {
            return 1; // Default fallback
        }
        
        DropRange range = dropRanges.get(rarity);
        if (range == null) {
            return 1; // Fallback
        }
        
        // Get base amount from range
        int baseAmount = range.getRandomAmount(random);
        
        // Apply multipliers
        double totalMultiplier = 1.0;
        
        // Evolution level multiplier
        totalMultiplier += evolutionLevel * evolutionLevelMultiplier;
        
        // Prestige level multiplier
        totalMultiplier += prestigeLevel * prestigeLevelMultiplier;
        
        // Cap the multiplier
        totalMultiplier = Math.min(totalMultiplier, maxMultiplier);
        
        // Calculate final amount
        int finalAmount = (int) Math.round(baseAmount * totalMultiplier);
        
        return Math.max(1, finalAmount); // Minimum 1 item
    }
    
    /**
     * Loads default configuration values
     */
    private void loadDefaults() {
        enabled = true;
        loadDefaultRanges();
    }
    
    /**
     * Loads default drop ranges
     */
    private void loadDefaultRanges() {
        dropRanges.clear();
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            dropRanges.put(rarity, new DropRange(
                getDefaultMin(rarity), 
                getDefaultMax(rarity)
            ));
        }
    }
    
    /**
     * Gets default minimum drop amount for a rarity
     */
    private int getDefaultMin(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 2;
            case LEGENDARY -> 3;
            case SPECIAL -> 3;
            case UNIQUE -> 4;
            case MYTHICAL -> 4;
            case DIVINE -> 5;
            case CELESTIAL -> 6;
            case TRANSCENDENT -> 7;
            case ETHEREAL -> 8;
            case COSMIC -> 10;
            case INFINITE -> 12;
            case OMNIPOTENT -> 15;
            default -> 1;
        };
    }
    
    /**
     * Gets default maximum drop amount for a rarity
     */
    private int getDefaultMax(EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 3;
            case UNCOMMON -> 4;
            case RARE -> 5;
            case EPIC -> 6;
            case LEGENDARY -> 7;
            case SPECIAL -> 8;
            case UNIQUE -> 9;
            case MYTHICAL -> 10;
            case DIVINE -> 12;
            case CELESTIAL -> 15;
            case TRANSCENDENT -> 20;
            case ETHEREAL -> 25;
            case COSMIC -> 30;
            case INFINITE -> 35;
            case OMNIPOTENT -> 40;
            default -> 3;
        };
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public double getEvolutionLevelMultiplier() { return evolutionLevelMultiplier; }
    public double getPrestigeLevelMultiplier() { return prestigeLevelMultiplier; }
    public double getMaxMultiplier() { return maxMultiplier; }
    
    /**
     * Gets the drop range for a specific rarity
     */
    public DropRange getDropRange(EEvolutionRarityType rarity) {
        return dropRanges.get(rarity);
    }
    
    /**
     * Inner class representing a drop amount range
     */
    public static class DropRange {
        private final int min;
        private final int max;
        
        public DropRange(int min, int max) {
            this.min = Math.max(1, min); // Minimum 1
            this.max = Math.max(this.min, max); // Max must be >= min
        }
        
        /**
         * Gets a random amount within the range
         */
        public int getRandomAmount(Random random) {
            if (min == max) {
                return min;
            }
            return random.nextInt(min, max + 1); // +1 because nextInt is exclusive of upper bound
        }
        
        // Getters
        public int getMin() { return min; }
        public int getMax() { return max; }
        
        @Override
        public String toString() {
            return String.format("DropRange{min=%d, max=%d}", min, max);
        }
    }
}