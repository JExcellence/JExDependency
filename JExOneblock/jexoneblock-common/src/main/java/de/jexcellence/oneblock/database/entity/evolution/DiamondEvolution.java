package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Diamond Evolution - Precious Gems and Mastery
 * Focus on diamond materials and advanced tools
 * Stage 13 of 50 - Tier 3: Ancient
 */
public class DiamondEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Diamond", 13, 5500)
            .showcase(Material.DIAMOND_BLOCK)
            .description("The pinnacle of craftsmanship, where diamonds forge the finest tools and armor")
            
            // Requirements for this evolution
            .requireCurrency(5000)
            .requireExperience(15)
            .requireItem(Material.IRON_INGOT, 32)
            .requireItem(Material.GOLD_INGOT, 16)
            
            // Common blocks - Basic gems
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND_BLOCK,
                Material.COAL_ORE, Material.IRON_ORE)

            // Uncommon blocks - Gem variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_BLOCK,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE)

            // Rare blocks - Precious stones
            .addBlocks(EEvolutionRarityType.RARE,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD_BLOCK,
                Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST)

            // Epic blocks - Crystal formations
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD,
                Material.AMETHYST_CLUSTER, Material.CALCITE)

            // Legendary blocks - Master tools
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.ENCHANTING_TABLE, Material.ANVIL, Material.SMITHING_TABLE,
                Material.GRINDSTONE, Material.STONECUTTER)

            // Special blocks - Ultimate gems
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.BEACON, Material.CONDUIT, Material.LODESTONE,
                Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR)

            // Unique blocks - Legendary crystals
            .addBlocks(EEvolutionRarityType.UNIQUE,
                Material.END_CRYSTAL, Material.DRAGON_EGG, Material.NETHER_STAR,
                Material.HEART_OF_THE_SEA, Material.NAUTILUS_SHELL)

            // Mythical blocks - Mythical gems
            .addBlocks(EEvolutionRarityType.MYTHICAL,
                Material.ECHO_SHARD, Material.RECOVERY_COMPASS, Material.DISC_FRAGMENT_5,
                Material.MUSIC_DISC_5, Material.SCULK_CATALYST)

            // Divine blocks - Divine crystals
            .addBlocks(EEvolutionRarityType.DIVINE,
                Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SHRIEKER, Material.SCULK_SENSOR,
                Material.SCULK_VEIN, Material.SCULK)

            // Celestial blocks - Celestial gems
            .addBlocks(EEvolutionRarityType.CELESTIAL,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY, Material.VAULT,
                Material.TRIAL_SPAWNER, Material.OMINOUS_BOTTLE)

            // Transcendent blocks - Transcendent crystals
            .addBlocks(EEvolutionRarityType.TRANSCENDENT,
                Material.HEAVY_CORE, Material.MACE, Material.WIND_CHARGE,
                Material.BREEZE_ROD, Material.ARMADILLO_SCUTE)
            
            // Gem entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.WANDERING_TRADER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.IRON_GOLEM_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.SPECIAL, Material.WARDEN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.UNIQUE, Material.ELDER_GUARDIAN_SPAWN_EGG)

            // Diamond items
            .addItem(EEvolutionRarityType.COMMON, Material.DIAMOND, 4)
            .addItem(EEvolutionRarityType.COMMON, Material.COAL, 16)
            .addItem(EEvolutionRarityType.COMMON, Material.IRON_INGOT, 8)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.LAPIS_LAZULI, 12)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.REDSTONE, 16)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLD_INGOT, 6)
            
            .addItem(EEvolutionRarityType.RARE, Material.EMERALD, 8)
            .addItem(EEvolutionRarityType.RARE, Material.AMETHYST_SHARD, 12)
            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND_SWORD, 1)
            .addItem(EEvolutionRarityType.RARE, Material.DIAMOND_PICKAXE, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_AXE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_SHOVEL, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.DIAMOND_HOE, 1)
            .addItem(EEvolutionRarityType.EPIC, Material.ENCHANTING_TABLE, 1)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND_HELMET, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.DIAMOND_BOOTS, 1)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.CONDUIT, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.EXPERIENCE_BOTTLE, 16)
            
            .addItem(EEvolutionRarityType.UNIQUE, Material.NETHER_STAR, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.DRAGON_EGG, 1)
            .addItem(EEvolutionRarityType.UNIQUE, Material.HEART_OF_THE_SEA, 1)
            
            .addItem(EEvolutionRarityType.MYTHICAL, Material.ECHO_SHARD, 4)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.RECOVERY_COMPASS, 1)
            .addItem(EEvolutionRarityType.MYTHICAL, Material.DISC_FRAGMENT_5, 1)
            
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_CATALYST, 2)
            .addItem(EEvolutionRarityType.DIVINE, Material.SCULK_SENSOR, 3)
            
            .addItem(EEvolutionRarityType.CELESTIAL, Material.TRIAL_KEY, 5)
            .addItem(EEvolutionRarityType.CELESTIAL, Material.OMINOUS_TRIAL_KEY, 2)
            
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.HEAVY_CORE, 1)
            .addItem(EEvolutionRarityType.TRANSCENDENT, Material.MACE, 1)

            // Master Jeweler's Kit - Ultimate crafting tool
            .addCustomItem(EEvolutionRarityType.TRANSCENDENT,
                player -> EvolutionItemFactory.createMasterJewelerKit(player))

            .build();
    }
}


