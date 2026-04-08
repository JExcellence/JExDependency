package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Stellar Evolution - Star Power
 * Focus on stellar energy and cosmic forces
 * Stage 41 of 50 - Tier 9: Cosmic
 */
public class StellarEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Stellar", 41, 500000)
            .showcase(Material.BEACON)
            .description("The power of stars themselves, where stellar fusion and cosmic radiation forge the elements of creation")
            
            // Requirements: Galactic materials
            .requireCurrency(450000)
            .requireItem(Material.NETHER_STAR, 8)
            .requireItem(Material.BEACON, 4)
            .requireItem(Material.END_CRYSTAL, 16)
            .requireExperience(45)
            
            // Common blocks - Stellar materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GLOWSTONE, Material.SHROOMLIGHT, Material.SEA_LANTERN,
                Material.BEACON, Material.CONDUIT)

            // Uncommon blocks - Solar energy
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS)

            // Rare blocks - Stellar cores
            .addBlocks(EEvolutionRarityType.RARE,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Epic blocks - Stellar phenomena
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Legendary blocks - Star formation
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Special blocks - Stellar collapse
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Unique blocks - Neutron stars
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Mythical blocks - Stellar remnants
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Divine blocks - Divine stars
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Celestial blocks - True stellar power
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Transcendent blocks - Stellar transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Ethereal blocks - Stellar ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Cosmic blocks - Stellar cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.END_PORTAL_FRAME, Material.KNOWLEDGE_BOOK)

            // Infinite blocks - Infinite stars
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.DEBUG_STICK, Material.LIGHT, Material.STRUCTURE_VOID,
                Material.CAVE_AIR, Material.VOID_AIR)

            // Omnipotent blocks - Perfect stellar mastery
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.MOVING_PISTON, Material.PISTON_HEAD, Material.REDSTONE_WIRE,
                Material.FIRE, Material.SOUL_FIRE)
            
            // Stellar entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.BLAZE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.GHAST_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.MAGMA_CUBE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)

            // Stellar items - Massive stellar quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 25)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 8)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 35)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 15)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 40)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 8)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 30)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 30)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 20)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 50)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 30)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 100)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 15)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 75)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 15)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 40)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 60)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 30)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 200)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 100)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 50)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 15)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 8)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 2000)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 25)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 150)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 75)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 8000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 40000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 25)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 400)

            // Stellar Forge - Ultimate star power
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createStellarForge(player))

            .build();
    }
}


