package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Eternity Evolution - Time Without End
 * Stage 46 of 50 - Tier 10: Transcendent
 */
public class EternityEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Eternity", 46, 1200000)
            .showcase(Material.CLOCK)
            .description("The endless flow of time, where past, present, and future merge into one")
            
            // Requirements: Cosmic materials
            .requireCurrency(1100000)
            .requireItem(Material.NETHER_STAR, 40)
            .requireItem(Material.DRAGON_EGG, 12)
            .requireItem(Material.HEAVY_CORE, 8)
            .requireExperience(55)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.LODESTONE)
            .addBlocks(EEvolutionRarityType.RARE, Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL)
            .addBlocks(EEvolutionRarityType.EPIC, Material.DRAGON_EGG, Material.NETHER_STAR, Material.HEAVY_CORE)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.BARRIER)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.SPAWNER, Material.END_PORTAL_FRAME, Material.BEDROCK)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.LIGHT, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 100)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 20)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEAVY_CORE, 25)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.MACE, 10)
            
            .build();
    }
}


