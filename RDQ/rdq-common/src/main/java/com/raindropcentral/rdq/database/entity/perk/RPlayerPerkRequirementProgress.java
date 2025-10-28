package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tracks a player's progress toward meeting a specific perk requirement.
 * Used to monitor completion status and provide feedback on requirement fulfillment.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_perk_requirement_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_id", "requirement_id"})
})
public final class RPlayerPerkRequirementProgress extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private RPerkRequirement requirement;

    @Column(name = "progress_value", nullable = false)
    private double progressValue = 0.0;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Framework-required constructor for JPA.
     */
    protected RPlayerPerkRequirementProgress() {}

    /**
     * Creates a new player perk requirement progress tracker.
     *
     * @param player the player tracking progress
     * @param requirement the requirement being tracked
     */
    public RPlayerPerkRequirementProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RPerkRequirement requirement
    ) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.startedAt = LocalDateTime.now();
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RPerkRequirement getRequirement() {
        return this.requirement;
    }

    public void setRequirement(final @NotNull RPerkRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    public double getProgressValue() {
        return this.progressValue;
    }

    public void setProgressValue(final double progressValue) {
        this.progressValue = Math.max(0.0, Math.min(1.0, progressValue));
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(final boolean completed) {
        this.completed = completed;
        if (completed && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public @Nullable LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    public @NotNull LocalDateTime getStartedAt() {
        return this.startedAt;
    }

    public @Nullable String getMetadata() {
        return this.metadata;
    }

    public void setMetadata(final @Nullable String metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the completion percentage (0-100).
     *
     * @return the completion percentage
     */
    public int getCompletionPercentage() {
        return (int) (this.progressValue * 100);
    }

    /**
     * Increments progress by the specified amount.
     * Automatically marks as completed when progress reaches 100%.
     *
     * @param amount the amount to increment (0.0 to 1.0)
     */
    public void incrementProgress(final double amount) {
        this.progressValue = Math.min(1.0, this.progressValue + Math.max(0.0, amount));
        if (this.progressValue >= 1.0) {
            this.setCompleted(true);
        }
    }

    /**
     * Resets progress to zero and clears completion status.
     */
    public void resetProgress() {
        this.progressValue = 0.0;
        this.completed = false;
        this.completedAt = null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerPerkRequirementProgress other)) return false;
        return Objects.equals(this.player, other.player) && 
               Objects.equals(this.requirement, other.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.requirement);
    }

    @Override
    public String toString() {
        return "RPlayerPerkRequirementProgress[player=%s, requirement=%s, progress=%d%%, completed=%b]"
            .formatted(
                player != null ? player.getPlayerName() : "null",
                requirement != null ? requirement.getRequirementType() : "null",
                this.getCompletionPercentage(),
                completed
            );
    }
}