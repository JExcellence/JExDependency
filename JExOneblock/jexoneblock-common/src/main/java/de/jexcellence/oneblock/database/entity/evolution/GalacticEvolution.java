package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Galactic Evolution - Galaxy Mastery
 * Focus on galactic structures and cosmic civilizations
 * Stage 42 of 50 - Tier 9: Cosmic
 */
public class GalacticEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Galactic", 42, 600000)
            .showcase(Material.END_CRYSTAL)
            .description("The galactic empire, where entire star systems bow to your cosmic will and galaxies dance to your design")
            
            // Requirements: Stellar materials
            .requireCurrency(550000)
            .requireItem(Material.NETHER_STAR, 12)
            .requireItem(Material.END_CRYSTAL, 24)
            .requireItem(Material.BEACON, 8)
            .requireExperience(48)
            
            // Common blocks - Galactic infrastructure
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.BEACON, Material.CONDUIT, Material.SEA_LANTERN,
                Material.GLOWSTONE, Material.SHROOMLIGHT)

            // Uncommon blocks - Stellar networks
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Rare blocks - Galactic cores
            .addBlocks(EEvolutionRarityType.RARE,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Epic blocks - Cosmic civilizations
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Legendary blocks - Galactic wonders
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Special blocks - Galactic phenomena
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Unique blocks - Galactic artifacts
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Mythical blocks - Galactic mysteries
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Divine blocks - Galactic divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Celestial blocks - True galactic power
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Transcendent blocks - Galactic transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Ethereal blocks - Galactic ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.END_PORTAL_FRAME, Material.KNOWLEDGE_BOOK)

            // Cosmic blocks - True galactic cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.DEBUG_STICK, Material.LIGHT, Material.STRUCTURE_VOID,
                Material.CAVE_AIR, Material.VOID_AIR)

            // Infinite blocks - Infinite galaxies
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.MOVING_PISTON, Material.PISTON_HEAD, Material.REDSTONE_WIRE,
                Material.FIRE, Material.SOUL_FIRE)

            // Omnipotent blocks - Perfect galactic mastery
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.WATER, Material.LAVA, Material.POWDER_SNOW,
                Material.AIR, Material.STICKY_PISTON)
            
            // Galactic entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.SHULKER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDERMAN_SPAWN_EGG)

            // Galactic items - Massive galactic quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 35)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 12)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 50)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 20)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 60)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 12)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 50)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 50)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 30)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 75)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 50)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 150)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 20)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 100)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 20)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 50)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 80)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 40)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 300)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 150)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 75)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 20)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 12)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 3000)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 35)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 200)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 100)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 10000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 50000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 35)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 500)

            // Galactic Command Center - Ultimate galactic control
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createGalacticCommandCenter(player))

            .build();
    }
}


