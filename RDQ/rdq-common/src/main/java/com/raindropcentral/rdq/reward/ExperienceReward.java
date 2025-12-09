package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExperienceReward extends AbstractReward {

    public enum ExperienceType {
        LEVELS,
        POINTS
    }

    @JsonProperty("amount")
    private final int amount;

    @JsonProperty("experienceType")
    private final ExperienceType experienceType;

    public ExperienceReward(int levels) {
        this(levels, ExperienceType.LEVELS);
    }

    @JsonCreator
    public ExperienceReward(@JsonProperty("amount") int amount,
                           @JsonProperty("experienceType") @Nullable ExperienceType experienceType) {
        super(Type.EXPERIENCE, "reward.experience");

        if (amount <= 0) {
            throw new IllegalArgumentException("Experience amount must be positive: " + amount);
        }

        this.amount = amount;
        this.experienceType = experienceType != null ? experienceType : ExperienceType.LEVELS;
    }

    @Override
    public @NotNull java.util.concurrent.CompletableFuture<Boolean> grant(@NotNull Player player) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            switch (experienceType) {
                case LEVELS -> player.giveExpLevels(amount);
                case POINTS -> player.giveExp(amount);
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() {
        return experienceType == ExperienceType.LEVELS ? amount * 10.0 : amount * 0.1;
    }

    public int getAmount() {
        return amount;
    }

    @NotNull
    public ExperienceType getExperienceType() {
        return experienceType;
    }
}