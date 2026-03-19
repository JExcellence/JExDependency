package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Entity representing a player's progress on a specific {@link QuestTaskRequirement} within a task.
 * <p>
 * This entity tracks the completion progress of individual task requirements, ranging from 0.0 (not started)
 * to 1.0 (completed). It provides a granular view of requirement completion within complex tasks.
 * </p>
 *
 * <p>
 * Each player can have only one progress record per requirement within a task, enforced by a unique constraint.
 * Progress is automatically capped at 1.0 to prevent overflow.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(
    name = "rdq_player_task_requirement_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_task_req_progress", 
        columnNames = {"task_progress_id", "requirement_id"}
    )
)
public class PlayerTaskRequirementProgress extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The task progress this requirement progress belongs to.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_progress_id", nullable = false)
    private PlayerTaskProgress taskProgress;
    
    /**
     * The requirement being progressed.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private QuestTaskRequirement requirement;
    
    /**
     * Progress value between 0.0 and 1.0.
     */
    @Column(name = "progress", nullable = false)
    private double progress = 0.0;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected PlayerTaskRequirementProgress() {}
    
    /**
     * Constructs a new {@code PlayerTaskRequirementProgress} for the specified task progress and requirement.
     *
     * @param taskProgress the task progress this belongs to
     * @param requirement  the requirement being progressed
     */
    public PlayerTaskRequirementProgress(@NotNull final PlayerTaskProgress taskProgress, 
                                        @NotNull final QuestTaskRequirement requirement) {
        this.taskProgress = taskProgress;
        this.requirement = requirement;
    }
    
    /**
     * Returns the task progress this requirement progress belongs to.
     *
     * @return the task progress
     */
    @NotNull
    public PlayerTaskProgress getTaskProgress() {
        return this.taskProgress;
    }
    
    /**
     * Sets the task progress for this requirement progress.
     *
     * @param taskProgress the task progress
     */
    public void setTaskProgress(@Nullable final PlayerTaskProgress taskProgress) {
        this.taskProgress = taskProgress;
    }
    
    /**
     * Returns the requirement being progressed.
     *
     * @return the requirement
     */
    @NotNull
    public QuestTaskRequirement getRequirement() {
        return this.requirement;
    }
    
    /**
     * Sets the requirement for this progress.
     *
     * @param requirement the requirement
     */
    public void setRequirement(@NotNull final QuestTaskRequirement requirement) {
        this.requirement = requirement;
    }
    
    /**
     * Returns the current progress value (0.0 to 1.0).
     *
     * @return the progress value
     */
    public double getProgress() {
        return this.progress;
    }
    
    /**
     * Sets the progress value, automatically capping at 1.0.
     *
     * @param progress the progress value
     */
    public void setProgress(final double progress) {
        this.progress = Math.min(1.0, Math.max(0.0, progress));
    }
    
    /**
     * Checks if this requirement is completed (progress >= 1.0).
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return this.progress >= 1.0;
    }
    
    /**
     * Increments the progress by the specified amount, capping at 1.0.
     *
     * @param amount the amount to increment
     * @return the new progress value
     */
    public double incrementProgress(final double amount) {
        setProgress(this.progress + amount);
        return this.progress;
    }
    
    /**
     * Resets the progress to 0.0.
     */
    public void resetProgress() {
        this.progress = 0.0;
    }
    
    /**
     * Gets the progress as a percentage (0-100).
     *
     * @return the progress percentage
     */
    public int getProgressPercentage() {
        return (int) Math.round(this.progress * 100);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerTaskRequirementProgress that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.taskProgress != null && that.taskProgress != null &&
                this.requirement != null && that.requirement != null) {
            return this.taskProgress.equals(that.taskProgress) && this.requirement.equals(that.requirement);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.taskProgress != null && this.requirement != null) {
            return Objects.hash(this.taskProgress, this.requirement);
        }
        
        return System.identityHashCode(this);
    }
}
