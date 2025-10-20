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
 * @since 1.0.0
 */
public final class ExperienceLevelRequirement extends AbstractRequirement {

    /**
     * Types of experience that can be evaluated for this requirement.
     */
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

    /**
     * Creates a level-based requirement that consumes experience upon completion and displays no
     * custom description.
     *
     * @param requiredLevel the minimum level a player must have
     */
    public ExperienceLevelRequirement(final int requiredLevel) {
        this(requiredLevel, ExperienceType.LEVEL, true, null);
    }

    /**
     * Creates a requirement with a specific experience type that consumes experience upon
     * completion and displays no custom description.
     *
     * @param requiredLevel    the minimum level or experience a player must have
     * @param experienceType   the type of experience that should be evaluated
     */
    public ExperienceLevelRequirement(
            final int requiredLevel,
            final @NotNull ExperienceType experienceType
    ) {
        this(requiredLevel, experienceType, true, null);
    }

    /**
     * Creates a requirement with all configuration options supplied, supporting JSON
     * deserialization.
     *
     * @param requiredLevel      the minimum level or experience a player must have
     * @param experienceType     the type of experience that should be evaluated, defaults to level
     * @param consumeOnComplete  whether the experience should be consumed when completed, defaults
     *                           to {@code true}
     * @param description        the optional description to display when presenting the requirement
     * @throws IllegalArgumentException if {@code requiredLevel} is negative
     */
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

    /**
     * Determines whether the provided player satisfies the requirement.
     *
     * @param player the player whose experience should be evaluated
     * @return {@code true} if the player meets or exceeds the required experience amount,
     *     {@code false} otherwise
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.experienceType) {
            case LEVEL -> player.getLevel() >= this.requiredLevel;
            case POINTS -> player.getTotalExperience() >= this.requiredLevel;
        };
    }

    /**
     * Calculates the completion progress for the provided player.
     *
     * @param player the player whose progress should be measured
     * @return a normalized progress value between 0 and 1
     */
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

    /**
     * Consumes the required experience from the player if configured to do so and the player meets
     * the requirement.
     *
     * @param player the player whose experience should be consumed
     */
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

    /**
     * Resolves the translation key describing this requirement based on its experience type.
     *
     * @return the translation key for rendering the requirement description
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return switch (this.experienceType) {
            case LEVEL -> "requirement.experience_level";
            case POINTS -> "requirement.experience_points";
        };
    }

    /**
     * Gets the required level or experience amount that must be met.
     *
     * @return the required level or experience points
     */
    public int getRequiredLevel() {
        return this.requiredLevel;
    }

    /**
     * Gets the configured experience type for this requirement.
     *
     * @return the type of experience evaluated by the requirement
     */
    @NotNull
    public ExperienceType getExperienceType() {
        return this.experienceType;
    }

    /**
     * Indicates whether experience should be consumed after the requirement is satisfied.
     *
     * @return {@code true} if the requirement consumes experience when completed
     */
    public boolean isConsumeOnComplete() {
        return this.consumeOnComplete;
    }

    /**
     * Gets the optional human-readable description for the requirement.
     *
     * @return the custom description, or {@code null} if none is set
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Retrieves the player's current level or experience points depending on the configured type.
     *
     * @param player the player whose experience should be read
     * @return the player's current level or total experience
     */
    @JsonIgnore
    public int getCurrentExperience(final @NotNull Player player) {
        return switch (this.experienceType) {
            case LEVEL -> player.getLevel();
            case POINTS -> player.getTotalExperience();
        };
    }

    /**
     * Calculates how much additional experience the player requires to meet the requirement.
     *
     * @param player the player to evaluate
     * @return the deficit between the requirement and the player's current experience
     */
    @JsonIgnore
    public int getShortage(final @NotNull Player player) {
        final int current = this.getCurrentExperience(player);
        return Math.max(0, this.requiredLevel - current);
    }

    /**
     * Determines whether the requirement is level-based.
     *
     * @return {@code true} if the requirement checks player levels
     */
    @JsonIgnore
    public boolean isLevelBased() {
        return this.experienceType == ExperienceType.LEVEL;
    }

    /**
     * Determines whether the requirement is experience-point based.
     *
     * @return {@code true} if the requirement checks total experience points
     */
    @JsonIgnore
    public boolean isPointsBased() {
        return this.experienceType == ExperienceType.POINTS;
    }

    /**
     * Validates that the requirement is internally consistent and ready for use.
     *
     * @throws IllegalStateException if the requirement was constructed with invalid data
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredLevel < 0) {
            throw new IllegalStateException("Required level cannot be negative: " + this.requiredLevel);
        }
        if (this.experienceType == null) {
            throw new IllegalStateException("Experience type cannot be null");
        }
    }

    /**
     * Creates a requirement instance from plain values, parsing the experience type from its
     * string representation.
     *
     * @param requiredLevel     the minimum level or experience a player must have
     * @param experienceTypeString the string representation of the experience type
     * @param consumeOnComplete whether the experience should be consumed when the requirement is
     *                          completed
     * @return a new {@link ExperienceLevelRequirement} configured with the provided values
     * @throws IllegalArgumentException if {@code experienceTypeString} cannot be parsed
     */
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