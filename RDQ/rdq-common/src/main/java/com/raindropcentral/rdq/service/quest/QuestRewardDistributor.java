package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestTaskReward;
import com.raindropcentral.rdq.model.quest.RewardDistributionResult;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for distributing quest and task rewards to players.
 * <p>
 * This service handles all types of rewards by delegating to the underlying
 * {@link com.raindropcentral.rdq.database.entity.reward.BaseReward} system.
 * It provides enhanced error handling, retry logic, and detailed result tracking.
 * </p>
 * 
 * <h2>Reward Distribution Process:</h2>
 * <ol>
 *     <li>Validate player is online and eligible</li>
 *     <li>Process each reward through BaseReward.grant() with retry logic</li>
 *     <li>Track success/failure for each reward</li>
 *     <li>Send confirmation messages to player</li>
 *     <li>Log reward distribution for audit</li>
 * </ol>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since TBD
 */
public class QuestRewardDistributor implements RewardDistributor {
    
    private final Logger LOGGER;
    private static final int DEFAULT_MAX_RETRIES = 1;
    private static final long RETRY_DELAY_MS = 100;
    
    private final RDQ rdq;
    
    /**
     * Constructs a new quest reward distributor.
     *
     * @param rdq the RDQ plugin instance
     */
    public QuestRewardDistributor(@NotNull final RDQ rdq) {
        this.rdq = rdq;
        this.LOGGER = rdq.getPlugin().getLogger();
    }
    
    @Override
    @NotNull
    public CompletableFuture<RewardDistributionResult> distributeQuestRewards(
            @NotNull final UUID playerId,
            @NotNull final Quest quest
    ) {
        LOGGER.fine("Distributing quest rewards for player " + playerId + ", quest: " + quest.getIdentifier());
        
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            LOGGER.warning("Cannot distribute quest rewards - player offline: " + playerId);
            return CompletableFuture.completedFuture(
                    new RewardDistributionResult(false, List.of(), List.of())
            );
        }
        
        // Get rewards - they should be eagerly loaded now
        final List<QuestReward> rewards = quest.getRewards();
        if (rewards.isEmpty()) {
            LOGGER.fine("No quest rewards to distribute for quest: " + quest.getIdentifier());
            return CompletableFuture.completedFuture(
                    new RewardDistributionResult(true, List.of(), List.of())
            );
        }
        
        // Send reward notification header
        rdq.getPlatform().getScheduler().runAtEntity(player, () -> {
            new I18n.Builder("quest.rewards.header", player)
                    .withPlaceholder("quest", quest.getIdentifier())
                    .build()
                    .sendMessage();
        });
        
        // Distribute each reward and collect results
        final List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (QuestReward questReward : rewards) {
            if (questReward.isAutoGrant()) {
                futures.add(distributeRewardWithRetry(player, questReward, DEFAULT_MAX_RETRIES));
            } else {
                // Skip non-auto-grant rewards
                futures.add(CompletableFuture.completedFuture(true));
            }
        }
        
        // Wait for all rewards to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    final List<Boolean> results = futures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    
                    final boolean allSuccessful = results.stream().allMatch(Boolean::booleanValue);
                    final RewardDistributionResult result = new RewardDistributionResult(
                            allSuccessful,
                            rewards,
                            results
                    );
                    
                    // Send completion message
                    rdq.getPlatform().getScheduler().runAtEntity(player, () -> {
                        if (allSuccessful) {
                            new I18n.Builder("quest.rewards.complete", player)
                                    .withPlaceholder("count", String.valueOf(result.getSuccessCount()))
                                    .build()
                                    .sendMessage();
                        } else {
                            new I18n.Builder("quest.rewards.partial", player)
                                    .withPlaceholder("success", String.valueOf(result.getSuccessCount()))
                                    .withPlaceholder("total", String.valueOf(result.getTotalCount()))
                                    .build()
                                    .sendMessage();
                        }
                    });
                    
                    LOGGER.info("Distributed " + result.getSuccessCount() + "/" + result.getTotalCount() + 
                               " quest rewards to player " + player.getName() + 
                               " for quest " + quest.getIdentifier());
                    
                    return result;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error distributing quest rewards for player " + 
                              player.getName() + ", quest: " + quest.getIdentifier(), ex);
                    return new RewardDistributionResult(false, rewards, 
                            rewards.stream().map(r -> false).toList());
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<RewardDistributionResult> distributeTaskRewards(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final QuestTask task
    ) {
        LOGGER.fine("Distributing task rewards for player " + playerId + 
                   ", quest: " + quest.getIdentifier() + 
                   ", task: " + task.getTaskIdentifier());
        
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            LOGGER.warning("Cannot distribute task rewards - player offline: " + playerId);
            return CompletableFuture.completedFuture(
                    new RewardDistributionResult(false, List.of(), List.of())
            );
        }
        
        // Get task rewards - they should be eagerly loaded now
        final List<QuestTaskReward> taskRewards = task.getRewards();
        if (taskRewards.isEmpty()) {
            LOGGER.fine("No task rewards to distribute for task: " + task.getTaskIdentifier());
            return CompletableFuture.completedFuture(
                    new RewardDistributionResult(true, List.of(), List.of())
            );
        }
        
        // Send task reward notification
        rdq.getPlatform().getScheduler().runAtEntity(player, () -> {
            new I18n.Builder("quest.task.rewards.header", player)
                    .withPlaceholder("task", task.getTaskIdentifier())
                    .build()
                    .sendMessage();
        });
        
        // Distribute each reward and collect results
        final List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (QuestTaskReward taskReward : taskRewards) {
            if (taskReward.isAutoGrant()) {
                futures.add(distributeTaskRewardInternal(player, taskReward));
            } else {
                futures.add(CompletableFuture.completedFuture(true));
            }
        }
        
        // Wait for all rewards to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    final List<Boolean> results = futures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    
                    final boolean allSuccessful = results.stream().allMatch(Boolean::booleanValue);
                    
                    // Convert task rewards to quest rewards for result (empty list since different type)
                    final RewardDistributionResult result = new RewardDistributionResult(
                            allSuccessful,
                            List.of(),  // Task rewards are different type
                            results
                    );
                    
                    LOGGER.info("Distributed " + result.getSuccessCount() + "/" + result.getTotalCount() + 
                               " task rewards to player " + player.getName() + 
                               " for task " + task.getTaskIdentifier());
                    
                    return result;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error distributing task rewards for player " + 
                              player.getName() + ", task: " + task.getTaskIdentifier(), ex);
                    return new RewardDistributionResult(false, List.of(), 
                            taskRewards.stream().map(r -> false).toList());
                });
    }
    
    /**
     * Distributes a single task reward with retry logic.
     *
     * @param player     the player
     * @param taskReward the task reward
     * @return a future completing with true if successful
     */
    @NotNull
    private CompletableFuture<Boolean> distributeTaskRewardInternal(
            @NotNull final Player player,
            @NotNull final QuestTaskReward taskReward
    ) {
        return taskReward.grant(player)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error granting task reward", ex);
                    // Retry once
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                        return taskReward.grant(player).join();
                    } catch (Exception retryEx) {
                        LOGGER.log(Level.SEVERE, "Retry failed for task reward", retryEx);
                        return false;
                    }
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> distributeReward(
            @NotNull final Player player,
            @NotNull final QuestReward reward
    ) {
        return distributeRewardWithRetry(player, reward, DEFAULT_MAX_RETRIES);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> distributeRewardWithRetry(
            @NotNull final Player player,
            @NotNull final QuestReward reward,
            final int maxRetries
    ) {
        return attemptRewardDistribution(player, reward, 0, maxRetries);
    }
    
    /**
     * Attempts to distribute a reward with retry logic.
     *
     * @param player      the player
     * @param reward      the reward
     * @param attempt     the current attempt number
     * @param maxRetries  the maximum number of retries
     * @return a future completing with true if successful
     */
    @NotNull
    private CompletableFuture<Boolean> attemptRewardDistribution(
            @NotNull final Player player,
            @NotNull final QuestReward reward,
            final int attempt,
            final int maxRetries
    ) {
        return reward.grant(player)
                .thenApply(success -> {
                    if (success) {
                        LOGGER.fine("Successfully distributed reward to " + player.getName() + 
                                   " (attempt " + (attempt + 1) + ")");
                    } else {
                        LOGGER.warning("Failed to distribute reward to " + player.getName() + 
                                      " (attempt " + (attempt + 1) + ")");
                    }
                    return success;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Error distributing reward to " + player.getName() + 
                              " (attempt " + (attempt + 1) + ")", ex);
                    return false;
                })
                .thenCompose(success -> {
                    if (success || attempt >= maxRetries) {
                        return CompletableFuture.completedFuture(success);
                    }
                    
                    // Retry after delay
                    LOGGER.fine("Retrying reward distribution for " + player.getName() + 
                               " (attempt " + (attempt + 2) + "/" + (maxRetries + 1) + ")");
                    
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }).thenCompose(v -> attemptRewardDistribution(player, reward, attempt + 1, maxRetries));
                });
    }
}
