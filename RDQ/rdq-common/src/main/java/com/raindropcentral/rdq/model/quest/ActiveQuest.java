package com.raindropcentral.rdq.model.quest;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an active quest that a player is currently working on.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public record ActiveQuest(
        @NotNull Long questId,
        @NotNull String questIdentifier,
        @NotNull String questName,
        @NotNull UUID playerId,
        @NotNull Instant startedAt,
        int completedTasks,
        int totalTasks,
        double progressPercentage
) {
    /**
     * Creates a new ActiveQuest instance.
     *
     * @param questId           the unique identifier of the quest
     * @param questIdentifier   the string identifier of the quest
     * @param questName         the display name of the quest
     * @param playerId          the UUID of the player
     * @param startedAt         when the quest was started
     * @param completedTasks    number of completed tasks
     * @param totalTasks        total number of tasks
     * @param progressPercentage the completion percentage (0-100)
     */
    public ActiveQuest {
        if (questId == null) {
            throw new IllegalArgumentException("questId cannot be null");
        }
        if (questIdentifier == null || questIdentifier.isBlank()) {
            throw new IllegalArgumentException("questIdentifier cannot be null or blank");
        }
        if (questName == null || questName.isBlank()) {
            throw new IllegalArgumentException("questName cannot be null or blank");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt cannot be null");
        }
        if (completedTasks < 0) {
            throw new IllegalArgumentException("completedTasks cannot be negative");
        }
        if (totalTasks < 0) {
            throw new IllegalArgumentException("totalTasks cannot be negative");
        }
        if (progressPercentage < 0 || progressPercentage > 100) {
            throw new IllegalArgumentException("progressPercentage must be between 0 and 100");
        }
    }
}
