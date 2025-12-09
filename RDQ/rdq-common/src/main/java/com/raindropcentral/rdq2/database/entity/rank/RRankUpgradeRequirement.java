/*
package com.raindropcentral.rdq2.database.entity.rank;

import com.raindropcentral.rdq2.config.item.IconSection;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_rank_upgrade_requirement")
public final class RRankUpgradeRequirement extends RequirementAssociation {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_id", nullable = false)
    private RRank rank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private RRequirement requirement;

    protected RRankUpgradeRequirement() {}

    public RRankUpgradeRequirement(@Nullable RRank rank, @NotNull RRequirement requirement,
                                   @NotNull IconSection icon) {
        this.rank = rank;
        this.requirement = Objects.requireNonNull(requirement);
        this.setIcon(Objects.requireNonNull(icon));
        if (rank != null) {
            rank.addUpgradeRequirement(this);
        }
    }

    public @Nullable RRank getRank() {
        return this.rank;
    }

    public void setRank(@Nullable RRank rank) {
        if (this.rank != null && this.rank != rank) {
            this.rank.getUpgradeRequirements();
        }
        this.rank = rank;
        if (rank != null) {
            rank.addUpgradeRequirement(this);
        }
    }

    @Override
    public @NotNull RRequirement getRequirement() {
        return this.requirement;
    }

    @Override
    public void setRequirement(@NotNull RRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement);
    }

    public boolean isMet(@NotNull Player player) {
        return this.requirement.isMet(player);
    }

    public double calculateProgress(@NotNull Player player) {
        return this.requirement.calculateProgress(player);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RRankUpgradeRequirement other)) return false;
        if (getId() != null && other.getId() != null) {
            return getId().equals(other.getId());
        }
        return Objects.equals(this.requirement, other.requirement) &&
                Objects.equals(this.rank, other.rank) &&
                this.getDisplayOrder() == other.getDisplayOrder();
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return Objects.hash(this.requirement, this.rank, this.getDisplayOrder());
    }

    @Override
    public String toString() {
        return "RRankUpgradeRequirement[id=%d, rank=%s, displayOrder=%d]"
                .formatted(getId(), rank != null ? rank.getIdentifier() : "null", getDisplayOrder());
    }
}*/
