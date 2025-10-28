package com.raindropcentral.rdq.type;

import org.jetbrains.annotations.NotNull;

/**
 * Enumerates all available perk identifiers in the RaindropQuests system.
 * <p>
 * Each constant represents a unique perk that can be granted to players.
 * The identifier is used for configuration, database lookups, and runtime registration.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public enum EPerkIdentifier {

    // Event-triggered perks
    DAMAGE_REDUCTION("damage_reduction"),
    DEATH_PROTECTION("death_protection"),
    DOUBLE_EXPERIENCE("double_experience"),
    KEEP_EXPERIENCE("keep_experience"),
    KEEP_INVENTORY("keep_inventory"),

    // Potion effect perks (toggleable passives)
    FIRE_RESISTANCE("fire_resistance"),
    GLOW("glow"),
    HASTE("haste"),
    JUMP_BOOST("jump_boost"),
    NIGHT_VISION("night_vision"),
    RESISTANCE("resistance"),
    SATURATION("saturation"),
    STRENGTH("strength"),
    SPEED("speed");

    private final String identifier;

    /**
     * Constructs a perk identifier with the given string value.
     *
     * @param identifier the unique string identifier for this perk
     */
    EPerkIdentifier(final @NotNull String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the string identifier for this perk.
     *
     * @return the unique identifier string
     */
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Attempts to find a perk identifier by its string value.
     *
     * @param identifier the string identifier to search for
     * @return the matching EPerkIdentifier, or null if not found
     */
    public static EPerkIdentifier fromIdentifier(final @NotNull String identifier) {
        for (final EPerkIdentifier perk : values()) {
            if (perk.identifier.equalsIgnoreCase(identifier)) {
                return perk;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.identifier;
    }
}
