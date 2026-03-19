package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Nebula Evolution - Cosmic Clouds of Creation
 * Stage 38 of 50 - Tier 8: Stellar
 */
public class NebulaEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Nebula", 38, 280000)
            .showcase(Material.PURPLE_STAINED_GLASS)
            .description("The stellar nurseries where new stars are born from cosmic dust and gas")
            
            // Requirements: Solar materials
            .requireCurrency(250000)
            .requireItem(Material.AMETHYST_SHARD, 128)
            .requireItem(Material.PURPLE_CONCRETE, 64)
            .requireItem(Material.CHORUS_FRUIT, 64)
            .requireExperience(42)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.PURPLE_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS, Material.AMETHYST_BLOCK)
            .addBlocks(EEvolutionRarityType.RARE, Material.BUDDING_AMETHYST, Material.AMETHYST_CLUSTER, Material.LARGE_AMETHYST_BUD)
            .addBlocks(EEvolutionRarityType.EPIC, Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR, Material.CHORUS_FLOWER)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, Material.BEACON, Material.CONDUIT)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEART_OF_THE_SEA)
            
            .addEntity(EEvolutionRarityType.RARE, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PHANTOM_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.RARE, Material.AMETHYST_SHARD, 64)
            .addItem(EEvolutionRarityType.EPIC, Material.CHORUS_FRUIT, 32)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_BREATH, 16)
            .addItem(EEvolutionRarityType.SPECIAL, Material.END_CRYSTAL, 4)
            
            .build();
    }
}


