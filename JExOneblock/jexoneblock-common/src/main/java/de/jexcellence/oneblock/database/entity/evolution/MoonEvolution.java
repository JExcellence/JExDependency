package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Moon Evolution - Lunar Surface Theme
 * Focuses on End stone materials, space exploration, and lunar environment
 * Late-game evolution with space-themed rewards across all rarities
 */
public class MoonEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Moon", 30, 81000)
            .showcase(Material.END_STONE)
            .description("The desolate evolution of the lunar surface, master of space exploration and isolation")
            
            // Requirements: Factory materials
            .requireCurrency(72000)
            .requireItem(Material.END_STONE, 128)
            .requireItem(Material.PURPUR_BLOCK, 64)
            .requireItem(Material.QUARTZ_BLOCK, 64)
            .requireExperience(34)
            
            // Common blocks - Basic lunar surface
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.END_STONE, Material.END_STONE_BRICKS, Material.PURPUR_BLOCK,
                Material.PURPUR_STAIRS, Material.PURPUR_SLAB)

            // Uncommon blocks - Lunar structures
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.END_STONE_BRICK_STAIRS, Material.END_STONE_BRICK_SLAB,
                Material.END_STONE_BRICK_WALL, Material.PURPUR_PILLAR)

            // Rare blocks - Space materials
            .addBlocks(EEvolutionRarityType.RARE,
                Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ, Material.QUARTZ_STAIRS,
                Material.QUARTZ_SLAB, Material.QUARTZ_PILLAR)

            // Epic blocks - Void materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE,
                Material.POLISHED_BLACKSTONE, Material.BLACKSTONE_STAIRS)

            // Legendary blocks - Rare space materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.END_CRYSTAL, Material.SHULKER_BOX,
                Material.WHITE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX)

            // Special blocks - Ultimate space power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.DRAGON_EGG, Material.END_PORTAL_FRAME, Material.CONDUIT)

            // Unique blocks - Legendary lunar structures
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.LODESTONE)

            // Mythical blocks - Divine space power
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.NETHER_STAR, Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Divine blocks - Cosmic lunar power
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Celestial blocks - Beyond lunar
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER)

            // Transcendent blocks - Spirit of space
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.SOUL_SAND, Material.SCULK_CATALYST, Material.SCULK_SENSOR)

            // Ethereal blocks - Universal space
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.VOID_AIR, Material.LIGHT, Material.DEBUG_STICK)

            // Cosmic blocks - Endless space
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.KNOWLEDGE_BOOK)

            // Infinite blocks - Perfect lunar exploration
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.BUNDLE, Material.TRIAL_KEY, Material.HEAVY_CORE)

            // Omnipotent blocks - Ultimate space mastery
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.MACE, Material.WIND_CHARGE, Material.BREEZE_ROD)

            // Lunar entities (sparse life)
            .addEntity(EEvolutionRarityType.RARE, Material.BAT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SILVERFISH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ENDERMITE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.PILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ENDERMAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PHANTOM_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.SKELETON_HORSE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ENDER_DRAGON_SPAWN_EGG)

            // Moon exploration items
            .addItem(EEvolutionRarityType.RARE, Material.END_STONE, 16)
            .addItem(EEvolutionRarityType.RARE, Material.END_STONE_BRICKS, 12)
            .addItem(EEvolutionRarityType.RARE, Material.PURPUR_BLOCK, 8)

            .addItem(EEvolutionRarityType.EPIC, Material.ENDER_PEARL, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.CHORUS_FRUIT, 5)
            .addItem(EEvolutionRarityType.EPIC, Material.POPPED_CHORUS_FRUIT, 3)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.SHULKER_SHELL, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 1)

            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_BREATH, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_HEAD, 1)

            .addItem(EEvolutionRarityType.UNIQUE, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.END_PORTAL_FRAME, 1)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.TOTEM_OF_UNDYING, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.CONDUIT, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.HEART_OF_THE_SEA, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.NAUTILUS_SHELL, 8)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ECHO_SHARD, 20)

            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_CATALYST, 15)
            .addItem(EEvolutionRarityType.COSMIC, Material.CALIBRATED_SCULK_SENSOR, 8)

            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_OTHERSIDE, 1)
            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_RELIC, 3)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 900)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 180)

            // Moon rocket - Ultimate space exploration item
            .addCustomItem(EEvolutionRarityType.UNIQUE,
                player -> EvolutionItemFactory.createMoonRocket(player))

            .build();
    }
}


