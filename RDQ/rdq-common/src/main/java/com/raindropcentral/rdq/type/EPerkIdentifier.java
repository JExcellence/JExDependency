package com.raindropcentral.rdq.type;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.event.*;
import com.raindropcentral.rdq.database.entity.perk.potion.*;
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

    DAMAGE_REDUCTION("damage_reduction", DamageReductionPerk.class),
    DEATH_PROTECTION("death_protection", DeathProtectionPerk.class),
    DOUBLE_EXPERIENCE("double_experience", DoubleExperiencePerk.class),
    KEEP_EXPERIENCE("keep_experience", KeepExperiencePerk.class),
    KEEP_INVENTORY("keep_inventory", KeepInventoryPerk.class),

    FIRE_RESISTANCE("fire_resistance", FireResistance.class),
    GLOW("glow", Glow.class),
    HASTE("haste", Haste.class),
    JUMP_BOOST("jump_boost", JumpBoost.class),
    NIGHT_VISION("night_vision", NightVision.class),
    RESISTANCE("resistance", Resistance.class),
    SATURATION("saturation", Saturation.class),
    STRENGTH("strength", Strength.class),
    SPEED("speed", Speed.class);

    private final String identifier;
    private final Class<? extends RPerk> clazz;

    /**
     * Constructs a perk identifier with the given string value.
     *
     * @param identifier the unique string identifier for this perk
     */
    EPerkIdentifier(
            final @NotNull String identifier,
            final @NotNull Class<? extends RPerk> clazz
    ) {
        this.identifier = identifier;
        this.clazz = clazz;
    }

    /**
     * Returns the string identifier for this perk.
     *
     * @return the unique identifier string
     */
    public String getIdentifier() {
        return this.identifier;
    }

    public Class<? extends RPerk> getClazz() { return this.clazz; }

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
