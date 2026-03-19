package com.raindropcentral.rdq.model.quest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of attempting to start a quest.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public record QuestStartResult(
        boolean success,
        @Nullable String failureReason,
        @Nullable Long questId
) {
    /**
     * Creates a new QuestStartResult instance.
     *
     * @param success       whether the quest was successfully started
     * @param failureReason the reason for failure, if applicable
     * @param questId       the ID of the started quest, if successful
     */
    public QuestStartResult {
        if (success && questId == null) {
            throw new IllegalArgumentException("questId must be provided when success is true");
        }
        if (!success && failureReason == null) {
            throw new IllegalArgumentException("failureReason must be provided when success is false");
        }
    }

    /**
     * Creates a successful quest start result.
     *
     * @param questId the ID of the started quest
     * @return a successful result
     */
    public static @NotNull QuestStartResult success(@NotNull final Long questId) {
        return new QuestStartResult(true, null, questId);
    }

    /**
     * Creates a failed quest start result.
     *
     * @param reason the reason for failure
     * @return a failed result
     */
    public static @NotNull QuestStartResult failure(@NotNull final String reason) {
        return new QuestStartResult(false, reason, null);
    }
}
