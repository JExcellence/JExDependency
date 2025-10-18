package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

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

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    @Column(name = "completion_percentage", nullable = false)
    private double completionPercentage = 0.0;

    protected RPlayerRankPath() {}

    public RPlayerRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RRankTree getRankTree() {
        return this.rankTree;
    }

    public void setRankTree(final @NotNull RRankTree rankTree) {
        this.rankTree = Objects.requireNonNull(rankTree, "rankTree cannot be null");
    }

    public boolean isCompleted() {
        return this.isCompleted;
    }

    public void setCompleted(final boolean completed) {
        this.isCompleted = completed;
        if (completed) {
            this.completionPercentage = 100.0;
        }
    }

    public double getCompletionPercentage() {
        return this.completionPercentage;
    }

    public void setCompletionPercentage(final double completionPercentage) {
        this.completionPercentage = Math.max(0.0, Math.min(100.0, completionPercentage));
        if (this.completionPercentage >= 100.0) {
            this.isCompleted = true;
        }
    }

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
        return "RPlayerRankPath[player=%s, tree=%s, completed=%b, progress=%.2f%%]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        rankTree != null ? rankTree.getIdentifier() : "null",
                        isCompleted, completionPercentage);
    }
}