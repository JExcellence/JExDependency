package com.raindropcentral.rdq.model.quest;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the progress of a single task within a quest.
 * <p>
 * This record contains information about a task's completion status and progress.
 * </p>
 *
 * @param taskId the unique identifier of the task (Long ID)
 * @param taskIdentifier the string identifier of the task
 * @param taskName the display name of the task
 * @param currentCount the current progress count
 * @param requiredCount the required count for completion
 * @param isCompleted whether the task is completed
 * @param progressPercentage the progress as a percentage (0.0 to 1.0)
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public record TaskProgress(
    Long taskId,
    @NotNull String taskIdentifier,
    @NotNull String taskName,
    int currentCount,
    int requiredCount,
    boolean isCompleted,
    double progressPercentage
) {
    
    /**
     * Constructs a new TaskProgress with validation.
     */
    public TaskProgress {
        if (currentCount < 0) {
            throw new IllegalArgumentException("Current count cannot be negative");
        }
        if (requiredCount < 0) {
            throw new IllegalArgumentException("Required count cannot be negative");
        }
        if (progressPercentage < 0.0 || progressPercentage > 1.0) {
            throw new IllegalArgumentException("Progress percentage must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Alias for isCompleted for backward compatibility.
     *
     * @return true if the task is completed
     */
    public boolean completed() {
        return isCompleted;
    }
    
    /**
     * Gets the current progress count.
     *
     * @return the current progress
     */
    public int currentProgress() {
        return currentCount;
    }
    
    /**
     * Gets the required progress count.
     *
     * @return the required progress
     */
    public int requiredProgress() {
        return requiredCount;
    }
    
    /**
     * Gets the progress as a percentage string.
     *
     * @return the progress formatted as a percentage (e.g., "75%")
     */
    public String getProgressString() {
        return String.format("%.1f%%", progressPercentage * 100);
    }
    
    /**
     * Gets the progress as a fraction string.
     *
     * @return the progress formatted as a fraction (e.g., "3/4")
     */
    public String getProgressFraction() {
        return currentCount + "/" + requiredCount;
    }
    
    /**
     * Gets the remaining count needed for completion.
     *
     * @return the remaining count
     */
    public int getRemainingCount() {
        return Math.max(0, requiredCount - currentCount);
    }
    
    /**
     * Gets the completed count (for backward compatibility).
     *
     * @return 1 if completed, 0 otherwise
     */
    public int completedCount() {
        return isCompleted ? 1 : 0;
    }
}
