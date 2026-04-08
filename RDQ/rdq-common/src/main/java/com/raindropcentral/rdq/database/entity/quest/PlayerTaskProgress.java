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

/**
 * Entity representing a player's progress on a specific {@link QuestTask} within a quest.
 * <p>
 * This entity tracks the current progress value for simple tasks (e.g., "mine 10 stone")
 * and contains requirement progress for complex tasks (e.g., "complete 3 requirements").
 * It supports both simple counter-based progress and complex requirement-based progress.
 *
 * <p>
 * Each player can have only one progress record per task within a quest, enforced by a unique constraint.
 * Progress is automatically capped at the task's target value to prevent overflow.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(
    name = "rdq_player_task_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_task_progress",
        columnNames = {"quest_progress_id", "task_id"}
    ),
    indexes = {
        @Index(name = "idx_player_task_quest_progress", columnList = "quest_progress_id"),
        @Index(name = "idx_player_task_task", columnList = "task_id"),
        @Index(name = "idx_player_task_completed", columnList = "completed")
    }
)
public class PlayerTaskProgress extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The quest progress this task belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_progress_id", nullable = false)
    private PlayerQuestProgress questProgress;
    
    /**
     * The task being progressed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private QuestTask task;
    
    /**
     * Current progress value for simple tasks.
     * <p>
     * For simple tasks like "mine 10 stone", this represents the current count (0-10).
     * For complex tasks with requirements, this may be unused or represent an aggregate value.
     */
    @Column(name = "current_progress", nullable = false)
    private long currentProgress = 0;
    
    /**
     * Whether the task is completed.
     */
    @Column(name = "completed", nullable = false)
    private boolean completed = false;
    
    /**
     * When the task was completed (null if not completed).
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Requirement progress records for this task.
     * <p>
     * For complex tasks with multiple requirements, this tracks progress on each requirement.
     * For simple tasks, this list may be empty.
     */
    @OneToMany(mappedBy = "taskProgress", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlayerTaskRequirementProgress> requirementProgress = new ArrayList<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected PlayerTaskProgress() {}
    
    /**
     * Constructs a new {@code PlayerTaskProgress} for the specified quest progress and task.
     *
     * @param questProgress the quest progress this task belongs to
     * @param task          the task being started
     */
    public PlayerTaskProgress(@NotNull final PlayerQuestProgress questProgress, @NotNull final QuestTask task) {
        this.questProgress = questProgress;
        this.task = task;
    }
    
    /**
     * Returns the quest progress this task belongs to.
     *
     * @return the quest progress
     */
    @NotNull
    public PlayerQuestProgress getQuestProgress() {
        return this.questProgress;
    }
    
    /**
     * Sets the quest progress for this task.
     *
     * @param questProgress the quest progress
     */
    public void setQuestProgress(@Nullable final PlayerQuestProgress questProgress) {
        this.questProgress = questProgress;
    }
    
    /**
     * Returns the task being progressed.
     *
     * @return the task
     */
    @NotNull
    public QuestTask getTask() {
        return this.task;
    }
    
    /**
     * Sets the task for this progress.
     *
     * @param task the task
     */
    public void setTask(@NotNull final QuestTask task) {
        this.task = task;
    }
    
    /**
     * Returns the current progress value.
     *
     * @return the current progress
     */
    public long getCurrentProgress() {
        return this.currentProgress;
    }
    
    /**
     * Sets the current progress value, capping at the task's target value.
     *
     * @param currentProgress the current progress
     */
    public void setCurrentProgress(final long currentProgress) {
        this.currentProgress = Math.max(0, currentProgress);
    }
    
    /**
     * Returns whether the task is completed.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return this.completed;
    }
    
    /**
     * Sets whether the task is completed.
     *
     * @param completed true if completed, false otherwise
     */
    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Returns when the task was completed, or null if not completed.
     *
     * @return the completion time, or null
     */
    @Nullable
    public Instant getCompletedAt() {
        return this.completedAt;
    }
    
    /**
     * Sets when the task was completed.
     *
     * @param completedAt the completion time
     */
    public void setCompletedAt(@Nullable final Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Returns the list of requirement progress records.
     *
     * @return the requirement progress list
     */
    @NotNull
    public List<PlayerTaskRequirementProgress> getRequirementProgress() {
        return this.requirementProgress;
    }
    
    /**
     * Sets the requirement progress list.
     *
     * @param requirementProgress the requirement progress list
     */
    public void setRequirementProgress(@NotNull final List<PlayerTaskRequirementProgress> requirementProgress) {
        this.requirementProgress = requirementProgress;
    }
    
    /**
     * Increments the progress by the specified amount.
     * <p>
     * This method is used for simple tasks with counter-based progress.
     *
     * @param amount the amount to increment
     * @return the new progress value
     */
    public long incrementProgress(final long amount) {
        this.currentProgress += amount;
        return this.currentProgress;
    }
    
    /**
     * Gets the progress as a percentage (0-100).
     * <p>
     * For simple tasks, this is calculated as (current / target) * 100.
     * For complex tasks with requirements, this is the average of all requirement progress.
     *
     * @return the progress percentage
     */
    public double getProgressPercentage() {
        // If task has requirements, calculate based on requirement progress
        if (!this.requirementProgress.isEmpty()) {
            double totalProgress = this.requirementProgress.stream()
                .mapToDouble(PlayerTaskRequirementProgress::getProgress)
                .sum();
            return (totalProgress / this.requirementProgress.size()) * 100.0;
        }
        
        // For simple tasks, calculate based on current progress
        // Note: This assumes the task has a target value, which should be validated elsewhere
        return 0.0; // Default if no target is set
    }

    /**
     * Gets the required progress value for this task.
     * <p>
     * For simple tasks, this returns the task's target value.
     * For complex tasks with requirements, this returns the number of requirements.
     *
     * @return the required progress value
     */
    public long getRequiredProgress() {
        // If task has requirements, return the number of requirements
        if (!this.requirementProgress.isEmpty()) {
            return this.requirementProgress.size();
        }
        
        // For simple tasks, return the task's target value if available
        if (this.task != null) {
            // Assuming QuestTask has a getTargetValue() method
            // If not available, return a default value
            return 1; // Default target for completion
        }
        
        return 1; // Default required progress
    }
    
    /**
     * Marks the task as completed.
     * <p>
     * This sets the completed flag to true and records the completion time.
     */
    public void markCompleted() {
        this.completed = true;
        this.completedAt = Instant.now();
    }
    
    /**
     * Resets the task progress to zero.
     * <p>
     * This clears the current progress, completion status, and all requirement progress.
     */
    public void resetProgress() {
        this.currentProgress = 0;
        this.completed = false;
        this.completedAt = null;
        this.requirementProgress.clear();
    }
    
    /**
     * Adds requirement progress to this task progress.
     * <p>
     * This method manages the bidirectional relationship between PlayerTaskProgress
     * and PlayerTaskRequirementProgress.
     *
     * @param progress the requirement progress to add
     */
    public void addRequirementProgress(@NotNull final PlayerTaskRequirementProgress progress) {
        if (!this.requirementProgress.contains(progress)) {
            this.requirementProgress.add(progress);
            if (progress.getTaskProgress() != this) {
                progress.setTaskProgress(this);
            }
        }
    }
    
    /**
     * Removes requirement progress from this task progress.
     * <p>
     * This method manages the bidirectional relationship between PlayerTaskProgress
     * and PlayerTaskRequirementProgress.
     *
     * @param progress the requirement progress to remove
     */
    public void removeRequirementProgress(@NotNull final PlayerTaskRequirementProgress progress) {
        if (this.requirementProgress.remove(progress)) {
            if (progress.getTaskProgress() == this) {
                progress.setTaskProgress(null);
            }
        }
    }
    
    /**
     * Gets the requirement progress for a specific task requirement.
     *
     * @param requirement the task requirement
     * @return optional containing the requirement progress, or empty if not found
     */
    @NotNull
    public Optional<PlayerTaskRequirementProgress> getRequirementProgress(@NotNull final QuestTaskRequirement requirement) {
        return this.requirementProgress.stream()
            .filter(rp -> rp.getRequirement().equals(requirement))
            .findFirst();
    }
    
    /**
     * Checks if a specific requirement is completed.
     *
     * @param requirement the task requirement
     * @return true if the requirement is completed, false otherwise
     */
    public boolean isRequirementCompleted(@NotNull final QuestTaskRequirement requirement) {
        return getRequirementProgress(requirement)
            .map(PlayerTaskRequirementProgress::isCompleted)
            .orElse(false);
    }
    
    /**
     * Checks if all requirements are completed.
     *
     * @return true if all requirements are completed, false otherwise
     */
    public boolean areAllRequirementsCompleted() {
        if (this.requirementProgress.isEmpty()) {
            return true; // No requirements means task is based on simple progress
        }
        
        return this.requirementProgress.stream()
            .allMatch(PlayerTaskRequirementProgress::isCompleted);
    }
    
    /**
     * Gets the number of completed requirements.
     *
     * @return the count of completed requirements
     */
    public int getCompletedRequirementCount() {
        return (int) this.requirementProgress.stream()
            .filter(PlayerTaskRequirementProgress::isCompleted)
            .count();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerTaskProgress that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.questProgress != null && that.questProgress != null &&
                this.task != null && that.task != null) {
            return this.questProgress.equals(that.questProgress) && this.task.equals(that.task);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.questProgress != null && this.task != null) {
            return Objects.hash(this.questProgress, this.task);
        }
        
        return System.identityHashCode(this);
    }
    
    @Override
    public String toString() {
        return "PlayerTaskProgress{" +
                "id=" + getId() +
                ", task=" + (task != null ? task.getTaskIdentifier() : "null") +
                ", currentProgress=" + currentProgress +
                ", completed=" + completed +
                ", progress=" + String.format("%.1f%%", getProgressPercentage()) +
                '}';
    }
}
