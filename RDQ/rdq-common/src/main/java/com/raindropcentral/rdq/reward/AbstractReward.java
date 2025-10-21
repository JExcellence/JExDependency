package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all reward types in the RaindropQuests system.
 * <p>
 * This class defines the contract for all rewards, including type identification,
 * application logic, and description key for localization. Concrete implementations
 * should extend this class to provide specific reward behaviors, such as executing
 * commands, giving items, or awarding currency.
 * </p>
 * <p>
 * Subclasses are expected to be immutable and thread-safe because rewards may be
 * evaluated from asynchronous quest progress pipelines.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public abstract class AbstractReward {

    /**
     * Enumerates the various types of rewards supported by the system.
     */
    public enum Type {
        /**
         * Executes a configured command when the reward is applied.
         */
        COMMAND,
        /**
         * Grants one or more items to the player.
         */
        ITEM,
        /**
         * Awards currency through the configured economy adapter.
         */
        CURRENCY,
        /**
         * Grants experience levels or points to the player.
         */
        EXPERIENCE,
        /**
         * Chains multiple rewards together for composite execution.
         */
        COMPOSITE,
        /**
         * Issues a permission node to the player.
         */
        PERMISSION,
        /**
         * Sends a title or subtitle to the player.
         */
        TITLE,
        /**
         * Plays a sound at the player's location.
         */
        SOUND,
        /**
         * Spawns a particle effect for the player.
         */
        PARTICLE
    }

    /**
     * The type of this reward.
     */
    protected final Type type;

    /**
     * Constructs a new {@code AbstractReward} with the specified type.
     *
     * @param type The reward type that identifies the concrete implementation. Must
     *             not be {@code null}.
     */
    protected AbstractReward(final @NotNull Type type) {
        this.type = type;
    }

    /**
     * Returns the type of this reward.
     *
     * @return The reward {@link Type} that this instance represents.
     */
    @NotNull
    public final Type getType() {
        return this.type;
    }

    /**
     * Applies the reward to the specified player.
     * <p>
     * Implementations should define the logic for granting the reward, such as
     * executing a command, giving an item, or awarding currency. Implementations
     * should perform any thread-sensitive interactions with Bukkit APIs on the
     * main thread.
     * </p>
     *
     * @param player The player to receive the reward. Must not be {@code null}.
     */
    public abstract void apply(final @NotNull Player player);

    /**
     * Gets the translation key for the reward's description.
     * <p>
     * This key is used to retrieve localized descriptions of the reward from the
     * plugin's language or resource files, enabling internationalization.
     * </p>
     *
     * @return The language key for this reward's description. Never {@code null}.
     */
    @JsonIgnore
    @NotNull
    public abstract String getDescriptionKey();
}