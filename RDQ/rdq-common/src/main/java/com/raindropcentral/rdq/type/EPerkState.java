package com.raindropcentral.rdq.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration defining the different states a perk can be in for a player.
 * <p>
 * This enum represents the relationship between a player and a perk:
 * <ul>
 *     <li>{@link #LOCKED} - Player has not unlocked this perk yet</li>
 *     <li>{@link #AVAILABLE} - Player has unlocked the perk but it's not currently active</li>
 *     <li>{@link #ACTIVE} - Player has the perk unlocked and it's currently active/enabled</li>
 *     <li>{@link #COOLDOWN} - Player has the perk but it's on cooldown</li>
 *     <li>{@link #DISABLED} - Perk is globally disabled by administrators</li>
 * </ul>
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public enum EPerkState {

    /**
     * The perk is locked and not available to the player.
     * Player needs to meet requirements or complete tasks to unlock it.
     */
    LOCKED("locked", "Locked", Material.BARRIER, "§c", false, false),

    /**
     * The perk is unlocked and available for use but not currently active.
     * Player can activate this perk if they choose to.
     */
    AVAILABLE("available", "Available", Material.LIME_DYE, "§a", true, false),

    /**
     * The perk is unlocked and currently active/enabled for the player.
     * The perk's effects are currently being applied.
     */
    ACTIVE("active", "Active", Material.GREEN_DYE, "§2", true, true),

    /**
     * The perk is unlocked but currently on cooldown.
     * Player must wait before they can use this perk again.
     */
    COOLDOWN("cooldown", "On Cooldown", Material.CLOCK, "§6", true, false),

    /**
     * The perk is globally disabled by administrators.
     * No player can use this perk regardless of unlock status.
     */
    DISABLED("disabled", "Disabled", Material.REDSTONE, "§8", false, false);

    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final String colorCode;
    private final boolean isUsable;
    private final boolean isActive;

    /**
     * Constructs a perk state with specified properties.
     *
     * @param identifier unique string identifier for the state
     * @param displayName human-readable display name
     * @param iconMaterial material to use as icon indicator
     * @param colorCode color code for text formatting
     * @param isUsable whether the perk can be used in this state
     * @param isActive whether the perk is currently active
     */
    EPerkState(
            final @NotNull String identifier,
            final @NotNull String displayName,
            final @NotNull Material iconMaterial,
            final @NotNull String colorCode,
            final boolean isUsable,
            final boolean isActive
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorCode = colorCode;
        this.isUsable = isUsable;
        this.isActive = isActive;
    }

    /**
     * Gets the unique identifier for this state.
     *
     * @return the state identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Gets the display name for this state.
     *
     * @return the human-readable state name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * Gets the material to use as an icon for this state.
     *
     * @return the icon material
     */
    public @NotNull Material getIconMaterial() {
        return this.iconMaterial;
    }

    /**
     * Gets the color code for this state.
     *
     * @return the color code string (e.g., "§a")
     */
    public @NotNull String getColorCode() {
        return this.colorCode;
    }

    /**
     * Checks if the perk can be used in this state.
     *
     * @return true if the perk is usable, false otherwise
     */
    public boolean isUsable() {
        return this.isUsable;
    }

    /**
     * Checks if the perk is currently active in this state.
     *
     * @return true if the perk is active, false otherwise
     */
    public boolean isActive() {
        return this.isActive;
    }

    /**
     * Gets the localization key for this state's display name.
     *
     * @return the i18n key for the state name
     */
    public @NotNull String getDisplayNameKey() {
        return "perk.state." + this.identifier + ".name";
    }

    /**
     * Gets the localization key for this state's description.
     *
     * @return the i18n key for the state description
     */
    public @NotNull String getDescriptionKey() {
        return "perk.state." + this.identifier + ".description";
    }

    /**
     * Gets a formatted display name with color code.
     *
     * @return the colored display name
     */
    public @NotNull String getColoredDisplayName() {
        return this.colorCode + this.displayName;
    }

    /**
     * Finds a perk state by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching state, or null if not found
     */
    public static EPerkState fromIdentifier(final @NotNull String identifier) {
        for (final EPerkState state : values()) {
            if (state.getIdentifier().equalsIgnoreCase(identifier)) {
                return state;
            }
        }
        return null;
    }
}