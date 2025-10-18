package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Requirement that checks if a player has a minimum experience level or experience points.
 * <p>
 * The {@code ExperienceLevelRequirement} is satisfied when the player has at least the specified
 * experience level or experience points. When consumed, the required amount is subtracted from
 * the player's experience.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public final class ExperienceLevelRequirement extends AbstractRequirement {

    public enum ExperienceType {
        LEVEL,
        POINTS
    }

    @JsonProperty("requiredLevel")
    private final int requiredLevel;

    @JsonProperty("experienceType")
    private final ExperienceType experienceType;

    @JsonProperty("consumeOnComplete")
    private final boolean consumeOnComplete;

    @JsonProperty("description")
    private final String description;

    public ExperienceLevelRequirement(final int requiredLevel) {
        this(requiredLevel, ExperienceType.LEVEL, true, null);
    }

    public ExperienceLevelRequirement(
            final int requiredLevel,
            final @NotNull ExperienceType experienceType
    ) {
        this(requiredLevel, experienceType, true, null);
    }

    @JsonCreator
    public ExperienceLevelRequirement(
            @JsonProperty("requiredLevel") final int requiredLevel,
            @JsonProperty("experienceType") final @Nullable ExperienceType experienceType,
            @JsonProperty("consumeOnComplete") final @Nullable Boolean consumeOnComplete,
            @JsonProperty("description") final @Nullable String description
    ) {
        super(Type.EXPERIENCE_LEVEL);

        if (requiredLevel < 0) {
            throw new IllegalArgumentException("Required level cannot be negative: " + requiredLevel);
        }

        this.requiredLevel = requiredLevel;
        this.experienceType = experienceType != null ? experienceType : ExperienceType.LEVEL;
        this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
        this.description = description;
    }

    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.experienceType) {
            case LEVEL -> player.getLevel() >= this.requiredLevel;
            case POINTS -> player.getTotalExperience() >= this.requiredLevel;
        };
    }

    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.requiredLevel <= 0) {
            return 1.0;
        }

        final double currentAmount = switch (this.experienceType) {
            case LEVEL -> player.getLevel();
            case POINTS -> player.getTotalExperience();
        };

        final double progress = currentAmount / this.requiredLevel;
        return Math.max(0.0, Math.min(1.0, progress));
    }

    @Override
    public void consume(final @NotNull Player player) {
        if (!this.consumeOnComplete) {
            return;
        }

        switch (this.experienceType) {
            case LEVEL -> {
                if (player.getLevel() >= this.requiredLevel) {
                    player.setLevel(player.getLevel() - this.requiredLevel);
                }
            }
            case POINTS -> {
                if (player.getTotalExperience() >= this.requiredLevel) {
                    final int newTotalExp = player.getTotalExperience() - this.requiredLevel;
                    player.setTotalExperience(0);
                    player.setLevel(0);
                    player.setExp(0);
                    if (newTotalExp > 0) {
                        player.giveExp(newTotalExp);
                    }
                }
            }
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return switch (this.experienceType) {
            case LEVEL -> "requirement.experience_level";
            case POINTS -> "requirement.experience_points";
        };
    }

    public int getRequiredLevel() {
        return this.requiredLevel;
    }

    @NotNull
    public ExperienceType getExperienceType() {
        return this.experienceType;
    }

    public boolean isConsumeOnComplete() {
        return this.consumeOnComplete;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @JsonIgnore
    public int getCurrentExperience(final @NotNull Player player) {
        return switch (this.experienceType) {
            case LEVEL -> player.getLevel();
            case POINTS -> player.getTotalExperience();
        };
    }

    @JsonIgnore
    public int getShortage(final @NotNull Player player) {
        final int current = this.getCurrentExperience(player);
        return Math.max(0, this.requiredLevel - current);
    }

    @JsonIgnore
    public boolean isLevelBased() {
        return this.experienceType == ExperienceType.LEVEL;
    }

    @JsonIgnore
    public boolean isPointsBased() {
        return this.experienceType == ExperienceType.POINTS;
    }

    @JsonIgnore
    public void validate() {
        if (this.requiredLevel < 0) {
            throw new IllegalStateException("Required level cannot be negative: " + this.requiredLevel);
        }
        if (this.experienceType == null) {
            throw new IllegalStateException("Experience type cannot be null");
        }
    }

    @JsonIgnore
    @NotNull
    public static ExperienceLevelRequirement fromString(
            final int requiredLevel,
            final @NotNull String experienceTypeString,
            final boolean consumeOnComplete
    ) {
        final ExperienceType experienceType;
        try {
            experienceType = ExperienceType.valueOf(experienceTypeString.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid experience type: " + experienceTypeString + ". Valid types are: LEVEL, POINTS."
            );
        }
        return new ExperienceLevelRequirement(requiredLevel, experienceType, consumeOnComplete, null);
    }
}