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

/**
 * Immutable record representing the progress of a single quest task.
 *
 * <p>This record tracks the current and required progress for a task,
 * along with its completion status.
 *
 * @param taskId      the unique task identifier
 * @param taskName    the display name of the task
 * @param current     the current progress value
 * @param required    the required progress value for completion
 * @param completed   whether the task is completed
 * @author RaindropCentral
 * @version 1.0.0
 */
public record TaskProgress(
        @NotNull String taskId,
        @NotNull String taskName,
        int current,
        int required,
        boolean completed
) {
    
    /**
     * Compact constructor with validation.
     */
    public TaskProgress {
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        
        if (taskName.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be null or blank");
        }
        
        if (current < 0) {
            throw new IllegalArgumentException("Current progress cannot be negative");
        }
        
        if (required < 0) {
            throw new IllegalArgumentException("Required progress cannot be negative");
        }
    }
    
    /**
     * Gets the progress as a percentage (0.0 to 1.0).
     *
     * @return the progress percentage
     */
    public double getPercentage() {
        if (required <= 0) {
            return completed ? 1.0 : 0.0;
        }
        
        return Math.min(1.0, (double) current / required);
    }
    
    /**
     * Gets the progress as a percentage (0-100).
     *
     * @return the progress percentage as an integer
     */
    public int getPercentageInt() {
        return (int) (getPercentage() * 100);
    }
    
    /**
     * Gets the remaining progress needed for completion.
     *
     * @return the remaining progress, or 0 if completed
     */
    public int getRemaining() {
        if (completed) {
            return 0;
        }
        
        return Math.max(0, required - current);
    }
    
    /**
     * Checks if the task is in progress (started but not completed).
     *
     * @return true if the task has progress but is not completed
     */
    public boolean isInProgress() {
        return current > 0 && !completed;
    }
}
