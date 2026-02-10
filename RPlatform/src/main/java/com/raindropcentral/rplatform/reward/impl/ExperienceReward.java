package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ExperienceReward extends AbstractReward {

    public enum ExperienceType {
        POINTS, LEVELS
    }

    private final int amount;
    private final ExperienceType type;

    @JsonCreator
    public ExperienceReward(
        @JsonProperty("amount") int amount,
        @JsonProperty("experienceType") ExperienceType type
    ) {
        this.amount = amount;
        this.type = type != null ? type : ExperienceType.POINTS;
    }

    @Override
    public @NotNull String getTypeId() {
        return "EXPERIENCE";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (type == ExperienceType.LEVELS) {
                player.giveExpLevels(amount);
            } else {
                player.giveExp(amount);
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() {
        return type == ExperienceType.LEVELS ? amount * 100.0 : amount;
    }

    public int getAmount() {
        return amount;
    }

    public ExperienceType getExperienceType() {
        return type;
    }

    @Override
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Experience amount must be positive");
        }
    }
}
