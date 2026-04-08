package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Infinity Evolution - Beyond All Limits
 * Stage 44 of 50 - Tier 9: Cosmic
 */
public class InfinityEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Infinity", 44, 850000)
            .showcase(Material.NETHER_STAR)
            .description("The concept of infinity made manifest, where limits cease to exist")
            
            // Requirements: Multiverse materials
            .requireCurrency(750000)
            .requireItem(Material.NETHER_STAR, 24)
            .requireItem(Material.DRAGON_EGG, 8)
            .requireItem(Material.BEACON, 16)
            .requireExperience(52)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.NETHERITE_BLOCK)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL)
            .addBlocks(EEvolutionRarityType.RARE, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEAVY_CORE)
            .addBlocks(EEvolutionRarityType.EPIC, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.BARRIER)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.LIGHT, Material.STRUCTURE_VOID, Material.DEBUG_STICK)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.SPAWNER, Material.END_PORTAL_FRAME, Material.BEDROCK)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 50)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 10)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEAVY_CORE, 15)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.MACE, 5)
            
            .build();
    }
}


