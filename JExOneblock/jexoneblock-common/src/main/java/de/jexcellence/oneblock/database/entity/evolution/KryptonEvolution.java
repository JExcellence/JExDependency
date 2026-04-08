package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Krypton Evolution - Noble Gas/Crystal Theme
 * Focuses on light blue materials, ice, and crystalline structures
 * High-level evolution with crystal rewards across all rarities
 */
public class KryptonEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Krypton", 25, 64360)
            .showcase(Material.DIAMOND_BLOCK)
            .description("The crystalline evolution of noble gases and pure energy")
            
            // Common blocks - Light blue glass blocks
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_WOOL,
                Material.LIGHT_BLUE_GLAZED_TERRACOTTA, Material.BUBBLE_CORAL_BLOCK)

            // Uncommon blocks - Crystal materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.LIGHT_BLUE_STAINED_GLASS, Material.LIGHT_BLUE_TERRACOTTA,
                Material.LIGHT_BLUE_CONCRETE, Material.LIGHT_BLUE_CONCRETE_POWDER)

            // Rare blocks - Ice blocks
            .addBlocks(EEvolutionRarityType.RARE,
                Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE,
                Material.WARPED_PLANKS, Material.WARPED_STEM)

            // Epic blocks - Advanced crystals
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.WARPED_NYLIUM, Material.DIAMOND_ORE,
                Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST)

            // Legendary blocks - Precious crystals
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.PRISMARINE, Material.DARK_PRISMARINE, Material.PRISMARINE_BRICKS,
                Material.SEA_LANTERN, Material.CONDUIT)

            // Special blocks - Noble gas power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.BEACON, Material.DEEPSLATE_DIAMOND_ORE,
                Material.TINTED_GLASS)

            // Unique blocks - Ultimate crystal power
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.LIGHT_BLUE_SHULKER_BOX, Material.DIAMOND_BLOCK,
                Material.SPAWNER)

            // Mythical blocks - Legendary crystal structures
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.END_CRYSTAL, Material.DRAGON_EGG,
                Material.NETHERITE_BLOCK)

            // Divine blocks - Divine crystals
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.NETHER_STAR, Material.TOTEM_OF_UNDYING,
                Material.HEART_OF_THE_SEA)

            // Celestial blocks - Cosmic crystals
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
                Material.END_PORTAL_FRAME)

            // Transcendent blocks - Beyond crystals
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.BARRIER, Material.BEDROCK,
                Material.HEAVY_CORE)

            // Ethereal blocks - Spirit crystals
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_SAND, Material.SCULK_CATALYST,
                Material.SCULK_SENSOR)

            // Cosmic blocks - Universal crystals
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.VOID_AIR, Material.LIGHT,
                Material.DEBUG_STICK)

            // Infinite blocks - Endless crystal power
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Perfect crystalline structure
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BUNDLE, Material.TRIAL_KEY,
                Material.MACE)

            // Cold/Ice entities progression
            .addEntity(EEvolutionRarityType.RARE, Material.STRAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.SQUID_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.GLOW_SQUID_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.COD_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.SILVERFISH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.POLAR_BEAR_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GHAST_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.PILLAGER_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.GUARDIAN_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.BREEZE_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.DIVINE, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.DIVINE, Material.ENDER_DRAGON_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.CELESTIAL, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.CELESTIAL, Material.VILLAGER_SPAWN_EGG)

            // Krypton items progression
            .addItem(EEvolutionRarityType.RARE, Material.WARPED_ROOTS, 2)
            .addItem(EEvolutionRarityType.RARE, Material.WARPED_FUNGUS, 2)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_BLUE_BANNER, 2)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_BLUE_CARPET, 2)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_BLUE_BED, 1)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_BLUE_CANDLE, 3)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_BLUE_STAINED_GLASS, 4)

            .addItem(EEvolutionRarityType.EPIC, Material.SOUL_CAMPFIRE, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.SOUL_TORCH, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.LIGHT_BLUE_CONCRETE, 9)
            .addItem(EEvolutionRarityType.EPIC, Material.LIGHT_BLUE_GLAZED_TERRACOTTA, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.TWISTING_VINES, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.NETHER_SPROUTS, 3)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.WARPED_PLANKS, 16)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BLUE_ORCHID, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CORNFLOWER, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.LIGHT_BLUE_WOOL, 16)

            .addItem(EEvolutionRarityType.SPECIAL, Material.AMETHYST_SHARD, 8)
            .addItem(EEvolutionRarityType.SPECIAL, Material.SPYGLASS, 1)

            .addItem(EEvolutionRarityType.UNIQUE, Material.PRISMARINE_CRYSTALS, 6)
            .addItem(EEvolutionRarityType.UNIQUE, Material.PRISMARINE_SHARD, 8)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.CONDUIT, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.HEART_OF_THE_SEA, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.END_CRYSTAL, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.DRAGON_BREATH, 4)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ECHO_SHARD, 15)

            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_CATALYST, 8)
            .addItem(EEvolutionRarityType.COSMIC, Material.CALIBRATED_SCULK_SENSOR, 4)

            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_CREATOR, 1)
            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_CREATOR_MUSIC_BOX, 1)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 1536)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 300)

            // Krypton armor set and sword - Complete crystal set
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createKryptonHelmet(player))
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createKryptonChestplate(player))
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createKryptonLeggings(player))
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createKryptonBoots(player))
            .addCustomItem(EEvolutionRarityType.UNIQUE, 
                player -> EvolutionItemFactory.createKryptonSword(player))

            .build();
    }
}


