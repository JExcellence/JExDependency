package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Factory Evolution - Mass Production
 * Focus on automation and mass manufacturing
 * Stage 28 of 50 - Tier 6: Industrial
 */
public class FactoryEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Factory", 28, 50000)
            .showcase(Material.CRAFTER)
            .description("The age of mass production, where automated factories churn out goods at unprecedented scales")
            
            // Requirements: Electric materials
            .requireCurrency(45000)
            .requireItem(Material.IRON_INGOT, 256)
            .requireItem(Material.REDSTONE, 256)
            .requireItem(Material.HOPPER, 32)
            .requireExperience(32)
            
            // Common blocks - Factory basics
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.CRAFTER, Material.HOPPER, Material.CHEST,
                Material.BARREL, Material.SHULKER_BOX)

            // Uncommon blocks - Production lines
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.DISPENSER, Material.DROPPER, Material.OBSERVER,
                Material.PISTON, Material.STICKY_PISTON)

            // Rare blocks - Automation systems
            .addBlocks(EEvolutionRarityType.RARE,
                Material.REDSTONE_BLOCK, Material.COMPARATOR, Material.REPEATER,
                Material.DAYLIGHT_DETECTOR, Material.TRIPWIRE_HOOK)

            // Epic blocks - Advanced manufacturing
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.BEACON, Material.CONDUIT, Material.LIGHTNING_ROD,
                Material.TARGET, Material.BELL)

            // Legendary blocks - Industrial marvels
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Special blocks - Factory power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Unique blocks - Revolutionary production
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Mythical blocks - Mythical factories
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Divine blocks - Divine manufacturing
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Celestial blocks - Celestial production
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Transcendent blocks - Transcendent factories
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.COPPER_GRATE, Material.COPPER_BULB, Material.TUFF_BRICKS,
                Material.POLISHED_TUFF, Material.CHISELED_TUFF)

            // Ethereal blocks - Ethereal manufacturing
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS, Material.TUFF_SLAB,
                Material.TUFF_WALL, Material.OXIDIZED_COPPER_GRATE)

            // Cosmic blocks - Cosmic factories
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE, Material.WEATHERED_COPPER_GRATE,
                Material.WAXED_OXIDIZED_COPPER_GRATE, Material.COMMAND_BLOCK)

            // Infinite blocks - Infinite production
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK,
                Material.JIGSAW, Material.BARRIER)

            // Omnipotent blocks - Perfect manufacturing
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BEDROCK, Material.SPAWNER, Material.KNOWLEDGE_BOOK,
                Material.DEBUG_STICK, Material.LIGHT)
            
            // Factory entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ALLAY_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.RAVAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)

            // Factory items - Mass production quantities
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 128)
            .addItem(EEvolutionRarityType.COMMON, Material.COPPER_INGOT, 96)
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_INGOT, 64)
            .addItem(EEvolutionRarityType.COMMON, Material.REDSTONE, 192)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.CRAFTER, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.HOPPER, 32)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.CHEST, 24)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.BARREL, 16)
            
            .addItem(EEvolutionRarityType.RARE, Material.DISPENSER, 24)
            .addItem(EEvolutionRarityType.RARE, Material.DROPPER, 24)
            .addItem(EEvolutionRarityType.RARE, Material.OBSERVER, 16)
            .addItem(EEvolutionRarityType.RARE, Material.PISTON, 20)
            
            .addItem(EEvolutionRarityType.EPIC, Material.SHULKER_BOX, 12)
            .addItem(EEvolutionRarityType.EPIC, Material.ENDER_CHEST, 6)
            .addItem(EEvolutionRarityType.EPIC, Material.BEACON, 5)
            .addItem(EEvolutionRarityType.EPIC, Material.CONDUIT, 5)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHER_STAR, 5)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.END_CRYSTAL, 8)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 2048)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_BOOK, 48)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 8)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ELYTRA, 3)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.TRIDENT, 2)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 4)
            .addItem(EEvolutionRarityType.UNIQUE, Material.NAUTILUS_SHELL, 20)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_SHELL, 16)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_MEMBRANE, 20)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.RECOVERY_COMPASS, 3)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 15)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 3)
            .addItem(EEvolutionRarityType.DIVINE, Material.MUSIC_DISC_5, 2)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 8)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 12)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.CALIBRATED_SCULK_SENSOR, 6)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 45)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 20)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_BOTTLE, 12)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 3)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 2)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.WIND_CHARGE, 320)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.BREEZE_ROD, 18)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 24)
            .addItem(EEvolutionRarityType.COSMIC, Material.ARMADILLO_SCUTE, 16)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 1800)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 9000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 6)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 90)

            // Assembly Line Controller - Ultimate factory automation
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createAssemblyLineController(player))

            .build();
    }
}


