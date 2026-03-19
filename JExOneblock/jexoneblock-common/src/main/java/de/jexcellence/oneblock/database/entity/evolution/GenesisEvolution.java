package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Genesis Evolution - The Very Beginning
 * The absolute starting point with only the most basic materials
 * Stage 1 of 50 - Tier 1: Genesis
 */
public class GenesisEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Genesis", 1, 100)
            .showcase(Material.GRASS_BLOCK)
            .description("The very beginning of existence, where all journeys start with a single block")
            .addBlocks(EEvolutionRarityType.COMMON, Material.GRASS_BLOCK, Material.DIRT, Material.STONE, Material.COBBLESTONE)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.SAND, Material.GRAVEL, Material.OAK_LOG, Material.OAK_PLANKS)
            .addBlocks(EEvolutionRarityType.RARE, Material.OAK_LEAVES, Material.COAL_ORE, Material.IRON_ORE)
            .addBlocks(EEvolutionRarityType.EPIC, Material.GOLD_ORE, Material.DIAMOND_ORE)

            .addEntity(EEvolutionRarityType.UNCOMMON, Material.PIG_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNCOMMON, Material.CHICKEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.COW_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SHEEP_SPAWN_EGG)

            .addItem(EEvolutionRarityType.COMMON, Material.STICK, 4)
            .addItem(EEvolutionRarityType.COMMON, Material.APPLE, 2)
            .addItem(EEvolutionRarityType.COMMON, Material.BREAD, 1)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.OAK_SAPLING, 2)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.COOKED_BEEF, 3)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.LEATHER, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.WOODEN_SWORD, 1)
            .addItem(EEvolutionRarityType.RARE, Material.WOODEN_PICKAXE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.WOODEN_AXE, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.COAL, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.IRON_INGOT, 2)

            .build();
    }
}


