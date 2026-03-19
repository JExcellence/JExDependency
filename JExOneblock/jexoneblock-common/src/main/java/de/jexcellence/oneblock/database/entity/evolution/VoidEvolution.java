package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Void Evolution - The Space Between Dimensions
 * Stage 37 of 50 - Tier 8: Stellar
 */
public class VoidEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Void", 37, 220000)
            .showcase(Material.BLACK_CONCRETE)
            .description("The emptiness between worlds, where nothing and everything coexist in perfect balance")
            
            // Requirements: Solar materials
            .requireCurrency(200000)
            .requireItem(Material.OBSIDIAN, 128)
            .requireItem(Material.END_STONE, 128)
            .requireItem(Material.SCULK, 64)
            .requireExperience(41)
            
            .addBlocks(EEvolutionRarityType.COMMON, Material.BLACK_CONCRETE, Material.OBSIDIAN, Material.CRYING_OBSIDIAN)
            .addBlocks(EEvolutionRarityType.UNCOMMON, Material.END_STONE, Material.END_STONE_BRICKS, Material.PURPUR_BLOCK)
            .addBlocks(EEvolutionRarityType.RARE, Material.SCULK, Material.SCULK_VEIN, Material.SCULK_CATALYST)
            .addBlocks(EEvolutionRarityType.EPIC, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SHRIEKER)
            .addBlocks(EEvolutionRarityType.LEGENDARY, Material.REINFORCED_DEEPSLATE, Material.RESPAWN_ANCHOR, Material.LODESTONE)
            .addBlocks(EEvolutionRarityType.SPECIAL, Material.END_PORTAL_FRAME, Material.DRAGON_EGG, Material.END_CRYSTAL)
            
            .addEntity(EEvolutionRarityType.RARE, Material.ENDERMAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ENDERMITE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            
            .addItem(EEvolutionRarityType.RARE, Material.ENDER_PEARL, 32)
            .addItem(EEvolutionRarityType.EPIC, Material.ENDER_EYE, 16)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.SHULKER_SHELL, 16)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ECHO_SHARD, 8)
            
            .build();
    }
}


