package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Omnipotence Evolution - Unlimited Power
 * Stage 47 of 50 - Tier 10: Transcendent
 */
public class OmnipotenceEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Omnipotence", 47, 1500000)
            .showcase(Material.COMMAND_BLOCK)
            .description("The ultimate power, where reality bends to your will and nothing is impossible")
            
            // Requirements: Eternity materials
            .requireCurrency(1400000)
            .requireItem(Material.NETHER_STAR, 64)
            .requireItem(Material.DRAGON_EGG, 16)
            .requireItem(Material.MACE, 4)
            .requireExperience(58)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.NETHERITE_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL)
            .addBlocks(EEvolutionRarityType.RARE, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEAVY_CORE)
            .addBlocks(EEvolutionRarityType.EPIC, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.BARRIER)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.SPAWNER, Material.END_PORTAL_FRAME, Material.BEDROCK)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.LIGHT, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK)
            .addBlocks(EEvolutionRarityType.MYTHICAL, Material.STRUCTURE_VOID, Material.CAVE_AIR, Material.VOID_AIR)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 200)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 50)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEAVY_CORE, 50)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.MACE, 20)
            .addItem(EEvolutionRarityType.DIVINE, Material.KNOWLEDGE_BOOK, 10)
            
            .build();
    }
}


