package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.model.quest.RewardDistributionResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for distributing quest and task rewards to players.
 * <p>
 * This interface defines the contract for reward distribution, including
 * error handling, retry logic, and result tracking.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since TBD
 */
public interface RewardDistributor {
    
    /**
     * Distributes all rewards for a completed quest.
     * <p>
     * This method attempts to distribute all rewards associated with the quest.
     * If any reward fails, the method continues with remaining rewards and
     * returns a result indicating which rewards succeeded and which failed.
     *
     * @param playerId the player's UUID
     * @param quest    the completed quest
     * @return a future completing with the distribution result
     */
    @NotNull
    CompletableFuture<RewardDistributionResult> distributeQuestRewards(
            @NotNull UUID playerId,
            @NotNull Quest quest
    );
    
    /**
     * Distributes all rewards for a completed quest task.
     * <p>
     * This method attempts to distribute all rewards associated with the task.
     * If any reward fails, the method continues with remaining rewards and
     * returns a result indicating which rewards succeeded and which failed.
     *
     * @param playerId the player's UUID
     * @param quest    the quest containing the task
     * @param task     the completed task
     * @return a future completing with the distribution result
     */
    @NotNull
    CompletableFuture<RewardDistributionResult> distributeTaskRewards(
            @NotNull UUID playerId,
            @NotNull Quest quest,
            @NotNull QuestTask task
    );
    
    /**
     * Distributes a single reward to a player.
     * <p>
     * This method attempts to distribute a single reward with retry logic
     * for transient failures (e.g., currency operations).
     *
     * @param player the player to reward
     * @param reward the reward to distribute
     * @return a future completing with true if successful, false otherwise
     */
    @NotNull
    CompletableFuture<Boolean> distributeReward(
            @NotNull Player player,
            @NotNull QuestReward reward
    );
    
    /**
     * Distributes a single reward to a player with retry logic.
     * <p>
     * This method attempts to distribute a reward up to the specified number
     * of times before giving up.
     *
     * @param player     the player to reward
     * @param reward     the reward to distribute
     * @param maxRetries the maximum number of retry attempts
     * @return a future completing with true if successful, false otherwise
     */
    @NotNull
    CompletableFuture<Boolean> distributeRewardWithRetry(
            @NotNull Player player,
            @NotNull QuestReward reward,
            int maxRetries
    );
}
