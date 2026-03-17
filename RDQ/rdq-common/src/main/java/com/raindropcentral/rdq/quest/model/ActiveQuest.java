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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Immutable record representing an active quest for a player.
 *
 * <p>This record contains all information about a quest that is currently in progress,
 * including time limits, progress tracking, and task details.
 *
 * @param questId         the unique quest identifier
 * @param questName       the display name of the quest
 * @param difficulty      the quest difficulty level
 * @param startedAt       when the quest was started
 * @param timeLimit       the time limit duration (null if no limit)
 * @param remainingTime   the remaining time (null if no limit or expired)
 * @param tasks           the list of task progress
 * @param overallProgress the overall completion percentage (0.0 to 1.0)
 * @author RaindropCentral
 * @version 1.0.0
 */
public record ActiveQuest(
        @NotNull String questId,
        @NotNull String questName,
        @NotNull QuestDifficulty difficulty,
        @NotNull Instant startedAt,
        @Nullable Duration timeLimit,
        @Nullable Duration remainingTime,
        @NotNull List<TaskProgress> tasks,
        double overallProgress
) {
    
    /**
     * Compact constructor with validation.
     */
    public ActiveQuest {
        if (questId.isBlank()) {
            throw new IllegalArgumentException("Quest ID cannot be null or blank");
        }
        
        if (questName.isBlank()) {
            throw new IllegalArgumentException("Quest name cannot be null or blank");
        }

        if (overallProgress < 0.0 || overallProgress > 1.0) {
            throw new IllegalArgumentException("Overall progress must be between 0.0 and 1.0");
        }
        
        // Make tasks list immutable
        tasks = List.copyOf(tasks);
    }
    
    /**
     * Checks if this quest has expired based on its time limit.
     *
     * @return true if the quest has a time limit and it has expired
     */
    public boolean isExpired() {
        if (timeLimit == null) {
            return false;
        }
        
        final Instant expiryTime = startedAt.plus(timeLimit);
        return Instant.now().isAfter(expiryTime);
    }
    
    /**
     * Checks if this quest has a time limit.
     *
     * @return true if the quest has a time limit
     */
    public boolean hasTimeLimit() {
        return timeLimit != null;
    }
    
    /**
     * Gets the number of completed tasks.
     *
     * @return the count of completed tasks
     */
    public int getCompletedTaskCount() {
        return (int) tasks.stream()
                .filter(TaskProgress::completed)
                .count();
    }
    
    /**
     * Gets the total number of tasks.
     *
     * @return the total task count
     */
    public int getTotalTaskCount() {
        return tasks.size();
    }
    
    /**
     * Checks if all tasks are completed.
     *
     * @return true if all tasks are completed
     */
    public boolean isFullyCompleted() {
        return overallProgress >= 1.0 && tasks.stream().allMatch(TaskProgress::completed);
    }
    
    /**
     * Gets the overall progress as a percentage (0-100).
     *
     * @return the progress percentage
     */
    public int getProgressPercentage() {
        return (int) (overallProgress * 100);
    }
}
