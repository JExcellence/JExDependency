package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Supernova Evolution - Explosive Stellar Death
 * Stage 39 of 50 - Tier 8: Stellar
 */
public class SupernovaEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Supernova", 39, 350000)
            .showcase(Material.TNT)
            .description("The explosive death of massive stars, seeding the universe with heavy elements")
            
            // Requirements: Nebula materials
            .requireCurrency(320000)
            .requireItem(Material.NETHERITE_INGOT, 16)
            .requireItem(Material.TNT, 64)
            .requireItem(Material.MAGMA_BLOCK, 128)
            .requireExperience(43)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.TNT, Material.REDSTONE_BLOCK, Material.ORANGE_CONCRETE)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.MAGMA_BLOCK, Material.LAVA_CAULDRON, Material.FIRE_CORAL_BLOCK)
            .addBlocks(EEvolutionRarityType.RARE, Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.GILDED_BLACKSTONE)
            .addBlocks(EEvolutionRarityType.EPIC, Material.RESPAWN_ANCHOR, Material.CRYING_OBSIDIAN, Material.LODESTONE)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEAVY_CORE)
            
            .addEntity(EEvolutionRarityType.RARE, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.WITHER_SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GHAST_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.RARE, Material.NETHERITE_SCRAP, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.NETHERITE_INGOT, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 5)
            .addItem(EEvolutionRarityType.SPECIAL, Material.HEAVY_CORE, 2)
            
            .build();
    }
}


