package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Explorer Evolution - Age of Exploration
 * Focus on exploration tools, maps, and discovery
 * Stage 21 of 50 - Tier 5: Renaissance
 */
public class ExplorerEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Explorer", 21, 15000)
            .showcase(Material.COMPASS)
            .description("The spirit of discovery, where brave souls venture into unknown lands seeking new horizons")
            
            // Requirements: Crusader materials
            .requireCurrency(13000)
            .requireItem(Material.COMPASS, 4)
            .requireItem(Material.MAP, 8)
            .requireItem(Material.LEATHER, 32)
            .requireExperience(28)
            
            // Common blocks - Travel materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
                Material.COBBLESTONE, Material.STONE_BRICKS)

            // Uncommon blocks - Navigation
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.COMPASS, Material.CLOCK, Material.SPYGLASS,
                Material.CARTOGRAPHY_TABLE, Material.MAP)

            // Rare blocks - Exploration gear
            .addBlocks(EEvolutionRarityType.RARE,
                Material.LEATHER, Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS)

            // Epic blocks - Advanced exploration
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.ENDER_PEARL, Material.ENDER_EYE, Material.BLAZE_ROD,
                Material.MAGMA_CREAM, Material.GHAST_TEAR)

            // Legendary blocks - Rare discoveries
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.GOLD_ORE,
                Material.LAPIS_ORE, Material.REDSTONE_ORE)

            // Special blocks - Legendary finds
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP)

            // Unique blocks - Ultimate discoveries
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.BEACON, Material.CONDUIT, Material.HEART_OF_THE_SEA)

            // Mythical blocks - Mythical discoveries
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.NETHER_STAR, Material.DRAGON_EGG, Material.ELYTRA)

            // Divine blocks - Divine exploration
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.TOTEM_OF_UNDYING, Material.TRIDENT, Material.NAUTILUS_SHELL)

            // Celestial blocks - Cosmic exploration
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.END_CRYSTAL, Material.CHORUS_FRUIT, Material.SHULKER_SHELL)

            // Transcendent blocks - Beyond exploration
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.RECOVERY_COMPASS, Material.ECHO_SHARD)

            // Ethereal blocks - Spirit of exploration
            .addBlocks(EEvolutionRarityType.ETHEREAL,
                Material.SCULK_CATALYST, Material.SCULK_SENSOR)

            // Cosmic blocks - Universal exploration
            .addBlocks(EEvolutionRarityType.COSMIC,
                Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SHRIEKER)

            // Infinite blocks - Endless exploration
            .addBlocks(EEvolutionRarityType.INFINITE,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY)

            // Omnipotent blocks - Perfect exploration
            .addBlocks(EEvolutionRarityType.OMNIPOTENT,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE)
            
            // Explorer entities
            .addEntity(EEvolutionRarityType.RARE, Material.HORSE_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.DONKEY_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.LLAMA_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.WANDERING_TRADER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.VILLAGER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PARROT_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.OCELOT_SPAWN_EGG)

            // Explorer items
            .addItem(EEvolutionRarityType.RARE, Material.COMPASS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.MAP, 3)
            .addItem(EEvolutionRarityType.RARE, Material.LEAD, 4)
            .addItem(EEvolutionRarityType.RARE, Material.SADDLE, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.SPYGLASS, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.BUNDLE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.ENDER_PEARL, 3)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND, 5)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EMERALD, 8)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.GOLD_INGOT, 12)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHERITE_INGOT, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.ANCIENT_DEBRIS, 2)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.CONDUIT, 1)

            // Explorer's compass - Ultimate navigation tool
            .addCustomItem(EEvolutionRarityType.UNIQUE,
                player -> EvolutionItemFactory.createExplorerCompass(player))

            .build();
    }
}


