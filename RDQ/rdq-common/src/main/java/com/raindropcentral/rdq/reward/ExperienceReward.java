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
 * @version 1.0.0
 * @since TBD
 */
public final class ExperienceReward extends AbstractReward {

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
     * @param levels The number of experience levels to reward.
     */
    public ExperienceReward(final int levels) {
        this(levels, ExperienceType.LEVELS);
    }

    /**
     * Constructs a new {@code ExperienceReward} with full configuration.
     *
     * @param amount          The amount of experience to reward.
     * @param experienceType  The type of experience (LEVELS or POINTS).
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

    @Override
    public void apply(final @NotNull Player player) {
        switch (this.experienceType) {
            case LEVELS -> player.giveExpLevels(this.amount);
            case POINTS -> player.giveExp(this.amount);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return switch (this.experienceType) {
            case LEVELS -> "reward.experience.levels";
            case POINTS -> "reward.experience.points";
        };
    }

    public int getAmount() {
        return this.amount;
    }

    @NotNull
    public ExperienceType getExperienceType() {
        return this.experienceType;
    }
}