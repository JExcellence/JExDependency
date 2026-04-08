package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Universal Evolution - Cosmic Mastery
 * Focus on universal forces and reality manipulation
 * Stage 49 of 50 - Tier 9: Cosmic
 */
public class UniversalEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Universal", 49, 900000)
            .showcase(Material.COMMAND_BLOCK)
            .description("The mastery of universal forces, where the fundamental laws of reality bend to your cosmic will")
            
            // Requirements: Dimensional materials
            .requireCurrency(850000)
            .requireItem(Material.NETHER_STAR, 32)
            .requireItem(Material.DRAGON_EGG, 10)
            .requireItem(Material.ELYTRA, 8)
            .requireExperience(58)
            
            // Common blocks - Universal constants
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK)

            // Uncommon blocks - Reality fabric
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.DEBUG_STICK,
                Material.KNOWLEDGE_BOOK, Material.SPAWNER)

            // Rare blocks - Dimensional anchors
            .addBlocks(EEvolutionRarityType.RARE,
                Material.END_PORTAL_FRAME, Material.NETHER_PORTAL, Material.OBSIDIAN,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Epic blocks - Universal energy
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Legendary blocks - Cosmic artifacts
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Special blocks - Reality warpers
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Unique blocks - Universal constants
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.SCULK_CATALYST,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR)

            // Mythical blocks - Cosmic mysteries
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Divine blocks - Universal divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Celestial blocks - Universal celestial
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.CRAFTER,
                Material.COPPER_GRATE, Material.COPPER_BULB)

            // Transcendent blocks - Universal transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS)

            // Ethereal blocks - Universal ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.TUFF_SLAB, Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE)

            // Cosmic blocks - True universal power
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE, Material.LIGHT,
                Material.STRUCTURE_VOID, Material.CAVE_AIR)

            // Infinite blocks - Infinite universe
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.VOID_AIR, Material.MOVING_PISTON, Material.PISTON_HEAD,
                Material.REDSTONE_WIRE, Material.FIRE)

            // Omnipotent blocks - Universal omnipotence
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.SOUL_FIRE, Material.WATER, Material.LAVA,
                Material.POWDER_SNOW, Material.AIR)
            
            // Universal entities - All existence
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.SHULKER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ENDERMAN_SPAWN_EGG)

            // Universal items - Reality-bending quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 50)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 15)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 75)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 25)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 100)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 15)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 75)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 75)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 50)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 100)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 75)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 200)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 25)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 125)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 25)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 75)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 100)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 50)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 500)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 250)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 125)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 25)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 15)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 5000)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 50)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 300)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 150)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 15000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 75000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 50)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 1000)

            // Reality Manipulator - Ultimate universal power
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createRealityManipulator(player))

            .build();
    }
}


