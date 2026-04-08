package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionBlock;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionEntity;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionItem;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Dynamic Evolution Content Provider
 * 
 * Provides evolution-based content generation by reading directly from
 * OneblockEvolution instances. All content (blocks, items, entities) is
 * sourced from the evolution definitions, not static configuration.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
public class EvolutionContentProvider {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    // Content caches for performance
    private final Map<String, List<WeightedMaterial>> blockCache = new ConcurrentHashMap<>();
    private final Map<String, List<WeightedItem>> itemCache = new ConcurrentHashMap<>();
    private final Map<String, List<WeightedMaterial>> entityCache = new ConcurrentHashMap<>();
    private final Map<String, ChestConfiguration> chestCache = new ConcurrentHashMap<>();
    
    /**
     * Gets available blocks for the given evolution with rarity weights
     * 
     * @param evolution the evolution to get blocks for
     * @return list of weighted materials for this evolution
     */
    @NotNull
    public List<WeightedMaterial> getAvailableBlocks(@NotNull OneblockEvolution evolution) {
        String cacheKey = evolution.getEvolutionName() + "_blocks";
        
        return blockCache.computeIfAbsent(cacheKey, k -> {
            List<WeightedMaterial> weightedBlocks = new ArrayList<>();
            
            for (EvolutionBlock block : evolution.getBlocks()) {
                if (!block.isValid()) continue;
                
                double weight = getRarityWeight(block.getRarity());
                for (Material material : block.getMaterials()) {
                    weightedBlocks.add(new WeightedMaterial(material, block.getRarity(), weight));
                }
            }
            
            LOGGER.fine("Loaded " + weightedBlocks.size() + " blocks for evolution: " + evolution.getEvolutionName());
            return weightedBlocks;
        });
    }
    
    /**
     * Gets available items for the given evolution with rarity weights
     * 
     * @param evolution the evolution to get items for
     * @return list of weighted items for this evolution
     */
    @NotNull
    public List<WeightedItem> getAvailableItems(@NotNull OneblockEvolution evolution) {
        String cacheKey = evolution.getEvolutionName() + "_items";
        
        return itemCache.computeIfAbsent(cacheKey, k -> {
            List<WeightedItem> weightedItems = new ArrayList<>();
            
            for (EvolutionItem item : evolution.getItems()) {
                if (!item.isValid()) continue;
                
                double weight = getRarityWeight(item.getRarity());
                for (ItemStack itemStack : item.getItemStacks()) {
                    weightedItems.add(new WeightedItem(itemStack.clone(), item.getRarity(), weight));
                }
            }
            
            LOGGER.fine("Loaded " + weightedItems.size() + " items for evolution: " + evolution.getEvolutionName());
            return weightedItems;
        });
    }
    
    /**
     * Gets available entity spawn eggs for the given evolution with rarity weights
     * 
     * @param evolution the evolution to get entities for
     * @return list of weighted spawn egg materials for this evolution
     */
    @NotNull
    public List<WeightedMaterial> getAvailableEntities(@NotNull OneblockEvolution evolution) {
        String cacheKey = evolution.getEvolutionName() + "_entities";
        
        return entityCache.computeIfAbsent(cacheKey, k -> {
            List<WeightedMaterial> weightedEntities = new ArrayList<>();
            
            for (EvolutionEntity entity : evolution.getEntities()) {
                if (!entity.isValid()) continue;
                
                double weight = getRarityWeight(entity.getRarity());
                for (Material spawnEgg : entity.getSpawnEggs()) {
                    weightedEntities.add(new WeightedMaterial(spawnEgg, entity.getRarity(), weight));
                }
            }
            
            LOGGER.fine("Loaded " + weightedEntities.size() + " entities for evolution: " + evolution.getEvolutionName());
            return weightedEntities;
        });
    }
    
    /**
     * Gets a random block from the evolution based on rarity weights
     * 
     * @param evolution the evolution to get a block from
     * @return random material, or STONE as fallback
     */
    @NotNull
    public Material getRandomBlock(@NotNull OneblockEvolution evolution) {
        List<WeightedMaterial> blocks = getAvailableBlocks(evolution);
        if (blocks.isEmpty()) {
            return Material.STONE;
        }
        return selectWeightedMaterial(blocks);
    }
    
    /**
     * Gets a random item from the evolution based on rarity weights
     * 
     * @param evolution the evolution to get an item from
     * @return random item, or null if no items available
     */
    @Nullable
    public ItemStack getRandomItem(@NotNull OneblockEvolution evolution) {
        List<WeightedItem> items = getAvailableItems(evolution);
        if (items.isEmpty()) {
            return null;
        }
        return selectWeightedItem(items).clone();
    }
    
    /**
     * Gets a random entity spawn egg from the evolution based on rarity weights
     * 
     * @param evolution the evolution to get an entity from
     * @return random spawn egg material, or null if no entities available
     */
    @Nullable
    public Material getRandomEntity(@NotNull OneblockEvolution evolution) {
        List<WeightedMaterial> entities = getAvailableEntities(evolution);
        if (entities.isEmpty()) {
            return null;
        }
        return selectWeightedMaterial(entities);
    }
    
    /**
     * Gets chest configuration for the given evolution
     * 
     * @param evolution the evolution to get chest config for
     * @return chest configuration for this evolution
     */
    @NotNull
    public ChestConfiguration getChestConfiguration(@NotNull OneblockEvolution evolution) {
        String cacheKey = evolution.getEvolutionName() + "_chest";
        
        return chestCache.computeIfAbsent(cacheKey, k -> {
            ChestConfiguration config = new ChestConfiguration();
            
            // Calculate spawn chance based on evolution level (higher level = higher chance)
            double baseChance = 0.02; // 2% base
            double levelBonus = evolution.getLevel() * 0.002; // +0.2% per level
            config.setSpawnChance(Math.min(0.15, baseChance + levelBonus)); // Cap at 15%
            
            // Items scale with level
            config.setMinItems(Math.max(1, evolution.getLevel() / 10));
            config.setMaxItems(Math.min(9, 3 + evolution.getLevel() / 10));
            
            // Use evolution's items for chest contents
            config.setAvailableItems(getAvailableItems(evolution));
            
            // Calculate rarity weights based on evolution level
            config.setRarityWeights(calculateRarityWeights(evolution));
            
            LOGGER.fine("Generated chest configuration for evolution: " + evolution.getEvolutionName());
            return config;
        });
    }
    
    /**
     * Gets blocks for a specific rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of materials for the specified rarity
     */
    @NotNull
    public List<Material> getBlocksByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return evolution.getBlocksByRarity(rarity);
    }
    
    /**
     * Gets items for a specific rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of items for the specified rarity
     */
    @NotNull
    public List<ItemStack> getItemsByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return evolution.getItemsByRarity(rarity);
    }
    
    /**
     * Gets entity spawn eggs for a specific rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of spawn egg materials for the specified rarity
     */
    @NotNull
    public List<Material> getEntitiesByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return evolution.getEntitiesByRarity(rarity);
    }
    
    /**
     * Gets all available rarities that have content in this evolution
     * 
     * @param evolution the evolution to check
     * @return set of rarities with content
     */
    @NotNull
    public Set<EEvolutionRarityType> getAvailableRarities(@NotNull OneblockEvolution evolution) {
        Set<EEvolutionRarityType> rarities = new HashSet<>();
        
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            if (evolution.hasContentForRarity(rarity)) {
                rarities.add(rarity);
            }
        }
        
        return rarities;
    }
    
    /**
     * Gets evolution by name from the factory
     * 
     * @param evolutionName the name of the evolution
     * @return the evolution, or null if not found
     */
    @Nullable
    public OneblockEvolution getEvolution(@NotNull String evolutionName) {
        return EvolutionFactory.getInstance().getCachedEvolution(evolutionName);
    }
    
    /**
     * Gets evolution by level from the factory
     * 
     * @param level the level of the evolution
     * @return the evolution, or null if not found
     */
    @Nullable
    public OneblockEvolution getEvolutionByLevel(int level) {
        return EvolutionFactory.getInstance().getEvolutionByLevel(level);
    }
    
    /**
     * Gets all evolutions sorted by level
     * 
     * @return list of evolutions sorted by level
     */
    @NotNull
    public List<OneblockEvolution> getAllEvolutionsSorted() {
        return EvolutionFactory.getInstance().getAllEvolutionsSortedByLevel();
    }
    
    /**
     * Gets the next evolution after the given one
     * 
     * @param currentEvolution the current evolution
     * @return the next evolution, or null if at max level
     */
    @Nullable
    public OneblockEvolution getNextEvolution(@NotNull OneblockEvolution currentEvolution) {
        return getEvolutionByLevel(currentEvolution.getLevel() + 1);
    }
    
    /**
     * Gets the previous evolution before the given one
     * 
     * @param currentEvolution the current evolution
     * @return the previous evolution, or null if at level 1
     */
    @Nullable
    public OneblockEvolution getPreviousEvolution(@NotNull OneblockEvolution currentEvolution) {
        if (currentEvolution.getLevel() <= 1) {
            return null;
        }
        return getEvolutionByLevel(currentEvolution.getLevel() - 1);
    }
    
    /**
     * Generates chest contents for the evolution
     * 
     * @param evolution the evolution
     * @param player optional player for custom items
     * @return list of items to put in chest
     */
    @NotNull
    public List<ItemStack> generateChestContents(@NotNull OneblockEvolution evolution, @Nullable Player player) {
        ChestConfiguration config = getChestConfiguration(evolution);
        List<ItemStack> contents = new ArrayList<>();
        
        int itemCount = ThreadLocalRandom.current().nextInt(config.getMinItems(), config.getMaxItems() + 1);
        List<WeightedItem> availableItems = config.getAvailableItems();
        
        if (availableItems.isEmpty()) {
            return contents;
        }
        
        for (int i = 0; i < itemCount; i++) {
            ItemStack item = selectWeightedItem(availableItems);
            if (item != null) {
                contents.add(item.clone());
            }
        }
        
        return contents;
    }
    
    /**
     * Checks if a chest should spawn based on evolution configuration
     * 
     * @param evolution the evolution
     * @return true if a chest should spawn
     */
    public boolean shouldSpawnChest(@NotNull OneblockEvolution evolution) {
        ChestConfiguration config = getChestConfiguration(evolution);
        return ThreadLocalRandom.current().nextDouble() < config.getSpawnChance();
    }
    
    /**
     * Gets the weight for a rarity type
     */
    private double getRarityWeight(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> 100.0;
            case UNCOMMON -> 50.0;
            case RARE -> 25.0;
            case EPIC -> 10.0;
            case LEGENDARY -> 5.0;
            case SPECIAL -> 3.0;
            case UNIQUE -> 2.0;
            case MYTHICAL -> 1.5;
            case DIVINE -> 1.0;
            case CELESTIAL -> 0.75;
            case TRANSCENDENT -> 0.5;
            case ETHEREAL -> 0.35;
            case COSMIC -> 0.25;
            case INFINITE -> 0.15;
            case OMNIPOTENT -> 0.1;
            case RESERVED -> 0.01;
        };
    }
    
    /**
     * Selects a material based on weights
     */
    @NotNull
    private Material selectWeightedMaterial(@NotNull List<WeightedMaterial> materials) {
        double totalWeight = materials.stream().mapToDouble(WeightedMaterial::weight).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        
        double cumulative = 0;
        for (WeightedMaterial wm : materials) {
            cumulative += wm.weight();
            if (random <= cumulative) {
                return wm.material();
            }
        }
        
        return materials.get(materials.size() - 1).material();
    }
    
    /**
     * Selects an item based on weights
     */
    @NotNull
    private ItemStack selectWeightedItem(@NotNull List<WeightedItem> items) {
        double totalWeight = items.stream().mapToDouble(WeightedItem::weight).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        
        double cumulative = 0;
        for (WeightedItem wi : items) {
            cumulative += wi.weight();
            if (random <= cumulative) {
                return wi.item();
            }
        }
        
        return items.get(items.size() - 1).item();
    }
    
    /**
     * Calculates rarity weights based on evolution level
     */
    @NotNull
    private Map<EEvolutionRarityType, Double> calculateRarityWeights(@NotNull OneblockEvolution evolution) {
        Map<EEvolutionRarityType, Double> weights = new HashMap<>();
        double levelProgress = evolution.getLevel() / 50.0; // Assuming 50 max levels
        
        weights.put(EEvolutionRarityType.COMMON, 70.0 - (levelProgress * 30));
        weights.put(EEvolutionRarityType.UNCOMMON, 20.0);
        weights.put(EEvolutionRarityType.RARE, 8.0 + (levelProgress * 10));
        weights.put(EEvolutionRarityType.EPIC, 2.0 + (levelProgress * 8));
        weights.put(EEvolutionRarityType.LEGENDARY, levelProgress * 5);
        weights.put(EEvolutionRarityType.SPECIAL, levelProgress * 4);
        weights.put(EEvolutionRarityType.UNIQUE, levelProgress * 3);
        weights.put(EEvolutionRarityType.MYTHICAL, Math.max(0, levelProgress - 0.3) * 4);
        weights.put(EEvolutionRarityType.DIVINE, Math.max(0, levelProgress - 0.5) * 3);
        weights.put(EEvolutionRarityType.CELESTIAL, Math.max(0, levelProgress - 0.6) * 2);
        weights.put(EEvolutionRarityType.TRANSCENDENT, Math.max(0, levelProgress - 0.7) * 1.5);
        weights.put(EEvolutionRarityType.ETHEREAL, Math.max(0, levelProgress - 0.8) * 1);
        weights.put(EEvolutionRarityType.COSMIC, Math.max(0, levelProgress - 0.85) * 0.75);
        weights.put(EEvolutionRarityType.INFINITE, Math.max(0, levelProgress - 0.9) * 0.5);
        weights.put(EEvolutionRarityType.OMNIPOTENT, Math.max(0, levelProgress - 0.95) * 0.25);
        
        return weights;
    }
    
    /**
     * Clears all content caches
     */
    public void clearCache() {
        blockCache.clear();
        itemCache.clear();
        entityCache.clear();
        chestCache.clear();
        LOGGER.info("Evolution content cache cleared");
    }
    
    /**
     * Clears cache for a specific evolution
     * 
     * @param evolutionName the evolution name to clear cache for
     */
    public void clearCacheForEvolution(@NotNull String evolutionName) {
        blockCache.remove(evolutionName + "_blocks");
        itemCache.remove(evolutionName + "_items");
        entityCache.remove(evolutionName + "_entities");
        chestCache.remove(evolutionName + "_chest");
    }
    
    /**
     * Gets cache statistics
     */
    @NotNull
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("blocks", blockCache.size());
        stats.put("items", itemCache.size());
        stats.put("entities", entityCache.size());
        stats.put("chests", chestCache.size());
        return stats;
    }
    
    /**
     * Gets content summary for an evolution
     * 
     * @param evolution the evolution
     * @return summary map with content counts
     */
    @NotNull
    public Map<String, Object> getContentSummary(@NotNull OneblockEvolution evolution) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("evolutionName", evolution.getEvolutionName());
        summary.put("level", evolution.getLevel());
        summary.put("experienceToPass", evolution.getExperienceToPass());
        summary.put("description", evolution.getDescription());
        summary.put("showcase", evolution.getShowcase());
        summary.put("isReady", evolution.isReady());
        summary.put("hasRequirements", evolution.hasRequirements());
        
        // Count content by rarity
        Map<String, Integer> blockCounts = new HashMap<>();
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, Integer> entityCounts = new HashMap<>();
        
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            int blockCount = evolution.getBlocksByRarity(rarity).size();
            int itemCount = evolution.getItemsByRarity(rarity).size();
            int entityCount = evolution.getEntitiesByRarity(rarity).size();
            
            if (blockCount > 0) blockCounts.put(rarity.name(), blockCount);
            if (itemCount > 0) itemCounts.put(rarity.name(), itemCount);
            if (entityCount > 0) entityCounts.put(rarity.name(), entityCount);
        }
        
        summary.put("blocksByRarity", blockCounts);
        summary.put("itemsByRarity", itemCounts);
        summary.put("entitiesByRarity", entityCounts);
        
        summary.put("totalBlocks", blockCounts.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalItems", itemCounts.values().stream().mapToInt(Integer::intValue).sum());
        summary.put("totalEntities", entityCounts.values().stream().mapToInt(Integer::intValue).sum());
        
        return summary;
    }
    
    // ==================== Record Classes ====================
    
    /**
     * Weighted material for random selection
     */
    public record WeightedMaterial(Material material, EEvolutionRarityType rarity, double weight) {}
    
    /**
     * Weighted item for random selection
     */
    public record WeightedItem(ItemStack item, EEvolutionRarityType rarity, double weight) {}
    
    /**
     * Chest configuration for evolution
     */
    @Getter
    public static class ChestConfiguration {
        private double spawnChance;
        private int minItems;
        private int maxItems;
        private List<WeightedItem> availableItems = new ArrayList<>();
        private Map<EEvolutionRarityType, Double> rarityWeights = new HashMap<>();
        
        public void setSpawnChance(double spawnChance) { this.spawnChance = spawnChance; }
        public void setMinItems(int minItems) { this.minItems = minItems; }
        public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
        public void setAvailableItems(List<WeightedItem> availableItems) { this.availableItems = availableItems; }
        public void setRarityWeights(Map<EEvolutionRarityType, Double> rarityWeights) { this.rarityWeights = rarityWeights; }
    }
}
