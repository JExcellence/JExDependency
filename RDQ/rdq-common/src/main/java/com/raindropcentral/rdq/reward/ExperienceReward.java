package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a reward that grants experience to a player.
 * <p>
 * Supports both experience levels and experience points.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public final class ExperienceReward extends AbstractReward {

    /**
     * Supported experience reward modes.
     */
    public enum ExperienceType {
        LEVELS,
        POINTS
    }

    @JsonProperty("amount")
    private final int amount;

    @JsonProperty("experienceType")
    private final ExperienceType experienceType;

    /**
     * Constructs a new {@code ExperienceReward} with the specified levels.
     *
     * @param levels the number of experience levels to reward
     * @since 1.0.0
     */
    public ExperienceReward(final int levels) {
        this(levels, ExperienceType.LEVELS);
    }

    /**
     * Constructs a new {@code ExperienceReward} with full configuration.
     *
     * @param amount         the amount of experience to reward
     * @param experienceType the type of experience (LEVELS or POINTS)
     * @since 1.0.0
     */
    @JsonCreator
    public ExperienceReward(
            @JsonProperty("amount") final int amount,
            @JsonProperty("experienceType") final @Nullable ExperienceType experienceType
    ) {
        super(Type.EXPERIENCE);

        if (amount <= 0) {
            throw new IllegalArgumentException("Experience amount must be positive: " + amount);
        }

        this.amount = amount;
        this.experienceType = experienceType != null ? experienceType : ExperienceType.LEVELS;
    }

    /**
     * Applies the configured experience reward to the provided player.
     *
     * @param player the player receiving the reward
     * @since 1.0.0
     */
    @Override
    public void apply(final @NotNull Player player) {
        switch (this.experienceType) {
            case LEVELS -> player.giveExpLevels(this.amount);
            case POINTS -> player.giveExp(this.amount);
        }
    }

    /**
     * Retrieves the translation key used to describe the reward in messages.
     *
     * @return the message key representing this reward type
     * @since 1.0.0
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return switch (this.experienceType) {
            case LEVELS -> "reward.experience.levels";
            case POINTS -> "reward.experience.points";
        };
    }

    /**
     * Gets the amount of experience granted by the reward.
     *
     * @return the configured experience amount
     * @since 1.0.0
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Gets the experience mode applied when rewarding the player.
     *
     * @return the configured experience type
     * @since 1.0.0
     */
    @NotNull
    public ExperienceType getExperienceType() {
        return this.experienceType;
    }
}