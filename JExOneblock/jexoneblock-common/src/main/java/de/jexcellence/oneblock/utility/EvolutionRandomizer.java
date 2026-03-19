package de.jexcellence.oneblock.utility;

import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class EvolutionRandomizer {
    
    private final WeightedRandomizer<EEvolutionRarityType> rarityRandomizer = new WeightedRandomizer<>();
    private final WeightedRandomizer<Boolean> entitySpawnRandomizer = new WeightedRandomizer<>();
    private final WeightedRandomizer<Boolean> itemSpawnRandomizer = new WeightedRandomizer<>();
    private final WeightedRandomizer<Material> startingBlockRandomizer = new WeightedRandomizer<>();
    
    public void initialize(
            @NotNull Map<String, Double> rarityWeights,
            @NotNull Map<String, Double> entitySpawnWeights,
            @NotNull Map<String, Double> itemSpawnWeights,
            @NotNull Map<String, Double> startingBlockWeights
    ) {
        clearAll();
        
        rarityWeights.forEach((rarityName, weight) -> {
            try {
                var rarity = EEvolutionRarityType.valueOf(rarityName.toUpperCase());
                rarityRandomizer.addEntry(rarity, weight);
            } catch (IllegalArgumentException e) {
            }
        });
        
        entitySpawnWeights.forEach((spawnFlag, weight) -> {
            entitySpawnRandomizer.addEntry(Boolean.parseBoolean(spawnFlag), weight);
        });
        
        itemSpawnWeights.forEach((spawnFlag, weight) -> {
            itemSpawnRandomizer.addEntry(Boolean.parseBoolean(spawnFlag), weight);
        });
        
        startingBlockWeights.forEach((materialName, weight) -> {
            try {
                var material = Material.valueOf(materialName.toUpperCase());
                startingBlockRandomizer.addEntry(material, weight);
            } catch (IllegalArgumentException e) {
            }
        });
    }
    
    /**
     * Gets a random evolution rarity type.
     * 
     * @return random rarity or COMMON if none configured
     */
    public @NotNull EEvolutionRarityType getRandomRarity() {
        EEvolutionRarityType rarity = rarityRandomizer.getRandom();
        return rarity != null ? rarity : EEvolutionRarityType.COMMON;
    }
    
    /**
     * Gets a random rarity with level-based bias towards higher rarities.
     * 
     * @param playerLevel the player's level
     * @return rarity with level bias applied
     */
    public @NotNull EEvolutionRarityType getRandomRarityWithLevelBias(int playerLevel) {
        EEvolutionRarityType baseRarity = getRandomRarity();
        
        // Higher levels have chance for better rarity
        if (playerLevel > 10) {
            double upgradeChance = Math.min((playerLevel - 10) * 0.01, 0.05); // Max 25% at level 35+
            
            if (Math.random() < upgradeChance && !baseRarity.isMaxRarity()) {
                return baseRarity.getNext();
            }
        }
        
        return baseRarity;
    }
    
    /**
     * Determines if an entity should spawn.
     * 
     * @return true if entity should spawn
     */
    public boolean shouldSpawnEntity() {
        Boolean result = entitySpawnRandomizer.getRandom();
        return result != null ? result : false;
    }
    
    /**
     * Determines if items should spawn.
     * 
     * @return true if items should spawn
     */
    public boolean shouldSpawnItems() {
        Boolean result = itemSpawnRandomizer.getRandom();
        return result != null ? result : false;
    }
    
    /**
     * Gets a random starting block material.
     * 
     * @return random starting block or null if none configured
     */
    public @Nullable Material getRandomStartingBlock() {
        return startingBlockRandomizer.getRandom();
    }
    
    /**
     * Gets a random starting block with fallback to default.
     * 
     * @param defaultMaterial fallback material if none configured
     * @return random starting block or default
     */
    public @NotNull Material getRandomStartingBlockOrDefault(@NotNull Material defaultMaterial) {
        Material material = getRandomStartingBlock();
        return material != null ? material : defaultMaterial;
    }
    
    /**
     * Checks if the randomizer is properly initialized.
     * 
     * @return true if all randomizers have entries
     */
    public boolean isInitialized() {
        return !rarityRandomizer.isEmpty() && 
               !entitySpawnRandomizer.isEmpty() && 
               !itemSpawnRandomizer.isEmpty() && 
               !startingBlockRandomizer.isEmpty();
    }
    
    /**
     * Gets statistics about the current configuration.
     * 
     * @return configuration statistics
     */
    public @NotNull RandomizerStats getStats() {
        return new RandomizerStats(
            rarityRandomizer.size(),
            entitySpawnRandomizer.size(),
            itemSpawnRandomizer.size(),
            startingBlockRandomizer.size()
        );
    }
    
    /**
     * Clears all randomizer entries.
     */
    public void clearAll() {
        rarityRandomizer.clear();
        entitySpawnRandomizer.clear();
        itemSpawnRandomizer.clear();
        startingBlockRandomizer.clear();
    }
    
    /**
     * Statistics record for randomizer configuration.
     */
    public record RandomizerStats(
        int rarityEntries,
        int entitySpawnEntries,
        int itemSpawnEntries,
        int startingBlockEntries
    ) {
        public boolean isFullyConfigured() {
            return rarityEntries > 0 && entitySpawnEntries > 0 && 
                   itemSpawnEntries > 0 && startingBlockEntries > 0;
        }
    }
}