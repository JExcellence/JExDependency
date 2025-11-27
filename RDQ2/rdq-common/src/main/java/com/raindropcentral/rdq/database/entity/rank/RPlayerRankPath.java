package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the progress a player has made through a specific rank tree.
 * This entity keeps track of the linked player, the associated rank tree, and
 * the completion metrics used to determine when a player has finished the path.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.3
 */
@Entity
@Table(name = "r_player_rank_path")
public final class RPlayerRankPath extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_tree_id", nullable = false)
    private RRankTree rankTree;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completion_percentage", nullable = false)
    private double completionPercentage = 0.0;

    /**
     * Creates an uninitialized rank path used by JPA.
     */
    protected RPlayerRankPath() {}

    /**
     * Creates a new rank path for the provided player and rank tree.
     *
     * @param player    the owning player
     * @param rankTree  the rank tree the player is progressing through
     */
    public RPlayerRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    /**
     * Creates a new rank path for the provided player and rank tree with active status.
     *
     * @param player    the owning player
     * @param rankTree  the rank tree the player is progressing through
     * @param isActive  whether this path is active
     */
    public RPlayerRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree, final boolean isActive) {
        this(player, rankTree);
        this.isActive = isActive;
    }

    /**
     * Gets the player that owns this rank path.
     *
     * @return the owning player
     */
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Updates the player reference for this rank path.
     *
     * @param player the new owning player
     */
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Gets the rank tree being tracked.
     *
     * @return the associated rank tree
     */
    public @NotNull RRankTree getRankTree() {
        return this.rankTree;
    }

    /**
     * Updates the rank tree for this path.
     *
     * @param rankTree the new rank tree
     */
    public void setRankTree(final @NotNull RRankTree rankTree) {
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    /**
     * Checks whether this rank path is currently active for the player.
     *
     * @return {@code true} if the path is active, {@code false} otherwise
     */
    public boolean isActive() {
        return this.isActive;
    }

    /**
     * Sets the active status of this rank path.
     *
     * @param isActive the new active status
     */
    public void setActive(final boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Checks if this rank path has been selected (is active) by the player.
     * This is a convenience method alias for {@link #isActive()}.
     *
     * @return {@code true} if the path is selected/active, {@code false} otherwise
     */
    public boolean hasSelectedRankPath() {
        return this.isActive;
    }

    /**
     * Determines whether the rank path is marked as completed.
     *
     * @return {@code true} if the path is complete, otherwise {@code false}
     */
    public boolean isCompleted() {
        return this.isCompleted;
    }

    /**
     * Sets the completion state of this rank path.
     * When marked as completed the completion percentage is forced to {@code 100.0}.
     *
     * @param completed {@code true} to mark the path as completed, otherwise {@code false}
     */
    public void setCompleted(final boolean completed) {
        this.isCompleted = completed;
        if (completed) {
            this.completionPercentage = 100.0;
        }
    }

    /**
     * Retrieves the percentage of the rank path that has been completed.
     *
     * @return the completion percentage in the inclusive range of {@code 0.0} to {@code 100.0}
     */
    public double getCompletionPercentage() {
        return this.completionPercentage;
    }

    /** Gets the currently active rank path for a player. */
    public @Nullable RPlayerRankPath getCurrentRankPath(final @NotNull RDQ rdq, final @NotNull RDQPlayer player) {
        try {
            final List<RPlayerRankPath> rankPaths = rdq.getPlayerRankPathRepository()
                    .findListByAttributes(Map.of("player", player));
            return rankPaths.stream()
                    .filter(RPlayerRankPath::isActive)
                    .findFirst()
                    .orElse(null);
        } catch (final Exception exception) {
            return null;
        }
    }

    /** Gets the currently selected (active) rank tree for a player. */
    public @Nullable RRankTree getSelectedRankPath(final @NotNull RDQ rdq, final @NotNull RDQPlayer player) {
        final RPlayerRankPath currentPath = getCurrentRankPath(rdq, player);
        return currentPath != null ? currentPath.getRankTree() : null;
    }

    /**
     * Updates the completion percentage.
     * The supplied value is clamped between {@code 0.0} and {@code 100.0}. Reaching the
     * maximum automatically marks the path as completed.
     *
     * @param completionPercentage the new completion percentage
     */
    public void setCompletionPercentage(final double completionPercentage) {
        this.completionPercentage = Math.max(0.0, Math.min(100.0, completionPercentage));
        if (this.completionPercentage >= 100.0) {
            this.isCompleted = true;
        }
    }

    /**
     * Increases the completion percentage by the specified amount while enforcing clamping
     * and completion rules.
     *
     * @param amount the amount to add to the current completion percentage
     */
    public void incrementCompletionPercentage(final double amount) {
        setCompletionPercentage(this.completionPercentage + amount);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerRankPath other)) return false;
        return Objects.equals(this.player, other.player) && Objects.equals(this.rankTree, other.rankTree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.rankTree);
    }

    @Override
    public String toString() {
        return "RPlayerRankPath[player=%s, tree=%s, active=%b, completed=%b, progress=%.2f%%]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        rankTree != null ? rankTree.getIdentifier() : "null",
                        isActive, isCompleted, completionPercentage);
    }
}