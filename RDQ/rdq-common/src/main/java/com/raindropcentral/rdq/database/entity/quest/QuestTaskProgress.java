package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a player's progress on a specific quest task.
 *
 * <p>Tracks the current progress value, required progress value, and completion status
 * for an individual task within a quest.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest_task_progress",
        indexes = {
                @Index(name = "idx_quest_task_progress_user", columnList = "quest_user_id"),
                @Index(name = "idx_quest_task_progress_identifier", columnList = "task_identifier"),
                @Index(name = "idx_quest_task_progress_completed", columnList = "completed")
        }
)
/**
 * Represents the QuestTaskProgress API type.
 */
public class QuestTaskProgress extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The quest user progress this task progress belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_user_id", nullable = false)
    private QuestUser questUser;
    
    /**
     * The task identifier this progress is tracking.
     */
    @Column(name = "task_identifier", nullable = false, length = 64)
    private String taskIdentifier;
    
    /**
     * Current progress value (e.g., 5 zombies killed out of 10).
     */
    @Column(name = "current_progress", nullable = false)
    private int currentProgress = 0;
    
    /**
     * Required progress value to complete this task (e.g., 10 zombies).
     */
    @Column(name = "required_progress", nullable = false)
    private int requiredProgress;
    
    /**
     * Whether this task has been completed.
     */
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    /**
     * Timestamp when this task was completed.
     * Null if not yet completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestTaskProgress() {
    }
    
    /**
     * Constructs a new quest task progress tracker.
     *
     * @param questUser        the quest user progress
     * @param taskIdentifier   the task identifier
     * @param requiredProgress the required progress value
     */
    public QuestTaskProgress(
            @NotNull final QuestUser questUser,
            @NotNull final String taskIdentifier,
            final int requiredProgress
    ) {
        this.questUser = questUser;
        this.taskIdentifier = taskIdentifier;
        this.requiredProgress = requiredProgress;
    }
    
    /**
     * Factory method to create a new task progress tracker.
     *
     * @param questUser        the quest user progress
     * @param taskIdentifier   the task identifier
     * @param requiredProgress the required progress value
     * @return a new quest task progress instance
     */
    public static QuestTaskProgress create(
            @NotNull final QuestUser questUser,
            @NotNull final String taskIdentifier,
            final int requiredProgress
    ) {
        return new QuestTaskProgress(questUser, taskIdentifier, requiredProgress);
    }
    
    /**
     * Gets the progress as a percentage (0-100).
     *
     * @return the progress percentage
     */
    public double getProgressPercentage() {
        if (requiredProgress == 0) {
            return 0.0;
        }
        return (double) currentProgress / requiredProgress * 100.0;
    }
    
    /**
     * Checks if this task is complete.
     *
     * @return true if complete, false otherwise
     */
    public boolean isComplete() {
        return completed || currentProgress >= requiredProgress;
    }
    
    /**
     * Increments the current progress by the specified amount.
     *
     * @param amount the amount to increment
     * @return the new current progress value
     */
    public int incrementProgress(final int amount) {
        this.currentProgress = Math.min(this.currentProgress + amount, this.requiredProgress);
        if (this.currentProgress >= this.requiredProgress && !this.completed) {
            this.completed = true;
            this.completedAt = Instant.now();
        }
        return this.currentProgress;
    }
    
    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestTaskProgress that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        return questUser != null && questUser.equals(that.questUser) &&
                taskIdentifier != null && taskIdentifier.equals(that.taskIdentifier);
    }
    
    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(questUser, taskIdentifier);
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "QuestTaskProgress{" +
                "id=" + getId() +
                ", taskIdentifier='" + taskIdentifier + '\'' +
                ", currentProgress=" + currentProgress +
                ", requiredProgress=" + requiredProgress +
                ", completed=" + completed +
                '}';
    }
}
