package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItemFactory;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Enhanced Helium Evolution - Air/Sky Theme
 * Focuses on light materials, floating blocks, and aerial elements
 * High-level evolution with sky-themed rewards across all rarities
 */
public class HeliumEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Helium", 22, 53400)
            .showcase(Material.ELYTRA)
            .description("The ethereal evolution of air and sky, master of flight and lightness")
            
            // Common blocks - Light/Air blocks
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE,
                Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                Material.WHITE_WOOL)

            // Uncommon blocks - Glass materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.GLASS, Material.WHITE_STAINED_GLASS,
                Material.LIGHT_GRAY_STAINED_GLASS, Material.GRAY_STAINED_GLASS,
                Material.TINTED_GLASS)

            // Rare blocks - Light building materials
            .addBlocks(EEvolutionRarityType.RARE,
                Material.OAK_LOG, Material.DIRT, Material.GRASS_BLOCK,
                Material.SAND, Material.GRAVEL, Material.CLAY)

            // Epic blocks - Floating materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.SCAFFOLDING, Material.IRON_BARS, Material.LANTERN,
                Material.SOUL_LANTERN, Material.END_ROD)

            // Legendary blocks - Sky materials
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.GLOWSTONE, Material.SEA_LANTERN, Material.SHROOMLIGHT,
                Material.BEACON, Material.CONDUIT)

            // Special blocks - Aerial power
            .addBlocks(EEvolutionRarityType.SPECIAL, 
                Material.IRON_BLOCK, Material.LIGHT, Material.BARRIER)

            // Unique blocks - Ultimate sky materials
            .addBlocks(EEvolutionRarityType.UNIQUE, 
                Material.WHITE_SHULKER_BOX, Material.SHULKER_BOX)

            // Mythical blocks - Legendary sky structures
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.END_CRYSTAL, Material.DRAGON_EGG,
                Material.ELYTRA)

            // Divine blocks - Divine flight
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.NETHER_STAR, Material.TOTEM_OF_UNDYING,
                Material.FIREWORK_ROCKET)

            // Celestial blocks - Cosmic sky
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
                Material.END_PORTAL_FRAME)

            // Transcendent blocks - Beyond sky
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.BEDROCK, Material.VOID_AIR,
                Material.CAVE_AIR)

            // Ethereal blocks - Spirit of air
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SOUL_TORCH, Material.SOUL_LANTERN,
                Material.SOUL_CAMPFIRE)

            // Cosmic blocks - Universal air
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.STRUCTURE_VOID, Material.MOVING_PISTON,
                Material.DEBUG_STICK)

            // Infinite blocks - Endless sky
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Perfect flight
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BUNDLE, Material.TRIAL_KEY,
                Material.WIND_CHARGE)

            // Flying/Air entities progression
            .addEntity(EEvolutionRarityType.RARE, Material.GHAST_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.STRAY_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.EPIC, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PARROT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.BAT_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.LEGENDARY, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SKELETON_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.UNIQUE, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.VILLAGER_SPAWN_EGG)

            .addEntity(EEvolutionRarityType.MYTHICAL, Material.BREEZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.WIND_CHARGE)

            .addEntity(EEvolutionRarityType.DIVINE, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.DIVINE, Material.WARDEN_SPAWN_EGG)

            // Light/Glass items progression
            .addItem(EEvolutionRarityType.RARE, Material.GLASS, 10)
            .addItem(EEvolutionRarityType.RARE, Material.GLASS_BOTTLE, 5)
            .addItem(EEvolutionRarityType.RARE, Material.GLASS_PANE, 12)
            .addItem(EEvolutionRarityType.RARE, Material.GRAY_STAINED_GLASS, 10)
            .addItem(EEvolutionRarityType.RARE, Material.GRAY_STAINED_GLASS_PANE, 12)
            .addItem(EEvolutionRarityType.RARE, Material.LIGHT_GRAY_STAINED_GLASS, 12)

            .addItem(EEvolutionRarityType.EPIC, Material.WHITE_STAINED_GLASS, 16)
            .addItem(EEvolutionRarityType.EPIC, Material.WHITE_STAINED_GLASS_PANE, 32)
            .addItem(EEvolutionRarityType.EPIC, Material.GHAST_TEAR, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.FEATHER, 8)

            .addItem(EEvolutionRarityType.LEGENDARY, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.FIREWORK_ROCKET, 16)

            .addItem(EEvolutionRarityType.SPECIAL, Material.PHANTOM_MEMBRANE, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_BREATH, 2)

            .addItem(EEvolutionRarityType.UNIQUE, Material.SHULKER_SHELL, 2)
            .addItem(EEvolutionRarityType.UNIQUE, Material.END_CRYSTAL, 1)

            .addItem(EEvolutionRarityType.MYTHICAL, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.BEACON, 1)

            .addItem(EEvolutionRarityType.DIVINE, Material.TOTEM_OF_UNDYING, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.CONDUIT, 1)

            .addItem(EEvolutionRarityType.CELESTIAL, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.HEART_OF_THE_SEA, 1)

            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, 1)

            .addItem(EEvolutionRarityType.ETHEREAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ECHO_SHARD, 6)

            .addItem(EEvolutionRarityType.COSMIC, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.COSMIC, Material.CALIBRATED_SCULK_SENSOR, 1)

            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_PRECIPICE, 1)
            .addItem(EEvolutionRarityType.INFINITE, Material.MUSIC_DISC_PIGSTEP, 2)

            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.EXPERIENCE_BOTTLE, 512)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.ENCHANTED_BOOK, 100)

            // Slow falling potions - Master of air
            .addCustomItem(EEvolutionRarityType.LEGENDARY, 
                player -> EvolutionItemFactory.createSlowFallingPotion(player))

            .build();
    }
}


