package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents the persisted association between a player and a specific rank within a rank tree.
 * <p>
 * Each record tracks the player's active rank alongside the tree it belongs to, allowing business logic to
 * determine eligibility, progression, and active status while ensuring uniqueness per player/tree pair.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_rank", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "rank_tree_id"}))
public final class RPlayerRank extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_rank_id", nullable = false)
    private RRank currentRank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_tree_id")
    private RRankTree rankTree;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected RPlayerRank() {}

    /**
     * Creates a player rank entry that links the provided player, current rank, and owning rank tree, defaulting the
     * active flag to {@code true}.
     *
     * @param player      the player the rank belongs to
     * @param currentRank the player's current rank within the specified tree
     * @param rankTree    the rank tree that contains the provided rank
     */
    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank currentRank, final @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank cannot be null");
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    /**
     * Creates a player rank entry while allowing explicit control over the active flag.
     *
     * @param player      the player the rank belongs to
     * @param currentRank the player's current rank within the specified tree
     * @param rankTree    the rank tree that contains the provided rank
     * @param isActive    {@code true} if the rank should be marked active, {@code false} otherwise
     */
    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank currentRank,
                       final @NotNull RRankTree rankTree, final boolean isActive) {
        this(player, currentRank, rankTree);
        this.isActive = isActive;
    }

    /**
     * Creates a player rank entry from a player and rank, resolving the associated rank tree from the rank itself.
     *
     * @param player the player the rank belongs to
     * @param rank   the player's current rank
     */
    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank rank) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.currentRank = Objects.requireNonNull(rank, "rank cannot be null");
        this.rankTree = rank.getRankTree();
    }

    /**
     * Retrieves the player associated with this rank entry.
     *
     * @return the player owning this rank
     */
    public @NotNull RDQPlayer getRdqPlayer() {
        return this.player;
    }

    /**
     * Updates the player reference for this rank entry.
     *
     * @param player the new player to associate
     */
    public void setRdqPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Retrieves the current rank tracked for the associated player.
     *
     * @return the player's current rank
     */
    public @NotNull RRank getCurrentRank() {
        return this.currentRank;
    }

    /**
     * Updates the current rank tracked for the associated player.
     *
     * @param currentRank the new rank to assign
     */
    public void setCurrentRank(final @NotNull RRank currentRank) {
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank cannot be null");
    }

    /**
     * Retrieves the rank tree connected to the current rank.
     *
     * @return the rank tree that owns the current rank
     */
    public @NotNull RRankTree getRankTree() {
        return this.rankTree;
    }

    /**
     * Updates the rank tree associated with this player rank.
     *
     * @param rankTree the new rank tree reference
     */
    public void setRankTree(final @NotNull RRankTree rankTree) {
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    /**
     * Indicates whether this rank entry is currently active for the associated player.
     *
     * @return {@code true} if the rank entry is active, {@code false} otherwise
     */
    public boolean isActive() {
        return this.isActive;
    }

    /**
     * Sets the active flag for this rank entry.
     *
     * @param active {@code true} to mark active, {@code false} to mark inactive
     */
    public void setActive(final boolean active) {
        this.isActive = active;
    }

    /**
     * Marks this rank entry as active for the associated player.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Marks this rank entry as inactive for the associated player.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Checks whether this rank entry belongs to the specified rank tree instance.
     *
     * @param rankTree the rank tree to compare against
     * @return {@code true} if the rank tree matches the stored reference, {@code false} otherwise
     */
    public boolean belongsToRankTree(final @NotNull RRankTree rankTree) {
        Objects.requireNonNull(rankTree, "rankTree cannot be null");
        return this.rankTree != null && this.rankTree.equals(rankTree);
    }

    /**
     * Checks whether this rank entry belongs to a rank tree by matching its identifier.
     *
     * @param rankTreeIdentifier the identifier to compare against the stored rank tree
     * @return {@code true} if the identifier matches the stored rank tree, {@code false} otherwise
     */
    public boolean belongsToRankTree(final @NotNull String rankTreeIdentifier) {
        Objects.requireNonNull(rankTreeIdentifier, "rankTreeIdentifier cannot be null");
        return this.rankTree != null && this.rankTree.getIdentifier() != null &&
               this.rankTree.getIdentifier().equals(rankTreeIdentifier);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerRank other)) return false;
        return Objects.equals(this.player, other.player) && Objects.equals(this.rankTree, other.rankTree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.rankTree);
    }

    @Override
    public String toString() {
        return "RPlayerRank[player=%s, rank=%s, tree=%s, active=%b]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        currentRank != null ? currentRank.getIdentifier() : "null",
                        rankTree != null ? rankTree.getIdentifier() : "null", isActive);
    }
}