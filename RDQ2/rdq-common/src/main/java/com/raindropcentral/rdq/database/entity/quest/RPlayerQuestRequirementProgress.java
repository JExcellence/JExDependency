package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Tracks the progress a player has made toward satisfying a specific quest upgrade requirement.
 * The progress value is normalized between {@code 0.0} and {@code 1.0} to simplify completion checks
 * and incremental updates.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_quest_requirement_progress")
public final class RPlayerQuestRequirementProgress extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_quest_id", nullable = false)
    private RPlayerQuest playerQuest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "upgrade_requirement_id", nullable = false)
    private RQuestUpgradeRequirement upgradeRequirement;

    @Column(name = "progress_value", nullable = false)
    private double progress;

    /**
     * Creates a new instance for JPA use.
     */
    protected RPlayerQuestRequirementProgress() {}

    /**
     * Creates a progress record for the supplied player quest and requirement pair with an initial
     * zeroed progress value.
     *
     * @param playerQuest the quest the player is attempting to complete
     * @param upgradeRequirement the requirement that must be fulfilled
     */
    public RPlayerQuestRequirementProgress(final @NotNull RPlayerQuest playerQuest,
                                           final @NotNull RQuestUpgradeRequirement upgradeRequirement) {
        this.playerQuest = Objects.requireNonNull(playerQuest, "playerQuest cannot be null");
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        this.progress = 0.0;
    }

    /**
     * Retrieves the quest associated with this progress record.
     *
     * @return the owning player quest
     */
    public @NotNull RPlayerQuest getPlayerQuest() {
        return this.playerQuest;
    }

    /**
     * Updates the quest associated with this progress record.
     *
     * @param playerQuest the quest to associate with this progress entry
     */
    public void setPlayerQuest(final @NotNull RPlayerQuest playerQuest) {
        this.playerQuest = Objects.requireNonNull(playerQuest, "playerQuest cannot be null");
    }

    /**
     * Retrieves the upgrade requirement being tracked.
     *
     * @return the requirement corresponding to this progress entry
     */
    public @NotNull RQuestUpgradeRequirement getUpgradeRequirement() {
        return this.upgradeRequirement;
    }

    /**
     * Updates the upgrade requirement linked to this progress record.
     *
     * @param upgradeRequirement the new requirement reference
     */
    public void setUpgradeRequirement(final @NotNull RQuestUpgradeRequirement upgradeRequirement) {
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
    }

    /**
     * Gets the normalized progress value.
     *
     * @return the progress between {@code 0.0} and {@code 1.0}
     */
    public double getProgress() {
        return this.progress;
    }

    /**
     * Sets the progress value while clamping it to the normalized range.
     *
     * @param progress the desired progress value
     */
    public void setProgress(final double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Increments the current progress by the specified amount, applying the standard clamping rules.
     *
     * @param amount the amount of progress to add
     */
    public void incrementProgress(final double amount) {
        setProgress(this.progress + amount);
    }

    /**
     * Resets the tracked progress back to zero.
     */
    public void resetProgress() {
        this.progress = 0.0;
    }

    /**
     * Determines whether the requirement has been fully satisfied.
     *
     * @return {@code true} when the progress is at least {@code 1.0}; {@code false} otherwise
     */
    public boolean isCompleted() {
        return this.progress >= 1.0;
    }

    /**
     * Compares this progress entry with another for equality based on the quest and requirement.
     *
     * @param obj the object to compare against
     * @return {@code true} when both entries refer to the same quest and requirement
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerQuestRequirementProgress other)) return false;
        return Objects.equals(this.playerQuest, other.playerQuest) &&
               Objects.equals(this.upgradeRequirement, other.upgradeRequirement);
    }

    /**
     * Generates a hash code consistent with {@link #equals(Object)}.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.playerQuest, this.upgradeRequirement);
    }

    /**
     * Provides a string representation of the progress entry, including the identifier and completion state.
     *
     * @return the formatted string summary
     */
    @Override
    public String toString() {
        return "RPlayerQuestRequirementProgress[id=%d, progress=%.2f, completed=%b]"
                .formatted(getId(), progress, isCompleted());
    }
}