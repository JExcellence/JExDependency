package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents an individual player's progress toward completing a specific rank upgrade requirement.
 * <p>
 * The progress value is normalized between {@code 0.0} and {@code 1.0} where {@code 1.0} marks completion. This
 * entity links a {@link RDQPlayer} to a {@link RRankUpgradeRequirement} and offers helper methods to manage the
 * progression lifecycle.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_rank_upgrade_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "upgrade_requirement_id"}))
public final class RPlayerRankUpgradeProgress extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "upgrade_requirement_id", nullable = false)
    private RRankUpgradeRequirement upgradeRequirement;

    @Column(name = "progress_value", nullable = false)
    private double progress;

    /**
     * Creates an empty progress record for the persistence provider.
     */
    protected RPlayerRankUpgradeProgress() {}

    /**
     * Creates a new progress record for the supplied player and upgrade requirement with the progress initialized to
     * {@code 0.0}.
     *
     * @param player the player whose rank upgrade progress is being tracked
     * @param upgradeRequirement the requirement the player must complete to advance to the target rank
     */
    public RPlayerRankUpgradeProgress(final @NotNull RDQPlayer player,
                                      final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        this.progress = 0.0;
    }

    /**
     * Retrieves the player whose rank progression is being tracked.
     *
     * @return the associated player
     */
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Updates the player reference for this progress record.
     *
     * @param player the new player reference, must not be {@code null}
     */
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Retrieves the rank upgrade requirement associated with this progress record.
     *
     * @return the relevant rank upgrade requirement
     */
    public @NotNull RRankUpgradeRequirement getUpgradeRequirement() {
        return this.upgradeRequirement;
    }

    /**
     * Updates the rank upgrade requirement this progress record references.
     *
     * @param upgradeRequirement the new requirement to track, must not be {@code null}
     */
    public void setUpgradeRequirement(final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
    }

    /**
     * Provides the normalized completion state for the upgrade requirement.
     *
     * @return the current progress value between {@code 0.0} and {@code 1.0}
     */
    public double getProgress() {
        return this.progress;
    }

    /**
     * Sets the current progress, capping the value at {@code 1.0} to denote completion.
     *
     * @param progress the new progress value, expected to be non-negative
     */
    public void setProgress(final double progress) {
        this.progress = Math.min(progress, 1.0);
    }

    /**
     * Increases the current progress by the supplied amount while ensuring the value remains capped at {@code 1.0}.
     *
     * @param amount the amount to add to the current progress
     * @return the updated progress value after the increment
     */
    public double incrementProgress(final double amount) {
        setProgress(this.progress + amount);
        return this.progress;
    }

    /**
     * Resets the progress to {@code 0.0}, clearing any accumulated advancement.
     */
    public void resetProgress() {
        this.progress = 0.0;
    }

    /**
     * Indicates whether the upgrade requirement has been fully completed.
     *
     * @return {@code true} if the progress is at least {@code 1.0}; otherwise {@code false}
     */
    public boolean isCompleted() {
        return this.progress >= 1.0;
    }

    /**
     * Resolves the rank the player will reach upon completing the requirement.
     *
     * @return the target rank tied to the tracked upgrade requirement
     */
    public @NotNull RRank getTargetRank() {
        return this.upgradeRequirement.getRank();
    }

    /**
     * Determines equality by comparing both the tracked player and the associated upgrade requirement.
     *
     * @param obj the object to compare with this progress record
     * @return {@code true} if both instances track the same player and requirement; otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerRankUpgradeProgress other)) return false;
        return Objects.equals(this.player, other.player) &&
               Objects.equals(this.upgradeRequirement, other.upgradeRequirement);
    }

    /**
     * Computes a hash code derived from the tracked player and requirement references.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.upgradeRequirement);
    }

    /**
     * Produces a human-readable representation of the progress including the player name, rank identifier, progress value,
     * and completion state.
     *
     * @return a formatted string summarizing the progress state
     */
    @Override
    public String toString() {
        return "RPlayerRankUpgradeProgress[player=%s, rank=%s, progress=%.2f, completed=%b]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        upgradeRequirement != null ? upgradeRequirement.getRank().getIdentifier() : "null",
                        progress, isCompleted());
    }
}