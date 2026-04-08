package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Solar Evolution - Harnessing the Power of Stars
 * Stage 36 of 50 - Tier 8: Stellar
 */
public class SolarEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Solar", 36, 180000)
            .showcase(Material.DAYLIGHT_DETECTOR)
            .description("The power of suns, where stellar energy fuels civilizations across the cosmos")
            
            // Requirements: Digital materials
            .requireCurrency(160000)
            .requireItem(Material.GLOWSTONE, 128)
            .requireItem(Material.SEA_LANTERN, 64)
            .requireItem(Material.DAYLIGHT_DETECTOR, 16)
            .requireExperience(40)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.GLOWSTONE, Material.SEA_LANTERN, Material.SHROOMLIGHT)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.GOLD_BLOCK, Material.YELLOW_CONCRETE, Material.YELLOW_TERRACOTTA)
            .addBlocks(EEvolutionRarityType.RARE, Material.BEACON, Material.DAYLIGHT_DETECTOR, Material.LIGHTNING_ROD)
            .addBlocks(EEvolutionRarityType.EPIC, Material.NETHER_STAR, Material.END_ROD, Material.LANTERN)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.RESPAWN_ANCHOR, Material.LODESTONE, Material.CRYING_OBSIDIAN)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.CONDUIT, Material.BELL, Material.ENCHANTING_TABLE)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.DRAGON_EGG, Material.END_CRYSTAL, Material.BEACON)
            
            .addEntity(EEvolutionRarityType.RARE, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.MAGMA_CUBE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GHAST_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.RARE, Material.BLAZE_ROD, 16)
            .addItem(EEvolutionRarityType.EPIC, Material.FIRE_CHARGE, 32)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.BEACON, 2)
            
            .build();
    }
}


