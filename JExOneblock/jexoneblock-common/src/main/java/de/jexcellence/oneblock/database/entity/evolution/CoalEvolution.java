package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Coal Evolution - Energy and Fuel
 * Focus on coal materials and early energy concepts
 * Stage 9 of 50 - Tier 2: Primordial
 */
public class CoalEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Coal", 9, 1500)
            .showcase(Material.COAL_BLOCK)
            .description("The power of energy, where coal fuels the fires of progress and industry")
            
            // Requirements: Iron tools + copper ingots from previous evolutions
            .requireCurrency(1200)
            .requireItem(Material.IRON_INGOT, 16)
            .requireItem(Material.COPPER_INGOT, 24)
            
            // Common blocks - Coal materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COAL_BLOCK,
                Material.CHARCOAL, Material.TORCH)

            // Uncommon blocks - Fuel-related
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
                Material.CAMPFIRE, Material.SOUL_CAMPFIRE)

            // Rare blocks - Advanced fuel
            .addBlocks(EEvolutionRarityType.RARE,
                Material.LANTERN, Material.SOUL_LANTERN, Material.REDSTONE_LAMP,
                Material.GLOWSTONE, Material.SHROOMLIGHT)

            // Epic blocks - Energy systems
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE_BLOCK,
                Material.REDSTONE_TORCH, Material.REPEATER)

            // Legendary blocks - Power systems
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.COMPARATOR, Material.OBSERVER, Material.DAYLIGHT_DETECTOR,
                Material.LIGHTNING_ROD, Material.TARGET)

            // Special blocks - Ultimate energy
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.TNT, Material.RESPAWN_ANCHOR)
            
            // Energy-related entities
            .addEntity(EEvolutionRarityType.RARE, Material.CREEPER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SKELETON_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.GHAST_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SKELETON_SPAWN_EGG)

            // Coal-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.COAL, 12)
            .addItem(EEvolutionRarityType.COMMON, Material.CHARCOAL, 8)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.TORCH, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.FURNACE, 2)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.CAMPFIRE, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.LANTERN, 3)
            .addItem(EEvolutionRarityType.RARE, Material.REDSTONE, 8)
            .addItem(EEvolutionRarityType.RARE, Material.GUNPOWDER, 4)
            
            .addItem(EEvolutionRarityType.EPIC, Material.REDSTONE_BLOCK, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.BLAZE_POWDER, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.FIRE_CHARGE, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.TNT, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.GLOWSTONE_DUST, 6)

            .build();
    }
}


