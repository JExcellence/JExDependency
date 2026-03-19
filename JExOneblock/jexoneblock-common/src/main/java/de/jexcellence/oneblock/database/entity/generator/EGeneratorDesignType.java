package de.jexcellence.oneblock.database.entity.generator;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enumeration of all generator design types with their tier progression.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public enum EGeneratorDesignType {
    
    FOUNDRY("foundry", 1, Material.FURNACE, 5, 5, 3),
    AQUATIC("aquatic", 2, Material.PRISMARINE, 5, 5, 4),
    VOLCANIC("volcanic", 3, Material.MAGMA_BLOCK, 7, 7, 4),
    CRYSTAL("crystal", 4, Material.AMETHYST_BLOCK, 7, 7, 5),
    MECHANICAL("mechanical", 5, Material.PISTON, 9, 9, 5),
    NATURE("nature", 6, Material.MOSS_BLOCK, 9, 9, 6),
    NETHER("nether", 7, Material.BLACKSTONE, 11, 11, 6),
    END("end", 8, Material.END_STONE, 11, 11, 7),
    ANCIENT("ancient", 9, Material.DEEPSLATE, 13, 13, 7),
    CELESTIAL("celestial", 10, Material.BEACON, 15, 15, 8);
    
    private final String key;
    private final int tier;
    private final Material icon;
    private final int width;
    private final int depth;
    private final int height;
    
    EGeneratorDesignType(
            @NotNull String key,
            int tier,
            @NotNull Material icon,
            int width,
            int depth,
            int height
    ) {
        this.key = key;
        this.tier = tier;
        this.icon = icon;
        this.width = width;
        this.depth = depth;
        this.height = height;
    }
    
    @NotNull
    public String getKey() {
        return key;
    }
    
    public int getTier() {
        return tier;
    }
    
    @NotNull
    public Material getIcon() {
        return icon;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public int getHeight() {
        return height;
    }
    
    @NotNull
    public String getNameKey() {
        return "generator.design." + key + ".name";
    }
    
    @NotNull
    public String getDescriptionKey() {
        return "generator.design." + key + ".description";
    }
    
    public double getSpeedMultiplier() {
        return 1.0 + (tier * 0.2);
    }
    
    public double getDefaultSpeedMultiplier() {
        return getSpeedMultiplier();
    }
    
    public double getDefaultXpMultiplier() {
        return 1.0 + (tier * 0.15);
    }
    
    @Nullable
    public EGeneratorDesignType getPreviousTier() {
        if (tier <= 1) return null;
        for (EGeneratorDesignType type : values()) {
            if (type.tier == this.tier - 1) {
                return type;
            }
        }
        return null;
    }
    
    @Nullable
    public EGeneratorDesignType getNextTier() {
        for (EGeneratorDesignType type : values()) {
            if (type.tier == this.tier + 1) {
                return type;
            }
        }
        return null;
    }
    
    @Nullable
    public static EGeneratorDesignType fromKey(@Nullable String key) {
        if (key == null) return null;
        for (EGeneratorDesignType type : values()) {
            if (type.key.equalsIgnoreCase(key) || type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
    
    @Nullable
    public static EGeneratorDesignType fromTier(int tier) {
        for (EGeneratorDesignType type : values()) {
            if (type.tier == tier) {
                return type;
            }
        }
        return null;
    }
}
