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

/**
 * Represents a composite reward that aggregates multiple sub-rewards.
 * <p>
 * When this reward is applied, it sequentially applies each contained {@link AbstractReward}
 * to the specified player. This allows for complex reward structures by combining
 * different reward types (e.g., items, commands, currency) into a single reward.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public final class CompositeReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(CompositeReward.class.getName());

    @JsonProperty("rewards")
    private final List<AbstractReward> rewards;

    @JsonProperty("continueOnError")
    private final boolean continueOnError;

    /**
     * Constructs a new {@code CompositeReward} with the specified list of sub-rewards.
     *
     * @param rewards The list of {@link AbstractReward} instances to be applied.
     */
    public CompositeReward(final @NotNull List<AbstractReward> rewards) {
        this(rewards, true);
    }

    /**
     * Constructs a new {@code CompositeReward} with full configuration.
     *
     * @param rewards          The list of sub-rewards.
     * @param continueOnError  Whether to continue applying rewards if one fails.
     */
    @JsonCreator
    public CompositeReward(
            @JsonProperty("rewards") final @NotNull List<AbstractReward> rewards,
            @JsonProperty("continueOnError") final boolean continueOnError
    ) {
        super(Type.COMPOSITE);

        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("Rewards list cannot be null or empty");
        }

        this.rewards = new ArrayList<>(rewards);
        this.continueOnError = continueOnError;
    }

    @Override
    public void apply(final @NotNull Player player) {
        for (final AbstractReward reward : this.rewards) {
            try {
                reward.apply(player);
            } catch (final Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to apply sub-reward in composite reward", exception);
                if (!this.continueOnError) {
                    throw new RuntimeException("Failed to apply composite reward", exception);
                }
            }
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.composite";
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

        for (int i = 0; i < this.rewards.size(); i++) {
            final AbstractReward reward = this.rewards.get(i);
            if (reward == null) {
                throw new IllegalStateException("Sub-reward at index " + i + " is null");
            }
        }
    }
}