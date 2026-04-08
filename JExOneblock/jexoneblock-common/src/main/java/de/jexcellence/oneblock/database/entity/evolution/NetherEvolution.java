package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Nether Evolution - Infernal Realms
 * Focus on nether materials and hellish power
 * Stage 14 of 50 - Tier 3: Ancient
 */
public class NetherEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Nether", 14, 7500)
            .showcase(Material.NETHER_PORTAL)
            .description("The gateway to hell, where infernal flames forge weapons of unimaginable power")
            
            // Requirements: Diamond gear + gold from previous evolutions
            .requireCurrency(6500)
            .requireItem(Material.DIAMOND, 16)
            .requireItem(Material.GOLD_INGOT, 32)
            .requireItem(Material.OBSIDIAN, 10)
            .requireExperience(20)
            
            // Common blocks - Basic nether
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
                Material.NETHER_BRICK_FENCE, Material.NETHER_BRICK_STAIRS)

            // Uncommon blocks - Nether variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.SOUL_SAND, Material.SOUL_SOIL, Material.BASALT,
                Material.SMOOTH_BASALT, Material.POLISHED_BASALT)

            // Rare blocks - Nether ores
            .addBlocks(EEvolutionRarityType.RARE,
                Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.BLACKSTONE,
                Material.POLISHED_BLACKSTONE, Material.GILDED_BLACKSTONE)

            // Epic blocks - Infernal materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.MAGMA_BLOCK, Material.GLOWSTONE, Material.SHROOMLIGHT,
                Material.WARPED_STEM, Material.CRIMSON_STEM)

            // Legendary blocks - Hellish power
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.ANCIENT_DEBRIS, Material.NETHERITE_BLOCK, Material.LODESTONE,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Special blocks - Portal magic
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.NETHER_PORTAL, Material.END_PORTAL_FRAME, Material.OBSIDIAN,
                Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE)

            // Unique blocks - Infernal artifacts
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.BEACON, Material.CONDUIT, Material.NETHER_STAR,
                Material.WITHER_SKELETON_SKULL, Material.SKELETON_SKULL)

            // Mythical blocks - Demonic power
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.DRAGON_HEAD, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.TRIDENT)

            // Divine blocks - Hellish divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.ELYTRA, Material.SHULKER_SHELL, Material.CHORUS_FRUIT,
                Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA)

            // Celestial blocks - Infernal cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Transcendent blocks - Beyond hell
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Ethereal blocks - Ethereal flames
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Cosmic blocks - Cosmic inferno
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Infinite blocks - Infinite hellfire
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Omnipotent blocks - Ultimate inferno
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)
            
            // Nether entities
            .addEntity(EEvolutionRarityType.RARE, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PIGLIN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.HOGLIN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.GHAST_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.MAGMA_CUBE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PIGLIN_BRUTE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.STRIDER_SPAWN_EGG)

            // Nether items
            .addItem(EEvolutionRarityType.COMMON, Material.NETHER_BRICK, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.QUARTZ, 12)
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_NUGGET, 24)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.SOUL_TORCH, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.SOUL_LANTERN, 4)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.MAGMA_CREAM, 6)
            
            .addItem(EEvolutionRarityType.RARE, Material.BLAZE_POWDER, 8)
            .addItem(EEvolutionRarityType.RARE, Material.BLAZE_ROD, 4)
            .addItem(EEvolutionRarityType.RARE, Material.GHAST_TEAR, 3)
            .addItem(EEvolutionRarityType.RARE, Material.GLOWSTONE_DUST, 12)
            
            .addItem(EEvolutionRarityType.EPIC, Material.FIRE_CHARGE, 6)
            .addItem(EEvolutionRarityType.EPIC, Material.GUNPOWDER, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.ENDER_PEARL, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.WARPED_FUNGUS, 3)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ANCIENT_DEBRIS, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_SCRAP, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.WITHER_SKELETON_SKULL, 1)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_INGOT, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_SWORD, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_PICKAXE, 1)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.ENCHANTED_GOLDEN_APPLE, 2)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.TOTEM_OF_UNDYING, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DRAGON_BREATH, 5)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.SHULKER_BOX, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.ENDER_CHEST, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.END_CRYSTAL, 1)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.ECHO_SHARD, 3)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SCULK_CATALYST, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SCULK_SENSOR, 2)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.TRIAL_KEY, 3)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.OMINOUS_TRIAL_KEY, 1)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 50)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 100)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 5)

            // Infernal Portal Key - Ultimate nether tool
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createInfernalPortalKey(player))

            .build();
    }
}


