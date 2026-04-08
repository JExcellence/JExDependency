package com.raindropcentral.rdq.quest.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.event.quest.QuestCompleteEvent;
import com.raindropcentral.rdq.event.quest.TaskCompleteEvent;
import com.raindropcentral.rdq.model.quest.RewardDistributionResult;
import com.raindropcentral.rdq.service.quest.RewardDistributor;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for quest and task completion events that sends notifications to players.
 * <p>
 * This listener handles:
 * <ul>
 *     <li>Task completion notifications</li>
 *     <li>Quest completion notifications with reward details</li>
 *     <li>Formatting of different reward types</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestNotificationListener implements Listener {
    
    private static final Logger LOGGER = Logger.getLogger(QuestNotificationListener.class.getName());
    
    private final RDQ plugin;
    private final RewardDistributor rewardDistributor;
    
    /**
     * Constructs a new quest notification listener.
     *
     * @param plugin            the RDQ plugin instance
     */
    public QuestNotificationListener(
            @NotNull final RDQ plugin
    ) {
        this.plugin = plugin;
        this.rewardDistributor = plugin.getRewardDistributor();
    }
    
    /**
     * Handles task completion events.
     * <p>
     * Sends a notification to the player when they complete a task.
     *
     * @param event the task complete event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTaskComplete(@NotNull final TaskCompleteEvent event) {
        final Player player = event.getPlayer();
        final Quest quest = event.getQuest();
        final String taskIdentifier = event.getTaskIdentifier();
        
        // Find the task
        // Find the task - need to reload with rewards to avoid LazyInitializationException
        final QuestTask task = quest.getTasks().stream()
                .filter(t -> t.getTaskIdentifier().equalsIgnoreCase(taskIdentifier))
                .findFirst()
                .orElse(null);
        
        if (task == null) {
            LOGGER.log(Level.WARNING, "Task not found for notification: " + taskIdentifier);
            return;
        }
        
        // Send task completion notification
        sendTaskCompletionNotification(player, quest, task);
        
        // Distribute task rewards if any - check if rewards collection is empty without triggering lazy load
        // The rewards collection might be lazy-loaded, so we check the size safely
        try {
            if (!task.getRewards().isEmpty()) {
                rewardDistributor.distributeTaskRewards(player.getUniqueId(), quest, task)
                        .thenAccept(result -> {
                            if (!result.allSuccessful()) {
                                LOGGER.log(Level.WARNING, "Some task rewards failed to distribute for player " + 
                                        player.getName() + " on task " + taskIdentifier);
                            }
                        })
                        .exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Error distributing task rewards for player " + 
                                    player.getName(), ex);
                            return null;
                        });
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Rewards collection not loaded - skip reward distribution for now
            // This is acceptable as task rewards are optional
            LOGGER.fine("Task rewards not loaded for task " + taskIdentifier + " - skipping reward distribution");
        }
    }
    
    /**
     * Handles quest completion events.
     * <p>
     * Sends a notification to the player when they complete a quest,
     * including a list of all rewards received.
     *
     * @param event the quest complete event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuestComplete(@NotNull final QuestCompleteEvent event) {
        final Player player = event.getPlayer();
        final Quest quest = event.getQuest();
        
        // Distribute quest rewards
        rewardDistributor.distributeQuestRewards(player.getUniqueId(), quest)
                .thenAccept(result -> {
                    // Send quest completion notification with rewards
                    sendQuestCompletionNotification(player, quest, result);
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error distributing quest rewards for player " + 
                            player.getName(), ex);
                    
                    // Still send completion notification even if rewards failed
                    sendQuestCompletionNotification(player, quest, null);
                    return null;
                });
    }
    
    /**
     * Sends a task completion notification to the player.
     *
     * @param player the player
     * @param quest  the quest
     * @param task   the completed task
     */
    private void sendTaskCompletionNotification(
            @NotNull final Player player,
            @NotNull final Quest quest,
            @NotNull final QuestTask task
    ) {
        // Get task name from icon
        final String taskName = task.getIcon().getDisplayNameKey();
        
        // Build notification message
        final Component message = new I18n.Builder("quest.task.complete", player)
                .withPlaceholder("quest", quest.getIcon().getDisplayNameKey())
                .withPlaceholder("task", taskName)
                .includePrefix()
                .build()
                .component();
        
        player.sendMessage(message);
    }
    
    /**
     * Sends a quest completion notification to the player with reward details.
     *
     * @param player the player
     * @param quest  the completed quest
     * @param result the reward distribution result (may be null if distribution failed)
     */
    private void sendQuestCompletionNotification(
            @NotNull final Player player,
            @NotNull final Quest quest,
            @NotNull final RewardDistributionResult result
    ) {
        // Build main completion message
        final Component completionMessage = new I18n.Builder("quest.complete", player)
                .withPlaceholder("quest", quest.getIcon().getDisplayNameKey())
                .includePrefix()
                .build()
                .component();
        
        player.sendMessage(completionMessage);
        
        // If no rewards or distribution failed, just send completion message
        if (result.getSuccessfulRewards().isEmpty()) {
            return;
        }
        
        // Send rewards header
        final Component rewardsHeader = new I18n.Builder("quest.rewards.header", player)
                .build()
                .component();
        
        player.sendMessage(rewardsHeader);
        
        // Send each reward
        for (final QuestReward reward : result.getSuccessfulRewards()) {
            final Component rewardMessage = formatReward(player, reward);
            if (rewardMessage != null) {
                player.sendMessage(rewardMessage);
            }
        }
    }
    
    /**
     * Formats a reward for display in the notification.
     *
     * @param player the player
     * @param reward the reward to format
     * @return the formatted reward component, or null if formatting failed
     */
    @NotNull
    private Component formatReward(
            @NotNull final Player player,
            @NotNull final QuestReward reward
    ) {
        final String rewardType = reward.getReward().getTypeId();
        final String descriptionKey = reward.getReward().getReward().getDescriptionKey();
        
        try {
            // Use generic reward formatting based on type ID
            return new I18n.Builder("quest.rewards." + rewardType.toLowerCase(), player)
                    .withPlaceholder("description", descriptionKey)
                    .withPlaceholder("value", String.format("%.2f", reward.getEstimatedValue()))
                    .build()
                    .component();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to format reward: " + rewardType, e);
            // Fallback to generic message
            return new I18n.Builder("quest.rewards.generic", player)
                    .withPlaceholder("type", rewardType)
                    .build()
                    .component();
        }
    }
}
