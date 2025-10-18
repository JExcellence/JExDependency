package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

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

    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank currentRank, final @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank cannot be null");
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank currentRank,
                       final @NotNull RRankTree rankTree, final boolean isActive) {
        this(player, currentRank, rankTree);
        this.isActive = isActive;
    }

    public RPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank rank) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.currentRank = Objects.requireNonNull(rank, "rank cannot be null");
        this.rankTree = rank.getRankTree();
    }

    public @NotNull RDQPlayer getRdqPlayer() {
        return this.player;
    }

    public void setRdqPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RRank getCurrentRank() {
        return this.currentRank;
    }

    public void setCurrentRank(final @NotNull RRank currentRank) {
        this.currentRank = Objects.requireNonNull(currentRank, "currentRank cannot be null");
    }

    public @NotNull RRankTree getRankTree() {
        return this.rankTree;
    }

    public void setRankTree(final @NotNull RRankTree rankTree) {
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(final boolean active) {
        this.isActive = active;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean belongsToRankTree(final @NotNull RRankTree rankTree) {
        Objects.requireNonNull(rankTree, "rankTree cannot be null");
        return this.rankTree != null && this.rankTree.equals(rankTree);
    }

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