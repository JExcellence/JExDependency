package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Wood Evolution - Forestry and Basic Crafting
 * Focus on wood materials and early crafting systems
 * Stage 10 of 50 - Tier 2: Primordial
 */
public class WoodEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Wood", 10, 2000)
            .showcase(Material.CRAFTING_TABLE)
            .description("The foundation of crafting, where wood provides the tools to shape the world")
            
            // Requirements: Coal + iron from previous evolutions
            .requireCurrency(1500)
            .requireItem(Material.COAL, 64)
            .requireItem(Material.IRON_INGOT, 24)
            
            // Common blocks - Basic wood
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OAK_LOG, Material.OAK_PLANKS, Material.OAK_STAIRS,
                Material.OAK_SLAB, Material.OAK_FENCE)

            // Uncommon blocks - Wood variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
                Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS)

            // Rare blocks - Advanced wood
            .addBlocks(EEvolutionRarityType.RARE,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG,
                Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS)

            // Epic blocks - Crafting stations
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.CRAFTING_TABLE, Material.CHEST, Material.BARREL,
                Material.FLETCHING_TABLE, Material.CARTOGRAPHY_TABLE)

            // Legendary blocks - Advanced crafting
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.LOOM, Material.COMPOSTER, Material.LECTERN,
                Material.BOOKSHELF, Material.ENCHANTING_TABLE)

            // Special blocks - Ultimate wood power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.JUKEBOX, Material.NOTE_BLOCK)
            
            // Forest entities
            .addEntity(EEvolutionRarityType.RARE, Material.PIG_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.COW_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SHEEP_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.CHICKEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.WOLF_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.FOX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.RABBIT_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VILLAGER_SPAWN_EGG)

            // Wood-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.STICK, 8)
            .addItem(EEvolutionRarityType.COMMON, Material.OAK_PLANKS, 16)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WOODEN_SWORD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WOODEN_PICKAXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WOODEN_AXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WOODEN_SHOVEL, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WOODEN_HOE, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.BOW, 1)
            .addItem(EEvolutionRarityType.RARE, Material.ARROW, 16)
            .addItem(EEvolutionRarityType.RARE, Material.OAK_BOAT, 1)
            .addItem(EEvolutionRarityType.RARE, Material.CHEST, 2)
            
            .addItem(EEvolutionRarityType.EPIC, Material.CRAFTING_TABLE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.BOOKSHELF, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.PAPER, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.BOOK, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTING_TABLE, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 3)

            // All wood saplings
            .addItem(EEvolutionRarityType.RARE, Material.OAK_SAPLING, 2)
            .addItem(EEvolutionRarityType.RARE, Material.BIRCH_SAPLING, 2)
            .addItem(EEvolutionRarityType.RARE, Material.SPRUCE_SAPLING, 2)
            .addItem(EEvolutionRarityType.RARE, Material.JUNGLE_SAPLING, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.ACACIA_SAPLING, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.DARK_OAK_SAPLING, 2)

            .build();
    }
}


