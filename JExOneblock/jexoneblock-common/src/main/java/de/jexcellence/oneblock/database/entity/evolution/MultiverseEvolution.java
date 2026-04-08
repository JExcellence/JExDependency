package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Multiverse Evolution - Infinite Parallel Realities
 * Stage 43 of 50 - Tier 9: Cosmic
 */
public class MultiverseEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Multiverse", 43, 700000)
            .showcase(Material.NETHER_PORTAL)
            .description("The infinite parallel realities, where every possibility exists simultaneously")
            
            // Requirements: Galactic materials
            .requireCurrency(650000)
            .requireItem(Material.OBSIDIAN, 256)
            .requireItem(Material.END_PORTAL_FRAME, 8)
            .requireItem(Material.NETHER_STAR, 16)
            .requireExperience(50)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.PURPLE_CONCRETE)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.END_PORTAL_FRAME, Material.RESPAWN_ANCHOR, Material.LODESTONE)
            .addBlocks(EEvolutionRarityType.RARE, Material.REINFORCED_DEEPSLATE, Material.SCULK_CATALYST, Material.SCULK_SHRIEKER)
            .addBlocks(EEvolutionRarityType.EPIC, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEAVY_CORE)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.JIGSAW)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_VOID)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 20)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 5)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEAVY_CORE, 8)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.MACE, 3)
            
            .build();
    }
}


