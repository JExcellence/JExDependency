package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Electric Evolution - Power and Energy
 * Focus on electrical systems and energy generation
 * Stage 27 of 50 - Tier 6: Industrial
 */
public class ElectricEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Electric", 27, 42000)
            .showcase(Material.LIGHTNING_ROD)
            .description("The age of electricity, where lightning is harnessed and power flows through copper veins across the world")
            
            // Requirements: Advanced materials from previous evolutions
            .requireCurrency(38000)
            .requireItem(Material.COPPER_INGOT, 256)
            .requireItem(Material.REDSTONE, 128)
            .requireItem(Material.GLOWSTONE_DUST, 64)
            .requireExperience(30)
            
            // Common blocks - Electrical basics
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COPPER_BLOCK, Material.LIGHTNING_ROD, Material.REDSTONE_LAMP,
                Material.DAYLIGHT_DETECTOR, Material.OBSERVER)

            // Uncommon blocks - Power systems
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.REDSTONE_BLOCK, Material.REDSTONE_TORCH, Material.REPEATER,
                Material.COMPARATOR, Material.TARGET)

            // Rare blocks - Electrical devices
            .addBlocks(EEvolutionRarityType.RARE,
                Material.DISPENSER, Material.DROPPER, Material.HOPPER,
                Material.PISTON, Material.STICKY_PISTON)

            // Epic blocks - Power generation
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.BEACON, Material.CONDUIT, Material.SEA_LANTERN,
                Material.GLOWSTONE, Material.SHROOMLIGHT)

            // Legendary blocks - Electrical marvels
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.END_CRYSTAL, Material.NETHER_STAR, Material.DRAGON_EGG,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Special blocks - High voltage
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Unique blocks - Electrical wonders
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Mythical blocks - Mythical electricity
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Divine blocks - Divine power
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Celestial blocks - Celestial energy
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Transcendent blocks - Transcendent electricity
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Ethereal blocks - Ethereal power
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Cosmic blocks - Cosmic electricity
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Infinite blocks - Infinite power
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect electricity
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)
            
            // Electric entities
            .addEntity(EEvolutionRarityType.RARE, Material.CREEPER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ALLAY_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)

            // Electric items
            .addItem(EEvolutionRarityType.COMMON, Material.COPPER_INGOT, 64)
            .addItem(EEvolutionRarityType.COMMON, Material.REDSTONE, 128)
            .addItem(EEvolutionRarityType.COMMON, Material.GLOWSTONE_DUST, 32)
            .addItem(EEvolutionRarityType.COMMON, Material.LIGHTNING_ROD, 8)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.REDSTONE_LAMP, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.DAYLIGHT_DETECTOR, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.OBSERVER, 12)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.TARGET, 6)
            
            .addItem(EEvolutionRarityType.RARE, Material.REPEATER, 16)
            .addItem(EEvolutionRarityType.RARE, Material.COMPARATOR, 16)
            .addItem(EEvolutionRarityType.RARE, Material.DISPENSER, 12)
            .addItem(EEvolutionRarityType.RARE, Material.DROPPER, 12)
            
            .addItem(EEvolutionRarityType.EPIC, Material.BEACON, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.CONDUIT, 4)
            .addItem(EEvolutionRarityType.EPIC, Material.SEA_LANTERN, 16)
            .addItem(EEvolutionRarityType.EPIC, Material.GLOWSTONE, 24)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 6)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 1024)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_BOOK, 32)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 6)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 2)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.TRIDENT, 2)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 3)
            .addItem(EEvolutionRarityType.UNIQUE, Material.NAUTILUS_SHELL, 16)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 10)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 5)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_MEMBRANE, 15)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 12)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 2)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 6)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 10)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 5)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 40)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 18)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 10)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 2)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 256)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 5)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 20)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 15)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 1500)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 7500)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 5)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 75)

            // Tesla Coil - Ultimate electrical device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createTeslaCoil(player))

            .build();
    }
}


