/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the ExperienceReward API type.
 */
public final class ExperienceReward extends AbstractReward {

    /**
     * Represents the ExperienceType API type.
     */
    public enum ExperienceType {
        POINTS, LEVELS
    }

    private final int amount;
    private final ExperienceType type;

    /**
     * Executes ExperienceReward.
     */
    @JsonCreator
    public ExperienceReward(
        @JsonProperty("amount") int amount,
        @JsonProperty("experienceType") ExperienceType type
    ) {
        this.amount = amount;
        this.type = type != null ? type : ExperienceType.POINTS;
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "EXPERIENCE";
    }

    /**
     * Executes grant.
     */
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

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return type == ExperienceType.LEVELS ? amount * 100.0 : amount;
    }

    /**
     * Gets amount.
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Gets experienceType.
     */
    public ExperienceType getExperienceType() {
        return type;
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Experience amount must be positive");
        }
    }
}
