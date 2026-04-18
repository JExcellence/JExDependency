package de.jexcellence.jexplatform.reward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Sealed interface for all reward types in the platform.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface Reward permits AbstractReward {

    /**
     * Returns the type identifier for this reward.
     *
     * @return the type ID
     */
    @NotNull String typeId();

    /**
     * Grants this reward to the player.
     *
     * @param player the player to reward
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);

    /**
     * Returns the estimated value of this reward (for display/sorting).
     *
     * @return the estimated value
     */
    double estimatedValue();

    /**
     * Returns the translation key for this reward's description.
     *
     * @return the description key
     */
    @JsonIgnore
    @NotNull String descriptionKey();
}
