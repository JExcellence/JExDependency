package de.jexcellence.oneblock.database.entity.biome;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of biome categories for island biome selection.
 * Each category contains a list of biomes with their requirements.
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Getter
public enum EBiomeCategory {

    ALL("All", Material.COMPASS, "All available biomes"),

    TEMPERATE("Temperate", Material.GRASS_BLOCK, "Mild climate biomes",
        new BiomeInfo(Biome.PLAINS, Material.GRASS_BLOCK, "Plains", "Classic grasslands", 0, 0),
        new BiomeInfo(Biome.FOREST, Material.OAK_LOG, "Forest", "Dense woodland", 5, 1000),
        new BiomeInfo(Biome.BIRCH_FOREST, Material.BIRCH_LOG, "Birch Forest", "Light birch trees", 8, 2500),
        new BiomeInfo(Biome.DARK_FOREST, Material.DARK_OAK_LOG, "Dark Forest", "Mysterious woods", 12, 5000),
        new BiomeInfo(Biome.FLOWER_FOREST, Material.POPPY, "Flower Forest", "Colorful blooms", 15, 8000)
    ),

    COLD("Cold", Material.SNOW_BLOCK, "Frozen and snowy biomes",
        new BiomeInfo(Biome.TAIGA, Material.SPRUCE_LOG, "Taiga", "Coniferous forest", 10, 3000),
        new BiomeInfo(Biome.SNOWY_TAIGA, Material.SNOW_BLOCK, "Snowy Taiga", "Frozen forest", 18, 12000),
        new BiomeInfo(Biome.SNOWY_PLAINS, Material.POWDER_SNOW, "Snowy Plains", "Frozen grasslands", 20, 15000),
        new BiomeInfo(Biome.ICE_SPIKES, Material.PACKED_ICE, "Ice Spikes", "Crystalline spires", 25, 25000),
        new BiomeInfo(Biome.FROZEN_OCEAN, Material.BLUE_ICE, "Frozen Ocean", "Icy waters", 30, 40000)
    ),

    HOT("Hot", Material.SAND, "Desert and dry biomes",
        new BiomeInfo(Biome.DESERT, Material.SAND, "Desert", "Sandy wasteland", 8, 2000),
        new BiomeInfo(Biome.SAVANNA, Material.ACACIA_LOG, "Savanna", "African plains", 12, 4500),
        new BiomeInfo(Biome.BADLANDS, Material.TERRACOTTA, "Badlands", "Red rock formations", 22, 18000),
        new BiomeInfo(Biome.ERODED_BADLANDS, Material.RED_TERRACOTTA, "Eroded Badlands", "Weathered canyons", 28, 35000)
    ),

    AQUATIC("Aquatic", Material.WATER_BUCKET, "Ocean and water biomes",
        new BiomeInfo(Biome.OCEAN, Material.WATER_BUCKET, "Ocean", "Deep blue waters", 6, 1500),
        new BiomeInfo(Biome.WARM_OCEAN, Material.TROPICAL_FISH_BUCKET, "Warm Ocean", "Tropical waters", 14, 6000),
        new BiomeInfo(Biome.LUKEWARM_OCEAN, Material.SALMON_BUCKET, "Lukewarm Ocean", "Temperate seas", 16, 9000),
        new BiomeInfo(Biome.COLD_OCEAN, Material.COD_BUCKET, "Cold Ocean", "Chilly waters", 20, 15000),
        new BiomeInfo(Biome.DEEP_OCEAN, Material.HEART_OF_THE_SEA, "Deep Ocean", "Abyssal depths", 35, 50000)
    ),

    EXOTIC("Exotic", Material.JUNGLE_LOG, "Rare and unique biomes",
        new BiomeInfo(Biome.JUNGLE, Material.JUNGLE_LOG, "Jungle", "Dense tropical forest", 18, 10000),
        new BiomeInfo(Biome.BAMBOO_JUNGLE, Material.BAMBOO, "Bamboo Jungle", "Asian wilderness", 24, 20000),
        new BiomeInfo(Biome.MUSHROOM_FIELDS, Material.RED_MUSHROOM_BLOCK, "Mushroom Fields", "Fungal paradise", 40, 75000),
        new BiomeInfo(Biome.CHERRY_GROVE, Material.CHERRY_LOG, "Cherry Grove", "Pink blossoms", 45, 100000)
    ),

    NETHER("Nether", Material.NETHERRACK, "Hellish dimension biomes",
        new BiomeInfo(Biome.NETHER_WASTES, Material.NETHERRACK, "Nether Wastes", "Hellish landscape", 30, 40000),
        new BiomeInfo(Biome.CRIMSON_FOREST, Material.CRIMSON_STEM, "Crimson Forest", "Red fungal forest", 35, 60000),
        new BiomeInfo(Biome.WARPED_FOREST, Material.WARPED_STEM, "Warped Forest", "Blue fungal forest", 35, 60000),
        new BiomeInfo(Biome.SOUL_SAND_VALLEY, Material.SOUL_SAND, "Soul Sand Valley", "Valley of souls", 40, 80000),
        new BiomeInfo(Biome.BASALT_DELTAS, Material.BASALT, "Basalt Deltas", "Volcanic terrain", 42, 90000)
    ),

    END("End", Material.END_STONE, "Void dimension biomes",
        new BiomeInfo(Biome.THE_END, Material.END_STONE, "The End", "Void dimension", 50, 150000),
        new BiomeInfo(Biome.SMALL_END_ISLANDS, Material.CHORUS_PLANT, "Small End Islands", "Floating isles", 55, 200000),
        new BiomeInfo(Biome.END_HIGHLANDS, Material.PURPUR_BLOCK, "End Highlands", "Purple peaks", 60, 300000),
        new BiomeInfo(Biome.END_MIDLANDS, Material.END_STONE_BRICKS, "End Midlands", "Void plains", 58, 250000),
        new BiomeInfo(Biome.END_BARRENS, Material.CHORUS_FLOWER, "End Barrens", "Desolate void", 65, 400000)
    );

    private final String displayName;
    private final Material icon;
    private final String description;
    private final List<BiomeInfo> biomes;

    EBiomeCategory(String displayName, Material icon, String description, BiomeInfo... biomes) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.biomes = Arrays.asList(biomes);
    }

    /**
     * Gets all biomes from all categories (excluding ALL).
     *
     * @return list of all biomes
     */
    @NotNull
    public static List<BiomeInfo> getAllBiomes() {
        List<BiomeInfo> allBiomes = new ArrayList<>();
        for (EBiomeCategory category : values()) {
            if (category != ALL) {
                allBiomes.addAll(category.getBiomes());
            }
        }
        return allBiomes;
    }

    /**
     * Gets biomes for this category. If ALL, returns all biomes from all categories.
     *
     * @return list of biomes for this category
     */
    @NotNull
    public List<BiomeInfo> getBiomesForCategory() {
        if (this == ALL) {
            return getAllBiomes();
        }
        return biomes;
    }

    /**
     * Gets a category by name (case-insensitive).
     *
     * @param name the category name
     * @return the category or null if not found
     */
    @Nullable
    public static EBiomeCategory fromName(@NotNull String name) {
        for (EBiomeCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) || category.displayName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Gets all category names for tab completion.
     *
     * @return array of category names
     */
    @NotNull
    public static String[] getCategoryNames() {
        return Arrays.stream(values())
            .map(EBiomeCategory::name)
            .toArray(String[]::new);
    }

    /**
     * Gets categories that have biomes (excludes ALL for iteration purposes).
     *
     * @return list of categories with biomes
     */
    @NotNull
    public static List<EBiomeCategory> getActualCategories() {
        return Arrays.stream(values())
            .filter(c -> c != ALL)
            .toList();
    }

    /**
     * Gets the order index for display purposes.
     *
     * @return the order index
     */
    public int getOrder() {
        return ordinal();
    }

    /**
     * Record representing biome information.
     */
    public record BiomeInfo(
        Biome biome,
        Material material,
        String displayName,
        String description,
        int requiredLevel,
        long requiredCoins
    ) {}
}
