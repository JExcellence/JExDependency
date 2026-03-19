package de.jexcellence.oneblock.service;

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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Dynamic Evolution Service
 * 
 * Provides evolution management and content access by delegating to
 * EvolutionContentProvider. This service acts as the main entry point
 * for evolution-related operations.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Getter
public class DynamicEvolutionService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final EvolutionFactory evolutionFactory;
    private final EvolutionContentProvider contentProvider;
    
    public DynamicEvolutionService() {
        this.evolutionFactory = EvolutionFactory.getInstance();
        this.contentProvider = new EvolutionContentProvider();
    }
    
    // ==================== Evolution Access ====================
    
    /**
     * Gets an evolution by name
     * 
     * @param evolutionName the evolution name
     * @return the evolution, or null if not found
     */
    @Nullable
    public OneblockEvolution getEvolution(@NotNull String evolutionName) {
        return evolutionFactory.getCachedEvolution(evolutionName);
    }
    
    /**
     * Gets an evolution by level
     * 
     * @param level the evolution level
     * @return the evolution, or null if not found
     */
    @Nullable
    public OneblockEvolution getEvolutionByLevel(int level) {
        return evolutionFactory.getEvolutionByLevel(level);
    }
    
    /**
     * Checks if an evolution exists
     * 
     * @param evolutionName the evolution name
     * @return true if the evolution exists
     */
    public boolean evolutionExists(@NotNull String evolutionName) {
        return evolutionFactory.isEvolutionRegistered(evolutionName);
    }
    
    /**
     * Gets the starting evolution (level 1)
     * 
     * @return the starting evolution name
     */
    @NotNull
    public String getStartingEvolution() {
        OneblockEvolution first = evolutionFactory.getEvolutionByLevel(1);
        return first != null ? first.getEvolutionName() : "Genesis";
    }
    
    /**
     * Gets the next evolution after the current one
     * 
     * @param currentEvolution current evolution name
     * @param currentLevel current player level (for validation)
     * @return next evolution name, or null if at max
     */
    @Nullable
    public String getNextEvolution(@NotNull String currentEvolution, int currentLevel) {
        OneblockEvolution current = getEvolution(currentEvolution);
        if (current == null) {
            LOGGER.warning("Evolution not found: " + currentEvolution);
            return null;
        }
        
        OneblockEvolution next = contentProvider.getNextEvolution(current);
        if (next == null) {
            return null; // At max evolution
        }
        
        // Check if player level qualifies for next evolution
        if (currentLevel >= next.getLevel()) {
            return next.getEvolutionName();
        }
        
        return null;
    }
    
    /**
     * Gets the showcase material for an evolution
     * 
     * @param evolutionName the evolution name
     * @return the showcase material, or GRASS_BLOCK as default
     */
    @NotNull
    public Material getEvolutionMaterial(@NotNull String evolutionName) {
        OneblockEvolution evolution = getEvolution(evolutionName);
        if (evolution != null && evolution.getShowcase() != null) {
            return evolution.getShowcase();
        }
        return Material.GRASS_BLOCK;
    }
    
    /**
     * Gets all evolution names
     * 
     * @return set of all evolution names
     */
    @NotNull
    public Set<String> getAllEvolutionNames() {
        return evolutionFactory.getRegisteredEvolutionNames();
    }
    
    /**
     * Gets all evolutions sorted by level
     * 
     * @return list of evolutions sorted by level
     */
    @NotNull
    public List<OneblockEvolution> getAllEvolutionsSortedByLevel() {
        return evolutionFactory.getAllEvolutionsSortedByLevel();
    }
    
    /**
     * Gets total evolution count
     * 
     * @return number of registered evolutions
     */
    public int getTotalEvolutionCount() {
        return evolutionFactory.getRegisteredEvolutionNames().size();
    }
    
    // ==================== Content Access ====================
    
    /**
     * Gets a random block from the evolution
     * 
     * @param evolution the evolution
     * @return random block material
     */
    @NotNull
    public Material getRandomBlock(@NotNull OneblockEvolution evolution) {
        return contentProvider.getRandomBlock(evolution);
    }
    
    /**
     * Gets a random block by evolution name
     * 
     * @param evolutionName the evolution name
     * @return random block material, or STONE if evolution not found
     */
    @NotNull
    public Material getRandomBlock(@NotNull String evolutionName) {
        OneblockEvolution evolution = getEvolution(evolutionName);
        if (evolution == null) {
            return Material.STONE;
        }
        return contentProvider.getRandomBlock(evolution);
    }
    
    /**
     * Gets a random item from the evolution
     * 
     * @param evolution the evolution
     * @return random item, or null if no items available
     */
    @Nullable
    public ItemStack getRandomItem(@NotNull OneblockEvolution evolution) {
        return contentProvider.getRandomItem(evolution);
    }
    
    /**
     * Gets a random entity spawn egg from the evolution
     * 
     * @param evolution the evolution
     * @return random spawn egg material, or null if no entities available
     */
    @Nullable
    public Material getRandomEntity(@NotNull OneblockEvolution evolution) {
        return contentProvider.getRandomEntity(evolution);
    }
    
    /**
     * Gets blocks by rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of materials for the rarity
     */
    @NotNull
    public List<Material> getBlocksByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return contentProvider.getBlocksByRarity(evolution, rarity);
    }
    
    /**
     * Gets items by rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of items for the rarity
     */
    @NotNull
    public List<ItemStack> getItemsByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return contentProvider.getItemsByRarity(evolution, rarity);
    }
    
    /**
     * Gets entity spawn eggs by rarity from the evolution
     * 
     * @param evolution the evolution
     * @param rarity the rarity to filter by
     * @return list of spawn egg materials for the rarity
     */
    @NotNull
    public List<Material> getEntitiesByRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        return contentProvider.getEntitiesByRarity(evolution, rarity);
    }
    
    /**
     * Gets available rarities for an evolution
     * 
     * @param evolution the evolution
     * @return set of rarities with content
     */
    @NotNull
    public Set<EEvolutionRarityType> getAvailableRarities(@NotNull OneblockEvolution evolution) {
        return contentProvider.getAvailableRarities(evolution);
    }
    
    // ==================== Chest Generation ====================
    
    /**
     * Gets chest configuration for the evolution
     * 
     * @param evolution the evolution
     * @return chest configuration
     */
    @NotNull
    public EvolutionContentProvider.ChestConfiguration getChestConfiguration(@NotNull OneblockEvolution evolution) {
        return contentProvider.getChestConfiguration(evolution);
    }
    
    /**
     * Checks if a chest should spawn
     * 
     * @param evolution the evolution
     * @return true if a chest should spawn
     */
    public boolean shouldSpawnChest(@NotNull OneblockEvolution evolution) {
        return contentProvider.shouldSpawnChest(evolution);
    }
    
    /**
     * Generates chest contents
     * 
     * @param evolution the evolution
     * @param player optional player for custom items
     * @return list of items for the chest
     */
    @NotNull
    public List<ItemStack> generateChestContents(@NotNull OneblockEvolution evolution, @Nullable Player player) {
        return contentProvider.generateChestContents(evolution, player);
    }
    
    // ==================== Content Summary ====================
    
    /**
     * Gets a content summary for an evolution
     * 
     * @param evolution the evolution
     * @return summary map with content counts
     */
    @NotNull
    public Map<String, Object> getContentSummary(@NotNull OneblockEvolution evolution) {
        return contentProvider.getContentSummary(evolution);
    }
    
    /**
     * Gets evolution content asynchronously
     * 
     * @param evolution the evolution
     * @return future containing content summary
     */
    @NotNull
    public CompletableFuture<Map<String, Object>> getContentSummaryAsync(@NotNull OneblockEvolution evolution) {
        return CompletableFuture.supplyAsync(() -> contentProvider.getContentSummary(evolution));
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Clears content cache
     */
    public void clearContentCache() {
        contentProvider.clearCache();
        LOGGER.info("Evolution content cache cleared");
    }
    
    /**
     * Clears cache for a specific evolution
     * 
     * @param evolutionName the evolution name
     */
    public void clearCacheForEvolution(@NotNull String evolutionName) {
        contentProvider.clearCacheForEvolution(evolutionName);
    }
    
    /**
     * Gets cache statistics
     * 
     * @return map of cache statistics
     */
    @NotNull
    public Map<String, Integer> getContentCacheStats() {
        return contentProvider.getCacheStats();
    }
    
    // ==================== Requirements ====================
    
    /**
     * Checks if a player meets requirements for an evolution
     * 
     * @param evolution the evolution
     * @param player the player
     * @return true if requirements are met
     */
    public boolean areRequirementsMet(@NotNull OneblockEvolution evolution, @NotNull Player player) {
        return evolution.areRequirementsMet(player);
    }
    
    /**
     * Gets requirement progress for a player
     * 
     * @param evolution the evolution
     * @param player the player
     * @return progress value between 0.0 and 1.0
     */
    public double getRequirementProgress(@NotNull OneblockEvolution evolution, @NotNull Player player) {
        return evolution.calculateRequirementProgress(player);
    }
    
    /**
     * Consumes requirements for evolution advancement
     * 
     * @param evolution the evolution
     * @param player the player
     */
    public void consumeRequirements(@NotNull OneblockEvolution evolution, @NotNull Player player) {
        evolution.consumeRequirements(player);
    }
}
