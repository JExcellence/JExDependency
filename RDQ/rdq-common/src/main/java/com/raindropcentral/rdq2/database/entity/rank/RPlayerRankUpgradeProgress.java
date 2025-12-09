/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

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

    protected RPlayerRankUpgradeProgress() {}

    public RPlayerRankUpgradeProgress(@NotNull RDQPlayer player,
                                      @NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.player = Objects.requireNonNull(player);
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement);
        this.progress = 0.0;
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player);
    }

    public @NotNull RRankUpgradeRequirement getUpgradeRequirement() {
        return this.upgradeRequirement;
    }

    public void setUpgradeRequirement(@NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement);
    }

    public double getProgress() {
        return this.progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.min(progress, 1.0);
    }

    public double incrementProgress(double amount) {
        setProgress(this.progress + amount);
        return this.progress;
    }

    public boolean isCompleted() {
        return this.progress >= 1.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RPlayerRankUpgradeProgress other)) return false;
        return Objects.equals(this.player, other.player) &&
               Objects.equals(this.upgradeRequirement, other.upgradeRequirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.upgradeRequirement);
    }

    @Override
    public String toString() {
        return "RPlayerRankUpgradeProgress[player=%s, rank=%s, progress=%.2f, completed=%b]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        upgradeRequirement != null ? upgradeRequirement.getRank().getIdentifier() : "null",
                        progress, isCompleted());
    }
}*/
