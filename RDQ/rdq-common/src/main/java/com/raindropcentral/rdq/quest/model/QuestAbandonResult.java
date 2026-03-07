package com.raindropcentral.rdq.quest.model;

import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface representing the result of attempting to abandon a quest.
 * <p>
 * This uses Java's sealed types to provide type-safe result handling with
 * pattern matching support.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public sealed interface QuestAbandonResult {
    
    /**
     * Quest was successfully abandoned.
     *
     * @param questId   the quest identifier
     * @param questName the quest display name
     */
    record Success(
            @NotNull String questId,
            @NotNull String questName
    ) implements QuestAbandonResult {
        public Success {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
            if (questName.isBlank()) {
                throw new IllegalArgumentException("Quest name cannot be null or blank");
            }
        }
    }
    
    /**
     * Quest is not currently active for the player.
     *
     * @param questId the quest identifier
     */
    record NotActive(
            @NotNull String questId
    ) implements QuestAbandonResult {
        public NotActive {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
        }
    }
    
    /**
     * Quest was not found or is disabled.
     *
     * @param questId the quest identifier that was not found
     */
    record QuestNotFound(
            @NotNull String questId
    ) implements QuestAbandonResult {
        public QuestNotFound {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
        }
    }
}
