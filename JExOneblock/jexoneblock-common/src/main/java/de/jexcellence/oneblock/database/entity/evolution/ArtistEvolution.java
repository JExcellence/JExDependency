package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Artist Evolution - Beauty and Creation
 * Focus on decorative blocks and artistic expression
 * Stage 23 of 50 - Tier 5: Renaissance
 */
public class ArtistEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Artist", 23, 22000)
            .showcase(Material.PAINTING)
            .description("The renaissance of art, where beauty and creativity flourish in magnificent works of eternal splendor")
            
            // Requirements: Explorer materials
            .requireCurrency(19000)
            .requireItem(Material.WHITE_DYE, 64)
            .requireItem(Material.PAINTING, 16)
            .requireItem(Material.ITEM_FRAME, 32)
            .requireExperience(30)
            
            // Common blocks - Basic art materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.WHITE_WOOL, Material.BLACK_WOOL, Material.RED_WOOL,
                Material.BLUE_WOOL, Material.YELLOW_WOOL)

            // Uncommon blocks - Color palette
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.GREEN_WOOL, Material.PURPLE_WOOL, Material.ORANGE_WOOL,
                Material.PINK_WOOL, Material.LIME_WOOL)

            // Rare blocks - Artistic materials
            .addBlocks(EEvolutionRarityType.RARE,
                Material.WHITE_CONCRETE, Material.BLACK_CONCRETE, Material.RED_CONCRETE,
                Material.BLUE_CONCRETE, Material.YELLOW_CONCRETE)

            // Epic blocks - Masterwork materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.WHITE_STAINED_GLASS, Material.RED_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
                Material.YELLOW_STAINED_GLASS, Material.GREEN_STAINED_GLASS)

            // Legendary blocks - Artistic wonders
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.PAINTING, Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME,
                Material.ARMOR_STAND, Material.FLOWER_POT)

            // Special blocks - Renaissance art
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.BEACON, Material.CONDUIT, Material.SEA_LANTERN,
                Material.GLOWSTONE, Material.SHROOMLIGHT)

            // Unique blocks - Legendary artworks
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Mythical blocks - Mythical art
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Divine blocks - Divine artistry
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Celestial blocks - Celestial beauty
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Transcendent blocks - Transcendent art
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Ethereal blocks - Ethereal beauty
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Cosmic blocks - Cosmic artistry
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Infinite blocks - Infinite beauty
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect art
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)
            
            // Artistic entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.CAT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PARROT_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.AXOLOTL_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GLOW_SQUID_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.TROPICAL_FISH_SPAWN_EGG)

            // Artist items
            .addItem(EEvolutionRarityType.COMMON, Material.WHITE_DYE, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.BLACK_DYE, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.RED_DYE, 16)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.BLUE_DYE, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.YELLOW_DYE, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GREEN_DYE, 16)
            
            .addItem(EEvolutionRarityType.RARE, Material.PAINTING, 8)
            .addItem(EEvolutionRarityType.RARE, Material.ITEM_FRAME, 12)
            .addItem(EEvolutionRarityType.RARE, Material.GLOW_ITEM_FRAME, 8)
            .addItem(EEvolutionRarityType.RARE, Material.ARMOR_STAND, 4)
            
            .addItem(EEvolutionRarityType.EPIC, Material.MUSIC_DISC_CAT, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.MUSIC_DISC_BLOCKS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.MUSIC_DISC_CHIRP, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.JUKEBOX, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BEACON, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CONDUIT, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.SEA_LANTERN, 8)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 64)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHER_STAR, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ENCHANTED_BOOK, 20)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 3)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.END_CRYSTAL, 3)
            .addItem(EEvolutionRarityType.UNIQUE, Material.ELYTRA, 1)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 5)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 3)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_MEMBRANE, 8)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 6)
            .addItem(EEvolutionRarityType.DIVINE, Material.MUSIC_DISC_5, 1)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 3)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 5)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 20)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 8)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 2)
            .addItem(EEvolutionRarityType.COSMIC, Material.WIND_CHARGE, 64)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 600)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 3000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 35)

            // Divine Palette - Ultimate artistic tool
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createDivinePalette(player))

            .build();
    }
}


