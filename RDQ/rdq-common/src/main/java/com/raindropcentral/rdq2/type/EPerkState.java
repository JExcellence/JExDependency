package com.raindropcentral.rdq2.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum EPerkState {
    LOCKED("locked", "Locked", Material.BARRIER, "§c", false, false),
    AVAILABLE("available", "Available", Material.LIME_DYE, "§a", true, false),
    ACTIVE("active", "Active", Material.GREEN_DYE, "§2", true, true),
    COOLDOWN("cooldown", "On Cooldown", Material.CLOCK, "§6", true, false),
    DISABLED("disabled", "Disabled", Material.REDSTONE, "§8", false, false);

    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final String colorCode;
    private final boolean isUsable;
    private final boolean isActive;

    EPerkState(@NotNull String identifier, @NotNull String displayName, @NotNull Material iconMaterial, 
               @NotNull String colorCode, boolean isUsable, boolean isActive) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorCode = colorCode;
        this.isUsable = isUsable;
        this.isActive = isActive;
    }

    public @NotNull String getIdentifier() { return identifier; }
    public @NotNull String getDisplayName() { return displayName; }
    public @NotNull Material getIconMaterial() { return iconMaterial; }
    public @NotNull String getColorCode() { return colorCode; }
    public boolean isUsable() { return isUsable; }
    public boolean isActive() { return isActive; }
    public @NotNull String getDisplayNameKey() { return "perk.state." + identifier + ".name"; }
    public @NotNull String getDescriptionKey() { return "perk.state." + identifier + ".description"; }
    public @NotNull String getColoredDisplayName() { return colorCode + displayName; }

    public static @Nullable EPerkState fromIdentifier(@NotNull String identifier) {
        return Arrays.stream(values())
            .filter(state -> state.identifier.equalsIgnoreCase(identifier))
            .findFirst()
            .orElse(null);
    }

    public boolean canTransitionTo(@NotNull EPerkState newState) {
        return switch (this) {
            case LOCKED -> newState == AVAILABLE;
            case AVAILABLE -> newState == ACTIVE || newState == DISABLED;
            case ACTIVE -> newState == AVAILABLE || newState == COOLDOWN || newState == DISABLED;
            case COOLDOWN -> newState == AVAILABLE || newState == DISABLED;
            case DISABLED -> newState == AVAILABLE;
        };
    }
}