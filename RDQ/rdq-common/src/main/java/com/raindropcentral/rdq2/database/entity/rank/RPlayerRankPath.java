/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
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

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completion_percentage", nullable = false)
    private double completionPercentage = 0.0;

    protected RPlayerRankPath() {}

    public RPlayerRankPath(@NotNull RDQPlayer player, @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player);
        this.rankTree = Objects.requireNonNull(rankTree);
    }

    public RPlayerRankPath(@NotNull RDQPlayer player, @NotNull RRankTree rankTree, boolean active) {
        this(player, rankTree);
        this.active = active;
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player);
    }

    public @NotNull RRankTree getRankTree() {
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

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.completionPercentage = 100.0;
        }
    }

    public double getCompletionPercentage() {
        return this.completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = Math.max(0.0, Math.min(100.0, completionPercentage));
        if (this.completionPercentage >= 100.0) {
            this.completed = true;
        }
    }

    public void incrementCompletionPercentage(double amount) {
        setCompletionPercentage(this.completionPercentage + amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RPlayerRankPath other)) return false;
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
                        active, completed, completionPercentage);
    }
}*/
