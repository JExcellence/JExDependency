package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Ignis Evolution - Fire and Basic Heat Materials
 * Focus on fire-related blocks and early heat/energy concepts
 * Stage 4 of 50 - Tier 1: Genesis
 */
public class IgnisEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Ignis", 4, 400)
            .showcase(Material.CAMPFIRE)
            .description("The spark of energy, where fire brings warmth, light, and transformation")
            
            // Requirements: Water materials from Aqua
            .requireCurrency(300)
            .requireItem(Material.COAL, 16)
            .requireItem(Material.COBBLESTONE, 64)
            
            // Common blocks - Basic fire materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COBBLESTONE, Material.STONE, Material.NETHERRACK,
                Material.BLACKSTONE, Material.BASALT)

            // Uncommon blocks - Heat sources
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.FURNACE, Material.CAMPFIRE, Material.TORCH,
                Material.COAL_BLOCK, Material.CHARCOAL)

            // Rare blocks - Advanced fire
            .addBlocks(EEvolutionRarityType.RARE,
                Material.BLAST_FURNACE, Material.SMOKER, Material.LANTERN,
                Material.SOUL_TORCH, Material.SOUL_CAMPFIRE)

            // Epic blocks - Magma and lava
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.MAGMA_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.GLOWSTONE, Material.SHROOMLIGHT)

            // Legendary blocks - Ultimate fire power
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.FIRE_CORAL_BLOCK, Material.REDSTONE_LAMP)
            
            // Fire entities
            .addEntity(EEvolutionRarityType.RARE, Material.CHICKEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PIG_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.SKELETON_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.BLAZE_SPAWN_EGG)

            // Fire-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.COAL, 3)
            .addItem(EEvolutionRarityType.COMMON, Material.CHARCOAL, 2)
            .addItem(EEvolutionRarityType.COMMON, Material.STICK, 4)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.TORCH, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.FLINT_AND_STEEL, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.COOKED_BEEF, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.CAMPFIRE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.LANTERN, 2)
            .addItem(EEvolutionRarityType.RARE, Material.COOKED_PORKCHOP, 3)
            
            .addItem(EEvolutionRarityType.EPIC, Material.LAVA_BUCKET, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.FIRE_CHARGE, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.BLAZE_POWDER, 1)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.GLOWSTONE_DUST, 4)

            .build();
    }
}


