package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Terra Evolution - Earth and Basic Terrain
 * Focus on earth materials and basic terrain blocks
 * Stage 2 of 50 - Tier 1: Genesis
 */
public class TerraEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Terra", 2, 200)
            .showcase(Material.DIRT)
            .description("The foundation of earth, where solid ground provides stability for growth")
            
            // Requirements: Basic materials from Genesis
            .requireCurrency(100)
            .requireItem(Material.COBBLESTONE, 32)
            
            // Common blocks - Earth materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT,
                Material.ROOTED_DIRT, Material.PODZOL)

            // Uncommon blocks - Stone variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.STONE, Material.COBBLESTONE, Material.ANDESITE,
                Material.DIORITE, Material.GRANITE)

            // Rare blocks - Clay and sand
            .addBlocks(EEvolutionRarityType.RARE,
                Material.CLAY, Material.SAND, Material.RED_SAND,
                Material.GRAVEL, Material.SANDSTONE)

            // Epic blocks - First ores
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE)

            // Legendary blocks - Very rare
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.IRON_ORE)
            
            // Basic earth entities
            .addEntity(EEvolutionRarityType.RARE, Material.PIG_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SHEEP_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.COW_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.ZOMBIE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.SKELETON_SPAWN_EGG)

            // Earth-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.CLAY_BALL, 3)
            .addItem(EEvolutionRarityType.COMMON, Material.FLINT, 2)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.BRICK, 4)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.COBBLESTONE, 8)
            
            .addItem(EEvolutionRarityType.RARE, Material.STONE_SWORD, 1)
            .addItem(EEvolutionRarityType.RARE, Material.STONE_PICKAXE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.STONE_AXE, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.COAL, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.FURNACE, 1)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.RAW_IRON, 1)

            .build();
    }
}


