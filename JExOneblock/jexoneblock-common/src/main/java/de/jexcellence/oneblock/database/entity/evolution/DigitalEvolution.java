package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Digital Evolution - Information Age
 * Focus on data, computation, and digital systems
 * Stage 35 of 50 - Tier 7: Modern
 */
public class DigitalEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Digital", 35, 200000)
            .showcase(Material.CALIBRATED_SCULK_SENSOR)
            .description("The information revolution, where data flows like rivers and digital consciousness awakens")
            
            // Requirements: Bio materials
            .requireCurrency(175000)
            .requireItem(Material.SCULK_CATALYST, 8)
            .requireItem(Material.CALIBRATED_SCULK_SENSOR, 4)
            .requireItem(Material.ECHO_SHARD, 16)
            .requireExperience(38)
            
            // Common blocks - Digital basics
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.REDSTONE_BLOCK, Material.OBSERVER, Material.COMPARATOR,
                Material.REPEATER, Material.TARGET)

            // Uncommon blocks - Data processing
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_CATALYST,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Rare blocks - Computing systems
            .addBlocks(EEvolutionRarityType.RARE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Epic blocks - Advanced computing
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Legendary blocks - Digital consciousness
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Special blocks - Quantum computing
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Unique blocks - AI systems
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Mythical blocks - Digital transcendence
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Divine blocks - Digital divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.CRAFTER,
                Material.COPPER_GRATE, Material.COPPER_BULB)

            // Celestial blocks - Digital cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS)

            // Transcendent blocks - Digital transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TUFF_SLAB, Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE)

            // Ethereal blocks - Digital ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE, Material.BARRIER,
                Material.BEDROCK, Material.SPAWNER)

            // Cosmic blocks - Digital cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.END_PORTAL_FRAME, Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK,
                Material.LIGHT, Material.STRUCTURE_VOID)

            // Infinite blocks - Infinite data
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CAVE_AIR, Material.VOID_AIR, Material.MOVING_PISTON,
                Material.PISTON_HEAD, Material.STICKY_PISTON)

            // Omnipotent blocks - Digital omnipotence
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.REDSTONE_WIRE, Material.FIRE, Material.SOUL_FIRE,
                Material.WATER, Material.LAVA)
            
            // Digital entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDERMAN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WITHER_SPAWN_EGG)

            // Digital items - Massive data quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 15)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 5)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 20)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 8)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 25)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 5)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 15)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 15)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 10)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 25)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 15)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 50)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 8)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 40)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 8)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 20)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 30)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 15)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 100)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 50)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 25)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 8)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 4)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 1000)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 15)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 100)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 50)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 5000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 25000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 15)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 200)

            // Quantum Processor - Ultimate digital device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createQuantumProcessor(player))

            .build();
    }
}


