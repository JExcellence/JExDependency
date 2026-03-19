package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Dimensional Evolution - Reality Layers
 * Focus on dimensional travel and multiverse mastery
 * Stage 48 of 50 - Tier 9: Cosmic
 */
public class DimensionalEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Dimensional", 48, 850000)
            .showcase(Material.END_PORTAL_FRAME)
            .description("The mastery of dimensions, where infinite realities converge and the multiverse bends to your will")
            
            // Requirements: Multiverse materials
            .requireCurrency(800000)
            .requireItem(Material.END_PORTAL_FRAME, 12)
            .requireItem(Material.ENDER_EYE, 64)
            .requireItem(Material.OBSIDIAN, 256)
            .requireExperience(56)
            
            // Common blocks - Dimensional anchors
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.END_PORTAL_FRAME, Material.NETHER_PORTAL, Material.OBSIDIAN,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Uncommon blocks - Reality fabric
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Rare blocks - Dimensional gates
            .addBlocks(EEvolutionRarityType.RARE,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)

            // Epic blocks - Reality warpers
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Legendary blocks - Dimensional artifacts
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Special blocks - Multiverse keys
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Unique blocks - Reality controllers
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.SCULK_CATALYST,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR)

            // Mythical blocks - Dimensional mysteries
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Divine blocks - Dimensional divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Celestial blocks - Dimensional cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.CRAFTER,
                Material.COPPER_GRATE, Material.COPPER_BULB)

            // Transcendent blocks - True dimensional power
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.CHISELED_TUFF,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS)

            // Ethereal blocks - Dimensional ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.TUFF_SLAB, Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE)

            // Cosmic blocks - Dimensional cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE, Material.LIGHT,
                Material.STRUCTURE_VOID, Material.CAVE_AIR)

            // Infinite blocks - Infinite dimensions
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.VOID_AIR, Material.MOVING_PISTON, Material.PISTON_HEAD,
                Material.REDSTONE_WIRE, Material.FIRE)

            // Omnipotent blocks - Perfect dimensional mastery
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.SOUL_FIRE, Material.WATER, Material.LAVA,
                Material.POWDER_SNOW, Material.AIR)
            
            // Dimensional entities - Beings from all realities
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.SHULKER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.MYTHICAL, Material.ENDERMAN_SPAWN_EGG)

            // Dimensional items - Reality-bending quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 45)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 18)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 90)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 30)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 120)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 18)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 90)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 90)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 60)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 120)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 90)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 240)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 30)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 150)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 30)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 90)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 120)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 60)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 750)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 375)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 180)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 30)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 18)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 7500)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 60)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 400)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 200)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 18000)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 90000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 60)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 900)

            // Multiverse Gateway - Ultimate dimensional device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createMultiverseGateway(player))

            .build();
    }
}


