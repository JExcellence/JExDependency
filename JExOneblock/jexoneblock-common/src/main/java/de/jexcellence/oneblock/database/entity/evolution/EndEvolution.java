package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * End Evolution - Void and Dragons
 * Focus on end materials and ultimate power
 * Stage 15 of 50 - Tier 3: Ancient
 */
public class EndEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("End", 15, 10000)
            .showcase(Material.DRAGON_EGG)
            .description("The realm beyond reality, where dragons soar through the void and ultimate power awaits")
            
            // Requirements: Nether materials from Nether evolution
            .requireCurrency(8500)
            .requireItem(Material.BLAZE_ROD, 16)
            .requireItem(Material.ENDER_PEARL, 16)
            .requireItem(Material.NETHER_BRICK, 64)
            .requireExperience(25)
            
            // Common blocks - Basic end
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.END_STONE, Material.END_STONE_BRICKS, Material.PURPUR_BLOCK,
                Material.PURPUR_PILLAR, Material.PURPUR_STAIRS)

            // Uncommon blocks - End variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER, Material.PURPUR_SLAB,
                Material.END_ROD, Material.END_STONE_BRICK_STAIRS)

            // Rare blocks - Void materials
            .addBlocks(EEvolutionRarityType.RARE,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR,
                Material.ENDER_CHEST, Material.SHULKER_BOX)

            // Epic blocks - Dragon power
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.DRAGON_HEAD, Material.DRAGON_WALL_HEAD, Material.END_CRYSTAL,
                Material.END_PORTAL_FRAME, Material.BEACON)

            // Legendary blocks - Ultimate void
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.DRAGON_EGG, Material.NETHER_STAR, Material.ELYTRA,
                Material.TOTEM_OF_UNDYING, Material.CONDUIT)

            // Special blocks - Transcendent void
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Unique blocks - Cosmic void
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Mythical blocks - Mythical void
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Divine blocks - Divine void
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Celestial blocks - Celestial void
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Transcendent blocks - Transcendent void
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Ethereal blocks - Ethereal void
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Cosmic blocks - Cosmic void
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Infinite blocks - Infinite void
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect void
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)
            
            // End entities
            .addEntity(EEvolutionRarityType.RARE, Material.ENDERMAN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.ENDERMITE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.SHULKER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PHANTOM_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)

            // End items
            .addItem(EEvolutionRarityType.COMMON, Material.END_STONE, 32)
            .addItem(EEvolutionRarityType.COMMON, Material.PURPUR_BLOCK, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.CHORUS_FRUIT, 8)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.POPPED_CHORUS_FRUIT, 6)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.END_ROD, 4)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.ENDER_PEARL, 8)
            
            .addItem(EEvolutionRarityType.RARE, Material.ENDER_EYE, 6)
            .addItem(EEvolutionRarityType.RARE, Material.BLAZE_POWDER, 8)
            .addItem(EEvolutionRarityType.RARE, Material.OBSIDIAN, 12)
            .addItem(EEvolutionRarityType.RARE, Material.SHULKER_BOX, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.END_CRYSTAL, 2)
            .addItem(EEvolutionRarityType.EPIC, Material.DRAGON_BREATH, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.EXPERIENCE_BOTTLE, 32)
            .addItem(EEvolutionRarityType.EPIC, Material.ENCHANTED_BOOK, 5)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DRAGON_HEAD, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CONDUIT, 1)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHER_STAR, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 2)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.SHULKER_SHELL, 4)
            .addItem(EEvolutionRarityType.UNIQUE, Material.PHANTOM_MEMBRANE, 6)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 1)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ECHO_SHARD, 4)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DISC_FRAGMENT_5, 1)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_SENSOR, 3)
            .addItem(EEvolutionRarityType.DIVINE, Material.CALIBRATED_SCULK_SENSOR, 1)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.TRIAL_KEY, 8)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.OMINOUS_TRIAL_KEY, 3)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.OMINOUS_BOTTLE, 2)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.MACE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.WIND_CHARGE, 16)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.BREEZE_ROD, 3)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.ARMADILLO_SCUTE, 5)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 4)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 100)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 500)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 10)

            // Void Walker's Crown - Ultimate end tool
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createVoidWalkerCrown(player))

            .build();
    }
}


