package com.raindropcentral.rdq.model.quest;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Represents the progress of a player on a specific quest.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public record QuestProgress(
        @NotNull Long questId,
        @NotNull String questIdentifier,
        @NotNull UUID playerId,
        @NotNull List<TaskProgress> taskProgress,
        boolean completed,
        double overallProgress
) {
    /**
     * Creates a new QuestProgress instance.
     *
     * @param questId          the unique identifier of the quest
     * @param questIdentifier  the string identifier of the quest
     * @param playerId         the UUID of the player
     * @param taskProgress     list of task progress entries
     * @param completed        whether the quest is completed
     * @param overallProgress  the overall completion percentage (0-100)
     */
    public QuestProgress {
        if (questId == null) {
            throw new IllegalArgumentException("questId cannot be null");
        }
        if (questIdentifier == null || questIdentifier.isBlank()) {
            throw new IllegalArgumentException("questIdentifier cannot be null or blank");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (taskProgress == null) {
            throw new IllegalArgumentException("taskProgress cannot be null");
        }
        if (overallProgress < 0 || overallProgress > 100) {
            throw new IllegalArgumentException("overallProgress must be between 0 and 100");
        }
    }
}
