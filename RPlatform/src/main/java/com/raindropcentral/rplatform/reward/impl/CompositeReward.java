package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the CompositeReward API type.
 */
@JsonTypeName("COMPOSITE")
public final class CompositeReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CompositeReward.class.getName());

    private final List<AbstractReward> rewards;
    private final boolean continueOnError;

    /**
     * Executes CompositeReward.
     */
    @JsonCreator
    public CompositeReward(
        @JsonProperty("rewards") @NotNull List<AbstractReward> rewards,
        @JsonProperty("continueOnError") boolean continueOnError
    ) {
        this.rewards = new ArrayList<>(rewards);
        this.continueOnError = continueOnError;
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "COMPOSITE";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        if (rewards.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        if (continueOnError) {
            return grantAllContinueOnError(player);
        } else {
            return grantAllStopOnError(player);
        }
    }

    private CompletableFuture<Boolean> grantAllStopOnError(@NotNull Player player) {
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        
        for (AbstractReward reward : rewards) {
            result = result.thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.completedFuture(false);
                }
                return reward.grant(player);
            });
        }
        
        return result;
    }

    private CompletableFuture<Boolean> grantAllContinueOnError(@NotNull Player player) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (AbstractReward reward : rewards) {
            CompletableFuture<Boolean> future = reward.grant(player)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to grant reward: " + reward.getTypeId(), throwable);
                    return false;
                });
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                boolean anySuccess = false;
                for (CompletableFuture<Boolean> future : futures) {
                    if (future.join()) {
                        anySuccess = true;
                    }
                }
                return anySuccess;
            });
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return rewards.stream()
            .mapToDouble(AbstractReward::getEstimatedValue)
            .sum();
    }

    /**
     * Gets rewards.
     */
    public List<AbstractReward> getRewards() {
        return List.copyOf(rewards);
    }

    /**
     * Returns whether continueOnError.
     */
    public boolean isContinueOnError() {
        return continueOnError;
    }

    /**
     * Gets rewardCount.
     */
    public int getRewardCount() {
        return rewards.size();
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (rewards.isEmpty()) {
            throw new IllegalArgumentException("Composite reward must have at least one reward");
        }
        for (AbstractReward reward : rewards) {
            reward.validate();
        }
    }
}
