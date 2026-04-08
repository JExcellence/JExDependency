package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Copper Evolution - First Metal Age
 * Focus on copper materials and early metallurgy
 * Stage 7 of 50 - Tier 2: Primordial
 */
public class CopperEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Copper", 7, 1000)
            .showcase(Material.COPPER_BLOCK)
            .description("The dawn of metallurgy, where copper marks humanity's first steps into the metal age")
            
            // Requirements: Stone tools + cobblestone from Stone evolution
            .requireCurrency(750)
            .requireItem(Material.COBBLESTONE, 128)
            .requireItem(Material.STONE, 64)
            
            // Common blocks - Basic copper
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER_BLOCK,
                Material.COPPER_BLOCK, Material.CUT_COPPER)

            // Uncommon blocks - Copper variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.EXPOSED_COPPER, Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER,
                Material.CUT_COPPER_STAIRS, Material.CUT_COPPER_SLAB)

            // Rare blocks - Waxed copper
            .addBlocks(EEvolutionRarityType.RARE,
                Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER, Material.WAXED_WEATHERED_COPPER,
                Material.WAXED_OXIDIZED_COPPER, Material.WAXED_CUT_COPPER)

            // Epic blocks - Advanced copper
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.LIGHTNING_ROD, Material.SPYGLASS, Material.COPPER_GRATE,
                Material.EXPOSED_COPPER_GRATE, Material.WEATHERED_COPPER_GRATE)

            // Legendary blocks - Ultimate copper
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE)

            // Special blocks - Copper technology
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.COPPER_BULB, Material.EXPOSED_COPPER_BULB)
            
            // Copper-age entities
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.CREEPER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.SPIDER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ENDERMAN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.IRON_GOLEM_SPAWN_EGG)

            // Copper-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.RAW_COPPER, 8)
            .addItem(EEvolutionRarityType.COMMON, Material.COPPER_INGOT, 6)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.COPPER_BLOCK, 3)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.CUT_COPPER, 4)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.HONEYCOMB, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.LIGHTNING_ROD, 1)
            .addItem(EEvolutionRarityType.RARE, Material.SPYGLASS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.BRUSH, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.COPPER_GRATE, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.COPPER_BULB, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.AMETHYST_SHARD, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.WAXED_COPPER_BLOCK, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.WAXED_COPPER_GRATE, 1)

            .build();
    }
}


