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

package com.raindropcentral.rplatform.reward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the Reward API type.
 */
public sealed interface Reward permits AbstractReward {

    /**
     * Gets typeId.
     */
    @NotNull String getTypeId();
    
    /**
     * Executes grant.
     */
    @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);
    
    /**
     * Gets estimatedValue.
     */
    double getEstimatedValue();
    
    /**
     * Executes this member.
     */
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
