package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Cyber Evolution - Digital Reality
 * Focus on cybernetics and virtual worlds
 * Stage 31 of 50 - Tier 7: Modern
 */
public class CyberEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Cyber", 31, 90000)
            .showcase(Material.SCULK_SENSOR)
            .description("The cybernetic revolution, where digital and physical reality merge into a new form of existence")
            
            // Requirements: Nano materials
            .requireCurrency(80000)
            .requireItem(Material.SCULK_SENSOR, 16)
            .requireItem(Material.REDSTONE_BLOCK, 32)
            .requireItem(Material.OBSERVER, 24)
            .requireExperience(32)
            
            // Common blocks - Digital infrastructure
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.REDSTONE_BLOCK, Material.OBSERVER, Material.COMPARATOR,
                Material.REPEATER, Material.TARGET)

            // Uncommon blocks - Cyber components
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_CATALYST,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Rare blocks - Neural networks
            .addBlocks(EEvolutionRarityType.RARE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Epic blocks - Cybernetic enhancement
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Legendary blocks - Digital consciousness
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Special blocks - Virtual reality
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Unique blocks - Cyber transcendence
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Mythical blocks - Digital gods
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Divine blocks - Cyber divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.CRAFTER,
                Material.COPPER_GRATE, Material.COPPER_BULB)

            // Celestial blocks - Digital cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS)

            // Transcendent blocks - Cyber transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TUFF_SLAB, Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE)

            // Ethereal blocks - Digital ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE, Material.BARRIER,
                Material.BEDROCK, Material.SPAWNER)

            // Cosmic blocks - Cyber cosmos
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
            
            // Cyber entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDERMAN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WITHER_SPAWN_EGG)

            // Cyber items - Digital quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 12)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 4)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 18)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 16384)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 6)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 15)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 4)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 18)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 18)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 8)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 30)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 18)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 40)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 6)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 25)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 6)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 15)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 25)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 12)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 75)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 35)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 20)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 6)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 3)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 600)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 12)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 60)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 30)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 3000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 15000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 10)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 150)

            // Neural Interface - Ultimate cyber enhancement
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createNeuralInterface(player))

            .build();
    }
}


