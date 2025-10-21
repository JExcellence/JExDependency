package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all requirement types in the RaindropQuests system.
 * <p>
 * Implementations encapsulate the logic required to evaluate whether a {@link Player} satisfies a
 * particular condition, expose the identifier {@link Type}, and optionally consume player
 * resources once fulfilled. The abstract contract keeps requirement evaluation consistent across
 * multiple gameplay features including quest progression, generator upgrades, and unlock flows.
 * </p>
 * <p>
 * Subclasses must document any thread-affinity or side effects and should avoid mutating player
 * state outside of {@link #consume(Player)}. Implementations are expected to be stateless aside
 * from immutable configuration captured during construction so they may be reused safely across
 * checks.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class AbstractRequirement {

    /**
     * Enumerates the various types of requirements supported by the system.
     * <p>
     * Each enum value corresponds to a specific requirement implementation and is used for type
     * identification, serialization, and deserialization. New requirement categories should be
     * appended here to maintain backwards compatible enum ordinal ordering.
     * </p>
     */
    public enum Type {
        ITEM,
        PLAYTIME,
        ACHIEVEMENT,
        PREVIOUS_LEVEL,
        COMPOSITE,
        CHOICE,
        TIME_BASED,
        EXPERIENCE_LEVEL,
        PERMISSION,
        LOCATION,
        CUSTOM,
        CURRENCY,
        JOBS,
        SKILLS
    }

    /**
     * The type of this requirement.
     * <p>
     * Used for identification, serialization, and determining the concrete implementation.
     * </p>
     */
    protected final Type type;

    /**
     * Constructs a new {@code AbstractRequirement} with the specified type.
     *
     * @param type the requirement type that identifies the concrete implementation; must not be
     *             {@code null}.
     */
    protected AbstractRequirement(final @NotNull Type type) {
        this.type = type;
    }

    /**
     * Returns the type of this requirement.
     *
     * @return the requirement {@link Type} configured during construction.
     */
    @NotNull
    public final Type getType() {
        return this.type;
    }

    /**
     * Checks if this requirement is fully met for the specified player.
     * <p>
     * Implementations should evaluate whether all conditions of the requirement are satisfied by
     * examining the player's current state, inventory, statistics, or other relevant attributes.
     * The method must be free of side effects and may be invoked repeatedly to confirm eligibility.
     * </p>
     *
     * @param player the player to check against this requirement; never {@code null}.
     * @return {@code true} if the requirement is fully met, {@code false} otherwise.
     */
    public abstract boolean isMet(final @NotNull Player player);

    /**
     * Calculates the progress toward fulfilling this requirement for the specified player.
     * <p>
     * The return value is typically between {@code 0.0} (no progress) and {@code 1.0} (fully met),
     * representing the percentage of completion toward meeting the requirement. Implementations
     * should ensure the value is clamped within this range and document any deviations (for
     * example, if a requirement can exceed 100% to track overflow progress).
     * </p>
     *
     * @param player the player whose progress is being calculated; never {@code null}.
     * @return a double representing the completion progress (0.0 to 1.0).
     */
    public abstract double calculateProgress(final @NotNull Player player);

    /**
     * Consumes the necessary resources or components from the player to fulfill this requirement.
     * <p>
     * This method is called after {@link #isMet(Player)} has confirmed eligibility and is expected
     * to perform any side effects such as deducting currency or removing items. Implementations
     * should document whether the operation is idempotent and must avoid consuming resources when
     * {@link #isMet(Player)} would return {@code false}.
     * </p>
     *
     * @param player the player from whom resources will be consumed; never {@code null}.
     */
    public abstract void consume(final @NotNull Player player);

    /**
     * Gets the translation key for the requirement's description.
     * <p>
     * This key is used to retrieve localized descriptions of the requirement from the plugin's
     * language or resource files, enabling internationalization. Implementations should return a
     * stable key so cached translations remain valid across reloads.
     * </p>
     *
     * @return the language key for this requirement's description.
     */
    @JsonIgnore
    @NotNull
    public abstract String getDescriptionKey();
}