package com.raindropcentral.rdq.perk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Record representing a perk/ability that players can unlock and use.
 *
 * <p>Perks have different types (toggleable, event-based, passive), effects,
 * cooldowns, and requirements. Built-in perks include speed, strength, flight,
 * experience multipliers, and death prevention.
 *
 * @param id unique identifier for the perk
 * @param displayNameKey translation key for the display name
 * @param descriptionKey translation key for the description
 * @param type the perk activation type
 * @param category optional category for grouping
 * @param cooldownSeconds cooldown between uses (0 = no cooldown)
 * @param durationSeconds effect duration (0 = permanent while active)
 * @param enabled whether this perk is active
 * @param effect the effect applied when active
 * @param iconMaterial material for GUI display
 * @param requirements list of requirements to unlock
 * @see PerkType
 * @see PerkEffect
 * @see PerkRequirement
 */
public record Perk(
    @NotNull String id,
    @NotNull String displayNameKey,
    @NotNull String descriptionKey,
    @NotNull PerkType type,
    @Nullable String category,
    int cooldownSeconds,
    int durationSeconds,
    boolean enabled,
    @NotNull PerkEffect effect,
    @NotNull String iconMaterial,
    @NotNull List<PerkRequirement> requirements
) {
    public Perk {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayNameKey, "displayNameKey");
        Objects.requireNonNull(descriptionKey, "descriptionKey");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(iconMaterial, "iconMaterial");
        requirements = requirements != null ? List.copyOf(requirements) : List.of();
        if (cooldownSeconds < 0) throw new IllegalArgumentException("cooldownSeconds must be non-negative");
        if (durationSeconds < 0) throw new IllegalArgumentException("durationSeconds must be non-negative");
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    public boolean hasDuration() {
        return durationSeconds > 0;
    }

    public boolean isToggleable() {
        return type instanceof PerkType.Toggleable;
    }

    public boolean isEventBased() {
        return type instanceof PerkType.EventBased;
    }

    public boolean isPassive() {
        return type instanceof PerkType.Passive;
    }
}
