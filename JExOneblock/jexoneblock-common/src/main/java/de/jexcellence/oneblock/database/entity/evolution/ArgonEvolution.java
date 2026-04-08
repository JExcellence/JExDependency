package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Argon Evolution - Purple Magic Theme
 * Focuses on End dimension, magic, and purple/magenta materials
 * Late-game evolution with magical rewards across all rarities
 */
public class ArgonEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Argon", 24, 62000)
            .showcase(Material.DRAGON_EGG)
            .description("A mystical evolution infused with End magic and purple energy")
            
            // Common blocks - Purple glass and wool
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
                Material.PURPLE_WOOL, Material.MAGENTA_WOOL,
                Material.PURPLE_GLAZED_TERRACOTTA, Material.BUBBLE_CORAL_BLOCK)

            // Uncommon blocks - Purple building materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.PURPLE_STAINED_GLASS, Material.PURPLE_TERRACOTTA,
                Material.PURPLE_CONCRETE, Material.PURPLE_CONCRETE_POWDER,
                Material.MAGENTA_STAINED_GLASS, Material.MAGENTA_TERRACOTTA)

            // Rare blocks - End stone and purpur
            .addBlocks(EEvolutionRarityType.RARE,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.PURPUR_BLOCK, Material.PURPUR_PILLAR, Material.PURPUR_STAIRS)

            // Epic blocks - Amethyst materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST,
                Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD)

            // Legendary blocks - Respawn anchor and End materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.RESPAWN_ANCHOR, Material.END_STONE_BRICKS,
                Material.END_STONE_BRICK_STAIRS, Material.END_STONE_BRICK_SLAB)

            // Special blocks - Dragon egg and shulker boxes
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.DRAGON_EGG, Material.END_PORTAL_FRAME,
                Material.END_GATEWAY, Material.END_ROD)

            // Unique blocks - Ultimate purple materials
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.PURPLE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, 
                Material.SHULKER_BOX, Material.SPAWNER)

            // Mythical blocks - Chorus materials
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER,
                Material.POPPED_CHORUS_FRUIT, Material.CHORUS_FRUIT)

            // Divine blocks - Portal materials
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.END_PORTAL, Material.NETHER_PORTAL,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Celestial blocks - Cosmic purple
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BEACON, Material.CONDUIT,
                Material.END_CRYSTAL, Material.DRAGON_HEAD)

            // Transcendent blocks - Beyond dimensions
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.STRUCTURE_VOID, Material.BARRIER,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK)

            // Ethereal blocks - Spirit realm
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_LANTERN, Material.SOUL_TORCH,
                Material.SOUL_CAMPFIRE, Material.SOUL_SAND)

            // Cosmic blocks - Universe materials
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.VOID_AIR, Material.CAVE_AIR,
                Material.LIGHT, Material.STRUCTURE_BLOCK)

            // Infinite blocks - Endless power
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.REPEATING_COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
                Material.JIGSAW, Material.DEBUG_STICK)

            // Omnipotent blocks - Ultimate reality control
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BEDROCK, Material.KNOWLEDGE_BOOK,
                Material.BUNDLE, Material.TRIAL_KEY)

            // Magical entities progression
            .addEntity(EEvolutionRarityType.RARE, Material.WITCH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PIG_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.ENDERMAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VEX_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.EVOKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITCH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VINDICATOR_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.AXOLOTL_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.GLOW_SQUID_SPAWN_EGG)

            // Purple-themed items progression
            .addItem(EEvolutionRarityType.RARE, Material.ALLIUM, 2)
            .addItem(EEvolutionRarityType.RARE, Material.PINK_TULIP, 2)
            .addItem(EEvolutionRarityType.RARE, Material.PURPLE_CANDLE, 2)
            .addItem(EEvolutionRarityType.RARE, Material.PURPLE_CARPET, 2)
            .addItem(EEvolutionRarityType.RARE, Material.MAGENTA_CARPET, 2)
            .addItem(EEvolutionRarityType.RARE, Material.BUBBLE_CORAL, 2)
            .addItem(EEvolutionRarityType.RARE, Material.BUBBLE_CORAL_BLOCK, 2)

            .addItem(EEvolutionRarityType.EPIC, Material.MAGENTA_STAINED_GLASS, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.PURPLE_STAINED_GLASS, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.CHORUS_FRUIT, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.PURPLE_BANNER, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.PURPLE_BED, 1)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.AMETHYST_SHARD, 16)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.SPYGLASS, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.TINTED_GLASS, 8)

            .addItem(EEvolutionRarityType.SPECIAL, Material.SHULKER_SHELL, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_BREATH, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ENDER_PEARL, 16)

            .addItem(EEvolutionRarityType.UNIQUE, Material.ENDER_EYE, 12)
            .addItem(EEvolutionRarityType.UNIQUE, Material.END_CRYSTAL, 1)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DRAGON_HEAD, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.TOTEM_OF_UNDYING, 2)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CONDUIT, 1)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 1)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.MUSIC_DISC_5, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.DISC_FRAGMENT_5, 3)

            .addItem(EEvolutionRarityType.COSMIC, Material.ECHO_SHARD, 5)
            .addItem(EEvolutionRarityType.COSMIC, Material.RECOVERY_COMPASS, 1)

            .addItem(EEvolutionRarityType.INFINITE, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.INFINITE, Material.SCULK_SENSOR, 3)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 128)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 20)

            // Magical custom items
            .addCustomItem(EEvolutionRarityType.EPIC, EvolutionItemFactory::createCursedBook)
            .addCustomItem(EEvolutionRarityType.SPECIAL, EvolutionItemFactory::createYeetFruit)

            .build();
    }
}


