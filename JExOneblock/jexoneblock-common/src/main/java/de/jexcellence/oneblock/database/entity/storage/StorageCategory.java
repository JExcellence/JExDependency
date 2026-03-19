package de.jexcellence.oneblock.database.entity.storage;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public enum StorageCategory {
    
    BLOCKS("Building Blocks", Material.COBBLESTONE, 1, 
           "STONE", "COBBLESTONE", "DIRT", "GRASS_BLOCK", "SAND", "GRAVEL", 
           "WOOD", "LOG", "PLANKS", "GLASS", "WOOL", "CONCRETE", "TERRACOTTA",
           "BRICK", "SANDSTONE", "GRANITE", "DIORITE", "ANDESITE", "DEEPSLATE",
           "BLACKSTONE", "BASALT", "TUFF", "CALCITE", "DRIPSTONE"),
    
    ORES("Ores & Minerals", Material.DIAMOND_ORE, 2,
         "COAL", "IRON", "GOLD", "DIAMOND", "EMERALD", "LAPIS", "REDSTONE", 
         "QUARTZ", "NETHERITE", "_ORE", "_INGOT", "_NUGGET", "COPPER",
         "AMETHYST", "PRISMARINE", "ANCIENT_DEBRIS"),
    
    TOOLS("Tools & Weapons", Material.DIAMOND_SWORD, 3,
          "_SWORD", "_AXE", "_PICKAXE", "_SHOVEL", "_HOE", "_BOW", "_CROSSBOW",
          "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "SHIELD", "TRIDENT",
          "FISHING_ROD", "FLINT_AND_STEEL", "SHEARS", "COMPASS", "CLOCK"),
    
    FOOD("Food & Consumables", Material.BREAD, 4,
         "BREAD", "APPLE", "CARROT", "POTATO", "BEEF", "PORK", "CHICKEN", 
         "FISH", "COOKIE", "CAKE", "STEW", "SOUP", "_FOOD", "MUSHROOM",
         "BEETROOT", "MELON", "PUMPKIN", "SWEET_BERRIES", "GLOW_BERRIES"),
    
    MATERIALS("Crafting Materials", Material.STICK, 5,
              "STICK", "STRING", "LEATHER", "PAPER", "BOOK", "GUNPOWDER",
              "BONE", "FEATHER", "SLIME", "_DUST", "_POWDER", "BLAZE_ROD",
              "ENDER_PEARL", "GHAST_TEAR", "SPIDER_EYE", "ROTTEN_FLESH"),
    
    PLANTS("Plants & Nature", Material.OAK_SAPLING, 6,
           "SAPLING", "LEAVES", "FLOWER", "GRASS", "FERN", "VINE", "LILY",
           "KELP", "SEAGRASS", "BAMBOO", "CACTUS", "SUGAR_CANE", "WHEAT",
           "SEEDS", "MOSS", "AZALEA", "SPORE_BLOSSOM"),
    
    REDSTONE("Redstone & Tech", Material.REDSTONE, 7,
             "REDSTONE", "REPEATER", "COMPARATOR", "PISTON", "DISPENSER",
             "DROPPER", "HOPPER", "OBSERVER", "LEVER", "BUTTON", "PRESSURE_PLATE",
             "TRIPWIRE", "DAYLIGHT_DETECTOR", "RAIL", "MINECART"),
    
    DECORATIVE("Decorative Items", Material.PAINTING, 8,
               "PAINTING", "ITEM_FRAME", "FLOWER_POT", "BANNER", "CARPET",
               "CANDLE", "LANTERN", "TORCH", "CAMPFIRE", "BELL", "LECTERN",
               "BOOKSHELF", "ENCHANTING_TABLE", "ANVIL", "CAULDRON"),
    
    RARE("Rare & Special", Material.NETHER_STAR, 9,
         "NETHER_STAR", "DRAGON", "ELYTRA", "TOTEM", "HEART_OF_THE_SEA",
         "CONDUIT", "BEACON", "_FRAGMENT", "_SHARD", "RECOVERY_COMPASS",
         "ECHO_SHARD", "DISC_FRAGMENT", "MUSIC_DISC", "ENCHANTED_BOOK"),
    
    NETHER("Nether Items", Material.NETHERRACK, 10,
           "NETHER", "SOUL", "CRIMSON", "WARPED", "SHROOMLIGHT", "WEEPING_VINES",
           "TWISTING_VINES", "NYLIUM", "WART", "BLAZE", "MAGMA", "OBSIDIAN"),
    
    END("End Items", Material.END_STONE, 11,
        "END_", "SHULKER", "CHORUS", "PURPUR", "DRAGON_", "ELYTRA",
        "ENDER_", "ENDERMAN", "ENDERMITE"),
    
    MISC("Miscellaneous", Material.CHEST, 12,
         "CHEST", "BARREL", "FURNACE", "CRAFTING_TABLE", "LADDER", "DOOR",
         "TRAPDOOR", "FENCE", "GATE", "STAIRS", "SLAB", "WALL", "SIGN");

    private final String displayName;
    private final Material icon;
    private final int sortOrder;
    private final Set<String> materialPatterns;

    StorageCategory(@NotNull String displayName, @NotNull Material icon, int sortOrder, String... patterns) {
        this.displayName = displayName;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.materialPatterns = Arrays.stream(patterns).collect(Collectors.toSet());
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public Material getIcon() {
        return icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @NotNull
    public Set<String> getMaterialPatterns() {
        return materialPatterns;
    }

    @NotNull
    public static StorageCategory categorize(@NotNull Material material) {
        String materialName = material.name();
        
        StorageCategory[] priorityOrder = {
            RARE, NETHER, END, ORES, TOOLS, REDSTONE, PLANTS, DECORATIVE,
            FOOD, MATERIALS, BLOCKS, MISC
        };
        
        for (StorageCategory category : priorityOrder) {
            for (String pattern : category.materialPatterns) {
                if (materialName.contains(pattern)) {
                    return category;
                }
            }
        }
        
        return MISC;
    }

    @NotNull
    public List<Material> getMaterials() {
        return Arrays.stream(Material.values())
                .filter(material -> categorize(material) == this)
                .collect(Collectors.toList());
    }

    public boolean contains(@NotNull Material material) {
        return categorize(material) == this;
    }

    @NotNull
    public static StorageCategory fromName(@NotNull String name) {
        for (StorageCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) || 
                category.displayName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return MISC;
    }

    @NotNull
    public static StorageCategory[] getSortedCategories() {
        return Arrays.stream(values())
                .sorted((a, b) -> Integer.compare(a.sortOrder, b.sortOrder))
                .toArray(StorageCategory[]::new);
    }

    @NotNull
    public static StorageCategory[] getMainCategories() {
        return new StorageCategory[]{BLOCKS, ORES, TOOLS, FOOD, MATERIALS, RARE};
    }

    @Override
    public String toString() {
        return displayName;
    }
}