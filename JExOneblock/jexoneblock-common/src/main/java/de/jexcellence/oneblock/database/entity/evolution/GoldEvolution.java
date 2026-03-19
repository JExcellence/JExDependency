package de.jexcellence.oneblock.database.entity.evolution;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Material;

/**
 * Gold Evolution - Wealth and Prosperity
 * Focus on gold materials and trading systems
 * Stage 12 of 50 - Tier 3: Ancient
 */
public class GoldEvolution {

    public static OneblockEvolution create() {
        return new PredefinedEvolutionBuilder("Gold", 12, 4000)
            .showcase(Material.GOLD_BLOCK)
            .description("The allure of wealth, where golden treasures fuel trade and prosperity")
            
            // Requirements: Bronze age materials
            .requireCurrency(3500)
            .requireItem(Material.IRON_INGOT, 48)
            .requireItem(Material.COPPER_INGOT, 64)
            .requireExperience(10)
            
            // Common blocks - Gold materials
            .addBlocks(EEvolutionRarityType.COMMON,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
                Material.RAW_GOLD_BLOCK, Material.GOLD_BLOCK)

            // Uncommon blocks - Gold variants
            .addBlocks(EEvolutionRarityType.UNCOMMON,
                Material.GILDED_BLACKSTONE, Material.GOLD_NUGGET, Material.GOLDEN_CARROT,
                Material.GOLDEN_APPLE, Material.GLISTERING_MELON_SLICE)

            // Rare blocks - Precious items
            .addBlocks(EEvolutionRarityType.RARE,
                Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_HORSE_ARMOR, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                Material.POWERED_RAIL, Material.DETECTOR_RAIL)

            // Epic blocks - Trading systems
            .addBlocks(EEvolutionRarityType.EPIC,
                Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
                Material.BARREL, Material.SHULKER_BOX)

            // Legendary blocks - Wealth systems
            .addBlocks(EEvolutionRarityType.LEGENDARY,
                Material.BEACON, Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK,
                Material.NETHERITE_BLOCK, Material.LODESTONE)

            // Special blocks - Ultimate wealth
            .addBlocks(EEvolutionRarityType.SPECIAL,
                Material.DRAGON_EGG, Material.NETHER_STAR)
            
            // Wealth entities
            .addEntity(EEvolutionRarityType.RARE, Material.VILLAGER_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.RARE, Material.WANDERING_TRADER_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.EPIC, Material.PIGLIN_SPAWN_EGG)
            .addEntity(EEvolutionRarityType.EPIC, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG)
            
            .addEntity(EEvolutionRarityType.LEGENDARY, Material.PIGLIN_BRUTE_SPAWN_EGG)

            // Golden items
            .addItem(EEvolutionRarityType.COMMON, Material.RAW_GOLD, 8)
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_INGOT, 6)
            .addItem(EEvolutionRarityType.COMMON, Material.GOLD_NUGGET, 16)
            
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_SWORD, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_PICKAXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_AXE, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_SHOVEL, 1)
            .addItem(EEvolutionRarityType.UNCOMMON, Material.GOLDEN_HOE, 1)
            
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_HELMET, 1)
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_CHESTPLATE, 1)
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_LEGGINGS, 1)
            .addItem(EEvolutionRarityType.RARE, Material.GOLDEN_BOOTS, 1)
            
            .addItem(EEvolutionRarityType.EPIC, Material.GOLDEN_APPLE, 3)
            .addItem(EEvolutionRarityType.EPIC, Material.GOLDEN_CARROT, 8)
            .addItem(EEvolutionRarityType.EPIC, Material.GLISTERING_MELON_SLICE, 4)
            
            .addItem(EEvolutionRarityType.LEGENDARY, Material.ENCHANTED_GOLDEN_APPLE, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.GOLDEN_HORSE_ARMOR, 1)
            .addItem(EEvolutionRarityType.LEGENDARY, Material.EMERALD, 12)
            
            .addItem(EEvolutionRarityType.SPECIAL, Material.BEACON, 1)
            .addItem(EEvolutionRarityType.SPECIAL, Material.NETHER_STAR, 1)

            .build();
    }
}


