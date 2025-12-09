package com.raindropcentral.rdq2.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum EPerkCategory {
    COMBAT("combat", "Combat", Material.DIAMOND_SWORD, 0),
    MOVEMENT("movement", "Movement", Material.FEATHER, 1),
    UTILITY("utility", "Utility", Material.COMPASS, 2),
    SURVIVAL("survival", "Survival", Material.GOLDEN_APPLE, 3),
    ECONOMY("economy", "Economy", Material.EMERALD, 4),
    SOCIAL("social", "Social", Material.PLAYER_HEAD, 5),
    COSMETIC("cosmetic", "Cosmetic", Material.FIREWORK_ROCKET, 6),
    SPECIAL("special", "Special", Material.NETHER_STAR, 7);

    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final int sortOrder;

    EPerkCategory(@NotNull String identifier, @NotNull String displayName, @NotNull Material iconMaterial, int sortOrder) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.sortOrder = sortOrder;
    }

    public @NotNull String getIdentifier() { return identifier; }
    public @NotNull String getDisplayName() { return displayName; }
    public @NotNull Material getIconMaterial() { return iconMaterial; }
    public int getSortOrder() { return sortOrder; }
    public @NotNull String getDisplayNameKey() { return "perk.category." + identifier + ".name"; }
    public @NotNull String getDescriptionKey() { return "perk.category." + identifier + ".description"; }

    public static @Nullable EPerkCategory fromIdentifier(@NotNull String identifier) {
        return Arrays.stream(values())
            .filter(category -> category.identifier.equalsIgnoreCase(identifier))
            .findFirst()
            .orElse(null);
    }

    public static List<EPerkCategory> getSorted() {
        return Arrays.stream(values())
            .sorted(Comparator.comparingInt(EPerkCategory::getSortOrder))
            .toList();
    }

    public boolean isGameplayAffecting() {
        return this != COSMETIC;
    }
}