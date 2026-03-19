package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Iron Evolution - Iron Age Begins
 * Focus on iron materials and advanced metallurgy
 * Stage 8 of 50 - Tier 2: Primordial
 */
public class IronEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Iron", 8, 1250)
            .showcase(Material.IRON_BLOCK)
            .description("The strength of civilization, where iron tools and weapons forge the path to progress")
            
            // Requirements: Copper ingots from Copper evolution
            .requireCurrency(1000)
            .requireItem(Material.COPPER_INGOT, 32)
            .requireItem(Material.RAW_COPPER, 16)
            
            // Common blocks - Basic iron
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON_BLOCK,
                Material.IRON_BLOCK, Material.IRON_BARS)

            // Uncommon blocks - Iron structures
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.IRON_BARS, Material.LANTERN, Material.SOUL_LANTERN)

            // Rare blocks - Advanced iron
            .addBlocks(EEvolutionRarityType.RARE,
                Material.BLAST_FURNACE, Material.SMITHING_TABLE, Material.GRINDSTONE,
                Material.STONECUTTER, Material.CAULDRON)

            // Epic blocks - Iron machinery
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.HOPPER, Material.RAIL, Material.POWERED_RAIL,
                Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL)

            // Legendary blocks - Advanced iron tech
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.PISTON, Material.STICKY_PISTON, Material.DISPENSER,
                Material.DROPPER, Material.OBSERVER)

            // Special blocks - Ultimate iron power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.IRON_DOOR, Material.IRON_TRAPDOOR, Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
            
            // Iron-age entities
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.CREEPER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SPIDER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VILLAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITCH_SPAWN_EGG)

            // Iron-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.RAW_IRON, 10)
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 8)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_SWORD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_PICKAXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_AXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_SHOVEL, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_HOE, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.IRON_HELMET, 1)
            .addItem(EEvolutionRarityType.RARE, Material.IRON_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.IRON_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.IRON_BOOTS, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.BUCKET, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.SHEARS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.FLINT_AND_STEEL, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.MINECART, 1)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.COMPASS, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CLOCK, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CROSSBOW, 1)

            .build();
    }
}


