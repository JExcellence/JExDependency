package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Nano Evolution - Molecular Engineering
 * Focus on nanotechnology and molecular manipulation
 * Stage 32 of 50 - Tier 7: Modern
 */
public class NanoEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Nano", 32, 110000)
            .showcase(Material.SCULK_VEIN)
            .description("The molecular revolution, where matter is manipulated atom by atom to create impossible structures")
            
            // Requirements: Cyber materials
            .requireCurrency(95000)
            .requireItem(Material.SCULK_VEIN, 32)
            .requireItem(Material.SCULK, 64)
            .requireItem(Material.REDSTONE, 256)
            .requireExperience(33)
            
            // Common blocks - Molecular components
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.SCULK_VEIN, Material.SCULK, Material.COBWEB,
                Material.STRING, Material.TRIPWIRE)

            // Uncommon blocks - Nano structures
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_CATALYST,
                Material.SCULK_SHRIEKER, Material.OBSERVER)

            // Rare blocks - Molecular machines
            .addBlocks(EEvolutionRarityType.RARE,
                Material.REDSTONE_BLOCK, Material.COMPARATOR, Material.REPEATER,
                Material.TARGET, Material.DAYLIGHT_DETECTOR)

            // Epic blocks - Nano fabrication
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.CRAFTER, Material.HOPPER, Material.DISPENSER,
                Material.DROPPER, Material.PISTON)

            // Legendary blocks - Molecular assembly
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT, Material.END_CRYSTAL,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Special blocks - Nano enhancement
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA, Material.TRIDENT,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Unique blocks - Molecular perfection
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.RECOVERY_COMPASS,
                Material.ECHO_SHARD, Material.DISC_FRAGMENT_5)

            // Mythical blocks - Nano transcendence
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.MUSIC_DISC_5, Material.GOAT_HORN, Material.TRIAL_KEY,
                Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER)

            // Divine blocks - Molecular divinity
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.VAULT, Material.OMINOUS_BOTTLE, Material.HEAVY_CORE,
                Material.MACE, Material.WIND_CHARGE)

            // Celestial blocks - Nano cosmos
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE, Material.COPPER_GRATE,
                Material.COPPER_BULB, Material.TUFF_BRICKS)

            // Transcendent blocks - Molecular transcendence
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.POLISHED_TUFF, Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS,
                Material.TUFF_STAIRS, Material.TUFF_SLAB)

            // Ethereal blocks - Nano ethereal
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE,
                Material.EXPOSED_COPPER_GRATE, Material.WEATHERED_COPPER_GRATE)

            // Cosmic blocks - Molecular cosmos
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.WAXED_OXIDIZED_COPPER_GRATE, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
                Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK)

            // Infinite blocks - Infinite molecules
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.JIGSAW, Material.BARRIER, Material.BEDROCK,
                Material.SPAWNER, Material.END_PORTAL_FRAME)

            // Omnipotent blocks - Perfect nano control
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK, Material.LIGHT,
                Material.STRUCTURE_VOID, Material.CAVE_AIR)
            
            // Nano entities
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.SILVERFISH_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDERMITE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.BEE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.VEX_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.UNIQUE, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.SHULKER_SPAWN_EGG)

            // Nano items - Molecular quantities
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 18)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_EGG, 5)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 25)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 32768)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 8)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 20)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TRIDENT, 5)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 25)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 25)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 12)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 35)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 20)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 60)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 8)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 35)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 8)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 18)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 30)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 15)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 85)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 40)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 25)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 8)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 4)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 800)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 15)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 80)
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 40)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 3500)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 17500)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 12)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 175)

            // Molecular Assembler - Ultimate nano device
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createMolecularAssembler(player))

            .build();
    }
}


