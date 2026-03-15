package com.raindropcentral.rdq.quest.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Sealed interface representing the result of attempting to start a quest.
 *
 * <p>This uses Java's sealed types to provide type-safe result handling with
 * pattern matching support. Each result type provides specific information
 * about why the quest start succeeded or failed.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public sealed interface QuestStartResult {
    
    /**
     * Quest was successfully started.
     *
     * @param questId   the quest identifier
     * @param questName the quest display name
     */
    record Success(
            @NotNull String questId,
            @NotNull String questName
    ) implements QuestStartResult {
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
     * Quest is already active for the player.
     *
     * @param questId   the quest identifier
     * @param questName the quest display name
     * @param startedAt when the quest was started
     */
    record AlreadyActive(
            @NotNull String questId,
            @NotNull String questName,
            @NotNull Instant startedAt
    ) implements QuestStartResult {
        public AlreadyActive {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
            if (questName.isBlank()) {
                throw new IllegalArgumentException("Quest name cannot be null or blank");
            }
        }
    }
    
    /**
     * Quest is on cooldown and cannot be started yet.
     *
     * @param questId         the quest identifier
     * @param questName       the quest display name
     * @param remainingTime   the remaining cooldown duration
     * @param nextAvailableAt when the quest will be available again
     */
    record OnCooldown(
            @NotNull String questId,
            @NotNull String questName,
            @NotNull Duration remainingTime,
            @NotNull Instant nextAvailableAt
    ) implements QuestStartResult {
        public OnCooldown {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
            if (questName.isBlank()) {
                throw new IllegalArgumentException("Quest name cannot be null or blank");
            }
        }
    }
    
    /**
     * Quest requirements are not met.
     *
     * @param questId          the quest identifier
     * @param questName        the quest display name
     * @param missingRequirement a description of the missing requirement
     */
    record RequirementsNotMet(
            @NotNull String questId,
            @NotNull String questName,
            @Nullable String missingRequirement
    ) implements QuestStartResult {
        public RequirementsNotMet {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
            if (questName.isBlank()) {
                throw new IllegalArgumentException("Quest name cannot be null or blank");
            }
        }
    }
    
    /**
     * Player has reached the maximum number of active quests.
     *
     * @param currentActive the current number of active quests
     * @param maxActive     the maximum allowed active quests
     */
    record MaxActiveReached(
            int currentActive,
            int maxActive
    ) implements QuestStartResult {
        public MaxActiveReached {
            if (currentActive < 0) {
                throw new IllegalArgumentException("Current active cannot be negative");
            }
            if (maxActive < 0) {
                throw new IllegalArgumentException("Max active cannot be negative");
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
    ) implements QuestStartResult {
        public QuestNotFound {
            if (questId.isBlank()) {
                throw new IllegalArgumentException("Quest ID cannot be null or blank");
            }
        }
    }
}
