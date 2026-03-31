package com.raindropcentral.rdq.model.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Contains comprehensive state information about a quest for a specific player.
 *
 * @param state the current quest state
 * @param activeQuest the active quest data (if ACTIVE)
 * @param completionHistory the completion history (if completed before)
 * @param remainingCooldownSeconds remaining cooldown time in seconds
 * @param missingRequirements list of missing requirement descriptions
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public record QuestStateInfo(
        @NotNull QuestState state,
        @Nullable QuestUser activeQuest,
        @Nullable QuestCompletionHistory completionHistory,
        long remainingCooldownSeconds,
        @NotNull List<String> missingRequirements
) {
    
    /**
     * Checks if the quest can be started.
     *
     * @return true if the quest can be started
     */
    public boolean canStart() {
        return state == QuestState.AVAILABLE || 
               state == QuestState.AVAILABLE_TO_RESTART;
    }
    
    /**
     * Checks if the quest is currently active.
     *
     * @return true if the quest is active
     */
    public boolean isActive() {
        return state == QuestState.ACTIVE;
    }
    
    /**
     * Checks if the quest has been completed.
     *
     * @return true if the quest is completed or finished
     */
    public boolean isCompleted() {
        return state == QuestState.COMPLETED || 
               state == QuestState.FINISHED;
    }
    
    /**
     * Checks if the quest is locked.
     *
     * @return true if the quest is locked
     */
    public boolean isLocked() {
        return state == QuestState.LOCKED;
    }
    
    /**
     * Checks if the quest is on cooldown.
     *
     * @return true if the quest is on cooldown
     */
    public boolean isOnCooldown() {
        return state == QuestState.ON_COOLDOWN;
    }
    
    /**
     * Gets the progress percentage for active quests.
     *
     * @return progress percentage (0-100), or 0 if not active
     */
    public double getProgressPercentage() {
        if (activeQuest == null) {
            return 0.0;
        }
        
        final long completedTasks = activeQuest.getTaskProgress().stream()
                .mapToLong(tp -> tp.isCompleted() ? 1 : 0)
                .sum();
        
        final int totalTasks = activeQuest.getQuest().getTasks().size();
        
        return totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;
    }
    
    /**
     * Gets the number of completed tasks for active quests.
     *
     * @return number of completed tasks, or 0 if not active
     */
    public int getCompletedTaskCount() {
        if (activeQuest == null) {
            return 0;
        }
        
        return (int) activeQuest.getTaskProgress().stream()
                .mapToLong(tp -> tp.isCompleted() ? 1 : 0)
                .sum();
    }
    
    /**
     * Gets the total number of tasks for active quests.
     *
     * @return total number of tasks, or 0 if not active
     */
    public int getTotalTaskCount() {
        if (activeQuest == null) {
            return 0;
        }
        
        return activeQuest.getQuest().getTasks().size();
    }
}
