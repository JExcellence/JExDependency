package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Castle Evolution - Fortress and Defense
 * Focus on defensive structures and siege warfare
 * Stage 17 of 50 - Tier 4: Medieval
 */
public class CastleEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Castle", 17, 14000)
            .showcase(Material.STONE_BRICK_WALL)
            .description("The mighty fortress, where stone walls and iron gates protect the realm from all threats")
            
            // Requirements: Knight materials
            .requireCurrency(12000)
            .requireItem(Material.STONE_BRICKS, 256)
            .requireItem(Material.IRON_INGOT, 64)
            .requireItem(Material.COBBLESTONE, 512)
            .requireExperience(28)
            
            // Common blocks - Basic fortress materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS,
                Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS)

            // Uncommon blocks - Wall materials
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.COBBLESTONE_WALL, Material.MOSSY_COBBLESTONE_WALL, Material.STONE_BRICK_WALL,
                Material.BRICK_WALL, Material.SANDSTONE_WALL)

            // Rare blocks - Defensive structures
            .addBlocks(EEvolutionRarityType.RARE,
                Material.IRON_DOOR, Material.IRON_TRAPDOOR, Material.IRON_BARS,
                Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.TRIPWIRE_HOOK)

            // Epic blocks - Siege equipment
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.DISPENSER, Material.DROPPER, Material.TNT,
                Material.REDSTONE_BLOCK, Material.OBSERVER)

            // Legendary blocks - Royal chambers
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.BEACON, Material.CONDUIT)

            // Special blocks - Magical defenses
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.ENCHANTING_TABLE, Material.ANVIL, Material.SMITHING_TABLE,
                Material.LODESTONE, Material.RESPAWN_ANCHOR)

            // Unique blocks - Legendary fortifications
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN,
                Material.NETHER_STAR, Material.DRAGON_EGG)

            // Mythical blocks - Mythical defenses
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.END_CRYSTAL, Material.TOTEM_OF_UNDYING, Material.ELYTRA,
                Material.TRIDENT, Material.HEART_OF_THE_SEA)

            // Divine blocks - Divine fortress
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.SHULKER_SHELL, Material.PHANTOM_MEMBRANE, Material.NAUTILUS_SHELL,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD)

            // Celestial blocks - Celestial castle
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR,
                Material.SCULK_SHRIEKER, Material.SCULK_VEIN)

            // Transcendent blocks - Transcendent fortress
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_SPAWNER,
                Material.VAULT, Material.OMINOUS_BOTTLE)

            // Ethereal blocks - Ethereal defenses
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)

            // Cosmic blocks - Cosmic fortress
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CRAFTER, Material.COPPER_GRATE, Material.COPPER_BULB,
                Material.TUFF_BRICKS, Material.POLISHED_TUFF)

            // Infinite blocks - Infinite castle
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK, Material.JIGSAW)

            // Omnipotent blocks - Perfect fortress
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.BARRIER, Material.BEDROCK, Material.SPAWNER,
                Material.KNOWLEDGE_BOOK, Material.DEBUG_STICK)
            
            // Castle entities
            .addEntity(EEvolutionRarityType.RARE, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.CAT_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.PILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VINDICATOR_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.EVOKER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.RAVAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.WITHER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.SPECIAL, Material.ENDER_DRAGON_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)

            // Castle items
            .addItem(EEvolutionRarityType.COMMON, Material.COBBLESTONE, 64)
            .addItem(EEvolutionRarityType.COMMON, Material.STONE_BRICKS, 32)
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 16)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_DOOR, 4)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.IRON_BARS, 8)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.REDSTONE, 16)
            
            .addItem(EEvolutionRarityType.RARE, Material.DISPENSER, 4)
            .addItem(EEvolutionRarityType.RARE, Material.TNT, 8)
            .addItem(EEvolutionRarityType.RARE, Material.CROSSBOW, 2)
            .addItem(EEvolutionRarityType.RARE, Material.SHIELD, 2)
            
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_SWORD, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.ENCHANTED_BOOK, 8)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_SWORD, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.NETHERITE_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.BEACON, 1)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.TOTEM_OF_UNDYING, 2)
            .addItem(EEvolutionRarityType.SPECIAL, Material.EXPERIENCE_BOTTLE, 64)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.ELYTRA, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.TRIDENT, 1)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.SHULKER_BOX, 3)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ENDER_CHEST, 2)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.END_CRYSTAL, 2)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.DIVINE, Material.ECHO_SHARD, 4)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.SCULK_SENSOR, 3)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.TRIAL_KEY, 8)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.OMINOUS_TRIAL_KEY, 3)
            
            .addItem(EEvolutionRarityType.ETHEREAL, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.ETHEREAL, Material.MACE, 1)
            
            .addItem(EEvolutionRarityType.COSMIC, Material.CRAFTER, 1)
            .addItem(EEvolutionRarityType.COSMIC, Material.COPPER_BULB, 4)
            
            .addItem(EEvolutionRarityType.INFINITE, Material.ENCHANTED_BOOK, 150)
            .addItem(EEvolutionRarityType.INFINITE, Material.EXPERIENCE_BOTTLE, 750)
            
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.KNOWLEDGE_BOOK, 1)
            .addItem(EEvolutionRarityType.OMNIPOTENT, Material.BUNDLE, 12)

            // Fortress Gate Key - Ultimate defense tool
            .addCustomItem(EEvolutionRarityType.OMNIPOTENT,
                player -> EvolutionItemFactory.createFortressGateKey(player))

            .build();
    }
}


