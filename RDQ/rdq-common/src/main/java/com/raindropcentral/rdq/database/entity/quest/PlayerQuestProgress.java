package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity representing a player's progress on a specific {@link Quest}.
 * <p>
 * This entity tracks when a player started a quest, whether it's completed, and contains
 * all task progress for the quest. It serves as the root entity for tracking a player's
 * active quest state.
 *
 * <p>
 * Each player can have only one active progress record per quest, enforced by a unique constraint.
 * Once a quest is completed, the progress record is typically archived to {@link QuestCompletionHistory}
 * and removed from active progress.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(
    name = "rdq_player_quest_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_quest_progress",
        columnNames = {"player_id", "quest_id"}
    ),
    indexes = {
        @Index(name = "idx_player_quest_player", columnList = "player_id"),
        @Index(name = "idx_player_quest_quest", columnList = "quest_id"),
        @Index(name = "idx_player_quest_completed", columnList = "completed"),
        @Index(name = "idx_player_quest_started", columnList = "started_at")
    }
)
public class PlayerQuestProgress extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The UUID of the player who started this quest.
     */
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
    
    /**
     * The quest being progressed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * When the quest was started.
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    /**
     * When the quest was completed (null if not completed).
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Whether the quest is completed.
     */
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    /**
     * Task progress records for this quest.
     * <p>
     * These are loaded lazily as they may be numerous and are not always needed.
     * Use JOIN FETCH when you need to load all task progress at once.
     */
    @OneToMany(mappedBy = "questProgress", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<PlayerTaskProgress> taskProgress = new ArrayList<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected PlayerQuestProgress() {}
    
    /**
     * Constructs a new {@code PlayerQuestProgress} for the specified player and quest.
     *
     * @param playerId the player's UUID
     * @param quest    the quest being started
     */
    public PlayerQuestProgress(@NotNull final UUID playerId, @NotNull final Quest quest) {
        this.playerId = playerId;
        this.quest = quest;
        this.startedAt = Instant.now();
    }
    
    /**
     * Returns the player's UUID.
     *
     * @return the player UUID
     */
    @NotNull
    public UUID getPlayerId() {
        return this.playerId;
    }
    
    /**
     * Sets the player's UUID.
     *
     * @param playerId the player UUID
     */
    public void setPlayerId(@NotNull final UUID playerId) {
        this.playerId = playerId;
    }
    
    /**
     * Returns the quest being progressed.
     *
     * @return the quest
     */
    @NotNull
    public Quest getQuest() {
        return this.quest;
    }
    
    /**
     * Sets the quest for this progress.
     *
     * @param quest the quest
     */
    public void setQuest(@NotNull final Quest quest) {
        this.quest = quest;
    }
    
    /**
     * Returns when the quest was started.
     *
     * @return the start time
     */
    @NotNull
    public Instant getStartedAt() {
        return this.startedAt;
    }
    
    /**
     * Sets when the quest was started.
     *
     * @param startedAt the start time
     */
    public void setStartedAt(@NotNull final Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    /**
     * Returns when the quest was completed, or null if not completed.
     *
     * @return the completion time, or null
     */
    @Nullable
    public Instant getCompletedAt() {
        return this.completedAt;
    }
    
    /**
     * Sets when the quest was completed.
     *
     * @param completedAt the completion time
     */
    public void setCompletedAt(@Nullable final Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Returns whether the quest is completed.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return this.completed;
    }
    
    /**
     * Sets whether the quest is completed.
     *
     * @param completed true if completed, false otherwise
     */
    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Returns the list of task progress records.
     *
     * @return the task progress list
     */
    @NotNull
    public List<PlayerTaskProgress> getTaskProgress() {
        return this.taskProgress;
    }
    
    /**
     * Sets the task progress list.
     *
     * @param taskProgress the task progress list
     */
    public void setTaskProgress(@NotNull final List<PlayerTaskProgress> taskProgress) {
        this.taskProgress = taskProgress;
    }
    
    /**
     * Adds task progress to this quest progress.
     * <p>
     * This method manages the bidirectional relationship between PlayerQuestProgress
     * and PlayerTaskProgress.
     *
     * @param progress the task progress to add
     */
    public void addTaskProgress(@NotNull final PlayerTaskProgress progress) {
        if (!this.taskProgress.contains(progress)) {
            this.taskProgress.add(progress);
            if (progress.getQuestProgress() != this) {
                progress.setQuestProgress(this);
            }
        }
    }
    
    /**
     * Removes task progress from this quest progress.
     * <p>
     * This method manages the bidirectional relationship between PlayerQuestProgress
     * and PlayerTaskProgress.
     *
     * @param progress the task progress to remove
     */
    public void removeTaskProgress(@NotNull final PlayerTaskProgress progress) {
        if (this.taskProgress.remove(progress)) {
            if (progress.getQuestProgress() == this) {
                progress.setQuestProgress(null);
            }
        }
    }
    
    /**
     * Gets the task progress for a specific quest task.
     *
     * @param task the quest task
     * @return optional containing the task progress, or empty if not found
     */
    @NotNull
    public Optional<PlayerTaskProgress> getTaskProgress(@NotNull final QuestTask task) {
        return this.taskProgress.stream()
            .filter(tp -> tp.getTask().equals(task))
            .findFirst();
    }
    
    /**
     * Checks if a specific task is completed.
     *
     * @param task the quest task
     * @return true if the task is completed, false otherwise
     */
    public boolean isTaskCompleted(@NotNull final QuestTask task) {
        return getTaskProgress(task)
            .map(PlayerTaskProgress::isCompleted)
            .orElse(false);
    }
    
    /**
     * Gets the number of completed tasks.
     *
     * @return the count of completed tasks
     */
    public int getCompletedTaskCount() {
        return (int) this.taskProgress.stream()
            .filter(PlayerTaskProgress::isCompleted)
            .count();
    }
    
    /**
     * Gets the overall progress percentage for this quest.
     * <p>
     * This is calculated as (completed tasks / total tasks) * 100.
     *
     * @return the progress percentage (0-100)
     */
    public double getOverallProgress() {
        if (this.taskProgress.isEmpty()) {
            return 0.0;
        }
        
        int completedTasks = getCompletedTaskCount();
        int totalTasks = this.taskProgress.size();
        
        return (double) completedTasks / totalTasks * 100.0;
    }
    
    /**
     * Checks if all tasks are completed.
     *
     * @return true if all tasks are completed, false otherwise
     */
    public boolean areAllTasksCompleted() {
        if (this.taskProgress.isEmpty()) {
            return false;
        }
        
        return this.taskProgress.stream()
            .allMatch(PlayerTaskProgress::isCompleted);
    }
    
    /**
     * Marks the quest as completed.
     * <p>
     * This sets the completed flag to true and records the completion time.
     */
    public void markCompleted() {
        this.completed = true;
        this.completedAt = Instant.now();
    }
    
    /**
     * Gets the time elapsed since the quest was started.
     *
     * @return the elapsed time in seconds
     */
    public long getElapsedSeconds() {
        Instant now = Instant.now();
        return java.time.Duration.between(this.startedAt, now).getSeconds();
    }
    
    /**
     * Checks if the quest has exceeded its time limit (if any).
     *
     * @return true if time limit exceeded, false otherwise
     */
    public boolean isTimeLimitExceeded() {
        if (!this.quest.hasTimeLimit()) {
            return false;
        }
        
        long elapsedSeconds = getElapsedSeconds();
        long timeLimitSeconds = this.quest.getTimeLimitSeconds();
        
        return elapsedSeconds > timeLimitSeconds;
    }
    
    /**
     * Gets the remaining time in seconds before the time limit expires.
     *
     * @return remaining seconds, or -1 if no time limit or already exceeded
     */
    public long getRemainingSeconds() {
        if (!this.quest.hasTimeLimit()) {
            return -1;
        }
        
        long elapsedSeconds = getElapsedSeconds();
        long timeLimitSeconds = this.quest.getTimeLimitSeconds();
        long remaining = timeLimitSeconds - elapsedSeconds;
        
        return Math.max(0, remaining);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerQuestProgress that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.playerId != null && that.playerId != null &&
                this.quest != null && that.quest != null) {
            return this.playerId.equals(that.playerId) && this.quest.equals(that.quest);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.playerId != null && this.quest != null) {
            return Objects.hash(this.playerId, this.quest);
        }
        
        return System.identityHashCode(this);
    }
    
    @Override
    public String toString() {
        return "PlayerQuestProgress{" +
                "id=" + getId() +
                ", playerId=" + playerId +
                ", quest=" + (quest != null ? quest.getIdentifier() : "null") +
                ", completed=" + completed +
                ", progress=" + String.format("%.1f%%", getOverallProgress()) +
                '}';
    }
}
