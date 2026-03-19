package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Bronze Evolution - Ancient Metallurgy
 * Focus on advanced metalworking and ancient civilizations
 * Stage 11 of 50 - Tier 3: Ancient
 */
public class BronzeEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Bronze", 11, 3000)
            .showcase(Material.COPPER_BLOCK)
            .description("The dawn of metallurgy, where copper and tin forge the tools of civilization")
            
            // Requirements: Wood planks + iron from previous evolutions
            .requireCurrency(2500)
            .requireItem(Material.OAK_PLANKS, 128)
            .requireItem(Material.IRON_INGOT, 32)
            .requireItem(Material.COPPER_INGOT, 48)
            
            // Common blocks - Basic metals
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER_BLOCK,
                Material.COPPER_BLOCK, Material.CUT_COPPER)

            // Uncommon blocks - Copper variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.EXPOSED_COPPER, Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER,
                Material.CUT_COPPER_STAIRS, Material.CUT_COPPER_SLAB)

            // Rare blocks - Advanced copper
            .addBlocks(EEvolutionRarityType.RARE,
                Material.WAXED_COPPER_BLOCK, Material.WAXED_CUT_COPPER, Material.COPPER_GRATE,
                Material.COPPER_BULB, Material.COPPER_DOOR)

            // Epic blocks - Ancient tools
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.CAULDRON, Material.HOPPER)

            // Legendary blocks - Masterwork items
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BELL, Material.LIGHTNING_ROD, Material.SPYGLASS,
                Material.COMPASS, Material.CLOCK)

            // Special blocks - Ancient wonders
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.CONDUIT, Material.HEART_OF_THE_SEA)
            
            // Ancient entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.IRON_GOLEM_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.WANDERING_TRADER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.LLAMA_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GUARDIAN_SPAWN_EGG)

            // Bronze age items
            .addItem(EEvolutionRarityType.COMMON, Material.RAW_COPPER, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.COPPER_INGOT, 12)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.BUCKET, 2)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.SHEARS, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.FLINT_AND_STEEL, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.COMPASS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.CLOCK, 1)
            .addItem(EEvolutionRarityType.RARE, Material.SPYGLASS, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.ANVIL, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.CAULDRON, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.HOPPER, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BELL, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.LIGHTNING_ROD, 2)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.CONDUIT, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.HEART_OF_THE_SEA, 1)

            .build();
    }
}


