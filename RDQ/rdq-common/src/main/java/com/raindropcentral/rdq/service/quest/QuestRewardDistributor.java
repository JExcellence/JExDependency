package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestTaskReward;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
 * </p>
 * 
 * <h2>Reward Distribution Process:</h2>
 * <ol>
 *     <li>Validate player is online and eligible</li>
 *     <li>Process each reward through BaseReward.grant()</li>
 *     <li>Send confirmation messages to player</li>
 *     <li>Log reward distribution for audit</li>
 * </ol>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since TBD
 */
public class QuestRewardDistributor {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final RDQ rdq;
    
    /**
     * Constructs a new quest reward distributor.
     *
     * @param rdq the RDQ plugin instance
     */
    public QuestRewardDistributor(@NotNull final RDQ rdq) {
        this.rdq = rdq;
    }
    
    /**
     * Distributes all rewards for completing a quest.
     *
     * @param playerId the player's UUID
     * @param quest the completed quest
     * @return a future that completes when all rewards are distributed
     */
    @NotNull
    public CompletableFuture<Void> distributeQuestRewards(
            @NotNull final UUID playerId,
            @NotNull final Quest quest
    ) {
        LOGGER.fine("Distributing quest rewards for player " + playerId + ", quest: " + quest.getIdentifier());
        
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            LOGGER.warning("Cannot distribute quest rewards - player offline: " + playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        final List<QuestReward> rewards = quest.getRewards();
        if (rewards.isEmpty()) {
            LOGGER.fine("No quest rewards to distribute for quest: " + quest.getIdentifier());
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Send reward notification header
                Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                    new I18n.Builder("quest.rewards.header", player)
                            .withPlaceholder("quest", quest.getDisplayName())
                            .build()
                            .sendMessage();
                });
                
                // Distribute each reward
                for (QuestReward questReward : rewards) {
                    if (questReward.isAutoGrant()) {
                        questReward.grant(player).exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Error granting quest reward", ex);
                            return false;
                        });
                    }
                }
                
                // Send completion message
                Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                    new I18n.Builder("quest.rewards.complete", player)
                            .withPlaceholder("count", String.valueOf(rewards.size()))
                            .build()
                            .sendMessage();
                });
                
                LOGGER.info("Successfully distributed " + rewards.size() + 
                           " quest rewards to player " + player.getName() + 
                           " for quest " + quest.getIdentifier());
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error distributing quest rewards for player " + 
                          player.getName() + ", quest: " + quest.getIdentifier(), e);
            }
        });
    }
    
    /**
     * Distributes all rewards for completing a quest task.
     *
     * @param playerId the player's UUID
     * @param quest the quest containing the task
     * @param task the completed task
     * @return a future that completes when all rewards are distributed
     */
    @NotNull
    public CompletableFuture<Void> distributeTaskRewards(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final QuestTask task
    ) {
        LOGGER.fine("Distributing task rewards for player " + playerId + 
                   ", quest: " + quest.getIdentifier() + 
                   ", task: " + task.getIdentifier());
        
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            LOGGER.warning("Cannot distribute task rewards - player offline: " + playerId);
            return CompletableFuture.completedFuture(null);
        }
        
        final List<QuestTaskReward> rewards = task.getRewards();
        if (rewards.isEmpty()) {
            LOGGER.fine("No task rewards to distribute for task: " + task.getIdentifier());
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Send task reward notification
                Bukkit.getScheduler().runTask(rdq.getPlugin(), () -> {
                    new I18n.Builder("quest.task.rewards.header", player)
                            .withPlaceholder("task", task.getDisplayName())
                            .build()
                            .sendMessage();
                });
                
                // Distribute each reward
                for (QuestTaskReward taskReward : rewards) {
                    if (taskReward.isAutoGrant()) {
                        taskReward.grant(player).exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Error granting task reward", ex);
                            return false;
                        });
                    }
                }
                
                LOGGER.info("Successfully distributed " + rewards.size() + 
                           " task rewards to player " + player.getName() + 
                           " for task " + task.getIdentifier());
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error distributing task rewards for player " + 
                          player.getName() + ", task: " + task.getIdentifier(), e);
            }
        });
    }
}
