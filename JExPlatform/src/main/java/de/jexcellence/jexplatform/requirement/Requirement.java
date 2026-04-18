package de.jexcellence.jexplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for all requirement types in the platform.
 *
 * <p>Each requirement can check whether a player meets a condition,
 * calculate progress towards completion, and optionally consume
 * resources when fulfilled.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface Requirement permits AbstractRequirement {

    /**
     * Returns the type identifier for this requirement.
     *
     * @return the type ID (e.g., {@code "ITEM"}, {@code "CURRENCY"})
     */
    @NotNull String typeId();

    /**
     * Checks whether the player meets this requirement.
     *
     * @param player the player to evaluate
     * @return {@code true} when the requirement is satisfied
     */
    boolean isMet(@NotNull Player player);

    /**
     * Calculates the player's progress towards meeting this requirement.
     *
     * @param player the player to evaluate
     * @return progress as a value between {@code 0.0} and {@code 1.0}
     */
    double calculateProgress(@NotNull Player player);

    /**
     * Consumes the resources required by this requirement.
     *
     * @param player the player whose resources are consumed
     */
    void consume(@NotNull Player player);

    /**
     * Returns the translation key for describing this requirement.
     *
     * @return the description key
     */
    @JsonIgnore
    @NotNull String descriptionKey();
}
