/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.quest.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Immutable record representing the overall progress of a quest.
 *
 * <p>This record provides a summary view of quest progress, including
 * completed task count and detailed task-by-task progress.
 *
 * @param questId        the unique quest identifier
 * @param completedTasks the number of completed tasks
 * @param totalTasks     the total number of tasks
 * @param taskProgress   map of task identifiers to their progress details
 * @author RaindropCentral
 * @version 1.0.0
 */
public record QuestProgress(
        @NotNull String questId,
        int completedTasks,
        int totalTasks,
        @NotNull Map<String, TaskProgress> taskProgress
) {
    
    /**
     * Compact constructor with validation.
     */
    public QuestProgress {
        if (questId.isBlank()) {
            throw new IllegalArgumentException("Quest ID cannot be null or blank");
        }
        
        if (completedTasks < 0) {
            throw new IllegalArgumentException("Completed tasks cannot be negative");
        }
        
        if (totalTasks < 0) {
            throw new IllegalArgumentException("Total tasks cannot be negative");
        }
        
        if (completedTasks > totalTasks) {
            throw new IllegalArgumentException("Completed tasks cannot exceed total tasks");
        }

        // Make task progress map immutable
        taskProgress = Map.copyOf(taskProgress);
    }
    
    /**
     * Gets the overall progress as a percentage (0.0 to 1.0).
     *
     * @return the overall progress percentage
     */
    public double getOverallProgress() {
        if (totalTasks == 0) {
            return 1.0;
        }
        
        return (double) completedTasks / totalTasks;
    }
    
    /**
     * Gets the overall progress as a percentage (0-100).
     *
     * @return the overall progress percentage as an integer
     */
    public int getOverallProgressPercentage() {
        return (int) (getOverallProgress() * 100);
    }
    
    /**
     * Checks if the quest is fully completed.
     *
     * @return true if all tasks are completed
     */
    public boolean isCompleted() {
        return completedTasks == totalTasks && totalTasks > 0;
    }
    
    /**
     * Gets the number of remaining tasks.
     *
     * @return the number of incomplete tasks
     */
    public int getRemainingTasks() {
        return totalTasks - completedTasks;
    }
    
    /**
     * Gets the progress for a specific task.
     *
     * @param taskId the task identifier
     * @return the task progress, or null if not found
     */
    public TaskProgress getTaskProgress(@NotNull final String taskId) {
        return taskProgress.get(taskId);
    }
    
    /**
     * Checks if a specific task is completed.
     *
     * @param taskId the task identifier
     * @return true if the task is completed, false if not completed or not found
     */
    public boolean isTaskCompleted(@NotNull final String taskId) {
        final TaskProgress progress = taskProgress.get(taskId);
        return progress != null && progress.completed();
    }
}
