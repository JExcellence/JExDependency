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

package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a player's progress on a quest.
 *
 * <p>Tracks when a player started a quest, their progress on individual tasks,
 * and completion status.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest_user",
        indexes = {
                @Index(name = "idx_quest_user_player", columnList = "player_id"),
                @Index(name = "idx_quest_user_quest", columnList = "quest_id"),
                @Index(name = "idx_quest_user_completed", columnList = "completed"),
                @Index(name = "idx_quest_user_active", columnList = "player_id, completed")
        }
)
/**
 * Represents the QuestUser API type.
 */
public class QuestUser extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The player's unique identifier.
     */
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
    
    /**
     * The quest being tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * Timestamp when the player started this quest.
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    /**
     * Timestamp when the player completed this quest.
     * Null if not yet completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Whether this quest has been completed.
     */
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    /**
     * Progress on individual tasks within this quest.
     */
    @OneToMany(mappedBy = "questUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestTaskProgress> taskProgress = new ArrayList<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestUser() {
    }
    
    /**
     * Constructs a new quest user progress tracker.
     *
     * @param playerId  the player's unique identifier
     * @param quest     the quest being tracked
     * @param startedAt when the quest was started
     */
    public QuestUser(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final Instant startedAt
    ) {
        this.playerId = playerId;
        this.quest = quest;
        this.startedAt = startedAt;
    }
    
    /**
     * Factory method to start a new quest for a player.
     *
     * @param playerId the player's unique identifier
     * @param quest    the quest to start
     * @return a new quest user instance
     */
    public static QuestUser start(@NotNull final UUID playerId, @NotNull final Quest quest) {
        return new QuestUser(playerId, quest, Instant.now());
    }
    
    /**
     * Checks if this quest is currently active (started but not completed).
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return !completed && completedAt == null;
    }
    
    /**
     * Checks if this quest has expired based on its time limit.
     *
     * @param quest the quest definition
     * @return true if expired, false otherwise
     */
    public boolean isExpired(@NotNull final Quest quest) {
        if (!quest.hasTimeLimit()) {
            return false;
        }
        final Instant deadline = startedAt.plus(quest.getTimeLimit());
        return Instant.now().isAfter(deadline);
    }
    
    /**
     * Gets the remaining time before this quest expires.
     *
     * @param quest the quest definition
     * @return the remaining duration, or Duration.ZERO if no time limit or already expired
     */
    public Duration getRemainingTime(@NotNull final Quest quest) {
        if (!quest.hasTimeLimit()) {
            return Duration.ZERO;
        }
        final Instant deadline = startedAt.plus(quest.getTimeLimit());
        final Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
    
    /**
     * Adds task progress to this quest.
     *
     * @param progress the task progress to add
     */
    public void addTaskProgress(@NotNull final QuestTaskProgress progress) {
        taskProgress.add(progress);
        progress.setQuestUser(this);
    }
    
    /**
     * Removes task progress from this quest.
     *
     * @param progress the task progress to remove
     */
    public void removeTaskProgress(@NotNull final QuestTaskProgress progress) {
        taskProgress.remove(progress);
        progress.setQuestUser(null);
    }
    
    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestUser questUser)) return false;
        
        if (this.getId() != null && questUser.getId() != null) {
            return this.getId().equals(questUser.getId());
        }
        
        return playerId != null && playerId.equals(questUser.playerId) &&
                quest != null && quest.equals(questUser.quest);
    }
    
    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(playerId, quest);
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "QuestUser{" +
                "id=" + getId() +
                ", playerId=" + playerId +
                ", quest=" + (quest != null ? quest.getIdentifier() : null) +
                ", completed=" + completed +
                ", startedAt=" + startedAt +
                '}';
    }
}
