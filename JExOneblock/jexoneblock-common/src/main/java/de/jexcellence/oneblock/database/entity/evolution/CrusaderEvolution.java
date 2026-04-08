package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Crusader Evolution - Holy War and Faith
 * Focus on religious warfare and divine power
 * Stage 20 of 50 - Tier 4: Medieval
 */
public class CrusaderEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Crusader", 20, 20000)
            .showcase(Material.GOLDEN_SWORD)
            .description("The holy crusade, where faith and steel unite in righteous battle against the forces of darkness")
            
            // Requirements: Explorer materials
            .requireCurrency(17500)
            .requireItem(Material.GOLD_INGOT, 64)
            .requireItem(Material.QUARTZ, 128)
            .requireItem(Material.WHITE_WOOL, 64)
            .requireExperience(30)
            
            // Common blocks - Holy materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GOLD_BLOCK, Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ,
                Material.CHISELED_QUARTZ_BLOCK, Material.QUARTZ_PILLAR)

            // Uncommon blocks - Sacred architecture
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.WHITE_WOOL, Material.WHITE_CONCRETE, Material.WHITE_TERRACOTTA,
                Material.WHITE_STAINED_GLASS, Material.WHITE_BANNER)

            // Rare blocks - Divine symbols
            .addBlocks(EEvolutionRarityType.RARE,
                Material.BEACON, Material.CONDUIT, Material.BELL,
                Material.LECTERN, Material.ENCHANTING_TABLE)

            // Epic blocks - Holy relics
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING,
                Material.EXPERIENCE_BOTTLE, Material.ENCHANTED_BOOK)

            // Legendary blocks - Sacred artifacts
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Special blocks - Divine intervention
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.ELYTRA, Material.TRIDENT, Material.SHULKER_SHELL,
                Material.PHANTOM_MEMBRANE, Material.DRAGON_BREATH)

            // Unique blocks - Heavenly power
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Mythical blocks - Mythical faith
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Divine blocks - Divine crusade
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Celestial blocks - Celestial warriors
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Transcendent blocks - Transcendent faith
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Ethereal blocks - Ethereal crusade
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS, Material.TUFF_STAIRS,
                Material.TUFF_SLAB, Material.TUFF_WALL)

            // Cosmic blocks - Cosmic faith
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.EXPOSED_COPPER_GRATE,
                Material.WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE)

            // Infinite blocks - Infinite crusade
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect faith
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)
            
            // Holy entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.HORSE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.PILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VINDICATOR_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.RAVAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)

            // Crusader items
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_INGOT, 24)
            .addItem(EEvolutionRarityType.COMMON, Material.QUARTZ, 32)
            .addItem(EEvolutionRarityType.COMMON, Material.WHITE_WOOL, 16)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_SWORD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_HELMET, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.SHIELD, 2)
            
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_BOOTS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.BOW, 2)
            .addItem(EEvolutionRarityType.RARE, Material.CROSSBOW, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.GOLDEN_APPLE, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.EXPERIENCE_BOTTLE, 64)
            .addItem(EEvolutionRarityType.EPIC, Material.ENCHANTED_BOOK, 12)
            .addItem(EEvolutionRarityType.EPIC, Material.BELL, 2)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_GOLDEN_APPLE, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BEACON, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.CONDUIT, 2)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.TOTEM_OF_UNDYING, 3)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHER_STAR, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.END_CRYSTAL, 3)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.TRIDENT, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 1)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 4)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 2)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.PHANTOM_MEMBRANE, 8)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 5)
            .addItem(EEvolutionRarityType.DIVINE, Material.DISC_FRAGMENT_5, 1)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 3)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 4)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 15)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 6)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.WIND_CHARGE, 48)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 400)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 2000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 25)

            // Holy Grail - Ultimate crusader artifact
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createHolyGrail(player))

            .build();
    }
}


