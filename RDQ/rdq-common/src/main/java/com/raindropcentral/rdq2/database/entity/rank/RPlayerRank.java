/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private boolean active = true;

    protected RPlayerRank() {}

    public RPlayerRank(@NotNull RDQPlayer player, @NotNull RRank currentRank, @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player);
        this.currentRank = Objects.requireNonNull(currentRank);
        this.rankTree = Objects.requireNonNull(rankTree);
    }

    public RPlayerRank(@NotNull RDQPlayer player, @NotNull RRank currentRank,
                       @NotNull RRankTree rankTree, boolean active) {
        this(player, currentRank, rankTree);
        this.active = active;
    }

    public RPlayerRank(@NotNull RDQPlayer player, @NotNull RRank rank) {
        this.player = Objects.requireNonNull(player);
        this.currentRank = Objects.requireNonNull(rank);
        this.rankTree = rank.getRankTree();
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player);
    }

    public @NotNull RRank getCurrentRank() {
        return this.currentRank;
    }

    public void setCurrentRank(@NotNull RRank currentRank) {
        this.currentRank = Objects.requireNonNull(currentRank);
    }

    public @Nullable RRankTree getRankTree() {
        return this.rankTree;
    }

    public void setRankTree(@NotNull RRankTree rankTree) {
        this.rankTree = Objects.requireNonNull(rankTree);
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean belongsToTree(@NotNull RRankTree rankTree) {
        return this.rankTree != null && this.rankTree.equals(Objects.requireNonNull(rankTree));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RPlayerRank other)) return false;
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
                        rankTree != null ? rankTree.getIdentifier() : "null", active);
    }
}*/
