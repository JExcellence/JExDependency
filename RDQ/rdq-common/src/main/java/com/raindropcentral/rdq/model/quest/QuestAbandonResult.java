package com.raindropcentral.rdq.model.quest;

import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface representing the result of attempting to abandon a quest.
 * <p>
 * This provides a type-safe way to handle all possible outcomes when a player
 * tries to abandon a quest.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public sealed interface QuestAbandonResult 
    permits QuestAbandonResult.Success, 
            QuestAbandonResult.QuestNotFound, 
            QuestAbandonResult.NotActive {
    
    /**
     * Represents a successful quest abandonment.
     *
     * @param questId the ID of the abandoned quest
     * @param questName the display name of the quest
     */
    record Success(
        @NotNull String questId,
        @NotNull String questName
    ) implements QuestAbandonResult {}
    
    /**
     * Represents a failure due to quest not being found.
     *
     * @param questId the ID of the quest that wasn't found
     */
    record QuestNotFound(
        @NotNull String questId
    ) implements QuestAbandonResult {}
    
    /**
     * Represents a failure due to the quest not being active.
     *
     * @param questId the ID of the quest
     */
    record NotActive(
        @NotNull String questId
    ) implements QuestAbandonResult {}
    
    /**
     * Checks if this result represents a successful quest abandonment.
     *
     * @return true if the quest was abandoned successfully
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    /**
     * Alias for isSuccess for backward compatibility.
     *
     * @return true if the quest was abandoned successfully
     */
    default boolean success() {
        return isSuccess();
    }
    
    /**
     * Gets the failure reason if this is a failure result.
     *
     * @return the failure reason, or null if successful
     */
    default String failureReason() {
        return switch (this) {
            case Success s -> null;
            case QuestNotFound qnf -> "Quest not found";
            case NotActive na -> "Quest is not active";
        };
    }
    
    /**
     * Gets the quest ID associated with this result.
     *
     * @return the quest ID
     */
    default String getQuestId() {
        return switch (this) {
            case Success s -> s.questId();
            case QuestNotFound qnf -> qnf.questId();
            case NotActive na -> na.questId();
        };
    }
    
    /**
     * Gets a human-readable message describing this result.
     *
     * @return a descriptive message
     */
    default String getMessage() {
        return switch (this) {
            case Success s -> "Quest '" + s.questName() + "' abandoned successfully.";
            case QuestNotFound qnf -> "Quest '" + qnf.questId() + "' not found.";
            case NotActive na -> "Quest '" + na.questId() + "' is not active.";
        };
    }
}
