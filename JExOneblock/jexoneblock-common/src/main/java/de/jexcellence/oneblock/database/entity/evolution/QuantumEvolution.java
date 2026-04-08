package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Quantum Evolution - Quantum Mechanics
 * Focus on quantum physics and probability manipulation
 * Stage 34 of 50 - Tier 7: Modern
 */
public class QuantumEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Quantum", 34, 150000)
            .showcase(Material.HEAVY_CORE)
            .description("The quantum realm, where probability becomes reality and the observer shapes the universe")
            
            // Requirements: Bio materials
            .requireCurrency(135000)
            .requireItem(Material.ENDER_PEARL, 64)
            .requireItem(Material.ENDER_EYE, 32)
            .requireItem(Material.END_ROD, 16)
            .requireExperience(36)
            
            // Common blocks - Quantum states
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OBSERVER, Material.COMPARATOR, Material.REPEATER,
                Material.TARGET, Material.DAYLIGHT_DETECTOR)

            // Uncommon blocks - Quantum mechanics
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.REDSTONE_BLOCK, Material.REDSTONE_LAMP, Material.LIGHTNING_ROD,
                Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SENSOR)

            // Rare blocks - Quantum entanglement
            .addBlocks(EEvolutionRarityType.RARE,
                Material.ENDER_PEARL, Material.ENDER_EYE, Material.END_ROD,
                Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT)

            // Epic blocks - Quantum tunneling
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.END_CRYSTAL, Material.BEACON, Material.CONDUIT,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Legendary blocks - Quantum superposition
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Special blocks - Quantum collapse
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Unique blocks - Quantum consciousness
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.SCULK_CATALYST,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Mythical blocks - Quantum mythology
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Divine blocks - Quantum divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Celestial blocks - Quantum cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Transcendent blocks - Quantum transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Ethereal blocks - Quantum ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Cosmic blocks - Quantum cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Infinite blocks - Infinite quantum
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.END_PORTAL_FRAME, Material.KNOWLEDGE_BOOK)

            // Omnipotent blocks - Perfect quantum control
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.DEBUG_STICK, Material.LIGHT, Material.STRUCTURE_VOID,
                Material.CAVE_AIR, Material.VOID_AIR)
            
            // Quantum entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDERMAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PHANTOM_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.VEX_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ALLAY_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ENDER_DRAGON_SPAWN_EGG)

            // Quantum items - Probability quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 28)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 7)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 40)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 131072)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 12)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 30)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 8)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 40)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 40)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 20)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 50)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 30)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 100)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 12)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 55)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 12)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 28)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 45)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 22)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 110)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 55)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 35)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 12)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 6)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 1200)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 22)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 120)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 60)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 5000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 25000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 18)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 250)

            // Probability Engine - Ultimate quantum device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createProbabilityEngine(player))

            .build();
    }
}


