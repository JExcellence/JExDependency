package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Artemis Evolution - Goddess of Hunt and Nature
 * Focuses on wilderness, hunting, and forest materials
 * Nature-themed evolution with hunting rewards across all rarities
 */
public class ArtemisEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Artemis", 18, 38000)
            .showcase(Material.BOW)
            .description("The wild evolution of the hunt goddess, master of forest and moon")
            
            // Common blocks - Forest materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, 
                Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG)

            // Uncommon blocks - Nature materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
                Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES)

            // Rare blocks - Wild materials
            .addBlocks(EEvolutionRarityType.RARE,
                Material.MOSS_BLOCK, Material.BONE_BLOCK, Material.GRASS_BLOCK,
                Material.PODZOL, Material.MYCELIUM, Material.ROOTED_DIRT)

            // Epic blocks - Ancient forest
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES,
                Material.MANGROVE_LOG, Material.MANGROVE_LEAVES, Material.MANGROVE_ROOTS)

            // Legendary blocks - Sacred grove
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.CHERRY_LOG, Material.CHERRY_LEAVES,
                Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK)

            // Special blocks - Moon materials
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.GLOWSTONE, Material.SEA_LANTERN,
                Material.SHROOMLIGHT, Material.SOUL_LANTERN)

            // Unique blocks - Divine hunt materials
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SPAWNER, Material.TRIAL_SPAWNER,
                Material.BEE_NEST, Material.BEEHIVE)

            // Mythical blocks - Ancient trees
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.WARPED_STEM, Material.CRIMSON_STEM,
                Material.WARPED_HYPHAE, Material.CRIMSON_HYPHAE)

            // Divine blocks - World tree materials
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER,
                Material.END_ROD, Material.PURPUR_BLOCK)

            // Celestial blocks - Cosmic forest
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BEACON, Material.CONDUIT,
                Material.END_CRYSTAL, Material.DRAGON_EGG)

            // Transcendent blocks - Beyond nature
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.STRUCTURE_BLOCK, Material.JIGSAW,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK)

            // Ethereal blocks - Spirit forest
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_SAND, Material.SOUL_SOIL,
                Material.SOUL_TORCH, Material.SOUL_CAMPFIRE)

            // Cosmic blocks - Universal nature
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.VOID_AIR, Material.CAVE_AIR,
                Material.LIGHT, Material.BARRIER)

            // Infinite blocks - Eternal forest
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.REPEATING_COMMAND_BLOCK, Material.COMMAND_BLOCK,
                Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Nature's ultimate power
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BEDROCK, Material.BUNDLE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY)

            // Wildlife entities progression
            .addEntity(EEvolutionRarityType.COMMON, Material.WOLF_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.COMMON, Material.FOX_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNCOMMON, Material.RABBIT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNCOMMON, Material.OCELOT_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.RARE, Material.PANDA_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.POLAR_BEAR_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.BEE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.CAT_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.HORSE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.LLAMA_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.SKELETON_HORSE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ZOMBIE_HORSE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.SNOW_GOLEM_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.VEX_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.DIVINE, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.DIVINE, Material.ENDER_DRAGON_SPAWN_EGG)

            // Hunting items progression
            .addItem(EEvolutionRarityType.COMMON, Material.BOW, 1)
            .addItem(EEvolutionRarityType.COMMON, Material.ARROW, 32)

            .addItem(EEvolutionRarityType.UNCOMMON, Material.CROSSBOW, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.SPECTRAL_ARROW, 16)

            .addItem(EEvolutionRarityType.RARE, Material.LEATHER, 8)
            .addItem(EEvolutionRarityType.RARE, Material.RABBIT_HIDE, 4)

            .addItem(EEvolutionRarityType.EPIC, Material.LEATHER_HELMET, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.LEATHER_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.LEATHER_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.LEATHER_BOOTS, 1)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.SADDLE, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.LEAD, 3)

            .addItem(EEvolutionRarityType.SPECIAL, Material.NAME_TAG, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.GOLDEN_APPLE, 3)

            .addItem(EEvolutionRarityType.UNIQUE, Material.ENCHANTED_GOLDEN_APPLE, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.TOTEM_OF_UNDYING, 1)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.TRIDENT, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.MACE, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.HEAVY_CORE, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 1)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.MUSIC_DISC_RELIC, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.BRICK, 1)

            .addItem(EEvolutionRarityType.COSMIC, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.ECHO_SHARD, 8)

            .addItem(EEvolutionRarityType.INFINITE, Material.SCULK_CATALYST, 3)
            .addItem(EEvolutionRarityType.INFINITE, Material.CALIBRATED_SCULK_SENSOR, 2)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 256)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 50)

            // Artemis' Divine Bow - Ultimate hunting weapon
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createBowOfArtemis(player))

            .build();
    }
}


