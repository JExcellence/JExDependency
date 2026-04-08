package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Knight Evolution - Chivalry and Honor
 * Focus on armor, weapons, and medieval warfare
 * Stage 16 of 50 - Tier 4: Medieval
 */
public class KnightEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Knight", 16, 12000)
            .showcase(Material.NETHERITE_CHESTPLATE)
            .description("The code of chivalry, where honor and steel forge the protectors of the realm")
            
            // Requirements: End materials
            .requireCurrency(10000)
            .requireItem(Material.IRON_INGOT, 64)
            .requireItem(Material.DIAMOND, 16)
            .requireItem(Material.LEATHER, 32)
            .requireExperience(26)
            
            // Common blocks - Basic armor materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON_BLOCK,
                Material.IRON_BLOCK, Material.IRON_BARS)

            // Uncommon blocks - Weapon materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.SMITHING_TABLE, Material.GRINDSTONE)

            // Rare blocks - Advanced metallurgy
            .addBlocks(EEvolutionRarityType.RARE,
                Material.IRON_BARS, Material.LANTERN, Material.SOUL_LANTERN,
                Material.CAULDRON, Material.HOPPER)

            // Epic blocks - Fortress materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
                Material.CHISELED_STONE_BRICKS, Material.STONE_BRICK_STAIRS)

            // Legendary blocks - Castle architecture
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.COBBLESTONE_WALL, Material.MOSSY_COBBLESTONE_WALL, Material.BRICK_WALL,
                Material.STONE_BRICK_WALL, Material.IRON_DOOR)

            // Special blocks - Royal materials
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.BEACON, Material.CONDUIT)

            // Unique blocks - Legendary artifacts
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.LODESTONE,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Mythical blocks - Mythical weapons
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.ENCHANTING_TABLE, Material.BOOKSHELF, Material.LECTERN,
                Material.EXPERIENCE_BOTTLE, Material.ENCHANTED_BOOK)

            // Divine blocks - Divine armaments
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.END_CRYSTAL,
                Material.TOTEM_OF_UNDYING, Material.ELYTRA)

            // Celestial blocks - Celestial armor
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA,
                Material.NAUTILUS_SHELL, Material.TRIDENT)

            // Transcendent blocks - Transcendent weapons
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Ethereal blocks - Ethereal equipment
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Cosmic blocks - Cosmic armaments
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Infinite blocks - Infinite chivalry
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Omnipotent blocks - Perfect knighthood
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)
            
            // Medieval entities
            .addEntity(EEvolutionRarityType.RARE, Material.HORSE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.IRON_GOLEM_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.SKELETON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ZOMBIE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.PILLAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.RAVAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.VINDICATOR_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WITHER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)

            // Knight equipment
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_SWORD, 1)
            .addItem(EEvolutionRarityType.COMMON, Material.SHIELD, 1)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_HELMET, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_BOOTS, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND_SWORD, 1)
            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND_HELMET, 1)
            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.BOW, 1)
            .addItem(EEvolutionRarityType.RARE, Material.ARROW, 64)
            
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_BOOTS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.CROSSBOW, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.SADDLE, 1)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_SWORD, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_HELMET, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_BOOK, 8)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 32)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_BOOTS, 1)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.TOTEM_OF_UNDYING, 2)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.TRIDENT, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DRAGON_HEAD, 1)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.END_CRYSTAL, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.SHULKER_BOX, 3)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.ECHO_SHARD, 5)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SCULK_SENSOR, 4)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.TRIAL_KEY, 10)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.OMINOUS_TRIAL_KEY, 4)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 200)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 1000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 15)

            // Excalibur - The legendary sword
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createExcalibur(player))

            .build();
    }
}


