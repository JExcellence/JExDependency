package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Black Hole Evolution - Gravitational Singularity
 * Stage 40 of 50 - Tier 8: Stellar
 */
public class BlackHoleEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Black Hole", 40, 450000)
            .showcase(Material.BLACK_GLAZED_TERRACOTTA)
            .description("The ultimate gravitational force, where even light cannot escape the event horizon")
            
            // Requirements: Supernova materials
            .requireCurrency(400000)
            .requireItem(Material.SCULK_CATALYST, 16)
            .requireItem(Material.ECHO_SHARD, 32)
            .requireItem(Material.HEAVY_CORE, 4)
            .requireExperience(44)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.BLACK_CONCRETE, Material.COAL_BLOCK, Material.BLACK_WOOL)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.REINFORCED_DEEPSLATE)
            .addBlocks(EEvolutionRarityType.RARE, Material.SCULK, Material.SCULK_CATALYST, Material.SCULK_SHRIEKER)
            .addBlocks(EEvolutionRarityType.EPIC, Material.END_PORTAL_FRAME, Material.RESPAWN_ANCHOR, Material.LODESTONE)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, Material.END_CRYSTAL, Material.BEACON)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.HEAVY_CORE, Material.NETHER_STAR, Material.CONDUIT)
            .addBlocks(EEvolutionRarityType.UNIQUE, Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.BARRIER)
            
            .addEntity(EEvolutionRarityType.RARE, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.RARE, Material.ECHO_SHARD, 16)
            .addItem(EEvolutionRarityType.EPIC, Material.RECOVERY_COMPASS, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.HEAVY_CORE, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.MACE, 1)
            
            .build();
    }
}


