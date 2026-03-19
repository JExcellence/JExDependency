package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Bio Evolution - Genetic Engineering
 * Focus on biotechnology and genetic manipulation
 * Stage 33 of 50 - Tier 7: Modern
 */
public class BioEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Bio", 33, 130000)
            .showcase(Material.CHORUS_FRUIT)
            .description("The biological revolution, where life itself becomes programmable and evolution is guided by design")
            
            // Requirements: Digital/Cyber materials
            .requireCurrency(115000)
            .requireItem(Material.CHORUS_FRUIT, 64)
            .requireItem(Material.NETHER_WART, 128)
            .requireItem(Material.GLOW_BERRIES, 64)
            .requireExperience(35)
            
            // Common blocks - Organic materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.MOSS_BLOCK, Material.MOSS_CARPET, Material.AZALEA_LEAVES,
                Material.FLOWERING_AZALEA_LEAVES, Material.SPORE_BLOSSOM)

            // Uncommon blocks - Bio structures
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER, Material.CHORUS_FRUIT,
                Material.POPPED_CHORUS_FRUIT, Material.WARPED_FUNGUS)

            // Rare blocks - Genetic material
            .addBlocks(EEvolutionRarityType.RARE,
                Material.CRIMSON_FUNGUS, Material.NETHER_WART, Material.WARPED_ROOTS,
                Material.CRIMSON_ROOTS, Material.TWISTING_VINES)

            // Epic blocks - Bio enhancement
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.WEEPING_VINES, Material.GLOW_BERRIES, Material.SWEET_BERRIES,
                Material.COCOA_BEANS, Material.KELP)

            // Legendary blocks - Life creation
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Special blocks - Genetic perfection
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Unique blocks - Bio transcendence
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Mythical blocks - Living mythology
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.SCULK_CATALYST,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR)

            // Divine blocks - Divine life
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Celestial blocks - Cosmic biology
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Transcendent blocks - Bio transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.CRAFTER,
                Material.COPPER_GRATE, Material.COPPER_BULB)

            // Ethereal blocks - Living ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS)

            // Cosmic blocks - Bio cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.TUFF_SLAB, Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE)

            // Infinite blocks - Infinite life
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE, Material.COMMAND_BLOCK,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK)

            // Omnipotent blocks - Perfect biology
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.BARRIER,
                Material.BEDROCK, Material.SPAWNER)
            
            // Bio entities - Living creatures
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.AXOLOTL_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GLOW_SQUID_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.TROPICAL_FISH_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.DOLPHIN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.TURTLE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.PANDA_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.BEE_SPAWN_EGG)

            // Bio items - Living quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 22)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 6)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 30)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 65536)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 10)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 25)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 6)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 30)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 30)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 15)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 40)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 25)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 80)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 10)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 45)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 10)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 22)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 35)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 18)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 95)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 45)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 30)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 10)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 5)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 1000)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 18)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 100)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 50)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 4000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 20000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 15)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 200)

            // Genesis Engine - Ultimate bio device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createGenesisEngine(player))

            .build();
    }
}


