package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Cosmic Evolution - Universal Power
 * Focus on cosmic materials and universal forces
 * Stage 45 of 50 - Tier 9: Cosmic
 */
public class CosmicEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Cosmic", 45, 750000)
            .showcase(Material.END_CRYSTAL)
            .description("The power of the cosmos, where universal forces bend to your will and reality itself becomes malleable")
            
            // Requirements: Infinity materials
            .requireCurrency(700000)
            .requireItem(Material.NETHER_STAR, 20)
            .requireItem(Material.DRAGON_EGG, 5)
            .requireItem(Material.TOTEM_OF_UNDYING, 32)
            .requireExperience(52)
            
            // Common blocks - Cosmic basics
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_STONE,
                Material.PURPUR_BLOCK, Material.END_STONE_BRICKS)

            // Uncommon blocks - Stellar materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.LODESTONE,
                Material.RESPAWN_ANCHOR, Material.GLOWSTONE)

            // Rare blocks - Galactic power
            .addBlocks(EEvolutionRarityType.RARE,
                Material.BEACON, Material.CONDUIT, Material.NETHER_STAR,
                Material.DRAGON_EGG, Material.END_CRYSTAL)

            // Epic blocks - Universal energy
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.ELYTRA, Material.TOTEM_OF_UNDYING, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Legendary blocks - Cosmic artifacts
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.DRAGON_BREATH,
                Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT)

            // Special blocks - Stellar phenomena
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Unique blocks - Galactic wonders
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Mythical blocks - Universal mysteries
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Divine blocks - Cosmic divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Celestial blocks - Celestial mastery
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Transcendent blocks - Reality transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Ethereal blocks - Ethereal cosmos
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Cosmic blocks - Pure cosmic energy
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Infinite blocks - Infinite cosmos
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.END_PORTAL_FRAME, Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Universal omnipotence
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.DEBUG_STICK, Material.LIGHT, Material.STRUCTURE_VOID,
                Material.CAVE_AIR, Material.VOID_AIR)
            
            // Cosmic entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.SHULKER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDERMAN_SPAWN_EGG)

            // Cosmic items - Ultimate cosmic rewards
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 10)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 3)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 8)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 3)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 10)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 2)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 5)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 5)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 3)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 10)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 5)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 15)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 3)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 15)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 3)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 8)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 12)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 5)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 50)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 20)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 10)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 3)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 2)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 128)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 5)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 20)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 10)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 1000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 5000)
            .addItem(EEvolutionRarityType.INFINITE, Material.BUNDLE, 50)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 5)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.DEBUG_STICK, 1)

            // Cosmic Singularity - Ultimate cosmic power
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createCosmicSingularity(player))

            .build();
    }
}


