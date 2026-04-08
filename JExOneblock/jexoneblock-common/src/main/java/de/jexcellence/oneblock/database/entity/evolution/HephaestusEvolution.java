package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Hephaestus Evolution - God of Fire and Forge
 * Focuses on metalworking, forging, and crafting materials
 * Divine evolution with forge rewards across all rarities
 */
public class HephaestusEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Hephaestus", 26, 68000)
            .showcase(Material.NETHERITE_BLOCK)
            .description("The divine evolution of the forge god, master of fire and metalworking")
            
            // Common blocks - Basic forge materials
            .addBasicOres(EEvolutionRarityType.COMMON)

            // Uncommon blocks - Metal ores
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.REDSTONE_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
                Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE)

            // Rare blocks - Forge blocks
            .addBlocks(EEvolutionRarityType.RARE,
                Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
                Material.ANVIL, Material.SMITHING_TABLE, Material.GRINDSTONE)

            // Epic blocks - Advanced forge materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.MAGMA_BLOCK, Material.LAVA_CAULDRON, Material.CAULDRON,
                Material.FIRE_CORAL_BLOCK, Material.NETHER_BRICKS)

            // Legendary blocks - Divine forge materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS,
                Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK)

            // Special blocks - Master forge tools
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.BEACON, Material.CONDUIT, Material.LODESTONE)

            // Unique blocks - Ultimate forge power
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT)

            // Mythical blocks - Legendary forge structures
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.NETHER_STAR)

            // Divine blocks - Divine smithing
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA,
                Material.TRIDENT, Material.MACE)

            // Celestial blocks - Cosmic forge
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
                Material.END_PORTAL_FRAME)

            // Transcendent blocks - Beyond forging
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.BARRIER, Material.BEDROCK,
                Material.HEAVY_CORE)

            // Ethereal blocks - Spirit of forge
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_SAND, Material.SOUL_TORCH,
                Material.SCULK_CATALYST)

            // Cosmic blocks - Universal forge
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.VOID_AIR, Material.LIGHT,
                Material.DEBUG_STICK)

            // Infinite blocks - Endless creation
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Perfect smithing
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BUNDLE, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY)

            // Forge entities progression
            .addEntity(EEvolutionRarityType.UNCOMMON, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNCOMMON, Material.SNOW_GOLEM_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.RARE, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.MAGMA_CUBE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.WITHER_SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PIGLIN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PIGLIN_BRUTE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.GHAST_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.STRIDER_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.WARDEN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.DIVINE, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.DIVINE, Material.VEX_SPAWN_EGG)

            // Forge items progression
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 5)
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_INGOT, 3)

            .addItem(EEvolutionRarityType.UNCOMMON, Material.COPPER_INGOT, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.RAW_IRON, 6)

            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND, 3)
            .addItem(EEvolutionRarityType.RARE, Material.EMERALD, 2)

            .addItem(EEvolutionRarityType.EPIC, Material.NETHERITE_SCRAP, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.ANCIENT_DEBRIS, 1)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_INGOT, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BLAZE_ROD, 4)

            .addItem(EEvolutionRarityType.SPECIAL, Material.GHAST_TEAR, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.MAGMA_CREAM, 4)

            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.TOTEM_OF_UNDYING, 2)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.TRIDENT, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.MACE, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.CONDUIT, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.HEART_OF_THE_SEA, 1)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.END_CRYSTAL, 2)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ECHO_SHARD, 10)

            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_CATALYST, 4)
            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_SHRIEKER, 2)

            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_PIGSTEP, 1)
            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_OTHERSIDE, 3)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 1024)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 200)

            // Hephaestus's legendary hammer - Divine smithing tool
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createHammerOfHephaestus(player))

            .build();
    }
}


