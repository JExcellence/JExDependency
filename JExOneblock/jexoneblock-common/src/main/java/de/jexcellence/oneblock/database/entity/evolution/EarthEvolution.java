package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Earth Evolution - Ultimate Natural World
 * Focuses on all natural resources, diverse biomes, and earth materials
 * End-game evolution with ultimate rewards across all rarities
 */
public class EarthEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Earth", 29, 78000)
            .showcase(Material.GRASS_BLOCK)
            .description("The ultimate evolution representing all of Earth's natural splendor and power")
            
            // Common blocks - Basic earth materials
            .addBasicOres(EEvolutionRarityType.COMMON)

            // Uncommon blocks - Natural world variety
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.DIRT, Material.GRASS_BLOCK, Material.STONE,
                Material.SAND, Material.GRAVEL, Material.JUNGLE_LOG,
                Material.OAK_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG,
                Material.ACACIA_LOG, Material.SPRUCE_LOG)

            // Rare blocks - Valuable ores
            .addBlocks(EEvolutionRarityType.RARE,
                Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.GOLD_ORE,
                Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_GOLD_ORE)

            // Epic blocks - Precious materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.EMERALD_ORE, Material.DIAMOND_ORE,
                Material.DEEPSLATE_EMERALD_ORE, Material.DEEPSLATE_DIAMOND_ORE)

            // Legendary blocks - Rare earth materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.COAL_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK,
                Material.COPPER_BLOCK, Material.RAW_COPPER_BLOCK, Material.WAXED_COPPER_BLOCK)

            // Special blocks - Earth's treasures
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.EMERALD_BLOCK, Material.NETHERITE_BLOCK)

            // Unique blocks - Ultimate earth power
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SPAWNER, Material.TRIAL_SPAWNER,
                Material.BEACON, Material.CONDUIT)

            // Mythical blocks - Legendary earth structures
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.ANCIENT_DEBRIS, Material.BUDDING_AMETHYST,
                Material.DRAGON_EGG, Material.END_CRYSTAL)

            // Divine blocks - Earth's divine power
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.HEART_OF_THE_SEA, Material.NETHER_STAR,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Celestial blocks - Cosmic earth
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
                Material.JIGSAW, Material.BARRIER)

            // Transcendent blocks - Beyond earthly limits
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.BEDROCK, Material.END_PORTAL_FRAME,
                Material.NETHER_PORTAL, Material.END_GATEWAY)

            // Ethereal blocks - Spirit of earth
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_SAND, Material.SOUL_SOIL,
                Material.SCULK, Material.SCULK_CATALYST)

            // Cosmic blocks - Universal earth
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.VOID_AIR, Material.CAVE_AIR,
                Material.LIGHT, Material.DEBUG_STICK)

            // Infinite blocks - Endless earth
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.REPEATING_COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
                Material.COMMAND_BLOCK, Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Earth's ultimate form
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BUNDLE, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.HEAVY_CORE)

            // Natural world entities progression
            .addPassiveMobs(EEvolutionRarityType.RARE)

            .addHostileMobs(EEvolutionRarityType.EPIC)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITCH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.BEE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.WOLF_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.FOX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.HORSE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.MULE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.SNOW_GOLEM_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ELDER_GUARDIAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.DIVINE, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.DIVINE, Material.ENDER_DRAGON_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.CELESTIAL, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.CELESTIAL, Material.VEX_SPAWN_EGG)

            // Natural items progression
            .addSaplings(EEvolutionRarityType.RARE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.BREAD, 6)

            .addItem(EEvolutionRarityType.EPIC, Material.LAPIS_LAZULI, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.COAL, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.IRON_INGOT, 3)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.EMERALD, 3)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.FIREWORK_ROCKET, 8)

            .addItem(EEvolutionRarityType.SPECIAL, Material.SADDLE, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DIAMOND, 10)

            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHERITE_INGOT, 2)
            .addItem(EEvolutionRarityType.UNIQUE, Material.ANCIENT_DEBRIS, 3)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.NETHER_STAR, 2)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.BEACON, 2)

            .addItem(EEvolutionRarityType.DIVINE, Material.TOTEM_OF_UNDYING, 3)
            .addItem(EEvolutionRarityType.DIVINE, Material.ELYTRA, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.CONDUIT, 2)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.HEART_OF_THE_SEA, 2)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.END_CRYSTAL, 3)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.RECOVERY_COMPASS, 2)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ECHO_SHARD, 15)

            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_CATALYST, 10)
            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_SHRIEKER, 5)

            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_5, 1)
            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_RELIC, 1)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 1024)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 200)

            // Earth's Ultimate Tool - Divine farming implement
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createAlphiriusHoe(player))

            .build();
    }
}


