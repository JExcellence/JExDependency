package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the ExperienceLevelRequirement API type.
 */
public final class ExperienceLevelRequirement extends AbstractRequirement {

    /**
     * Represents the ExperienceType API type.
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
     * Executes ExperienceLevelRequirement.
     */
    public ExperienceLevelRequirement(int requiredLevel) {
        this(requiredLevel, ExperienceType.LEVEL, true, null);
    }

    /**
     * Executes ExperienceLevelRequirement.
     */
    public ExperienceLevelRequirement(int requiredLevel, @NotNull ExperienceType experienceType) {
        this(requiredLevel, experienceType, true, null);
    }

    /**
     * Executes ExperienceLevelRequirement.
     */
    @JsonCreator
    public ExperienceLevelRequirement(@JsonProperty("requiredLevel") int requiredLevel,
                                     @JsonProperty("experienceType") @Nullable ExperienceType experienceType,
                                     @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete,
                                     @JsonProperty("description") @Nullable String description) {
        super("EXPERIENCE_LEVEL");

        if (requiredLevel < 0) {
            throw new IllegalArgumentException("Required level cannot be negative: " + requiredLevel);
        }

        this.requiredLevel = requiredLevel;
        this.experienceType = experienceType != null ? experienceType : ExperienceType.LEVEL;
        this.consumeOnComplete = consumeOnComplete != null ? consumeOnComplete : true;
        this.description = description;
    }

    /**
     * Returns whether met.
     */
    @Override
    public boolean isMet(@NotNull Player player) {
        return switch (experienceType) {
            case LEVEL -> player.getLevel() >= requiredLevel;
            case POINTS -> player.getTotalExperience() >= requiredLevel;
        };
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredLevel <= 0) {
            return 1.0;
        }

        var currentAmount = switch (experienceType) {
            case LEVEL -> player.getLevel();
            case POINTS -> player.getTotalExperience();
        };

        var progress = (double) currentAmount / requiredLevel;
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(@NotNull Player player) {
        if (!consumeOnComplete) return;

        switch (experienceType) {
            case LEVEL -> {
                if (player.getLevel() >= requiredLevel) {
                    player.setLevel(player.getLevel() - requiredLevel);
                }
            }
            case POINTS -> {
                if (player.getTotalExperience() >= requiredLevel) {
                    var newTotalExp = player.getTotalExperience() - requiredLevel;
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
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return switch (experienceType) {
            case LEVEL -> "requirement.experience_level";
            case POINTS -> "requirement.experience_points";
        };
    }

    /**
     * Gets requiredLevel.
     */
    public int getRequiredLevel() { return requiredLevel; }
    /**
     * Returns whether consumeOnComplete.
     */
    @NotNull public ExperienceType getExperienceType() { return experienceType; }
    public boolean isConsumeOnComplete() { return consumeOnComplete; }
    @Nullable public String getDescription() { return description; }

    /**
     * Gets currentExperience.
     */
    @JsonIgnore
    public int getCurrentExperience(@NotNull Player player) {
        return switch (experienceType) {
            case LEVEL -> player.getLevel();
            case POINTS -> player.getTotalExperience();
        };
    }

    /**
     * Gets shortage.
     */
    @JsonIgnore
    public int getShortage(@NotNull Player player) {
        var current = getCurrentExperience(player);
        return Math.max(0, requiredLevel - current);
    }

    /**
     * Returns whether levelBased.
     */
    @JsonIgnore
    public boolean isLevelBased() { return experienceType == ExperienceType.LEVEL; }

    /**
     * Returns whether pointsBased.
     */
    @JsonIgnore
    public boolean isPointsBased() { return experienceType == ExperienceType.POINTS; }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (requiredLevel < 0) {
            throw new IllegalStateException("Required level cannot be negative: " + requiredLevel);
        }
        if (experienceType == null) {
            throw new IllegalStateException("Experience type cannot be null");
        }
    }

    /**
     * Executes fromString.
     */
    @JsonIgnore
    @NotNull
    public static ExperienceLevelRequirement fromString(int requiredLevel, @NotNull String experienceTypeString, boolean consumeOnComplete) {
        ExperienceType experienceType;
        try {
            experienceType = ExperienceType.valueOf(experienceTypeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid experience type: " + experienceTypeString + ". Valid types are: LEVEL, POINTS."
            );
        }
        return new ExperienceLevelRequirement(requiredLevel, experienceType, consumeOnComplete, null);
    }
}
