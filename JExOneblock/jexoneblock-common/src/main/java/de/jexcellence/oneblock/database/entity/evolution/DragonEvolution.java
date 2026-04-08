package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Dragon Evolution - Ancient Beasts and Power
 * Focus on dragon materials and legendary creatures
 * Stage 19 of 50 - Tier 4: Medieval
 */
public class DragonEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Dragon", 19, 18000)
            .showcase(Material.DRAGON_HEAD)
            .description("The age of dragons, where ancient beasts soar through the skies and legendary power awaits the brave")
            
            // Common blocks - Dragon lair materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_STONE,
                Material.END_STONE_BRICKS, Material.PURPUR_BLOCK)

            // Uncommon blocks - Draconic elements
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD, Material.END_ROD,
                Material.PURPUR_PILLAR, Material.PURPUR_STAIRS)

            // Rare blocks - Ancient power
            .addBlocks(EEvolutionRarityType.RARE,
                Material.END_CRYSTAL, Material.BEACON, Material.CONDUIT,
                Material.RESPAWN_ANCHOR, Material.LODESTONE)

            // Epic blocks - Legendary artifacts
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.DRAGON_BREATH, Material.ELYTRA, Material.SHULKER_SHELL,
                Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT)

            // Legendary blocks - Dragon treasures
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.DRAGON_EGG, Material.NETHER_STAR, Material.TOTEM_OF_UNDYING,
                Material.TRIDENT, Material.HEART_OF_THE_SEA)

            // Special blocks - Ancient magic
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE, Material.EXPERIENCE_BOTTLE,
                Material.ENCHANTED_BOOK, Material.KNOWLEDGE_BOOK)

            // Unique blocks - Primordial power
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.PHANTOM_MEMBRANE, Material.NAUTILUS_SHELL, Material.PRISMARINE_CRYSTALS,
                Material.SEA_LANTERN, Material.SPONGE)

            // Mythical blocks - Mythical dragons
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Divine blocks - Divine dragons
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Celestial blocks - Celestial dragons
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Transcendent blocks - Transcendent dragons
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Ethereal blocks - Ethereal dragons
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Cosmic blocks - Cosmic dragons
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Infinite blocks - Infinite dragons
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect dragons
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.END_PORTAL_FRAME, Material.DEBUG_STICK)
            
            // Dragon entities
            .addEntity(EEvolutionRarityType.RARE, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ELDER_GUARDIAN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ENDERMAN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.RAVAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.GHAST_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.BLAZE_SPAWN_EGG)

            // Dragon items
            .addItem(EEvolutionRarityType.COMMON, Material.END_STONE, 64)
            .addItem(EEvolutionRarityType.COMMON, Material.OBSIDIAN, 32)
            .addItem(EEvolutionRarityType.COMMON, Material.PURPUR_BLOCK, 24)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.DRAGON_BREATH, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.CHORUS_FRUIT, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.END_ROD, 8)
            
            .addItem(EEvolutionRarityType.RARE, Material.END_CRYSTAL, 4)
            .addItem(EEvolutionRarityType.RARE, Material.SHULKER_SHELL, 6)
            .addItem(EEvolutionRarityType.RARE, Material.PHANTOM_MEMBRANE, 8)
            .addItem(EEvolutionRarityType.RARE, Material.DRAGON_HEAD, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.BEACON, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.CONDUIT, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.EXPERIENCE_BOTTLE, 64)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.TOTEM_OF_UNDYING, 3)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.TRIDENT, 1)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ENCHANTED_GOLDEN_APPLE, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ENCHANTED_BOOK, 16)
            .addItem(EEvolutionRarityType.SPECIAL, Material.HEART_OF_THE_SEA, 1)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.SHULKER_BOX, 5)
            .addItem(EEvolutionRarityType.UNIQUE, Material.ENDER_CHEST, 3)
            .addItem(EEvolutionRarityType.UNIQUE, Material.NAUTILUS_SHELL, 8)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ECHO_SHARD, 5)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DISC_FRAGMENT_5, 1)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_CATALYST, 3)
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_SENSOR, 4)
            .addItem(EEvolutionRarityType.DIVINE, Material.CALIBRATED_SCULK_SENSOR, 2)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.TRIAL_KEY, 12)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.OMINOUS_TRIAL_KEY, 5)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.OMINOUS_BOTTLE, 3)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.MACE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.WIND_CHARGE, 24)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.BREEZE_ROD, 4)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ARMADILLO_SCUTE, 6)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 6)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 300)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 1500)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 20)

            // Dragon Soul Orb - Ultimate dragon power
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createDragonSoulOrb(player))

            .build();
    }
}


