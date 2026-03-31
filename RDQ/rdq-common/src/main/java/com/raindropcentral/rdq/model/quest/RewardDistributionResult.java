package com.raindropcentral.rdq.model.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of distributing rewards to a player.
 * <p>
 * This record encapsulates the outcome of reward distribution, including
 * which rewards succeeded and which failed.
 * </p>
 *
 * @param allSuccessful true if all rewards were distributed successfully
 * @param rewards       the list of rewards that were attempted
 * @param results       the list of results (true = success, false = failure) corresponding to each reward
 * @author RaindropCentral
 * @version 1.0.0
 * @since TBD
 */
public record RewardDistributionResult(
        boolean allSuccessful,
        @NotNull List<QuestReward> rewards,
        @NotNull List<Boolean> results
) {
    
    /**
     * Constructs a new reward distribution result.
     *
     * @param allSuccessful true if all rewards succeeded
     * @param rewards       the rewards that were attempted
     * @param results       the results for each reward
     */
    public RewardDistributionResult {
        rewards = Collections.unmodifiableList(new ArrayList<>(rewards));
        results = Collections.unmodifiableList(new ArrayList<>(results));
        
        if (rewards.size() != results.size()) {
            throw new IllegalArgumentException(
                    "Rewards and results lists must have the same size"
            );
        }
    }
    
    /**
     * Gets the list of rewards that were successfully distributed.
     *
     * @return the successful rewards
     */
    @NotNull
    public List<QuestReward> getSuccessfulRewards() {
        final List<QuestReward> successful = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            if (results.get(i)) {
                successful.add(rewards.get(i));
            }
        }
        return successful;
    }
    
    /**
     * Gets the list of rewards that failed to distribute.
     *
     * @return the failed rewards
     */
    @NotNull
    public List<QuestReward> getFailedRewards() {
        final List<QuestReward> failed = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            if (!results.get(i)) {
                failed.add(rewards.get(i));
            }
        }
        return failed;
    }
    
    /**
     * Gets the number of successful rewards.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return (int) results.stream().filter(Boolean::booleanValue).count();
    }
    
    /**
     * Gets the number of failed rewards.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return (int) results.stream().filter(r -> !r).count();
    }
    
    /**
     * Gets the total number of rewards.
     *
     * @return the total count
     */
    public int getTotalCount() {
        return rewards.size();
    }
}
