package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Eden Evolution - Paradise and Rebirth
 * The ultimate evolution with prestige reset mechanics
 * Stage 50 of 50 - Tier 10: Eternal (RESETTABLE)
 */
public class EdenEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Eden", 50, 1000000)
            .showcase(Material.ENCHANTED_GOLDEN_APPLE)
            .description("The garden of paradise, where endings become beginnings and true mastery is achieved through rebirth")
            
            // Requirements: Universal materials - THE FINAL CHALLENGE
            .requireCurrency(950000)
            .requireItem(Material.NETHER_STAR, 64)
            .requireItem(Material.DRAGON_EGG, 16)
            .requireItem(Material.ELYTRA, 8)
            .requireItem(Material.TOTEM_OF_UNDYING, 32)
            .requireExperience(60)
            
            // Common blocks - Paradise basics
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GRASS_BLOCK, Material.FLOWERING_AZALEA_LEAVES, Material.AZALEA_LEAVES,
                Material.MOSS_BLOCK, Material.MOSS_CARPET)

            // Uncommon blocks - Garden materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.CHERRY_LOG, Material.CHERRY_PLANKS, Material.CHERRY_LEAVES,
                Material.PINK_PETALS, Material.TORCHFLOWER)

            // Rare blocks - Paradise flora
            .addBlocks(EEvolutionRarityType.RARE,
                Material.SPORE_BLOSSOM, Material.GLOW_BERRIES, Material.SWEET_BERRIES,
                Material.FLOWERING_AZALEA, Material.AZALEA)

            // Epic blocks - Divine materials
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.LAPIS_BLOCK, Material.REDSTONE_BLOCK)

            // Legendary blocks - Sacred structures
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.CONDUIT, Material.ENCHANTING_TABLE,
                Material.ANVIL, Material.SMITHING_TABLE)

            // Special blocks - Paradise power
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.LODESTONE,
                Material.RESPAWN_ANCHOR, Material.CRYING_OBSIDIAN)

            // Unique blocks - Ultimate paradise
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.ELYTRA,
                Material.TOTEM_OF_UNDYING, Material.TRIDENT)

            // Mythical blocks - Mythical paradise
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.END_CRYSTAL, Material.CHORUS_FRUIT, Material.SHULKER_SHELL,
                Material.PHANTOM_MEMBRANE, Material.HEART_OF_THE_SEA)

            // Divine blocks - Divine paradise
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.GOAT_HORN)

            // Celestial blocks - Celestial paradise
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Transcendent blocks - Transcendent paradise
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Ethereal blocks - Ethereal paradise
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Cosmic blocks - Cosmic paradise
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Infinite blocks - Infinite paradise
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect paradise
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.END_PORTAL_FRAME,
                Material.SPAWNER, Material.KNOWLEDGE_BOOK)
            
            // Paradise entities - All creatures in harmony
            .addEntity(EEvolutionRarityType.RARE, Material.ALLAY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.AXOLOTL_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.BEE_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.IRON_GOLEM_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ELDER_GUARDIAN_SPAWN_EGG)

            // Paradise items - Ultimate rewards
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_GOLDEN_APPLE, 5)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.GOLDEN_APPLE, 10)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EXPERIENCE_BOTTLE, 64)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_SWORD, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_PICKAXE, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_AXE, 1)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHER_STAR, 3)
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 2)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 2)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ELYTRA, 2)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.TOTEM_OF_UNDYING, 5)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.TRIDENT, 1)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.DRAGON_HEAD, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.DRAGON_BREATH, 10)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SHULKER_BOX, 5)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.ENDER_CHEST, 3)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 5)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 2)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.TRIAL_KEY, 10)
            .addItem(EEvolutionRarityType.COSMIC, Material.OMINOUS_TRIAL_KEY, 5)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 100)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 1000)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 10)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)

            // Eden's Fruit - The ultimate prestige item
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createEdenFruit(player))

            // Tree of Life - Prestige reset item
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createTreeOfLife(player))

            .build();
    }
}


