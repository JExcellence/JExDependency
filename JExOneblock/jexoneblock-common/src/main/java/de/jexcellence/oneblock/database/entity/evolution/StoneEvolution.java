package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Stone Evolution - Stone Variants and Basic Mining
 * Focus on stone materials and early mining concepts
 * Stage 6 of 50 - Tier 2: Primordial
 */
public class StoneEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Stone", 6, 750)
            .showcase(Material.STONE)
            .description("The foundation of civilization, where stone provides strength and permanence")
            
            // Requirements for this evolution
            .requireCurrency(500)
            .requireItem(Material.COBBLESTONE, 64)
            
            // Common blocks - Basic stone
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.STONE, Material.COBBLESTONE, Material.SMOOTH_STONE,
                Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS)

            // Uncommon blocks - Stone variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.POLISHED_ANDESITE, Material.POLISHED_DIORITE, Material.POLISHED_GRANITE)

            // Rare blocks - Advanced stone
            .addBlocks(EEvolutionRarityType.RARE,
                Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.POLISHED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS)

            // Epic blocks - Special stone
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.CALCITE, Material.TUFF, Material.DRIPSTONE_BLOCK,
                Material.POINTED_DRIPSTONE, Material.AMETHYST_BLOCK)

            // Legendary blocks - Rare stone materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BUDDING_AMETHYST, Material.AMETHYST_CLUSTER)

            // Special blocks - Ultimate stone power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.REINFORCED_DEEPSLATE, Material.BEDROCK)
            
            // Stone-related entities
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SPIDER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.SILVERFISH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.CAVE_SPIDER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.IRON_GOLEM_SPAWN_EGG)

            // Stone-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.COBBLESTONE, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.STONE, 12)
            .addItem(EEvolutionRarityType.COMMON, Material.FLINT, 4)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.STONE_SWORD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.STONE_PICKAXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.STONE_AXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.STONE_SHOVEL, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.STONE_BRICKS, 8)
            .addItem(EEvolutionRarityType.RARE, Material.DEEPSLATE, 6)
            .addItem(EEvolutionRarityType.RARE, Material.GRINDSTONE, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.AMETHYST_SHARD, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.SPYGLASS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.TINTED_GLASS, 4)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.STONECUTTER, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ANVIL, 1)

            .build();
    }
}


