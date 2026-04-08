package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Aqua Evolution - Water and Basic Ocean Materials
 * Focus on water-related blocks and early ocean exploration
 * Stage 3 of 50 - Tier 1: Genesis
 */
public class AquaEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Aqua", 3, 300)
            .showcase(Material.WATER_BUCKET)
            .description("The flow of life, where water brings movement and sustenance to the world")
            
            // Requirements: Earth materials from Terra
            .requireCurrency(200)
            .requireItem(Material.DIRT, 32)
            .requireItem(Material.CLAY_BALL, 8)
            
            // Common blocks - Water and ice
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.ICE, Material.SNOW_BLOCK, Material.SNOW,
                Material.SAND, Material.GRAVEL)

            // Uncommon blocks - Ocean floor
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.CLAY, Material.PRISMARINE, Material.SPONGE,
                Material.WET_SPONGE, Material.KELP)

            // Rare blocks - Ocean structures
            .addBlocks(EEvolutionRarityType.RARE,
                Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE,
                Material.SEA_LANTERN, Material.TUBE_CORAL_BLOCK)

            // Epic blocks - Advanced ocean
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.BRAIN_CORAL_BLOCK, Material.BUBBLE_CORAL_BLOCK,
                Material.FIRE_CORAL_BLOCK, Material.HORN_CORAL_BLOCK)

            // Legendary blocks - Rare ocean materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.CONDUIT, Material.HEART_OF_THE_SEA)
            
            // Water entities
            .addEntity(EEvolutionRarityType.RARE, Material.COD_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SALMON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SQUID_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.TROPICAL_FISH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PUFFERFISH_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.DOLPHIN_SPAWN_EGG)

            // Water-themed items
            .addItem(EEvolutionRarityType.COMMON, Material.KELP, 3)
            .addItem(EEvolutionRarityType.COMMON, Material.DRIED_KELP, 2)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.FISHING_ROD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.WATER_BUCKET, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.LILY_PAD, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.PRISMARINE_SHARD, 2)
            .addItem(EEvolutionRarityType.RARE, Material.PRISMARINE_CRYSTALS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.OAK_BOAT, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.COD, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.SALMON, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.INK_SAC, 3)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NAUTILUS_SHELL, 1)

            .build();
    }
}


