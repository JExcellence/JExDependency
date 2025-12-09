package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CompositeReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CompositeReward.class.getName());

    @JsonProperty("rewards")
    private final List<AbstractReward> rewards;

    @JsonProperty("continueOnError")
    private final boolean continueOnError;

    public CompositeReward(@NotNull List<AbstractReward> rewards) {
        this(rewards, true);
    }

    @JsonCreator
    public CompositeReward(
            @JsonProperty("rewards") @NotNull List<AbstractReward> rewards,
            @JsonProperty("continueOnError") boolean continueOnError
    ) {
        super(Type.COMPOSITE, "reward.composite");

        if (rewards.isEmpty()) {
            throw new IllegalArgumentException("Rewards list cannot be null or empty");
        }

        this.rewards = new ArrayList<>(rewards);
        this.continueOnError = continueOnError;
    }

    @Override
    public @NotNull java.util.concurrent.CompletableFuture<Boolean> grant(@NotNull Player player) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            for (var reward : this.rewards) {
                try {
                    reward.grant(player).join();
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Failed to apply sub-reward in composite reward", exception);
                    if (!this.continueOnError) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() {
        return rewards.stream().mapToDouble(AbstractReward::getEstimatedValue).sum();
    }

    @NotNull
    public List<AbstractReward> getRewards() {
        return new ArrayList<>(this.rewards);
    }

    public boolean isContinueOnError() {
        return this.continueOnError;
    }

    @JsonIgnore
    public int getRewardCount() {
        return this.rewards.size();
    }

    @JsonIgnore
    public void validate() {
        if (this.rewards.isEmpty()) {
            throw new IllegalStateException("CompositeReward must have at least one sub-reward");
        }

        for (var i = 0; i < this.rewards.size(); i++) {
            var reward = this.rewards.get(i);
            if (reward == null) {
                throw new IllegalStateException("Sub-reward at index " + i + " is null");
            }
        }
    }
}