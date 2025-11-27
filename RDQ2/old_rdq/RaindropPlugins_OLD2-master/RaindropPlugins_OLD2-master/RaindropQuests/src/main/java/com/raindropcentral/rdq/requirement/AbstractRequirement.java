package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all requirement types in the RaindropQuests generator system.
 * <p>
 * This class serves as the foundation for concrete requirement implementations such as
 * {@code CurrencyRequirement}, {@code ItemRequirement}, {@code PlaytimeRequirement}, and others.
 * It defines the common structure and contract for requirements, including type identification,
 * progress calculation, fulfillment checks, and resource consumption.
 * </p>
 * <p>
 * Requirements are used throughout the plugin to define conditions that must be met
 * for various actions such as generator upgrades, unlocks, quest progression, or special features.
 * Each requirement is associated with a {@link Type} and provides methods to check fulfillment,
 * calculate progress, and consume resources from a {@link Player}.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public abstract class AbstractRequirement {

    /**
     * Enumerates the various types of requirements supported by the system.
     * <p>
     * Each enum value corresponds to a specific requirement implementation and is used
     * for type identification, serialization, and deserialization.
     * </p>
     * <ul>
     *   <li>{@link #CURRENCY} - Requirement based on in-game currency.</li>
     *   <li>{@link #ITEM} - Requirement based on possession of specific items.</li>
     *   <li>{@link #PLAYTIME} - Requirement based on accumulated playtime.</li>
     *   <li>{@link #ACHIEVEMENT} - Requirement based on achievement completion.</li>
     *   <li>{@link #PREVIOUS_LEVEL} - Requirement based on reaching a previous upgrade or level.</li>
     *   <li>{@link #COMPOSITE} - Composite requirement combining multiple sub-requirements.</li>
     *   <li>{@link #CHOICE} - Requirement where the player can choose among alternatives.</li>
     *   <li>{@link #TIME_BASED} - Requirement based on time constraints or cooldowns.</li>
     *   <li>{@link #JOBS} - Requirement based on jobs.</li>
     *   <li>{@link #SKILLS} - Requirement based on skills.</li>
     *   <li>{@link #EXPERIENCE_LEVEL} - Requirement based on the experience level of the player.</li>
     * </ul>
     */
    public enum Type {
        // ~~~ VANILLA ~~~
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
        
        // ~~~ PLUGINS ~~~
        CURRENCY,
        JOBS,
        SKILLS,
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
     * @param type The requirement type that identifies the concrete implementation.
     */
    protected AbstractRequirement(
            @NotNull final Type type
    ) {
        this.type = type;
    }

    /**
     * Returns the type of this requirement.
     *
     * @return The requirement {@link Type}.
     */
    @NotNull
    public Type getType() {
        return this.type;
    }

    /**
     * Checks if this requirement is fully met for the specified player.
     * <p>
     * Implementations should evaluate whether all conditions of the requirement are satisfied
     * by examining the player's current state, inventory, statistics, or other relevant attributes.
     * </p>
     *
     * @param player The player to check against this requirement.
     * @return {@code true} if the requirement is fully met, {@code false} otherwise.
     */
    public abstract boolean isMet(
            @NotNull Player player
    );

    /**
     * Calculates the progress toward fulfilling this requirement for the specified player.
     * <p>
     * The return value is typically between {@code 0.0} (no progress) and {@code 1.0} (fully met),
     * representing the percentage of completion toward meeting the requirement.
     * Implementations should ensure the value is clamped within this range.
     * </p>
     *
     * @param player The player whose progress is being calculated.
     * @return A double representing the completion progress (0.0 to 1.0).
     */
    public abstract double calculateProgress(
            @NotNull Player player
    );

    /**
     * Consumes the necessary resources or components from the player to fulfill this requirement.
     * <p>
     * This method is called when a requirement is being applied, and it should
     * modify the player's state accordingly (e.g., deducting currency, removing items).
     * If the requirement does not require consumption, this method may perform no action.
     * </p>
     *
     * @param player The player from whom resources will be consumed.
     */
    public abstract void consume(
            @NotNull Player player
    );

    /**
     * Gets the translation key for the requirement's description.
     * <p>
     * This key is used to retrieve localized descriptions of the requirement
     * from the plugin's language or resource files, enabling internationalization.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @JsonIgnore
    @NotNull
    public abstract String getDescriptionKey();
}
