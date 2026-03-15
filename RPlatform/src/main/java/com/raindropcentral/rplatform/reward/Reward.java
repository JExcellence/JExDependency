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
