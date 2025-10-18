package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
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

    public RPlayerRankUpgradeProgress(final @NotNull RDQPlayer player,
                                      final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        this.progress = 0.0;
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RRankUpgradeRequirement getUpgradeRequirement() {
        return this.upgradeRequirement;
    }

    public void setUpgradeRequirement(final @NotNull RRankUpgradeRequirement upgradeRequirement) {
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
    }

    public double getProgress() {
        return this.progress;
    }

    public void setProgress(final double progress) {
        this.progress = Math.min(progress, 1.0);
    }

    public double incrementProgress(final double amount) {
        setProgress(this.progress + amount);
        return this.progress;
    }

    public void resetProgress() {
        this.progress = 0.0;
    }

    public boolean isCompleted() {
        return this.progress >= 1.0;
    }

    public @NotNull RRank getTargetRank() {
        return this.upgradeRequirement.getRank();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerRankUpgradeProgress other)) return false;
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
}